import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

internal fun Double.round(scale: Int = 1) = BigDecimal(this).setScale(scale, RoundingMode.HALF_UP).toDouble()

fun main() {
    println("Hello Chia!")

    val plotLogDir = File(
        listOf(
            System.getProperty("user.home"),
            ".chia",
            "mainnet",
            "plotter",
        ).joinToString(File.separator)
    ).also { require(it.isDirectory) { "Unable to load log directory '${it.absolutePath}'" } }

    val plotLogs = plotLogDir.walk().filter { it.isFile && it.canRead() }.mapNotNull(PlotLog::of)

    listOf("All", "Most Recent").forEach { surveyType ->
        println("# $surveyType temp paths:")
        plotLogs.groupBy { it.tempDir1 }.forEach { (tmpDir1, plots) ->
            val samplePlots = if (surveyType == "All") {
                plots
            } else {
                plots.sortedBy { it.lastModified }.takeLast(1)
            }
            println(
                "  Temp Dir: $tmpDir1 = average time ${
                    (samplePlots.map { it.totalSeconds }.toIntArray().average() / (60 * 60)).round(3)
                }h" +
                        " across ${samplePlots.size} plot(s)."
            )
        }
    }
}
