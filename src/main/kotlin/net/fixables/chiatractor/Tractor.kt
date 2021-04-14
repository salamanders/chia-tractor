package net.fixables.chiatractor

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.FileSystems
import java.util.concurrent.ConcurrentSkipListSet
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

    printtsv("TimeMS", "Store", "type", "total", "used", "avail")

    runBlocking {
        while (true) {
            val now = System.currentTimeMillis()

            PlotLog.loadLogs()
                .filterNot { it is CompletedPlotLog }
                .forEach { activePlot ->
                    when {
                        activePlot.p4Duration != null -> printtsvOnce(now, activePlot.id, activePlot.tempDir2, "p4done")
                        activePlot.p3Duration != null -> printtsvOnce(now, activePlot.id, activePlot.tempDir2, "p3done")
                        activePlot.p2Duration != null -> printtsvOnce(now, activePlot.id, activePlot.tempDir1, "p2done")
                        activePlot.p1Duration != null -> printtsvOnce(now, activePlot.id, activePlot.tempDir1, "p1done")
                    }
                }

            FileSystems.getDefault().fileStores.forEach { store ->
                val total = store.totalSpace / BYTES_GB
                if (total > 0) {
                    val used = (store.totalSpace - store.unallocatedSpace) / BYTES_GB
                    val avail = store.usableSpace / BYTES_GB
                    printtsv(now, store, store.type(), total, used, avail)
                }
            }
            delay(5.minutes)
        }
    }


    /*
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
     */
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

internal val printOnces: MutableSet<String> = ConcurrentSkipListSet()

/** Ignoring doubles, print each line at most once */
internal fun printtsvOnce(vararg elts: Any?) {
    if (elts.size == 1 && elts[0] is Collection<Any?>) {
        return printtsvOnce(*(elts[0] as Collection<Any?>).toTypedArray())
    }
    val key = elts.filter { it !is Double }.joinToString(",")
    check(key.isNotBlank()) { "Bad key for printtsvOnce" }
    if (!printOnces.contains(key)) {
        printOnces.add(key)
        printtsv(elts)
    }
}

/** For pasting into spreadsheets */
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

internal const val BYTES_KB = 1024L
internal const val BYTES_MB = BYTES_KB * 1024L
internal const val BYTES_GB = BYTES_MB * 1024L