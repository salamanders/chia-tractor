import java.math.BigDecimal
import java.math.RoundingMode

fun Double.round(scale: Int = 1) = BigDecimal(this).setScale(scale, RoundingMode.HALF_UP).toDouble()

fun Collection<String>.commonPrefix() = this.reduce { acc: String, s: String -> s.commonPrefixWith(acc) }
