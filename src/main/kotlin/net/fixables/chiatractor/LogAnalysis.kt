package net.fixables.chiatractor

import info.benjaminhill.utils.printlnt
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime


/**
 * A few days so we get some sense of parallel per day
 */
@OptIn(ExperimentalTime::class)
internal fun parallelRate(plotLogs: Collection<CompletedPlotLog>, numberOfDays: Int = 3) {
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
internal fun completionTimes(plotLogs: Collection<CompletedPlotLog>) {
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