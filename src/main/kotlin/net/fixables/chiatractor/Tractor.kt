package net.fixables.chiatractor

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.FileStore
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.time.Duration
import kotlin.time.DurationUnit


fun main(vararg args: String) {
    when {
        args.contains("-l") -> {
            println("Calculating layouts.")
            // TODO: get this from a log
            val timeToSpace: IntArray =
                loadSamples(File("single_plot_gb.tsv")).also { println("Read ${it.size} time samples.") }
            println("Samples: ${timeToSpace.size}")
            val delayMinutes = packThemIn(
                timeToSpaceSamples = timeToSpace,
                availableSpaceGb = 650, // TODO: get this from the system
            )
            println("Try plotting with a delay of $delayMinutes minutes.")
        }
        args.contains("-m") -> {
            logFileStoreSpace() // hide the past events
            runBlocking {
                while (true) {
                    logFileStoreSpace()
                    delay(Duration.minutes(0.9).toLong(DurationUnit.MILLISECONDS))
                }
            }
        }
        args.contains("-r") -> {
            completionTimes(completedLogs)
            parallelRate(completedLogs)
        }
        args.contains("-b") -> {
            val benchDir = File("./temp").canonicalFile.also { it.mkdirs() }
            println("Time to write writeP1:${benchmarkDir(dirToBenchmark = benchDir, parallel = 1)}")
            println("Time to write writeP2:${benchmarkDir(dirToBenchmark = benchDir, parallel = 2)}")
            println("Time to write writeP5:${benchmarkDir(dirToBenchmark = benchDir, parallel = 5)}")
            println("Time to write writeP10:${benchmarkDir(dirToBenchmark = benchDir, parallel = 10)}")
        }
        else -> {
            println("All output TSV-friendly. \nOptions:\n -l=calculate layouts\n -m=monitor free GB\n -r=read all plot logs")
        }
    }
}

// Big enough for a temp file or a k32
private val bigFileStores: Set<FileStore> by lazy {
    println("Loading all file stores that could be used.")
    FileSystems.getDefault().fileStores.filter {
        try {
            it.totalSpace / BYTES_GB > 100
        } catch (e: Exception) {
            false
        }
    }.toSet().also { println("Found ${it.size} big file stores.") }
}


private val allPlotLogs: List<PlotLog> by lazy { PlotLog.loadLogs().also { println("Found ${it.size} plot logs.") } }
private val completedLogs: List<CompletedPlotLog> by lazy {
    allPlotLogs.filterIsInstance<CompletedPlotLog>().also { println("Found ${it.size} completed plot logs.") }
}

// Recently used big locations
internal val activeFileStores: List<FileStore> by lazy {
    allPlotLogs.asSequence().map {
        listOf(it.tempDir1, it.tempDir2)
    }.flatten().filterNotNull().toSet()
        .map { Files.getFileStore(Paths.get(it)) }
        .filter { bigFileStores.contains(it) }.toList().also { println("Found ${it.size} active file stores.") }
}


internal const val BYTES_KB = 1000L
internal const val BYTES_MB = BYTES_KB * BYTES_KB
internal const val BYTES_GB = BYTES_MB * BYTES_KB


