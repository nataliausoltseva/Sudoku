package com.nataliausoltseva.sudoku

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.floor

private const val GRID_SIZE = 9
private const val GRID_SIZE_SQUARE_ROOT = 3

val LEVELS = arrayOf("Easy", "Medium", "Hard", "Expert", "Master")
private val NUM_TO_REMOVE = arrayOf(1, 2, 4, 6, 7)

class SudokuViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(GameUIState())
    val uiState: StateFlow<GameUIState> = _uiState.asStateFlow()
    private var grid = Array(GRID_SIZE) { Array(GRID_SIZE) { mutableIntStateOf(0) } }
    private var filledGrid = Array(GRID_SIZE) { Array(GRID_SIZE) { mutableIntStateOf(0) } }
    private var usersGrid = Array(GRID_SIZE) { Array(GRID_SIZE) { mutableIntStateOf(0) } }
    private var selectionNumbers: Array<MutableIntState> = Array(9) { mutableIntStateOf(0) }
    private var selectedDigit: Int = 0
    private var selectedCellRow: Int? = null
    private var selectedCellColumn: Int? = null
    private var mistakesNum: MutableIntState = mutableIntStateOf(0)
    private var selectedLevel: MutableState<String> = mutableStateOf("Easy")
    private var numToRemove: Int = NUM_TO_REMOVE[0]
    private var hasStarted: MutableState<Boolean> = mutableStateOf(false)

    private fun fillGrid() {
        fillDiagonally()
        fillRemaining(0, GRID_SIZE_SQUARE_ROOT)
        removeDigits()
        updateState()
    }

    private fun fillDiagonally() {
        for (i in 0 until GRID_SIZE step GRID_SIZE_SQUARE_ROOT) {
            fillBox(i, i)
        }
    }

    private fun fillBox(row: Int, column: Int) {
        var generatedDigit: Int

        for (i in 0 until GRID_SIZE_SQUARE_ROOT) {
            for (j in 0 until GRID_SIZE_SQUARE_ROOT) {
                do {
                    generatedDigit = getRandomNumber(GRID_SIZE)
                } while (!isUnusedInBox(row, column, generatedDigit))

                grid[row + i][column + j].intValue = generatedDigit
                filledGrid[row + i][column + j].intValue = generatedDigit
                usersGrid[row + i][column + j].intValue = generatedDigit
            }
        }
    }

    private fun isUnusedInBox(rowStart: Int, columnStart: Int, digit: Int) : Boolean {
        for (i in 0 until GRID_SIZE_SQUARE_ROOT) {
            for (j in 0 until GRID_SIZE_SQUARE_ROOT) {
                if (grid[rowStart + i][columnStart + j].intValue == digit) {
                    return false
                }
            }
        }
        return true
    }

    private fun fillRemaining(i: Int, j: Int) : Boolean {
        var i = i
        var j = j
        if (j >= GRID_SIZE && i < GRID_SIZE - 1) {
            i += 1
            j = 0
        }

        if (i >= GRID_SIZE && j >= GRID_SIZE) {
            return true
        }

        if (i < GRID_SIZE_SQUARE_ROOT) {
            if (j < GRID_SIZE_SQUARE_ROOT) {
                j = GRID_SIZE_SQUARE_ROOT
            }
        } else if (i < GRID_SIZE - GRID_SIZE_SQUARE_ROOT) {
            if (j == (i / GRID_SIZE_SQUARE_ROOT) * GRID_SIZE_SQUARE_ROOT) {
                j += GRID_SIZE_SQUARE_ROOT
            }
        } else {
            if (j == GRID_SIZE - GRID_SIZE_SQUARE_ROOT) {
                i += 1
                j = 0
                if (i >= GRID_SIZE) {
                    return true
                }
            }
        }

        for (num in 1..GRID_SIZE) {
            if (checkIfSafe(i, j, num)) {
                grid[i][j].intValue = num
                filledGrid[i][j].intValue = num
                usersGrid[i][j].intValue = num
                if (fillRemaining(i, j + 1)) {
                    return true
                }
                grid[i][j].intValue = 0
                usersGrid[i][j].intValue = 0
            }
        }
        return false
    }

    private fun checkIfSafe(row: Int, column: Int, digit: Int) : Boolean {
        return isUnusedInRow(row, digit) &&
                isUnusedInColumn(column, digit) &&
                isUnusedInBox(
                    row - row % GRID_SIZE_SQUARE_ROOT,
                    column - column % GRID_SIZE_SQUARE_ROOT,
                    digit
                )
    }

    private fun isUnusedInRow(row: Int, digit: Int) : Boolean {
        for (i in 0 until GRID_SIZE) {
            if (grid[row][i].intValue == digit) {
                return false
            }
        }
        return true
    }

    private fun isUnusedInColumn(column: Int, digit: Int) : Boolean {
        for (i in 0 until GRID_SIZE) {
            if (grid[i][column].intValue == digit) {
                return false
            }
        }

        return true
    }

    private fun removeDigits() {
        for (j in 0 until GRID_SIZE) {
            val level = numToRemove
            for (k in 0 until (level..GRID_SIZE).random()) {
                val i = getRandomUniqueCell(j)

                if (grid[i][j].intValue != 0) {
                    val currentValue = grid[i][j].intValue
                    selectionNumbers[currentValue - 1].intValue += 1
                    grid[i][j].intValue = 0
                    usersGrid[i][j].intValue = 0
                }
            }
        }
    }

    private fun getRandomUniqueCell(column: Int): Int {
        val cellId = getRandomNumber(GRID_SIZE * GRID_SIZE) - 1
        val i = cellId / GRID_SIZE
        if (grid[i][column].intValue == 0) {
            getRandomUniqueCell(column)
        }

        return i
    }

    fun onRegenerate() {
        hasStarted.value = false
        updateState()
    }

    fun onSelection(digit: Int) {
        selectedDigit = digit
        insertDigit()
    }

    fun onSelectCell(row: Int, column: Int) {
        selectedCellRow = row
        selectedCellColumn = column
        insertDigit()
    }

    fun onLevelSelect(index: Int) {
        selectedLevel.value = LEVELS[index]
        numToRemove = NUM_TO_REMOVE[index]
        fillGrid()
    }

    fun onStart(index: Int) {
        hasStarted.value = true
        grid = Array(GRID_SIZE) { Array(GRID_SIZE) { mutableIntStateOf(0) } }
        usersGrid = Array(GRID_SIZE) { Array(GRID_SIZE) { mutableIntStateOf(0) } }
        selectionNumbers = Array(9) { mutableIntStateOf(0) }
        mistakesNum = mutableIntStateOf(0)
        onLevelSelect(index)
    }

    private fun insertDigit() {
        if (selectedDigit != 0  && selectedCellRow != null && selectedCellColumn != null && grid[selectedCellRow!!][selectedCellColumn!!].intValue == 0) {
            if (filledGrid[selectedCellRow!!][selectedCellColumn!!].intValue != selectedDigit) {
                mistakesNum.intValue++
            }

            usersGrid[selectedCellRow!!][selectedCellColumn!!].intValue = selectedDigit
            selectionNumbers[selectedDigit - 1].intValue -= 1
            selectedDigit = 0
        }
        updateState()
    }

    private fun updateState() {
        _uiState.value = GameUIState(
            hasStarted,
            matrix = grid,
            usersMatrix = usersGrid,
            filledMatrix = filledGrid,
            selectionNumbers,
            selectedDigit,
            selectedCellRow,
            selectedCellColumn,
            mistakesNum,
            selectedLevel
        )
    }

    private fun getRandomNumber(multiplyBy: Int): Int {
        return floor((Math.random() * multiplyBy + 1)).toInt()
    }
}