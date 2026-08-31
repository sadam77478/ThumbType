package com.sadam.thumbtype.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5TrainingSessionTest {
    @Test
    fun wrongCharacterRemainsVisibleUntilBackspace() {
        val target = "cat"
        val wrong = V5TrainingSessionState().type(target, 'x')

        assertEquals(1, wrong.cursor)
        assertEquals('x', wrong.slotAt(0)?.entered)
        assertEquals(1, wrong.unresolvedErrors())
        assertEquals('a', wrong.expected(target))

        val repairedCursor = wrong.backspace()
        assertEquals(0, repairedCursor.cursor)
        assertEquals(0, repairedCursor.visibleSlots.size)
        assertEquals('c', repairedCursor.expected(target))
        assertEquals(1, repairedCursor.backspaceCount)
    }

    @Test
    fun backspaceThenRetypeProducesCleanVisibleState() {
        val target = "go"
        val state = V5TrainingSessionState()
            .type(target, 'x')
            .backspace()
            .type(target, 'g')
            .type(target, 'o')

        assertTrue(state.isComplete(target))
        assertTrue(state.isCleanComplete(target))
        assertEquals(0, state.unresolvedErrors())
        assertEquals("go", state.visibleSlots.joinToString("") { it.entered.toString() })
    }

    @Test
    fun completeTextCanRetainUnresolvedErrors() {
        val target = "ab"
        val state = V5TrainingSessionState()
            .type(target, 'x')
            .type(target, 'b')

        assertTrue(state.isComplete(target))
        assertFalse(state.isCleanComplete(target))
        assertEquals(1, state.unresolvedErrors())
        assertEquals(null, state.expected(target))
    }

    @Test
    fun typingAfterEndDoesNotMutateState() {
        val target = "a"
        val state = V5TrainingSessionState().type(target, 'a')
        assertEquals(state, state.type(target, 'b'))
    }
}
