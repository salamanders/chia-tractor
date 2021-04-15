package net.fixables.chiatractor

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.nio.file.FileSystems
import kotlin.time.ExperimentalTime
import kotlin.time.days
import kotlin.time.minutes


fun main() {
    println("Starting up the Chia Tractor. Reading plots, then monitoring.")

    val allPlotLogs = PlotLog.loadLogs()
    val completedLogs = allPlotLogs.filterIsInstance<CompletedPlotLog>()
    println("Found ${allPlotLogs.size} total logs, ${completedLogs.size} completed logs.")
    completionTimes(completedLogs)
    parallelRate(completedLogs)
    // dumpAllLogs(allPlotLogs)

    TSV.println("TimeMS", "Store", "type", "total", "used", "avail")

    runBlocking {
        while (true) {
            logFileStoreSpace()
            delay(5.minutes)
        }
    }
}

private fun logFileStoreSpace() {
    val now = System.currentTimeMillis()

    PlotLog.loadLogs()
        .filterNot { it is CompletedPlotLog }
        .forEach { activePlot ->
            when {
                activePlot.p4Duration != null -> TSV.once(now, activePlot.id, activePlot.tempDir2, "p4done")
                activePlot.p3Duration != null -> TSV.once(now, activePlot.id, activePlot.tempDir2, "p3done")
                activePlot.p2Duration != null -> TSV.once(now, activePlot.id, activePlot.tempDir1, "p2done")
                activePlot.p1Duration != null -> TSV.once(now, activePlot.id, activePlot.tempDir1, "p1done")
            }
        }

    FileSystems.getDefault().fileStores.forEach { store ->
        try {
            val totalGB = store.totalSpace / BYTES_GB
            if (totalGB > 50) {
                val used = (store.totalSpace - store.unallocatedSpace) / BYTES_GB
                val avail = store.usableSpace / BYTES_GB
                TSV.println(now, store, store.type(), totalGB, used, avail)
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
    TSV.println("Temp Dir", "plots/day over last $numberOfDays days")
    val fewDaysAgoMs = System.currentTimeMillis() - numberOfDays.days.inMilliseconds
    plotLogs.filter { it.lastModified > fewDaysAgoMs }
        .groupBy { it.tempDir1 }
        .toSortedMap()
        .forEach { (tmpDir1, plots) ->
            TSV.println(tmpDir1, plots.size / numberOfDays.toDouble())
        }
}

/**
 * Generic timing per plot
 */
@OptIn(ExperimentalTime::class)
private fun completionTimes(plotLogs: Collection<CompletedPlotLog>) {
    println()
    TSV.println("Temp Dir", "Total Completed", "Average (h)", "Most Recent (h)")
    plotLogs
        .groupBy { it.tempDir1 }
        .toSortedMap()
        .forEach { (tmpDir1, plots) ->
            val avg = plots.map { it.totalDuration.inHours }.average()
            val mostRecent = plots.maxByOrNull { it.lastModified }!!.totalDuration.inHours
            TSV.println(tmpDir1, plots.size, avg, mostRecent)
        }
}

internal const val BYTES_KB = 1024L
internal const val BYTES_MB = BYTES_KB * BYTES_KB
internal const val BYTES_GB = BYTES_MB * BYTES_KB



