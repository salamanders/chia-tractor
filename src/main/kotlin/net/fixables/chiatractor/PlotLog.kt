@file:Suppress("MemberVisibilityCanBePrivate")

package net.fixables.chiatractor

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readLines
import kotlin.streams.toList
import kotlin.time.*

@OptIn(ExperimentalTime::class, ExperimentalPathApi::class)
open class PlotLog(
    parsedLog: Map<InterestingLogLines, String>
) {
    open val bufferSize: Int? = parsedLog[InterestingLogLines.BUFFER_SIZE]?.toInt()
    open val tempDir1: String? = parsedLog[InterestingLogLines.TEMP_DIR_1]
    open val tempDir2: String? = parsedLog[InterestingLogLines.TEMP_DIR_2]
    open val buckets: Int? = parsedLog[InterestingLogLines.BUCKETS]?.toInt()
    open val threads: Int? = parsedLog[InterestingLogLines.THREADS]?.toInt()
    open val p1Duration: Duration? = parsedLog[InterestingLogLines.P1_SECONDS]?.toInt()?.seconds
    open val p2Duration: Duration? = parsedLog[InterestingLogLines.P2_SECONDS]?.toInt()?.seconds
    open val p3Duration: Duration? = parsedLog[InterestingLogLines.P3_SECONDS]?.toInt()?.seconds
    open val p4Duration: Duration? = parsedLog[InterestingLogLines.P4_SECONDS]?.toInt()?.seconds
    open val totalDuration: Duration? = parsedLog[InterestingLogLines.TOTAL_SECONDS]?.toInt()?.seconds
    val lastModified: Long = parsedLog[InterestingLogLines.LAST_MODIFIED]!!.toLong()

    fun asMap(): SortedMap<String, Any> = sortedMapOf(
        "tempDir1" to (tempDir1 ?: ""),
        "tempDir2" to (tempDir2 ?: ""),
        "bufferSizeGB" to (bufferSize?.let { it / 1000.0} ?: ""),
        "buckets" to (buckets ?: ""),
        "threads" to (threads ?: ""),
        "lastModifiedMs" to lastModified,
        "p1H" to (p1Duration?.inHours ?: ""),
        "p2H" to (p2Duration?.inHours ?: ""),
        "p3H" to (p3Duration?.inHours ?: ""),
        "p4H" to (p4Duration?.inHours ?: ""),
        "totalH" to (totalDuration?.inHours ?: ""),
    )

    companion object {
        fun loadLogs(
            plotLogDir: Path = Paths.get(
                System.getProperty("user.home"),
                ".chia",
                "mainnet",
                "plotter",
            )
        ): List<PlotLog> {
            require(Files.isDirectory(plotLogDir) && Files.isReadable(plotLogDir)) { "Unable to find or read log directory '$plotLogDir'" }
            return Files.walk(plotLogDir, 1)
                .filter { Files.isRegularFile(it) && Files.isReadable(it) }
                .map(PlotLog::pathToPlotLog)
                .filter {
                    // Either it completed, or it is recent.  (discard abandoned plotting)
                    it is CompletedPlotLog || (System.currentTimeMillis() - it.lastModified).milliseconds < 2.days
                }
                .toList()
        }

        private fun pathToPlotLog(logFilePath: Path): PlotLog {
            require(Files.isRegularFile(logFilePath) && Files.isReadable(logFilePath)) { "Unable to read log file '$logFilePath'" }
            val logMap = logFilePath.readLines()
                .filter { !it.startsWith("\t") }
                .map { it.trim() }
                .fold(
                    mutableMapOf(
                        InterestingLogLines.LAST_MODIFIED to Files.getLastModifiedTime(logFilePath).toMillis()
                            .toString()
                    )
                ) { agg, line ->
                    InterestingLogLines.values().forEach { ill ->
                        ill.pattern?.find(line)?.let { agg[ill] = it.groupValues[1] }
                    }
                    agg
                }
            return if (logMap.containsKey(InterestingLogLines.TOTAL_SECONDS)) {
                CompletedPlotLog(logMap)
            } else {
                PlotLog(logMap)
            }
        }
    }
}

enum class InterestingLogLines(val pattern: Regex? = null) {
    BUFFER_SIZE("^Buffer size is: (\\d+)MiB$".toRegex()),
    TEMP_DIR_1("^Starting plotting progress into temporary dirs: (.+) and .+$".toRegex()),
    TEMP_DIR_2("^Starting plotting progress into temporary dirs: .+ and (.+)$".toRegex()),
    BUCKETS("Using (\\d+) buckets".toRegex()),
    THREADS("^Using (\\d+) threads".toRegex()),
    P1_SECONDS("^Time for phase 1 = (\\d+)".toRegex()),
    P2_SECONDS("^Time for phase 2 = (\\d+)".toRegex()),
    P3_SECONDS("^Time for phase 3 = (\\d+)".toRegex()),
    P4_SECONDS("^Time for phase 4 = (\\d+)".toRegex()),
    TOTAL_SECONDS("Total time = (\\d+)".toRegex()),
    LAST_MODIFIED,
}

@OptIn(ExperimentalTime::class)
class CompletedPlotLog(parsedLog: Map<InterestingLogLines, String>) : PlotLog(parsedLog) {
    override val p1Duration: Duration = super.p1Duration!!
    override val p2Duration: Duration = super.p2Duration!!
    override val p3Duration: Duration = super.p3Duration!!
    override val p4Duration: Duration = super.p4Duration!!
    override val totalDuration: Duration = super.totalDuration!!

    override val tempDir1: String = super.tempDir1!!
    override val tempDir2: String = super.tempDir2!!

    override val buckets: Int = super.buckets!!
    override val threads: Int = super.threads!!
    override val bufferSize: Int = super.bufferSize!!

}


