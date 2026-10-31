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

class SudokuViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SudokuState())
    val uiState: StateFlow<SudokuState> = _uiState.asStateFlow()

    private var numToRemove: Int = NUM_TO_REMOVE[0]

    fun isUnusedInBox(
        rowStart: Int = uiState.value.selectedCellRow - uiState.value.selectedCellRow % GRID_SIZE_SQUARE_ROOT,
        columnStart: Int = uiState.value.selectedCellColumn - uiState.value.selectedCellColumn % GRID_SIZE_SQUARE_ROOT,
        digit: Int = uiState.value.selectedDigit,
        grid: Array<Array<Array<MutableIntState>>> = uiState.value.matrix,
        useUserGrid: Boolean = true
    ): IntArray? {
        val rStart = rowStart - rowStart % GRID_SIZE_SQUARE_ROOT
        val cStart = columnStart - columnStart % GRID_SIZE_SQUARE_ROOT
        for (i in 0 until GRID_SIZE_SQUARE_ROOT) {
            for (j in 0 until GRID_SIZE_SQUARE_ROOT) {
                if ((useUserGrid && grid[rStart + i][cStart + j][2].intValue == digit) ||
                    grid[rStart + i][cStart + j][0].intValue == digit
                ) {
                    return intArrayOf(rStart + i, cStart + j)
                }
            }
        }
        return null
    }

    fun isUnusedInRow(
        row: Int = uiState.value.selectedCellRow,
        digit: Int = uiState.value.selectedDigit,
        grid: Array<Array<Array<MutableIntState>>> = uiState.value.matrix,
        useUserGrid: Boolean = true
    ): IntArray? {
        for (i in 0 until GRID_SIZE) {
            if ((useUserGrid && grid[row][i][2].intValue == digit) || grid[row][i][0].intValue == digit) {
                return intArrayOf(row, i)
            }
        }
        return null
    }

    fun isUnusedInColumn(
        column: Int = uiState.value.selectedCellColumn,
        digit: Int = uiState.value.selectedDigit,
        grid: Array<Array<Array<MutableIntState>>> = uiState.value.matrix,
        useUserGrid: Boolean = true
    ): IntArray? {
        for (i in 0 until GRID_SIZE) {
            if ((useUserGrid && grid[i][column][2].intValue == digit) || grid[i][column][0].intValue == digit) {
                return intArrayOf(i, column)
            }
        }
        return null
    }

    fun onRegenerate() {
        _uiState.update {
            it.copy(
                hasStarted = false,
                hintNum = 3,
                unlockedCell = arrayOf(null, null),
                steps = listOf(),
                selectedCellRow = 0,
                selectedCellColumn = 0,
                hasSteps = false,
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
                matrix = Array(GRID_SIZE) { Array(GRID_SIZE) { Array(3) { mutableIntStateOf(0) } } },
                selectionNumbers = Array(9) { 0 },
                mistakesNum = 0,
                hasStepsToGo = true,
                hintNum = 3,
                unlockedCell = arrayOf(null, null),
                steps = listOf(),
                selectedCellRow = 0,
                selectedCellColumn = 0,
                isNotesEnabled = false,
                matrixWithNotes = Array(GRID_SIZE) { Array(GRID_SIZE) { Array(9) { mutableIntStateOf(0) } } },
                selectedDigit = 0,
                hasSteps = false,
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
        if (hintNum <= 0) return
        val grid = uiState.value.matrix
        val selectionNumbers = uiState.value.selectionNumbers.copyOf()
        val unlockedCell = uiState.value.unlockedCell.copyOf()

        unlockACell(grid, unlockedCell, selectionNumbers)

        _uiState.update {
            it.copy(
                hintNum = --hintNum,
                hasStepsToGo = selectionNumbers.any { number: Int -> number != 0 },
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
        val selectionNumbers = uiState.value.selectionNumbers.copyOf()
        val cell = grid[row][column]
        val currentDigit = cell[2].intValue
        val isDeletable = cell[0].intValue == 0

        if (currentDigit != 0 && isDeletable) {
            selectionNumbers[currentDigit - 1]++
            grid[row][column][2].intValue = 0

            val previousCellWithNote = Array(9) { mutableIntStateOf(uiState.value.matrixWithNotes[row][column][it].intValue) }

            recordStep(
                cellWithNote = uiState.value.matrixWithNotes[row][column],
                previousCellWithNote = previousCellWithNote,
                cellDigit = 0,
                previousCellDigit = currentDigit
            )

            _uiState.update {
                it.copy(
                    selectionNumbers = selectionNumbers,
                    matrix = grid,
                    selectedDigit = 0,
                    hasStepsToGo = selectionNumbers.any { count -> count != 0 }
                )
            }
        } else {
            val gridWithNotes = uiState.value.matrixWithNotes
            val hasNotes = gridWithNotes[row][column].any { it.intValue != 0 }
            if (hasNotes) {
                val previousCellWithNote = Array(9) { mutableIntStateOf(gridWithNotes[row][column][it].intValue) }
                gridWithNotes[row][column].forEach { it.intValue = 0 }

                recordStep(
                    cellWithNote = gridWithNotes[row][column],
                    previousCellWithNote = previousCellWithNote,
                    cellDigit = null,
                    previousCellDigit = null
                )

                _uiState.update {
                    it.copy(matrixWithNotes = gridWithNotes, selectedDigit = 0)
                }
            }
        }
    }

    fun onUndo() {
        val steps = uiState.value.steps.toMutableList()
        if (steps.isEmpty()) return

        val selectionNumbers = uiState.value.selectionNumbers.copyOf()
        val grid = uiState.value.matrix
        val gridWithNotes = uiState.value.matrixWithNotes

        val step = steps.removeLast()
        val cellRow = step.xIndex
        val cellColumn = step.yIndex

        if (step.digit != null) {
            val insertedDigit = step.digit
            val previousDigit = step.previousDigit ?: 0

            grid[cellRow][cellColumn][2].intValue = previousDigit

            if (insertedDigit in 1..9) {
                selectionNumbers[insertedDigit - 1]++
            }
            if (previousDigit in 1..9) {
                selectionNumbers[previousDigit - 1]--
            }

            gridWithNotes[cellRow][cellColumn].forEachIndexed { index, _ ->
                gridWithNotes[cellRow][cellColumn][index].intValue = step.previousNotes[index].intValue
            }
        } else {
            gridWithNotes[cellRow][cellColumn].forEachIndexed { index, _ ->
                gridWithNotes[cellRow][cellColumn][index].intValue = step.previousNotes[index].intValue
            }
        }

        val nextSelectedRow = if (steps.isNotEmpty()) steps.last().xIndex else cellRow
        val nextSelectedColumn = if (steps.isNotEmpty()) steps.last().yIndex else cellColumn

        _uiState.update {
            it.copy(
                selectionNumbers = selectionNumbers,
                matrix = grid,
                steps = steps,
                hasSteps = steps.isNotEmpty(),
                matrixWithNotes = gridWithNotes,
                selectedCellRow = nextSelectedRow,
                selectedCellColumn = nextSelectedColumn,
                selectedDigit = 0,
                hasStepsToGo = selectionNumbers.any { count -> count != 0 }
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

    private fun fillGrid() {
        val grid = uiState.value.matrix
        val selectionNumbers = uiState.value.selectionNumbers.copyOf()
        fillDiagonally(grid)
        fillRemaining(0, GRID_SIZE_SQUARE_ROOT, grid)
        removeDigits(grid, selectionNumbers)
        _uiState.update {
            it.copy(
                matrix = grid,
                selectionNumbers = selectionNumbers,
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
                } while (isUnusedInBox(row, column, generatedDigit, grid, false) != null)

                grid[row + i][column + j] = Array(3) { mutableIntStateOf(generatedDigit) }
            }
        }
    }

    private fun fillRemaining(i: Int, j: Int, grid: Array<Array<Array<MutableIntState>>>): Boolean {
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

    private fun checkIfSafe(row: Int, column: Int, digit: Int, grid: Array<Array<Array<MutableIntState>>>, useUserGrid: Boolean = false): Boolean {
        return isUnusedInRow(row, digit, grid, useUserGrid) == null &&
                isUnusedInColumn(column, digit, grid, useUserGrid) == null &&
                isUnusedInBox(
                    row - row % GRID_SIZE_SQUARE_ROOT,
                    column - column % GRID_SIZE_SQUARE_ROOT,
                    digit,
                    grid,
                    useUserGrid
                ) == null
    }

    private fun removeDigits(grid: Array<Array<Array<MutableIntState>>>, selectionNumbers: Array<Int>) {
        for (j in 0 until GRID_SIZE) {
            val level = numToRemove
            for (k in 0 until (level..GRID_SIZE).random()) {
                val i = getRandomUniqueCell(j, grid)

                if (grid[i][j][0].intValue != 0) {
                    val currentValue = grid[i][j][0].intValue
                    selectionNumbers[currentValue - 1] += 1
                    grid[i][j][0].intValue = 0
                    grid[i][j][2].intValue = 0
                }
            }
        }
    }

    private fun getRandomUniqueCell(column: Int, grid: Array<Array<Array<MutableIntState>>>): Int {
        val nonZeroRows = (0 until GRID_SIZE).filter { grid[it][column][0].intValue != 0 }
        return if (nonZeroRows.isNotEmpty()) nonZeroRows.random() else (0 until GRID_SIZE).random()
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

        val prevNotesCopy = Array(9) { mutableIntStateOf(previousCellWithNote[it].intValue) }
        val curNotesCopy = Array(9) { mutableIntStateOf(cellWithNote[it].intValue) }

        steps.add(
            Step(
                xIndex = selectedCellRow,
                yIndex = selectedCellColumn,
                digit = cellDigit,
                previousDigit = previousCellDigit,
                previousNotes = prevNotesCopy,
                notes = curNotesCopy
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
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until GRID_SIZE) {
            for (j in 0 until GRID_SIZE) {
                if (grid[i][j][0].intValue == 0 && grid[i][j][2].intValue == 0) {
                    emptyCells.add(Pair(i, j))
                }
            }
        }
        if (emptyCells.isEmpty()) return

        val (i, y) = emptyCells.random()
        val digitToInsert = grid[i][y][1].intValue
        grid[i][y][0].intValue = digitToInsert
        grid[i][y][2].intValue = digitToInsert
        unlockedCell[0] = i
        unlockedCell[1] = y
        if (digitToInsert in 1..9) {
            selectionNumbers[digitToInsert - 1]--
        }
    }

    private fun autoClearNotes(row: Int, column: Int, digit: Int) {
        val gridWithNotes = uiState.value.matrixWithNotes
        val noteIndex = digit - 1

        for (c in 0 until GRID_SIZE) {
            gridWithNotes[row][c][noteIndex].intValue = 0
        }
        for (r in 0 until GRID_SIZE) {
            gridWithNotes[r][column][noteIndex].intValue = 0
        }
        val boxRowStart = row - row % GRID_SIZE_SQUARE_ROOT
        val boxColStart = column - column % GRID_SIZE_SQUARE_ROOT
        for (r in 0 until GRID_SIZE_SQUARE_ROOT) {
            for (c in 0 until GRID_SIZE_SQUARE_ROOT) {
                gridWithNotes[boxRowStart + r][boxColStart + c][noteIndex].intValue = 0
            }
        }
    }

    private fun insertDigit(selectedDigit: Int = 0) {
        val grid = uiState.value.matrix
        val row = uiState.value.selectedCellRow
        val column = uiState.value.selectedCellColumn
        val selectionNumbers = uiState.value.selectionNumbers.copyOf()
        var mistakesNum = uiState.value.mistakesNum
        val isNotCurrentValue = grid[row][column][2].intValue != selectedDigit

        if (grid[row][column][0].intValue == 0 && isNotCurrentValue) {
            val previousCellDigit = grid[row][column][2].intValue
            val isCorrect = grid[row][column][1].intValue == selectedDigit
            if (!isCorrect) {
                mistakesNum++
            } else if (selectedDigit in 1..9) {
                autoClearNotes(row, column, selectedDigit)
            }

            if (previousCellDigit in 1..9) {
                selectionNumbers[previousCellDigit - 1]++
            }

            if (selectedDigit in 1..9) {
                selectionNumbers[selectedDigit - 1]--
            }

            grid[row][column][2].intValue = selectedDigit

            val previousCellWithNote = Array(9) { mutableIntStateOf(uiState.value.matrixWithNotes[row][column][it].intValue) }

            recordStep(
                cellDigit = selectedDigit,
                previousCellDigit = previousCellDigit,
                previousCellWithNote = previousCellWithNote,
                cellWithNote = uiState.value.matrixWithNotes[row][column],
            )

            _uiState.update {
                it.copy(
                    matrix = grid,
                    selectionNumbers = selectionNumbers,
                    mistakesNum = mistakesNum,
                    hasStepsToGo = selectionNumbers.any { number: Int -> number != 0 }
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