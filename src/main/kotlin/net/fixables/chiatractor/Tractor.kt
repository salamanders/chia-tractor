package net.fixables.chiatractor

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.time.ExperimentalTime
import kotlin.time.days

fun main() {
    println("Starting up the Chia Tractor")
    val allPlotLogs = PlotLog.loadLogs()
    val completedLogs = allPlotLogs.filterIsInstance<CompletedPlotLog>()

    println("Found ${allPlotLogs.size} total logs, ${completedLogs.size} completed logs.")
    completionTimes(completedLogs)
    parallelRate(completedLogs)
    dumpAllLogs(allPlotLogs)
}

fun dumpAllLogs(allPlotLogs: List<PlotLog>) {
    println()
    printtsv(PlotLog.asListHeaders().toTypedArray())
    allPlotLogs.forEach {
        printtsv(*it.asList().toTypedArray())
    }
}

/**
 * A few days so we get some sense of parallel per day
 */
@OptIn(ExperimentalTime::class)
fun parallelRate(plotLogs: Collection<CompletedPlotLog>, numberOfDays: Int = 3) {
    println()
    printtsv("Temp Dir", "plots/day over last $numberOfDays days")
    val fewDaysAgoMs = System.currentTimeMillis() - numberOfDays.days.inMilliseconds
    plotLogs.filter { it.lastModified > fewDaysAgoMs }
        .groupBy { it.tempDir1 }
        .toSortedMap()
        .forEach { (tmpDir1, plots) ->
            printtsv(tmpDir1, plots.size / numberOfDays.toDouble())
        }
}

/**
 * Generic timing per plot
 */
@OptIn(ExperimentalTime::class)
fun completionTimes(plotLogs: Collection<CompletedPlotLog>) {
    println()
    printtsv("Temp Dir", "Total Completed", "Average (h)", "Most Recent (h)")
    plotLogs
        .groupBy { it.tempDir1 }
        .toSortedMap()
        .forEach { (tmpDir1, plots) ->
            val avg = plots.map { it.totalDuration.inHours }.average()
            val mostRecent = plots.maxByOrNull { it.lastModified }!!.totalDuration.inHours
            printtsv(tmpDir1, plots.size, avg, mostRecent)
        }
}

internal fun printtsv(vararg elt: Any) = println(
    elt.joinToString("\t") {
        when (it) {
            is Double -> it.round(1)
            else -> elt
        }.toString()
    }
)

internal fun Double.round(scale: Int = 1) = BigDecimal(this).setScale(scale, RoundingMode.HALF_UP).toDouble()
