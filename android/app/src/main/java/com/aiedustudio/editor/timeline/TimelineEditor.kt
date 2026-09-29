package com.aiedustudio.editor.timeline

/**
 * Pure timeline operations, kept independent from Compose and Android media APIs.
 * Time values are milliseconds; callers persist the resulting project state.
 */
data class TimelineItem(
    val id: Long,
    val source: String,
    val type: String,
    val durationMs: Long,
    val sourceInMs: Long = 0L,
    val sourceOutMs: Long = durationMs
) {
    init {
        require(durationMs > 0)
        require(sourceInMs >= 0 && sourceOutMs > sourceInMs)
    }
}

object TimelineEditor {
    fun delete(items: List<TimelineItem>, id: Long) = items.filterNot { it.id == id }

    fun duplicate(items: List<TimelineItem>, id: Long, newId: Long): List<TimelineItem> {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return items
        return items.toMutableList().apply { add(index + 1, items[index].copy(id = newId)) }
    }

    fun move(items: List<TimelineItem>, id: Long, destination: Int): List<TimelineItem> {
        val from = items.indexOfFirst { it.id == id }
        if (from < 0 || items.isEmpty()) return items
        val copy = items.toMutableList()
        val item = copy.removeAt(from)
        copy.add(destination.coerceIn(0, copy.size), item)
        return copy
    }

    /** Split at a source-relative playhead; returns unchanged list for invalid boundaries. */
    fun split(items: List<TimelineItem>, id: Long, playheadMs: Long, secondId: Long): List<TimelineItem> {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return items
        val item = items[index]
        if (playheadMs <= item.sourceInMs || playheadMs >= item.sourceOutMs) return items
        val first = item.copy(durationMs = playheadMs - item.sourceInMs, sourceOutMs = playheadMs)
        val second = item.copy(id = secondId, durationMs = item.sourceOutMs - playheadMs, sourceInMs = playheadMs)
        return items.toMutableList().apply { removeAt(index); add(index, second); add(index, first) }
    }

    fun trim(items: List<TimelineItem>, id: Long, sourceInMs: Long, sourceOutMs: Long): List<TimelineItem> {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return items
        val item = items[index]
        if (sourceInMs < 0 || sourceOutMs <= sourceInMs) return items
        return items.toMutableList().apply {
            this[index] = item.copy(sourceInMs = sourceInMs, sourceOutMs = sourceOutMs, durationMs = sourceOutMs - sourceInMs)
        }
    }
}
