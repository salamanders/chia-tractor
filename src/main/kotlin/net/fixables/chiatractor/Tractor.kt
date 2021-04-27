package net.fixables.chiatractor

import info.benjaminhill.utils.printlnt
import info.benjaminhill.utils.printlntOnce
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.nio.file.FileSystems
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime


fun main() {
    println("Starting up the Chia Tractor. Reading plots, then monitoring.")

    val allPlotLogs = PlotLog.loadLogs()
    val completedLogs = allPlotLogs.filterIsInstance<CompletedPlotLog>()
    println("Found ${allPlotLogs.size} total logs, ${completedLogs.size} completed logs.")
    completionTimes(completedLogs)
    parallelRate(completedLogs)
    // dumpAllLogs(allPlotLogs)

    printlnt("TimeMS", "Store", "type", "total", "used", "avail")

    runBlocking {
        while (true) {
            logFileStoreSpace()
            delay(Duration.minutes(5))
        }
    }
}

private fun logFileStoreSpace() {
    val now = System.currentTimeMillis()

    PlotLog.loadLogs()
        .filterNot { it is CompletedPlotLog }
        .forEach { activePlot ->
            when {
                activePlot.p4Duration != null -> printlntOnce(now, activePlot.id, activePlot.tempDir2, "p4done")
                activePlot.p3Duration != null -> printlntOnce(now, activePlot.id, activePlot.tempDir2, "p3done")
                activePlot.p2Duration != null -> printlntOnce(now, activePlot.id, activePlot.tempDir1, "p2done")
                activePlot.p1Duration != null -> printlntOnce(now, activePlot.id, activePlot.tempDir1, "p1done")
            }
        }

    FileSystems.getDefault().fileStores.forEach { store ->
        try {
            val totalGB = store.totalSpace / BYTES_GB
            if (totalGB > 50) {
                val used = (store.totalSpace - store.unallocatedSpace) / BYTES_GB
                val avail = store.usableSpace / BYTES_GB
                printlnt(now, store, store.type(), totalGB, used, avail)
            }
        } catch (e: IOException) {
            // skipping
        }
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



