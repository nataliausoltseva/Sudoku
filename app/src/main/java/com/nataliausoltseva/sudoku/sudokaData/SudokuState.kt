package com.nataliausoltseva.sudoku.sudokaData

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf

@Stable
data class SudokuState(
    val hasStarted: Boolean = false,
    var matrix: Array<Array<Array<MutableIntState>>> = Array(9) { Array(9) {  Array(3) { mutableIntStateOf(0) } } },
    var selectionNumbers: Array<Int> =  Array(9) { 0 },
    val selectedCellRow: Int = 0,
    val selectedCellColumn: Int = 0,
    val mistakesNum: Int = 0,
    val selectedLevel: String = "Easy",
    val isPaused: Boolean = false,
    val stepsToGo: Int = 0,
    val hintNum: Int = 3,
    var unlockedCell: Array<Int?> = Array(2) { null },
    val steps: List<Step> = listOf(),
    val hasSteps: Boolean = false,
    val isRestartClicked: Boolean = false,
    val isNotesEnabled: Boolean = false,
    var matrixWithNotes: Array<Array<Array<MutableIntState>>> = Array(9) { Array(9) { Array(9) { mutableIntStateOf(0) }  } },
    val selectedDigit: Int = 0,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SudokuState

        if (hasStarted != other.hasStarted) return false
        if (!matrix.contentDeepEquals(other.matrix)) return false
        if (!selectionNumbers.contentEquals(other.selectionNumbers)) return false
        if (selectedCellRow != other.selectedCellRow) return false
        if (selectedCellColumn != other.selectedCellColumn) return false
        if (mistakesNum != other.mistakesNum) return false
        if (selectedLevel != other.selectedLevel) return false
        if (isPaused != other.isPaused) return false
        if (stepsToGo != other.stepsToGo) return false
        if (hintNum != other.hintNum) return false
        if (!unlockedCell.contentEquals(other.unlockedCell)) return false
        if (steps != other.steps) return false
        if (hasSteps != other.hasSteps) return false
        if (isRestartClicked != other.isRestartClicked) return false
        if (isNotesEnabled != other.isNotesEnabled) return false
        if (!matrixWithNotes.contentDeepEquals(other.matrixWithNotes)) return false
        if (selectedDigit != other.selectedDigit) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hasStarted.hashCode()
        result = 31 * result + matrix.contentDeepHashCode()
        result = 31 * result + selectionNumbers.contentHashCode()
        result = 31 * result + selectedCellRow
        result = 31 * result + selectedCellColumn
        result = 31 * result + mistakesNum
        result = 31 * result + selectedLevel.hashCode()
        result = 31 * result + isPaused.hashCode()
        result = 31 * result + stepsToGo
        result = 31 * result + hintNum
        result = 31 * result + unlockedCell.contentHashCode()
        result = 31 * result + steps.hashCode()
        result = 31 * result + hasSteps.hashCode()
        result = 31 * result + isRestartClicked.hashCode()
        result = 31 * result + isNotesEnabled.hashCode()
        result = 31 * result + matrixWithNotes.contentDeepHashCode()
        result = 31 * result + selectedDigit
        return result
    }
}