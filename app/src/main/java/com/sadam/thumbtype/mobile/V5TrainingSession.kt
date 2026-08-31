package com.sadam.thumbtype.mobile

/**
 * Editor-like state used by the V5 trainer.
 *
 * Every visible slot represents what is currently on screen. Physical key attempts remain
 * in PressEvent history separately so deleting a visible character does not erase the
 * analytics record of the attempt that happened.
 */
data class V5TypedSlot(
    val targetIndex: Int,
    val expected: Char,
    val entered: Char
) {
    val correct: Boolean get() = expected == entered
}

data class V5TrainingSessionState(
    val cursor: Int = 0,
    val visibleSlots: List<V5TypedSlot> = emptyList(),
    val backspaceCount: Int = 0
) {
    fun type(target: String, entered: Char): V5TrainingSessionState {
        if (cursor !in target.indices) return this
        val slot = V5TypedSlot(cursor, target[cursor], entered)
        val next = visibleSlots.take(cursor) + slot
        return copy(cursor = cursor + 1, visibleSlots = next)
    }

    fun backspace(): V5TrainingSessionState {
        if (cursor <= 0) return this
        val nextCursor = cursor - 1
        return copy(
            cursor = nextCursor,
            visibleSlots = visibleSlots.take(nextCursor),
            backspaceCount = backspaceCount + 1
        )
    }

    fun unresolvedErrors(): Int = visibleSlots.count { !it.correct }

    fun isComplete(target: String): Boolean =
        target.isNotEmpty() && cursor >= target.length && visibleSlots.size >= target.length

    fun isCleanComplete(target: String): Boolean = isComplete(target) && unresolvedErrors() == 0

    fun expected(target: String): Char? = target.getOrNull(cursor)

    fun slotAt(index: Int): V5TypedSlot? = visibleSlots.getOrNull(index)
}
