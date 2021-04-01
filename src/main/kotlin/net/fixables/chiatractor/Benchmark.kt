package net.fixables.chiatractor

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

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

private val headerWritePrinted = AtomicBoolean(false)

@OptIn(ExperimentalTime::class)
private fun benchmark(
    path: Path,
    totalDataToWriteGB: Int = 1,
    sharding: Int,
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
    benchmarkWrite(
        path = path,
        bytesPerShard = bytesPerShard,
        sharding = sharding,
    )
}

@OptIn(ExperimentalTime::class)
private fun benchmarkWrite(
    path: Path,
    bytesPerShard: Long,
    sharding: Int,
) = runBlocking {
    val allFileWriters = (0 until sharding).map { shardNum ->
        createWriterAsync(
            targetSizeBytes = bytesPerShard,
            tempPath = path.resolve("${sharding}_${shardNum}_${bytesPerShard}.temp"),
        )
    }

    val totalWriteTimeCPU: Duration
    val totalWriteTimeWall = measureTime {
        totalWriteTimeCPU = allFileWriters.awaitAll().sum()
    }


    if (!headerWritePrinted.get()) {
        printtsv("path", "sharding", "Write CPU Time (s)", "Write Wall Time (s)")
        headerWritePrinted.set(true)
    }
    printtsv(
        path,
        sharding,
        totalWriteTimeCPU.inSeconds,
        totalWriteTimeWall.inSeconds,
    )
}


@ExperimentalTime
private fun CoroutineScope.createWriterAsync(
    targetSizeBytes: Long,
    tempPath: Path,
) = async(context = IO, start = CoroutineStart.LAZY) {
    val totalDuration = tempPath.fillWithRandomData(targetSizeBytes)
    tempPath.toFile().deleteOnExit() // In case next line fails
    if (Files.size(tempPath) != targetSizeBytes) {
        println("WARNING: Final size didn't match: file=${Files.size(tempPath)}, target=$targetSizeBytes")
    }
    Files.delete(tempPath)
    totalDuration
}

@OptIn(ExperimentalTime::class)
private fun benchmarkSeekAndWrite(testDir: Path, trials: Int = 1_000): List<Duration> {
    val largeFile = testDir.resolve("seek.temp")
    largeFile.fillWithRandomData(100 * BYTES_MB)
    largeFile.toFile().deleteOnExit()
    val seekDurations = mutableListOf<Duration>()

    RandomAccessFile(largeFile.toFile(), "rw").use { rAccFile ->
        val fileSize = rAccFile.length()
        val block = ByteArray(BYTES_KB.toInt())

        repeat(trials) {
            val targetLocation = Random.nextLong(fileSize - block.size)
            FasterRandom.nextBytes(block)
            seekDurations += measureTime {
                rAccFile.seek(targetLocation)
                rAccFile.write(block)
            }
        }
    }

    Files.delete(largeFile)
    return seekDurations
}

@OptIn(ExperimentalTime::class)
private fun Path.fillWithRandomData(size: Long, writeBlockSize: Long = BYTES_MB): Duration {
    val fullBlock = ByteArray(writeBlockSize.toInt())
    var remainingBytes = size
    var totalDuration: Duration = Duration.ZERO

    while (remainingBytes > 0) {
        val currentBlock = if (remainingBytes >= writeBlockSize) {
            fullBlock
        } else {
            ByteArray(remainingBytes.toInt())
        }
        FasterRandom.nextBytes(currentBlock)
        totalDuration += measureTime {
            Files.write(
                this,
                currentBlock,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            )
        }
        remainingBytes -= currentBlock.size
    }
    return totalDuration
}


@OptIn(ExperimentalTime::class)
private fun Collection<Duration>.sum() = this.fold(Duration.ZERO, Duration::plus)




