package com.nataliausoltseva.sudoku

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.floor

private const val GRID_SIZE = 9
private const val GRID_SIZE_SQUARE_ROOT = 3

class SudokuViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(GameUIState())
    val uiState: StateFlow<GameUIState> = _uiState.asStateFlow()
    private var grid = Array(GRID_SIZE) { IntArray(GRID_SIZE) { 0 } }

    private fun fillGrid() {
        fillDiagonally()
        fillRemaining(0, GRID_SIZE_SQUARE_ROOT)
        removeDigits()
        _uiState.value = GameUIState(matrix = grid)
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

                grid[row + i][column + j] = generatedDigit
            }
        }
    }

    private fun isUnusedInBox(rowStart: Int, columnStart: Int, digit: Int) : Boolean {
        for (i in 0 until GRID_SIZE_SQUARE_ROOT) {
            for (j in 0 until GRID_SIZE_SQUARE_ROOT) {
                if (grid[rowStart + i][columnStart + j] == digit) {
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
                grid[i][j] = num
                if (fillRemaining(i, j + 1)) {
                    return true
                }

                grid[i][j] = 0
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
            if (grid[row][i] == digit) {
                return false
            }
        }
        return true
    }

    private fun isUnusedInColumn(column: Int, digit: Int) : Boolean {
        for (i in 0 until GRID_SIZE) {
            if (grid[i][column] == digit) {
                return false
            }
        }

        return true
    }

    private fun removeDigits() {
        var digitsToRemove = 20

        while (digitsToRemove > 0) {
            val cellId = getRandomNumber(GRID_SIZE * GRID_SIZE) - 1
            val i = cellId / GRID_SIZE
            var j = cellId % GRID_SIZE

            if (j != 0) {
                j -= 1
            }

            if (grid[i][j] != 0) {
                digitsToRemove--
                grid[i][j] = 0
            }
        }
    }

    fun onRegenerate() {
        grid = Array(GRID_SIZE) { IntArray(GRID_SIZE) { 0 } }
        fillGrid()
    }

    private fun getRandomNumber(multiplyBy: Int): Int {
        return floor((Math.random() * multiplyBy + 1)).toInt()
    }

    init {
        fillGrid()
    }
}