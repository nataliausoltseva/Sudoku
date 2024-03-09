package com.nataliausoltseva.sudoku

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf

data class GameUIState(
    val matrix: Array<Array<MutableIntState>> = Array(9) { Array(9) { mutableIntStateOf(0) } },
    val usersMatrix: Array<Array<MutableIntState>> = Array(9) { Array(9) { mutableIntStateOf(0) } },
    val filledMatrix: Array<Array<MutableIntState>> = Array(9) { Array(9) {  mutableIntStateOf(0) } },
    val selectionNumbers: Array<MutableIntState> =  Array(9) { mutableIntStateOf(0) },
    val selectedDigit: Int = 0,
    val selectedCellRow: Int? = null,
    val selectedCellColumn: Int? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameUIState

        if (!matrix.contentDeepEquals(other.matrix)) return false
        if (!usersMatrix.contentDeepEquals(other.usersMatrix)) return false
        if (!filledMatrix.contentDeepEquals(other.filledMatrix)) return false
        if (!selectionNumbers.contentEquals(other.selectionNumbers)) return false
        if (selectedDigit != other.selectedDigit) return false
        if (selectedCellRow != other.selectedCellRow) return false
        if (selectedCellColumn != other.selectedCellColumn) return false

        return true
    }

    override fun hashCode(): Int {
        var result = matrix.contentDeepHashCode()
        result = 31 * result + usersMatrix.contentDeepHashCode()
        result = 31 * result + filledMatrix.contentDeepHashCode()
        result = 31 * result + selectionNumbers.contentHashCode()
        result = 31 * result + selectedDigit
        result = 31 * result + (selectedCellRow ?: 0)
        result = 31 * result + (selectedCellColumn ?: 0)
        return result
    }
}