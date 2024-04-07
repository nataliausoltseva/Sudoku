package com.nataliausoltseva.sudoku.sudokaData

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.floor

const val GRID_SIZE = 9
const val GRID_SIZE_SQUARE_ROOT = 3

val LEVELS = arrayOf("Easy", "Medium", "Hard", "Expert", "Master")
private val NUM_TO_REMOVE = arrayOf(1, 2, 4, 6, 7)

class SudokuViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(SudokuState())
    val uiState: StateFlow<SudokuState> = _uiState.asStateFlow()

    private var numToRemove: Int = NUM_TO_REMOVE[0]

    private fun fillGrid() {
        val grid = uiState.value.matrix
        val selectionNumbers = uiState.value.selectionNumbers
        var stepsToGo = uiState.value.stepsToGo
        fillDiagonally(grid)
        fillRemaining(0, GRID_SIZE_SQUARE_ROOT, grid)
        stepsToGo = removeDigits(grid, selectionNumbers, stepsToGo)
        _uiState.update {
            it.copy(
                matrix = grid,
                selectionNumbers = selectionNumbers,
                stepsToGo = stepsToGo
            )
        }
    }

    private fun fillDiagonally(grid: Array<Array<Array<MutableIntState>>>) {
        for (i in 0 until GRID_SIZE step GRID_SIZE_SQUARE_ROOT) {
            fillBox(i, i, grid)
        }
    }

    private fun fillBox(row: Int, column: Int, grid: Array<Array<Array<MutableIntState>>>) {
        var generatedDigit: Int

        for (i in 0 until GRID_SIZE_SQUARE_ROOT) {
            for (j in 0 until GRID_SIZE_SQUARE_ROOT) {
                do {
                    generatedDigit = getRandomNumber(GRID_SIZE)
                } while (isUnusedInBox(row, column, generatedDigit, grid).isNotEmpty())

                grid[row + i][column + j] = Array(3) { mutableIntStateOf(generatedDigit) }
            }
        }
    }

    fun isUnusedInBox(rowStart: Int, columnStart: Int, digit: Int, grid: Array<Array<Array<MutableIntState>>>, useUserGrid: Boolean = false): IntArray {
        for (i in 0 until GRID_SIZE_SQUARE_ROOT) {
            for (j in 0 until GRID_SIZE_SQUARE_ROOT) {
                if ((useUserGrid && grid[rowStart + i][columnStart + j][2].intValue == digit ) ||
                    grid[rowStart + i][columnStart + j][0].intValue == digit
                ) {
                    return intArrayOf(rowStart + i, columnStart + j)
                }
            }
        }
        return intArrayOf()
    }

    private fun fillRemaining(i: Int, j: Int, grid: Array<Array<Array<MutableIntState>>>) : Boolean {
        var newI = i
        var newJ = j
        if (newJ >= GRID_SIZE && newI < GRID_SIZE - 1) {
            newI += 1
            newJ = 0
        }

        if (newI >= GRID_SIZE && newJ >= GRID_SIZE) {
            return true
        }

        if (newI < GRID_SIZE_SQUARE_ROOT) {
            if (newJ < GRID_SIZE_SQUARE_ROOT) {
                newJ = GRID_SIZE_SQUARE_ROOT
            }
        } else if (newI < GRID_SIZE - GRID_SIZE_SQUARE_ROOT) {
            if (newJ == (newI / GRID_SIZE_SQUARE_ROOT) * GRID_SIZE_SQUARE_ROOT) {
                newJ += GRID_SIZE_SQUARE_ROOT
            }
        } else {
            if (newJ == GRID_SIZE - GRID_SIZE_SQUARE_ROOT) {
                newI += 1
                newJ = 0
                if (newI >= GRID_SIZE) {
                    return true
                }
            }
        }

        for (num in 1..GRID_SIZE) {
            if (checkIfSafe(newI, newJ, num, grid)) {
                grid[newI][newJ] = Array(3) { mutableIntStateOf(num) }

                if (fillRemaining(newI, newJ + 1, grid)) {
                    return true
                }
                grid[newI][newJ][0].intValue = 0
                grid[newI][newJ][2].intValue = 0
            }
        }
        return false
    }

    private fun checkIfSafe(row: Int, column: Int, digit: Int, grid: Array<Array<Array<MutableIntState>>>, useUserGrid: Boolean = false) : Boolean {
        return isUnusedInRow(row, digit, grid, useUserGrid).isEmpty() &&
                isUnusedInColumn(column, digit, grid, useUserGrid).isEmpty() &&
                isUnusedInBox(
                    row - row % GRID_SIZE_SQUARE_ROOT,
                    column - column % GRID_SIZE_SQUARE_ROOT,
                    digit,
                    grid,
                    useUserGrid
                ).isEmpty()
    }

    fun isUnusedInRow(row: Int, digit: Int, grid: Array<Array<Array<MutableIntState>>>, useUserGrid: Boolean) : IntArray {
        for (i in 0 until GRID_SIZE) {
            if ((useUserGrid && grid[row][i][2].intValue == digit) || grid[row][i][0].intValue == digit) {
                return intArrayOf(row, i)
            }
        }
        return intArrayOf()
    }

    fun isUnusedInColumn(column: Int, digit: Int, grid: Array<Array<Array<MutableIntState>>>, useUserGrid: Boolean) : IntArray {
        for (i in 0 until GRID_SIZE) {
            if ((useUserGrid && grid[i][column][2].intValue == digit) || grid[i][column][0].intValue == digit) {
                return intArrayOf(i, column)
            }
        }

        return intArrayOf()
    }

    private fun removeDigits(grid: Array<Array<Array<MutableIntState>>>, selectionNumbers: Array<Int>, stepsToGo: Int): Int {
        var newStepsToGo = stepsToGo
        for (j in 0 until GRID_SIZE) {
            val level = numToRemove
            for (k in 0 until (level..GRID_SIZE).random()) {
                val i = getRandomUniqueCell(j, grid)

                if (grid[i][j][0].intValue != 0) {
                    val currentValue = grid[i][j][0].intValue
                    selectionNumbers[currentValue - 1] += 1
                    grid[i][j][0].intValue = 0
                    grid[i][j][2].intValue = 0
                    newStepsToGo++
                }
            }
        }
        return newStepsToGo
    }

    private fun getRandomUniqueCell(column: Int, grid: Array<Array<Array<MutableIntState>>>): Int {
        val cellId = getRandomNumber(GRID_SIZE * GRID_SIZE) - 1
        val i = cellId / GRID_SIZE
        if (grid[i][column][0].intValue == 0) {
            getRandomUniqueCell(column, grid)
        }

        return i
    }

    fun onRegenerate() {
        _uiState.update {
            it.copy(
                hasStarted = false,
                stepsToGo = 0,
                hintNum = 3,
                unlockedCell = arrayOf(null, null),
                steps = listOf(),
                selectedCellRow = 0,
                selectedCellColumn = 0,
            )
        }
    }

    fun onSelection(digit: Int, canInsert: Boolean) {
        _uiState.update {
            it.copy(
                selectedDigit = digit,
            )
        }

        if (canInsert) {
            if (uiState.value.isNotesEnabled) {
                insertNoteDigit(digit)
            } else {
                insertDigit(digit)
            }
        }
    }

    fun canInsert(digit: Int): Boolean {
        if (!uiState.value.isNotesEnabled) return true
        return checkIfSafe(
            uiState.value.selectedCellRow,
            uiState.value.selectedCellColumn,
            digit,
            uiState.value.matrix,
            true
        )
    }

    fun onSelectCell(row: Int, column: Int) {
        _uiState.update {
            it.copy(
                selectedCellRow = row,
                selectedCellColumn = column,
                selectedDigit = 0
            )
        }
    }

    fun onStart(index: Int) {
        _uiState.update {
            it.copy(
                selectedLevel = LEVELS[index],
                hasStarted = true,
                matrix = Array(GRID_SIZE) { Array(GRID_SIZE) { Array(3) { mutableIntStateOf(0) }  } },
                selectionNumbers = Array(9) { 0 },
                mistakesNum = 0,
                stepsToGo = 0,
                hintNum = 3,
                unlockedCell = arrayOf(null, null),
                steps = listOf(),
                selectedCellRow = 0,
                selectedCellColumn = 0,
            )
        }
        numToRemove = NUM_TO_REMOVE[index]
        fillGrid()
    }

    fun onPause() {
        _uiState.update {
            it.copy(
                isPaused = true,
            )
        }
    }

    fun onStart() {
        _uiState.update {
            it.copy(
                isPaused = false,
            )
        }
    }

    fun useHint() {
        var hintNum = uiState.value.hintNum
        var stepsToGo = uiState.value.stepsToGo
        val grid = uiState.value.matrix
        val selectionNumbers = uiState.value.selectionNumbers
        val unlockedCell = uiState.value.unlockedCell

        unlockACell(grid, unlockedCell, selectionNumbers)

        _uiState.update {
            it.copy(
                hintNum = --hintNum,
                stepsToGo = --stepsToGo,
                matrix = grid,
                unlockedCell = unlockedCell,
                selectionNumbers = selectionNumbers,
                selectedDigit = 0,
            )
        }
    }

    fun onErase() {
        val grid = uiState.value.matrix
        val row = uiState.value.selectedCellRow
        val column = uiState.value.selectedCellColumn
        val selectionNumbers = uiState.value.selectionNumbers
        var stepsToGo = uiState.value.stepsToGo
        val cell = grid[row][column]
        val digitNumber = cell[2].intValue
        val isDeletable = cell[0].intValue == 0
        var digit: Int? = null
        val previousCellDigit = grid[row][column][2].intValue
        if (digitNumber != 0 && isDeletable) {
            selectionNumbers[digitNumber - 1]++
            grid[row][column][2].intValue = 0
            stepsToGo++
            digit = 0

            _uiState.update {
                it.copy(
                    selectionNumbers = selectionNumbers,
                    matrix = grid,
                    stepsToGo = stepsToGo,
                )
            }
        }

        val gridWithNotes = uiState.value.matrixWithNotes
        val previousCellWithNote = uiState.value.matrixWithNotes[row][column]
        gridWithNotes[row][column].forEach {
            if (it.intValue == 0) return@forEach
            it.intValue = 0
        }

        recordStep(
            cellWithNote = Array(9) { mutableIntStateOf(0) },
            cellDigit = digit,
            previousCellDigit = previousCellDigit,
            previousCellWithNote = previousCellWithNote
        )

        _uiState.update {
            it.copy(matrixWithNotes = gridWithNotes, selectedDigit = 0,)
        }
    }

    fun onUndo() {
        val steps = uiState.value.steps.toMutableList()
        val selectionNumbers = uiState.value.selectionNumbers
        var selectedCellRow = uiState.value.selectedCellRow
        var selectedCellColumn = uiState.value.selectedCellColumn
        var stepsToGo = uiState.value.stepsToGo
        val grid = uiState.value.matrix
        val gridWithNotes = uiState.value.matrixWithNotes

        val step = steps.last()

        if (step.digit != null && step.previousDigit != null) {
            val digit = (step.digit - 1).coerceAtLeast(0)
            selectionNumbers[digit]++
            stepsToGo++
            grid[selectedCellRow][selectedCellColumn][2].intValue = step.previousDigit
        } else {
            gridWithNotes[selectedCellRow][selectedCellColumn].forEachIndexed { index, _ ->
                gridWithNotes[selectedCellRow][selectedCellColumn][index].intValue = step.previousNotes[index].intValue
            }
        }

        steps.removeLast()

        if (steps.size > 0) {
            val previousStep = steps.last()
            selectedCellRow = previousStep.xIndex
            selectedCellColumn = previousStep.yIndex
        } else {
            selectedCellRow = 0
            selectedCellColumn = 0
        }

        _uiState.update {
            it.copy(
                selectionNumbers = selectionNumbers,
                matrix = grid,
                stepsToGo = stepsToGo,
                steps = steps,
                hasSteps = steps.size > 0,
                matrixWithNotes = gridWithNotes,
                selectedCellRow = selectedCellRow,
                selectedCellColumn = selectedCellColumn,
                selectedDigit = 0,
            )
        }
    }

    fun onNote() {
        _uiState.update {
            it.copy(
                isNotesEnabled = !uiState.value.isNotesEnabled,
                selectedDigit = 0,
            )
        }
    }

    private fun recordStep(
        cellWithNote: Array<MutableIntState>,
        previousCellWithNote: Array<MutableIntState>,
        cellDigit: Int?,
        previousCellDigit: Int?,
    ) {
        val steps = uiState.value.steps.toMutableList()
        val selectedCellRow = uiState.value.selectedCellRow
        val selectedCellColumn = uiState.value.selectedCellColumn
        steps.add(
            Step(
                xIndex = selectedCellRow,
                yIndex = selectedCellColumn,
                digit = if (cellDigit != 0) cellDigit else null,
                previousDigit = if (cellDigit != 0) previousCellDigit else null,
                previousNotes = previousCellWithNote,
                notes = cellWithNote
            )
        )

        _uiState.update {
            it.copy(
                steps = steps,
                hasSteps = true,
            )
        }
    }

    private fun unlockACell(grid: Array<Array<Array<MutableIntState>>>, unlockedCell: Array<Int?>, selectionNumbers: Array<Int>) {
        val cellId = getRandomNumber(GRID_SIZE * GRID_SIZE) - 1
        val i = cellId / GRID_SIZE
        val y = cellId % GRID_SIZE
        if (grid[i][y][0].intValue == 0 && grid[i][y][2].intValue == 0) {
            val digitToInsert = grid[i][y][1].intValue
            grid[i][y][0].intValue = digitToInsert
            grid[i][y][2].intValue = digitToInsert
            unlockedCell[0] = i
            unlockedCell[1] = y
            selectionNumbers[digitToInsert - 1]--
        } else {
            unlockACell(grid, unlockedCell, selectionNumbers)
        }
    }

    private fun insertDigit(selectedDigit: Int = 0) {
        val grid = uiState.value.matrix
        val row = uiState.value.selectedCellRow
        val column = uiState.value.selectedCellColumn
        val selectionNumbers = uiState.value.selectionNumbers
        var mistakesNum = uiState.value.mistakesNum
        var stepsToGo = uiState.value.stepsToGo
        val previousCellDigit: Int?
        if (selectedDigit != 0 && grid[row][column][0].intValue == 0) {
            previousCellDigit = grid[row][column][2].intValue
            if (grid[row][column][1].intValue != selectedDigit) {
                mistakesNum++
            } else {
                stepsToGo--
            }
            val isEmptyCell = grid[row][column][2].intValue != 0
            val isNotCurrentValue = grid[row][column][2].intValue != selectedDigit
            if (isEmptyCell && isNotCurrentValue) {
                selectionNumbers[grid[row][column][2].intValue - 1]++

                if (grid[row][column][1].intValue != selectedDigit) {
                    stepsToGo++
                }
            }

            selectionNumbers[selectedDigit - 1]--

            grid[row][column][2].intValue = selectedDigit

            recordStep(
                cellDigit = selectedDigit,
                previousCellDigit = previousCellDigit,
                previousCellWithNote = uiState.value.matrixWithNotes[row][column],
                cellWithNote = uiState.value.matrixWithNotes[row][column],
            )

            _uiState.update {
                it.copy(
                    matrix = grid,
                    selectionNumbers = selectionNumbers,
                    mistakesNum = mistakesNum,
                    stepsToGo = stepsToGo
                )
            }
        }
    }

    private fun insertNoteDigit(digit: Int) {
        val gridWithNotes = uiState.value.matrixWithNotes
        val row = uiState.value.selectedCellRow
        val column = uiState.value.selectedCellColumn
        val previousCellWithNote = Array(9) { mutableIntStateOf(0) }

        gridWithNotes[row][column].forEachIndexed { index, value ->
            previousCellWithNote[index].intValue = value.intValue
        }

        if (gridWithNotes[row][column][digit - 1].intValue == 0) {
            gridWithNotes[row][column][digit - 1].intValue = digit
        } else {
            gridWithNotes[row][column][digit - 1].intValue = 0
        }

        recordStep(
            cellWithNote = gridWithNotes[row][column],
            previousCellWithNote = previousCellWithNote,
            cellDigit = null,
            previousCellDigit = null
        )

        _uiState.update {
            it.copy(matrixWithNotes = gridWithNotes)
        }
    }


    private fun getRandomNumber(multiplyBy: Int): Int {
        return floor((Math.random() * multiplyBy + 1)).toInt()
    }
}