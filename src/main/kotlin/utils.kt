import java.math.BigDecimal
import java.math.RoundingMode

internal fun Double.round(scale: Int = 1) = BigDecimal(this).setScale(scale, RoundingMode.HALF_UP).toDouble()

internal fun Collection<String>.commonPrefix() = this.reduce { acc: String, s: String -> s.commonPrefixWith(acc) }

internal const val DAYS_IN_MS = 1000 * 60 * 60 * 24