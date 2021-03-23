import java.io.File

data class PlotLog(
    private val parsedLog: Map<InterestingLogLines, String>
) {
    val bufferSize: Int = parsedLog[InterestingLogLines.BUFFER_SIZE]!!.toInt()
    val tempDir1: String = parsedLog[InterestingLogLines.TEMP_DIR_1]!!
    val tempDir2: String = parsedLog[InterestingLogLines.TEMP_DIR_2]!!
    val buckets: Int = parsedLog[InterestingLogLines.BUCKETS]!!.toInt()
    val threads: Int = parsedLog[InterestingLogLines.THREADS]!!.toInt()
    val p1Seconds: Int = parsedLog[InterestingLogLines.P1_SECONDS]!!.toInt()
    val p2Seconds: Int = parsedLog[InterestingLogLines.P2_SECONDS]!!.toInt()
    val p3Seconds: Int = parsedLog[InterestingLogLines.P3_SECONDS]!!.toInt()
    val p4Seconds: Int = parsedLog[InterestingLogLines.P4_SECONDS]!!.toInt()
    val totalSeconds: Int = parsedLog[InterestingLogLines.TOTAL_SECONDS]!!.toInt()
    val lastModified: Long = parsedLog[InterestingLogLines.LAST_MODIFIED]!!.toLong()

    companion object {
        enum class InterestingLogLines(val pattern: Regex? = null) {
            BUFFER_SIZE("^Buffer size is: (\\d+)MiB$".toRegex()),
            TEMP_DIR_1("^Starting plotting progress into temporary dirs: (.+) and (?:.+)$".toRegex()),
            TEMP_DIR_2("^Starting plotting progress into temporary dirs: (?:.+) and (.+)$".toRegex()),
            BUCKETS("Using (\\d+) buckets".toRegex()),
            THREADS("^Using (\\d+) threads".toRegex()),
            P1_SECONDS("^Time for phase 1 = (\\d+)".toRegex()),
            P2_SECONDS("^Time for phase 2 = (\\d+)".toRegex()),
            P3_SECONDS("^Time for phase 3 = (\\d+)".toRegex()),
            P4_SECONDS("^Time for phase 4 = (\\d+)".toRegex()),
            TOTAL_SECONDS("Total time = (\\d+)".toRegex()),
            LAST_MODIFIED,
        }

        fun of(logFile: File): PlotLog? {
            require(logFile.isFile && logFile.canRead()) { "Unable to read log file '${logFile.absolutePath}'" }

            val logMap = logFile.readLines()
                .filter { !it.startsWith("\t") }
                .map { it.trim() }
                .fold(
                    mutableMapOf(
                        InterestingLogLines.LAST_MODIFIED to logFile.lastModified().toString()
                    )
                ) { agg, line ->
                    InterestingLogLines.values().forEach { ill ->
                        ill.pattern?.find(line)?.let { agg[ill] = it.groupValues[1] }
                    }
                    agg
                }
            if (!logMap.containsKey(InterestingLogLines.TOTAL_SECONDS)) {
                return null
            }
            return PlotLog(logMap)
        }
    }
}

