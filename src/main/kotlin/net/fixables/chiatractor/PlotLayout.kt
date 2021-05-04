package net.fixables.chiatractor

import java.io.File
import kotlin.time.Duration
import kotlin.time.DurationUnit

/** Load a two-column tsv file into a map */
internal fun loadSamples(plotTiming: File): IntArray {
    require(plotTiming.isFile && plotTiming.canRead() && plotTiming.length() > 0) {
        "Unable to read plot timing from '${plotTiming.absolutePath}'"
    }
    val samples = plotTiming.readLines()
        .drop(1).associate {
            val (minutes, gb) = it.split("\t")
            minutes.toInt() to gb.toInt()
        }.toSortedMap()
    val samplesNoGap = samples.toMutableMap()

    // Fill in the blanks
    (samples.firstKey()..samples.lastKey())
        .subtract(samples.keys)
        .forEach { k ->
            val before = samples[samples.headMap(k).lastKey()]!!
            val after = samples[samples.tailMap(k).firstKey()]!!
            samplesNoGap[k] = (before + after) / 2
            println("Filling in gap: ${k}=${samplesNoGap[k]}")
        }
    return IntArray(samplesNoGap.size) { samplesNoGap[it]!! }
}

/** What is the minimum minutes of delay that won't crash your system */
internal fun packThemIn(
    timeToSpaceSamples: IntArray,
    availableSpaceGb: Int,
    horizon: Duration = Duration.days(14)
): Int? {
    require(timeToSpaceSamples.isNotEmpty()) { "Empty samples." }
    val onePlotDurationMinutes = timeToSpaceSamples.size
    val horizonMinutes = horizon.toInt(DurationUnit.MINUTES)
    require(onePlotDurationMinutes < horizonMinutes) { "A single plot won't fit in the time horizon" }
    require(timeToSpaceSamples.none { it > availableSpaceGb }) { "A single plot won't fit in the space." }
    val spaceUsedAtTime = IntArray(horizonMinutes) { 0 }

    for (delayStartMinutes in 1..onePlotDurationMinutes) {
        spaceUsedAtTime.fill(0)
        for (startMinute in 0 until (horizonMinutes - onePlotDurationMinutes) step delayStartMinutes) {
            // println("Start minute:$startMinute")
            timeToSpaceSamples.forEachIndexed { plotMinute, space ->
                spaceUsedAtTime[startMinute + plotMinute] += space
            }
            if (spaceUsedAtTime.any { it > availableSpaceGb }) {
                println("Ran out of space with plot starting at t=$startMinute (${spaceUsedAtTime.maxOrNull()})")
                break
            }
        }
        if (spaceUsedAtTime.none { it > availableSpaceGb }) {
            return delayStartMinutes
        }
    }
    return null
}
