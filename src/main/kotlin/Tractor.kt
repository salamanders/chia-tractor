import java.io.File

fun main() {
    println("Starting up the Chia Tractor")

    val plotLogDir = File(
        listOf(
            System.getProperty("user.home"),
            ".chia",
            "mainnet",
            "plotter",
        ).joinToString(File.separator)
    ).also { require(it.isDirectory) { "Unable to load log directory '${it.absolutePath}'" } }

    val plotLogs = plotLogDir.walk().filter { it.isFile && it.canRead() }.mapNotNull(PlotLog::of).toList()
    println("Found ${plotLogs.size} completed logs.")
    val commonPrefix = plotLogs.map { it.tempDir1 }.commonPrefix()

    listOf("All", "Most Recent").forEach { surveyType ->
        println("# $surveyType temp paths:")
        plotLogs
            .groupBy { it.tempDir1.removePrefix(commonPrefix) }
            .toSortedMap()
            .forEach { (tmpDir1, plots) ->
                val samplePlots = if (surveyType == "All") {
                    plots
                } else {
                    plots.sortedBy { it.lastModified }.takeLast(1)
                }
                val hours = (samplePlots.map { it.totalSeconds }.toIntArray().average() / (60 * 60)).round(1)
                println("  Temp Dir: $tmpDir1 = average time ${hours}h across ${samplePlots.size} plot(s).")
            }
    }

}

