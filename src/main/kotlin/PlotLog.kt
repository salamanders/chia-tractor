import java.io.File

data class PlotLog(
    val bufferSize: Int,
    val tempDir1: String,
    val tempDir2: String,
    val buckets: Int,
    val threads: Int,
    val p1Seconds: Int,
    val p2Seconds: Int,
    val p3Seconds: Int,
    val p4Seconds: Int,
    val totalSeconds: Int,
    val lastModified: Long,
) {
    companion object {
        private fun fileToMap(logFile: File) = logFile.readLines()
            .filter { !it.startsWith("\t") }
            .map { it.trim() }
            .fold(
                mutableMapOf(
                    PKs.LAST_MODIFIED to logFile.lastModified().toString()
                )
            ) { agg, line ->
                lineParser.forEach { (pk, reg) ->
                    reg.find(line)?.let { matchResult ->
                        agg[pk] = matchResult.groupValues[1]
                    }
                }
                agg
            }

        private fun mapToObject(logMap: MutableMap<PKs, String>): PlotLog? =
            if (logMap.containsKey(PKs.TOTAL_SECONDS)) {
                PlotLog(
                    bufferSize = logMap[PKs.BUFFER_SIZE]!!.toInt(),
                    tempDir1 = logMap[PKs.TEMP_DIR_1]!!,
                    tempDir2 = logMap[PKs.TEMP_DIR_2]!!,
                    buckets = logMap[PKs.BUCKETS]!!.toInt(),
                    threads = logMap[PKs.THREADS]!!.toInt(),
                    p1Seconds = logMap[PKs.P1_SECONDS]!!.toInt(),
                    p2Seconds = logMap[PKs.P2_SECONDS]!!.toInt(),
                    p3Seconds = logMap[PKs.P3_SECONDS]!!.toInt(),
                    p4Seconds = logMap[PKs.P4_SECONDS]!!.toInt(),
                    totalSeconds = logMap[PKs.TOTAL_SECONDS]!!.toInt(),
                    lastModified = logMap[PKs.LAST_MODIFIED]!!.toLong(),
                )
            } else {
                null
            }

        fun of(logFile: File): PlotLog? {
            require(logFile.isFile && logFile.canRead()) { "Unable to read log file '${logFile.absolutePath}'" }
            return mapToObject(fileToMap(logFile))
        }

        private enum class PKs {
            BUFFER_SIZE,
            TEMP_DIR_1,
            TEMP_DIR_2,
            BUCKETS,
            THREADS,
            P1_SECONDS,
            P2_SECONDS,
            P3_SECONDS,
            P4_SECONDS,
            TOTAL_SECONDS,
            LAST_MODIFIED,
        }

        private val lineParser: Map<PKs, Regex> = mapOf(
            PKs.BUFFER_SIZE to "^Buffer size is: (\\d+)MiB$".toRegex(),
            PKs.TEMP_DIR_1 to "^Starting plotting progress into temporary dirs: (.+) and (?:.+)$".toRegex(),
            PKs.TEMP_DIR_2 to "^Starting plotting progress into temporary dirs: (?:.+) and (.+)$".toRegex(),
            PKs.BUCKETS to "Using (\\d+) buckets".toRegex(),
            PKs.THREADS to "^Using (\\d+) threads".toRegex(),
            PKs.P1_SECONDS to "^Time for phase 1 = (\\d+)".toRegex(),
            PKs.P2_SECONDS to "^Time for phase 2 = (\\d+)".toRegex(),
            PKs.P3_SECONDS to "^Time for phase 3 = (\\d+)".toRegex(),
            PKs.P4_SECONDS to "^Time for phase 4 = (\\d+)".toRegex(),
            PKs.TOTAL_SECONDS to "Total time = (\\d+)".toRegex(),
        )
    }
}