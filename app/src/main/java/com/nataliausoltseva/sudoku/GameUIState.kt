package com.nataliausoltseva.sudoku

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf

data class GameUIState(
    val hasStarted: MutableState<Boolean> = mutableStateOf(false),
    val matrix: Array<Array<MutableIntState>> = Array(9) { Array(9) { mutableIntStateOf(0) } },
    val usersMatrix: Array<Array<MutableIntState>> = Array(9) { Array(9) { mutableIntStateOf(0) } },
    val filledMatrix: Array<Array<MutableIntState>> = Array(9) { Array(9) {  mutableIntStateOf(0) } },
    val selectionNumbers: Array<MutableIntState> =  Array(9) { mutableIntStateOf(0) },
    val selectedDigit: Int = 0,
    val selectedCellRow: Int? = null,
    val selectedCellColumn: Int? = null,
    val mistakesNum: MutableIntState = mutableIntStateOf(0),
    val selectedLevel: MutableState<String> = mutableStateOf("Easy"),
    val isPaused: MutableState<Boolean> = mutableStateOf(false),
    val stepsToGo: MutableState<Int> = mutableIntStateOf(0),
    val timer: MutableState<Long> = mutableLongStateOf(0),
    val hintNum: MutableIntState = mutableIntStateOf(3),
    val unlockedCell: Array<MutableState<Int?>> = Array(2) { mutableStateOf(null) },
    val steps: MutableList<Step> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameUIState

        if (hasStarted != other.hasStarted) return false
        if (!matrix.contentDeepEquals(other.matrix)) return false
        if (!usersMatrix.contentDeepEquals(other.usersMatrix)) return false
        if (!filledMatrix.contentDeepEquals(other.filledMatrix)) return false
        if (!selectionNumbers.contentEquals(other.selectionNumbers)) return false
        if (selectedDigit != other.selectedDigit) return false
        if (selectedCellRow != other.selectedCellRow) return false
        if (selectedCellColumn != other.selectedCellColumn) return false
        if (mistakesNum != other.mistakesNum) return false
        if (selectedLevel != other.selectedLevel) return false
        if (isPaused != other.isPaused) return false
        if (stepsToGo != other.stepsToGo) return false
        if (timer != other.timer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hasStarted.hashCode()
        result = 31 * result + matrix.contentDeepHashCode()
        result = 31 * result + usersMatrix.contentDeepHashCode()
        result = 31 * result + filledMatrix.contentDeepHashCode()
        result = 31 * result + selectionNumbers.contentHashCode()
        result = 31 * result + selectedDigit
        result = 31 * result + (selectedCellRow ?: 0)
        result = 31 * result + (selectedCellColumn ?: 0)
        result = 31 * result + mistakesNum.hashCode()
        result = 31 * result + selectedLevel.hashCode()
        result = 31 * result + isPaused.hashCode()
        result = 31 * result + stepsToGo.hashCode()
        result = 31 * result + timer.hashCode()
        return result
    }
}

data class Step(
    val xIndex: Int,
    val yIndex: Int,
    val digit: Int
)