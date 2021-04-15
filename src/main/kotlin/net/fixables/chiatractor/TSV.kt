package net.fixables.chiatractor

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.ConcurrentSkipListSet

/** For pasting into spreadsheets */
object TSV {
    private val alreadySeen: MutableSet<String> = ConcurrentSkipListSet()

    private fun Double.round(scale: Int = 1) = BigDecimal(this).setScale(scale, RoundingMode.HALF_UP).toDouble()

    /** Ignoring doubles, print each line at most once */
    fun once(vararg elements: Any?) {
        if (elements.size == 1 && elements[0] is Collection<Any?>) {
            return once(*(elements[0] as Collection<Any?>).toTypedArray())
        }
        val key = elements.filter { it !is Double }.joinToString(",")
        check(key.isNotBlank()) { "Bad key for printtsvOnce, need at least 1 not-Double" }
        if (!alreadySeen.contains(key)) {
            alreadySeen.add(key)
            this.println(*elements)
        }
    }

    fun println(vararg elements: Any?) {
        if (elements.size == 1 && elements[0] is Collection<Any?>) {
            return this.println(*(elements[0] as Collection<Any?>).toTypedArray())
        }
        return println(
            elements.joinToString("\t") {
                when (it) {
                    is Double -> it.round(1)
                    else -> it
                }.toString()
            }
        )
    }
}