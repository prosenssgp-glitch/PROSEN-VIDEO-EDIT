package com.aiedustudio.editor.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TimelineEditorTest {
    private fun item(id: Long, duration: Long = 10_000L) =
        TimelineItem(id = id, source = "content://media/$id", type = "video", durationMs = duration)

    @Test
    fun deleteRemovesOnlyMatchingClip() {
        val input = listOf(item(1), item(2), item(3))
        assertEquals(listOf(1L, 3L), TimelineEditor.delete(input, 2).map { it.id })
    }

    @Test
    fun duplicateInsertsCopyAfterOriginalWithNewId() {
        val result = TimelineEditor.duplicate(listOf(item(1), item(2)), 1, 9)
        assertEquals(listOf(1L, 9L, 2L), result.map { it.id })
        assertEquals(item(1), result[0])
        assertEquals("content://media/1", result[1].source)
    }

    @Test
    fun moveReordersClipAndClampsDestination() {
        val input = listOf(item(1), item(2), item(3))
        assertEquals(listOf(2L, 3L, 1L), TimelineEditor.move(input, 1, 99).map { it.id })
        assertEquals(listOf(3L, 1L, 2L), TimelineEditor.move(input, 3, 0).map { it.id })
    }

    @Test
    fun splitCreatesAdjacentSourceRanges() {
        val result = TimelineEditor.split(listOf(item(1, 8_000)), 1, 3_000, 2)
        assertEquals(listOf(1L, 2L), result.map { it.id })
        assertEquals(0L, result[0].sourceInMs)
        assertEquals(3_000L, result[0].sourceOutMs)
        assertEquals(3_000L, result[0].durationMs)
        assertEquals(3_000L, result[1].sourceInMs)
        assertEquals(8_000L, result[1].sourceOutMs)
        assertEquals(5_000L, result[1].durationMs)
    }

    @Test
    fun splitAtBoundaryLeavesTimelineUnchanged() {
        val input = listOf(item(1))
        assertEquals(input, TimelineEditor.split(input, 1, 0, 2))
        assertEquals(input, TimelineEditor.split(input, 1, 10_000, 2))
    }

    @Test
    fun trimUpdatesSourceRangeAndDuration() {
        val result = TimelineEditor.trim(listOf(item(1)), 1, 1_250, 6_750).single()
        assertEquals(1_250L, result.sourceInMs)
        assertEquals(6_750L, result.sourceOutMs)
        assertEquals(5_500L, result.durationMs)
    }

    @Test
    fun trimWithInvalidRangeDoesNotChangeTimeline() {
        val input = listOf(item(1))
        assertEquals(input, TimelineEditor.trim(input, 1, 5_000, 5_000))
        assertNotEquals(input, TimelineEditor.trim(input, 1, 5_000, 6_000))
    }
}
