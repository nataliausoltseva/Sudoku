package com.nataliausoltseva.sudoku.sudokaData

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.floor

private const val GRID_SIZE = 9
private const val GRID_SIZE_SQUARE_ROOT = 3

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

    private fun fillDiagonally(grid: Array<Array<Array<Int>>>) {
        for (i in 0 until GRID_SIZE step GRID_SIZE_SQUARE_ROOT) {
            fillBox(i, i, grid)
        }
    }

    private fun fillBox(row: Int, column: Int, grid: Array<Array<Array<Int>>>) {
        var generatedDigit: Int

        for (i in 0 until GRID_SIZE_SQUARE_ROOT) {
            for (j in 0 until GRID_SIZE_SQUARE_ROOT) {
                do {
                    generatedDigit = getRandomNumber(GRID_SIZE)
                } while (!isUnusedInBox(row, column, generatedDigit, grid))

                grid[row + i][column + j] = Array(3) { generatedDigit }
            }
        }
    }

    private fun isUnusedInBox(rowStart: Int, columnStart: Int, digit: Int, grid: Array<Array<Array<Int>>>) : Boolean {
        for (i in 0 until GRID_SIZE_SQUARE_ROOT) {
            for (j in 0 until GRID_SIZE_SQUARE_ROOT) {
                if (grid[rowStart + i][columnStart + j][0] == digit) {
                    return false
                }
            }
        }
        return true
    }

    private fun fillRemaining(i: Int, j: Int, grid: Array<Array<Array<Int>>>) : Boolean {
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
                grid[newI][newJ] = Array(3) { num }

                if (fillRemaining(newI, newJ + 1, grid)) {
                    return true
                }
                grid[newI][newJ][0] = 0
                grid[newI][newJ][2] = 0
            }
        }
        return false
    }

    private fun checkIfSafe(row: Int, column: Int, digit: Int, grid: Array<Array<Array<Int>>>) : Boolean {
        return isUnusedInRow(row, digit, grid) &&
                isUnusedInColumn(column, digit, grid) &&
                isUnusedInBox(
                    row - row % GRID_SIZE_SQUARE_ROOT,
                    column - column % GRID_SIZE_SQUARE_ROOT,
                    digit,
                    grid
                )
    }

    private fun isUnusedInRow(row: Int, digit: Int, grid: Array<Array<Array<Int>>>) : Boolean {
        for (i in 0 until GRID_SIZE) {
            if (grid[row][i][0] == digit) {
                return false
            }
        }
        return true
    }

    private fun isUnusedInColumn(column: Int, digit: Int, grid: Array<Array<Array<Int>>>) : Boolean {
        for (i in 0 until GRID_SIZE) {
            if (grid[i][column][0] == digit) {
                return false
            }
        }

        return true
    }

    private fun removeDigits(grid: Array<Array<Array<Int>>>, selectionNumbers: Array<Int>, stepsToGo: Int): Int {
        var newStepsToGo = stepsToGo
        for (j in 0 until GRID_SIZE) {
            val level = numToRemove
            for (k in 0 until (level..GRID_SIZE).random()) {
                val i = getRandomUniqueCell(j, grid)

                if (grid[i][j][0] != 0) {
                    val currentValue = grid[i][j][0]
                    selectionNumbers[currentValue - 1] += 1
                    grid[i][j][0] = 0
                    grid[i][j][2] = 0
                    newStepsToGo++
                }
            }
        }
        return newStepsToGo
    }

    private fun getRandomUniqueCell(column: Int, grid: Array<Array<Array<Int>>>): Int {
        val cellId = getRandomNumber(GRID_SIZE * GRID_SIZE) - 1
        val i = cellId / GRID_SIZE
        if (grid[i][column][0] == 0) {
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

    fun onSelection(digit: Int) {
        recordStep()
        insertDigit(digit)
    }

    fun onSelectCell(row: Int, column: Int) {
        _uiState.update {
            it.copy(
                selectedCellRow = row,
                selectedCellColumn = column,
            )
        }
    }

    fun onStart(index: Int) {
        _uiState.update {
            it.copy(
                selectedLevel = LEVELS[index],
                hasStarted = true,
                matrix = Array(GRID_SIZE) { Array(GRID_SIZE) { Array(3) { 0 }  } },
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

    fun setTimer(timeValue: Long) {
        _uiState.update {
            it.copy(
                timer = timeValue,
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
                selectionNumbers = selectionNumbers
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
        val digitNumber = cell[2]
        val isDeletable = cell[0] == 0
        if (digitNumber != 0 && isDeletable) {
            recordStep()
            selectionNumbers[digitNumber - 1]++
            grid[row][column][2] = 0
            stepsToGo++

            _uiState.update {
                it.copy(
                    selectionNumbers = selectionNumbers,
                    matrix = grid,
                    stepsToGo = stepsToGo,
                )
            }
        }
    }

    fun onUndo() {
        val selectionNumbers = uiState.value.selectionNumbers
        val grid = uiState.value.matrix
        val steps = uiState.value.steps.toMutableList()
        var stepsToGo = uiState.value.stepsToGo
        val step = steps.last()
        val currentCellDigit = grid[step.xIndex][step.yIndex][2]

        if (step.digit != currentCellDigit) {
            if (step.digit != 0 && currentCellDigit == 0) {
                stepsToGo--
                selectionNumbers[step.digit - 1]--
            } else if (step.digit == 0) {
                stepsToGo++
                selectionNumbers[currentCellDigit - 1]++
            } else {
                selectionNumbers[step.digit - 1]--
                selectionNumbers[currentCellDigit - 1]++
            }
        }

        grid[step.xIndex][step.yIndex][2] = step.digit
        steps.removeLast()

        _uiState.update {
            it.copy(
                selectionNumbers = selectionNumbers,
                matrix = grid,
                stepsToGo = stepsToGo,
                steps = steps,
                hasSteps = steps.size > 0,
            )
        }
    }

    private fun recordStep() {
        val steps = uiState.value.steps.toMutableList()
        val selectedCellRow = uiState.value.selectedCellRow
        val selectedCellColumn = uiState.value.selectedCellColumn
        val grid = uiState.value.matrix
        steps.add(
            Step(
                selectedCellRow,
                selectedCellColumn,
                grid[selectedCellRow][selectedCellColumn][2]
            )
        )
        _uiState.update {
            it.copy(
                steps = steps,
                hasSteps = true,
            )
        }
    }

    private fun unlockACell(grid: Array<Array<Array<Int>>>, unlockedCell: Array<Int?>, selectionNumbers: Array<Int>) {
        val cellId = getRandomNumber(GRID_SIZE * GRID_SIZE) - 1
        val i = cellId / GRID_SIZE
        val y = cellId % GRID_SIZE
        if (grid[i][y][0] == 0 && grid[i][y][2] == 0) {
            val digitToInsert = grid[i][y][1]
            grid[i][y][0] = digitToInsert
            grid[i][y][2] = digitToInsert
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

        if (selectedDigit != 0 && grid[row][column][0] == 0) {
            if (grid[row][column][1] != selectedDigit) {
                mistakesNum++
            } else {
                stepsToGo--
            }
            val isEmptyCell = grid[row][column][2] != 0
            val isNotCurrentValue = grid[row][column][2] != selectedDigit
            if (isEmptyCell && isNotCurrentValue)
            {
                selectionNumbers[grid[row][column][2] - 1]++

                if (grid[row][column][1] != selectedDigit) {
                    stepsToGo++
                }
            }

            selectionNumbers[selectedDigit - 1]--

            grid[row][column][2] = selectedDigit

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


    private fun getRandomNumber(multiplyBy: Int): Int {
        return floor((Math.random() * multiplyBy + 1)).toInt()
    }
}