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
    val hintNum: Int = 3,
    var unlockedCell: Array<Int?> = Array(2) { null },
    val steps: List<Step> = listOf(),
    val hasSteps: Boolean = false,
    val isRestartClicked: Boolean = false,
    val isNotesEnabled: Boolean = false,
    var matrixWithNotes: Array<Array<Array<MutableIntState>>> = Array(9) { Array(9) { Array(9) { mutableIntStateOf(0) }  } },
    val selectedDigit: Int = 0,
    val hasStepsToGo: Boolean = true,
) {
}