package net.fixables.chiatractor

import info.benjaminhill.utils.printlnt
import info.benjaminhill.utils.printlntOnce
import info.benjaminhill.utils.printlntOnceSkip
import java.nio.file.FileSystems
import java.util.concurrent.atomic.AtomicBoolean

private val firstRun = AtomicBoolean(true)

internal fun logFileStoreSpace() {
    val now = System.currentTimeMillis()
    PlotLog.loadLogs()
        .forEach { plot ->
            val logLine = listOf(now, "phase", plot.id, plot.latestFolder(), plot.latestPhase()).toTypedArray()
            if (firstRun.get()) {
                printlnt("time_ms", "event_type", "Store", "used", "avail")
                printlntOnceSkip(*logLine)
            } else {
                printlntOnce(*logLine)
            }
        }
    firstRun.set(false)

    FileSystems.getDefault().fileStores
        .filter { activeFileStores.contains(it) }
        .forEach { store ->
            val used = (store.totalSpace - store.unallocatedSpace) / BYTES_GB
            val avail = store.usableSpace / BYTES_GB
            printlnt(now, "space", store, used, avail)
        }
}