package net.fixables.chiatractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.atomic.LongAdder
import kotlin.time.Duration
import kotlin.time.measureTime

fun benchmarkDir(dirToBenchmark: File, parallel: Int = 1, gb: Int = 20): Duration = runBlocking {
    require(dirToBenchmark.isDirectory)
    require(dirToBenchmark.canWrite())

    val fullBlock = ByteArray(BYTES_GB.toInt())
    FasterRandom.nextBytes(fullBlock)

    // require(benchmarkFile.usableSpace > (BYTES_GB * 10)) { "Not enough free space: ${benchmarkFile.usableSpace}GB"}
    val totalWritten = LongAdder()

    // Don't sum up the writes, just measure wall time.
    measureTime {
        List(parallel) { p ->
            async(Dispatchers.IO) {
                val benchmarkFile = File(dirToBenchmark, "benchmark_test_$p.data")
                benchmarkFile.deleteOnExit()
                benchmarkFile.outputStream().use { fos ->
                    // println("Writing ${gb/parallel}GB to benchmark_test_${parallel}_$p.data")
                    repeat(gb / parallel) {
                        fos.write(fullBlock)
                        totalWritten.add(fullBlock.size.toLong())
                    }
                    fos.flush()
                    fos.fd.sync()
                }
                // benchmarkFile.delete()
            }
        }.awaitAll().also {
            check(totalWritten.sum() == gb * BYTES_GB) {
                "Total written ${totalWritten.sum()} != ${gb * BYTES_GB}"
            }
        }
    }
}


fun Iterable<Duration>.sum(): Duration {
    var sum: Duration = Duration.ZERO
    for (element in this) {
        sum += element
    }
    return sum
}
