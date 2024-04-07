package com.nataliausoltseva.sudoku.sudokaData

import androidx.compose.runtime.MutableIntState

data class Step(
    val xIndex: Int,
    val yIndex: Int,
    val digit: Int?,
    val previousDigit: Int?,
    val previousNotes: Array<MutableIntState>,
    val notes: Array<MutableIntState>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Step

        if (xIndex != other.xIndex) return false
        if (yIndex != other.yIndex) return false
        if (digit != other.digit) return false
        if (!notes.contentEquals(other.notes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = xIndex
        result = 31 * result + yIndex
        result = 31 * result + (digit ?: 0)
        result = 31 * result + notes.contentHashCode()
        return result
    }
}