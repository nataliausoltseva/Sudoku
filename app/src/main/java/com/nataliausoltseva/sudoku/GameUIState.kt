package com.nataliausoltseva.sudoku

data class GameUIState(
    val matrix: Array<IntArray> = Array(9) { IntArray(9) { 0 } },
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameUIState

        return matrix.contentDeepEquals(other.matrix)
    }

    override fun hashCode(): Int {
        return matrix.contentDeepHashCode()
    }
}