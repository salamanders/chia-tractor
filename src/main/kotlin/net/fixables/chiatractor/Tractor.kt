package net.fixables.chiatractor

import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Path
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

    allPlotLogs
        .map { Path.of(it.tempDir1) }
        .distinctBy { Files.getFileStore(it.toRealPath()).name() }
        .filter { Files.exists(it) && Files.isDirectory(it) && Files.isWritable(it) }
        .let { tempDirs ->
            println("Benchmarking ${tempDirs.size} file stores: ${tempDirs.joinToString(", ")}")
            tempDirs.forEach {
                benchmarkGrid(path = it)
            }
        }
}

private fun dumpAllLogs(allPlotLogs: List<PlotLog>) {
    require(allPlotLogs.isNotEmpty()) { "No plot logs found." }
    println()
    printtsv(allPlotLogs.first().asMap().keys)
    allPlotLogs.forEach {
        printtsv(it.asMap().values)
    }
}

/**
 * A few days so we get some sense of parallel per day
 */
@OptIn(ExperimentalTime::class)
private fun parallelRate(plotLogs: Collection<CompletedPlotLog>, numberOfDays: Int = 3) {
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
private fun completionTimes(plotLogs: Collection<CompletedPlotLog>) {
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

internal fun printtsv(vararg elts: Any?) {
    if (elts.size == 1 && elts[0] is Collection<Any?>) {
        return printtsv(*(elts[0] as Collection<Any?>).toTypedArray())
    }
    return println(
        elts.joinToString("\t") {
            when (it) {
                is Double -> it.round(1)
                else -> it
            }.toString()
        }
    )
}

internal fun Double.round(scale: Int = 1) = BigDecimal(this).setScale(scale, RoundingMode.HALF_UP).toDouble()
