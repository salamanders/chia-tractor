package net.fixables.chiatractor

import info.benjaminhill.utils.printlnt
import info.benjaminhill.utils.printlntOnce
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.nio.file.FileStore
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime



// Recently used big locations
private val activeFileStores: MutableSet<FileStore> = mutableSetOf()

fun main() {
    println("Starting up the Chia Tractor. Reading plots, then monitoring.  Ok to >> this to a tsv file.")

    val allPlotLogs = PlotLog.loadLogs()
    val completedLogs = allPlotLogs.filterIsInstance<CompletedPlotLog>()
    println("Found ${allPlotLogs.size} total logs, ${completedLogs.size} completed logs.")
    completionTimes(completedLogs)
    parallelRate(completedLogs)

    // Big enough for a temp file or a k32
    val bigFileStores: Set<FileStore> = FileSystems.getDefault().fileStores.filter {
        try {
            it.totalSpace / BYTES_GB > 100
        } catch (e:Exception) {
            false
        }
    }.toSet()

    allPlotLogs.forEach { plotLog ->
        try {
            activeFileStores.addAll(
                listOfNotNull(plotLog.tempDir1, plotLog.tempDir2)
                    .map { Files.getFileStore(Paths.get(it)) }
                    .filter { bigFileStores.contains(it) }
            )
        } catch (e: IOException) {
            // ignore
        }
    }
    println("Found ${bigFileStores.size} big stores, of which ${activeFileStores.size} are active: ${activeFileStores.joinToString()}")
    check(activeFileStores.isNotEmpty()) { "Must have at least one active file store to watch." }

    println()
    printlnt("time_ms", "event_type", "Store", "used", "avail")
    runBlocking {
        while (true) {
            logFileStoreSpace()
            delay(Duration.minutes(1))
        }
    }
}

private fun logFileStoreSpace() {
    val now = System.currentTimeMillis()

    PlotLog.loadLogs()
        .forEach { plot ->
            when {
                plot.p4Duration != null -> printlntOnce(now, "phase", plot.id, plot.tempDir2, "p4done")
                plot.p3Duration != null -> printlntOnce(now, "phase", plot.id, plot.tempDir2, "p3done")
                plot.p2Duration != null -> printlntOnce(now, "phase", plot.id, plot.tempDir1, "p2done")
                plot.p1Duration != null -> printlntOnce(now, "phase", plot.id, plot.tempDir1, "p1done")
            }
        }

    FileSystems.getDefault().fileStores
        .filter { activeFileStores.contains(it) }
        .forEach { store ->
            val used = (store.totalSpace - store.unallocatedSpace) / BYTES_GB
            val avail = store.usableSpace / BYTES_GB
            printlnt(now, "space", store, used, avail)
        }
}


/**
 * A few days so we get some sense of parallel per day
 */
@OptIn(ExperimentalTime::class)
private fun parallelRate(plotLogs: Collection<CompletedPlotLog>, numberOfDays: Int = 3) {
    println()
    printlnt("Temp Dir", "plots/day over last $numberOfDays days")
    val fewDaysAgoMs = System.currentTimeMillis() - Duration.days(numberOfDays).toDouble(DurationUnit.MILLISECONDS)
    plotLogs.filter { it.lastModified > fewDaysAgoMs }
        .groupBy { it.tempDir1 }
        .toSortedMap()
        .forEach { (tmpDir1, plots) ->
            printlnt(tmpDir1, plots.size / numberOfDays.toDouble())
        }
}

/**
 * Generic timing per plot
 */
@OptIn(ExperimentalTime::class)
private fun completionTimes(plotLogs: Collection<CompletedPlotLog>) {
    println()
    printlnt("Temp Dir", "Total Completed", "Average (h)", "Most Recent (h)")
    plotLogs
        .groupBy { it.tempDir1 }
        .toSortedMap()
        .forEach { (tmpDir1, plots) ->
            val avg = plots.map { it.totalDuration.toDouble(DurationUnit.HOURS) }.average()
            val mostRecent = plots.maxByOrNull { it.lastModified }!!.totalDuration.toDouble(DurationUnit.HOURS)
            printlnt(tmpDir1, plots.size, avg, mostRecent)
        }
}

internal const val BYTES_KB = 1024L
internal const val BYTES_MB = BYTES_KB * BYTES_KB
internal const val BYTES_GB = BYTES_MB * BYTES_KB



