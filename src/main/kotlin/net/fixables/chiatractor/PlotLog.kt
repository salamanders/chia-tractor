@file:Suppress("MemberVisibilityCanBePrivate")

package net.fixables.chiatractor

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readLines
import kotlin.streams.toList
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@OptIn(ExperimentalTime::class, ExperimentalPathApi::class)
open class PlotLog(
    parsedLog: Map<InterestingLogLines, String>
) {
    val bufferSize: Int = parsedLog[InterestingLogLines.BUFFER_SIZE]!!.toInt()
    val tempDir1: String = parsedLog[InterestingLogLines.TEMP_DIR_1]!!
    val tempDir2: String = parsedLog[InterestingLogLines.TEMP_DIR_2]!!
    val buckets: Int = parsedLog[InterestingLogLines.BUCKETS]!!.toInt()
    val threads: Int = parsedLog[InterestingLogLines.THREADS]!!.toInt()
    open val p1Duration: Duration? = parsedLog[InterestingLogLines.P1_SECONDS]?.toInt()?.seconds
    open val p2Duration: Duration? = parsedLog[InterestingLogLines.P2_SECONDS]?.toInt()?.seconds
    open val p3Duration: Duration? = parsedLog[InterestingLogLines.P3_SECONDS]?.toInt()?.seconds
    open val p4Duration: Duration? = parsedLog[InterestingLogLines.P4_SECONDS]?.toInt()?.seconds
    open val totalDuration: Duration? = parsedLog[InterestingLogLines.TOTAL_SECONDS]?.toInt()?.seconds
    val lastModified: Long = parsedLog[InterestingLogLines.LAST_MODIFIED]!!.toLong()

    fun asList() = listOf(
        tempDir1,
        tempDir2,
        bufferSize,
        buckets,
        threads,
        lastModified,
        p1Duration ?: "",
        p2Duration ?: "",
        p3Duration ?: "",
        p4Duration ?: "",
        totalDuration ?: "",
    )

    companion object {
        fun asListHeaders() = listOf(
            "tempDir1",
            "tempDir2",
            "bufferSize",
            "buckets",
            "threads",
            "lastModified",
            "p1Duration",
            "p2Duration",
            "p3Duration",
            "p4Duration",
            "totalDuration",
        )

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
            return if (logMap.containsKey(Companion.InterestingLogLines.TOTAL_SECONDS)) {
                CompletedPlotLog(logMap)
            } else {
                PlotLog(logMap)
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
class CompletedPlotLog(parsedLog: Map<Companion.InterestingLogLines, String>) : PlotLog(parsedLog) {
    override val p1Duration: Duration = super.p1Duration!!
    override val p2Duration: Duration = super.p2Duration!!
    override val p3Duration: Duration = super.p3Duration!!
    override val p4Duration: Duration = super.p4Duration!!
    override val totalDuration: Duration = super.totalDuration!!
}


