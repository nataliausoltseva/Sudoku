package com.nataliausoltseva.sudoku.sudokaData

data class SudokuState(
    val hasStarted: Boolean = false,
    var matrix: Array<Array<Array<Int>>> = Array(9) { Array(9) {  Array(3) { 0 } } },
    var selectionNumbers: Array<Int> =  Array(9) { 0 },
    val selectedCellRow: Int = 0,
    val selectedCellColumn: Int = 0,
    val mistakesNum: Int = 0,
    val selectedLevel: String = "Easy",
    val isPaused: Boolean = false,
    val stepsToGo: Int = 0,
    val timer: Long = 0,
    val hintNum: Int = 3,
    var unlockedCell: Array<Int?> = Array(2) { null },
    val steps: List<Step> = listOf(),
    val hasSteps: Boolean = false,
    val isRestartClicked: Boolean = false
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
        if (timer != other.timer) return false
        if (hintNum != other.hintNum) return false
        if (!unlockedCell.contentEquals(other.unlockedCell)) return false
        if (steps != other.steps) return false
        if (hasSteps != other.hasSteps) return false

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
        result = 31 * result + timer.hashCode()
        result = 31 * result + hintNum
        result = 31 * result + unlockedCell.contentHashCode()
        result = 31 * result + steps.hashCode()
        result = 31 * result + hasSteps.hashCode()
        return result
    }
}