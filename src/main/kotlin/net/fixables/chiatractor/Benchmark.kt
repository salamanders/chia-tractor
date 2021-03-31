package net.fixables.chiatractor

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.milliseconds


private const val BYTES_KB = 1024L
private const val BYTES_MB = BYTES_KB * 1024L
private const val BYTES_GB = BYTES_MB * 1024L

fun benchmarkGrid(path: Path) {
    (1..16).shuffled().forEach { sharding ->
        benchmark(
            path = path,
            sharding = sharding,
        )
    }
}

private val headerPrinted = AtomicBoolean(false)

@OptIn(ExperimentalTime::class)
private fun benchmark(
    path: Path,
    totalDataToWriteGB: Int = 1,
    sharding: Int,
    writeBlockSize: Long = 10 * BYTES_MB
) {
    require(Files.isDirectory(path)) { "Must be a directory: $path" }
    require(Files.isWritable(path)) { "Must be writable: $path" }
    require(sharding in 1..100) { "Sharding out of range (1..100)" }
    require(totalDataToWriteGB > 0) { "totalDataToWriteGB out of range >0" }
    val totalDataToWriteBytes = totalDataToWriteGB * BYTES_GB
    val bytesPerShard = totalDataToWriteBytes / sharding
    val fileStore = Files.getFileStore(path.toRealPath())
    val usableSpace = fileStore.usableSpace
    println("FileStore: ${fileStore.name()}, Usable space: ${(usableSpace / BYTES_GB).toInt()}GB")
    check(usableSpace > totalDataToWriteBytes) { "Not enough free space." }
    benchmarkValidated(
        path = path,
        bytesPerShard = bytesPerShard,
        sharding = sharding,
        writeBlockSize = writeBlockSize
    )
}

@OptIn(ExperimentalTime::class)
private fun benchmarkValidated(
    path: Path,
    bytesPerShard: Long,
    sharding: Int,
    writeBlockSize: Long
) {
    runBlocking {
        val allFileWriters = (0 until sharding).map { shardNum ->
            createWriterAsync(
                targetSizeBytes = bytesPerShard,
                tempPath = path.resolve("${sharding}_${shardNum}_${bytesPerShard}_${writeBlockSize}.temp"),
                writeBlockSize = writeBlockSize,
            )
        }

        check(allFileWriters.size == sharding) { "Error, wrong number of shards" }

        val totalTime: Duration
        val wallDuration = measureTime {
            totalTime = allFileWriters.awaitAll().sumBy { it.inMilliseconds.toInt() }.milliseconds
        }

        if (!headerPrinted.get()) {
            printtsv("path", "sharding", "writeBlockSize", "CPU Time (s)", "Wall Time (s)")
            headerPrinted.set(true)
        }
        printtsv(
            path,
            sharding,
            writeBlockSize,
            totalTime.inSeconds,
            wallDuration.inSeconds,
        )
    }
}

@ExperimentalTime
private fun CoroutineScope.createWriterAsync(
    targetSizeBytes: Long,
    tempPath: Path,
    writeBlockSize: Long,
) = async(context = IO, start = CoroutineStart.LAZY) {
    var remainingBytes = targetSizeBytes
    var totalDuration: Duration = Duration.ZERO
    var bytes = ByteArray(writeBlockSize.toInt())
    while (remainingBytes > 0) {
        if (remainingBytes < writeBlockSize) {
            // Should only happen once at the end
            bytes = ByteArray(remainingBytes.toInt())
        }
        Random.nextBytes(bytes)
        totalDuration += measureTime {
            Files.write(
                tempPath,
                bytes,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            )
        }
        remainingBytes -= bytes.size
    }
    tempPath.toFile().deleteOnExit() // In case next line fails
    if (Files.size(tempPath) != targetSizeBytes) {
        println("WARNING: Final size didn't match: file=${Files.size(tempPath)}, target=$targetSizeBytes")
    }
    Files.delete(tempPath)
    totalDuration
}






