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

    // This is used to know how many numbers we need to remove from the grid. It resets when a new
    // game starts.
    private var numToRemove: Int = NUM_TO_REMOVE[0]

    /**
     * If a 3x3 box includes a digit that is going to be inserted, then it returns the cell where that
     * digit is located. Else returns null.
     */
    fun isUnusedInBox(
        rowStart: Int = uiState.value.selectedCellRow - uiState.value.selectedCellRow % GRID_SIZE_SQUARE_ROOT,
        columnStart: Int = uiState.value.selectedCellColumn - uiState.value.selectedCellColumn  % GRID_SIZE_SQUARE_ROOT,
        digit: Int = uiState.value.selectedDigit,
        grid: Array<Array<Array<MutableIntState>>> = uiState.value.matrix,
        useUserGrid: Boolean = true
    ): IntArray? {
        for (i in 0 until GRID_SIZE_SQUARE_ROOT) {
            for (j in 0 until GRID_SIZE_SQUARE_ROOT) {
                if ((useUserGrid && grid[rowStart + i][columnStart + j][2].intValue == digit ) ||
                    grid[rowStart + i][columnStart + j][0].intValue == digit
                ) {
                    return intArrayOf(rowStart + i, columnStart + j)
                }
            }
        }
        return null
    }

    /**
     * If a horizontal row includes a digit that is going to be inserted, then it returns the cell where that
     * digit is located. Else returns null.
     */
    fun isUnusedInRow(
        row: Int = uiState.value.selectedCellRow,
        digit: Int = uiState.value.selectedDigit,
        grid: Array<Array<Array<MutableIntState>>> = uiState.value.matrix,
        useUserGrid: Boolean = true
    ) : IntArray? {
        for (i in 0 until GRID_SIZE) {
            if ((useUserGrid && grid[row][i][2].intValue == digit) || grid[row][i][0].intValue == digit) {
                return intArrayOf(row, i)
            }
        }
        return null
    }

    /**
     * If a vertical row includes a digit that is going to be inserted, then it returns the cell where that
     * digit is located. Else returns null.
     */
    fun isUnusedInColumn(
        column: Int = uiState.value.selectedCellColumn,
        digit: Int = uiState.value.selectedDigit,
        grid: Array<Array<Array<MutableIntState>>> = uiState.value.matrix,
        useUserGrid: Boolean = true
    ) : IntArray? {
        for (i in 0 until GRID_SIZE) {
            if ((useUserGrid && grid[i][column][2].intValue == digit) || grid[i][column][0].intValue == digit) {
                return intArrayOf(i, column)
            }
        }

        return null
    }

    /**
     * When the game is finished either by losing or winning, the state is set to default so that
     * the welcome dialog can be shown.
     */
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

    /**
     * Saves the selected digit that was getting inserted. If it can be inserted, then include in the
     * grid. Else UI will show the animation for other cells with the same digits.
     */
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

    /**
     * Checks if a selected digit can be inserted into a cell when notes is enabled. Uses the
     * following functions to determine if the value can be inserted:
     * @see isUnusedInRow
     * @see isUnusedInColumn
     * @see isUnusedInBox
     */
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

    /**
     * Saves coordinates of currently selected cell, also resets selected digit, so that UI
     * would not trigger animation.
     */
    fun onSelectCell(row: Int, column: Int) {
        _uiState.update {
            it.copy(
                selectedCellRow = row,
                selectedCellColumn = column,
                selectedDigit = 0
            )
        }
    }

    /**
     * Begins the new game by resetting the state to the default values, followed by calling fillGrid()
     * @see fillGrid
     */
    fun onStart(index: Int) {
        _uiState.update {
            it.copy(
                selectedLevel = LEVELS[index],
                hasStarted = true,
                matrix = Array(GRID_SIZE) { Array(GRID_SIZE) { Array(3) { mutableIntStateOf(0) }  } },
                selectionNumbers = Array(9) { 0 },
                mistakesNum = 0,
                hasStepsToGo = true,
                hintNum = 3,
                unlockedCell = arrayOf(null, null),
                steps = listOf(),
                selectedCellRow = 0,
                selectedCellColumn = 0,
                isNotesEnabled = false,
                matrixWithNotes = Array(9) { Array(9) { Array(9) { mutableIntStateOf(0) }  } },
                selectedDigit = 0,
                hasSteps = false,
            )
        }
        numToRemove = NUM_TO_REMOVE[index]
        fillGrid()
    }

    /**
     * Saves the state when the game was paused.
     */
    fun onPause() {
        _uiState.update {
            it.copy(
                isPaused = true,
            )
        }
    }

    /**
     * Saves the state when the game was resumed.
     */
    fun onStart() {
        _uiState.update {
            it.copy(
                isPaused = false,
            )
        }
    }

    /**
     * Gets a random cell to insert a hint (correct value from the solved grid).
     */
    fun useHint() {
        var hintNum = uiState.value.hintNum
        val grid = uiState.value.matrix
        val selectionNumbers = uiState.value.selectionNumbers
        val unlockedCell = uiState.value.unlockedCell

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

    /**
     * Checks if a value can be erased i.e. if the initial grid has that cell set to 0. This is applied
     * to both normal and notes enabled modes. After the value is removed, it is stored as a step.
     */
    fun onErase() {
        val grid = uiState.value.matrix
        val row = uiState.value.selectedCellRow
        val column = uiState.value.selectedCellColumn
        val selectionNumbers = uiState.value.selectionNumbers
        val cell = grid[row][column]
        val digitNumber = cell[2].intValue
        val isDeletable = cell[0].intValue == 0
        var digit: Int? = null
        val previousCellDigit = grid[row][column][2].intValue
        if (digitNumber != 0 && isDeletable) {
            selectionNumbers[digitNumber - 1]++
            grid[row][column][2].intValue = 0
            digit = 0

            _uiState.update {
                it.copy(
                    selectionNumbers = selectionNumbers,
                    matrix = grid,
                )
            }
        }

        val gridWithNotes = uiState.value.matrixWithNotes
        val previousCellWithNote = uiState.value.matrixWithNotes[row][column]
        gridWithNotes[row][column].forEach {
            if (it.intValue == 0) return@forEach
            it.intValue = 0
        }

        // Saves the erase step so that it would be part of reverting steps
        recordStep(
            cellWithNote = Array(9) { mutableIntStateOf(0) },
            cellDigit = digit,
            previousCellDigit = previousCellDigit,
            previousCellWithNote = previousCellWithNote
        )

        _uiState.update {
            it.copy(matrixWithNotes = gridWithNotes, selectedDigit = 0)
        }
    }

    /**
     * Reverts the state of the user's grid to the previous step that was recorded from the user's
     * activity. It also removes the last step from the record.
     */
    fun onUndo() {
        val steps = uiState.value.steps.toMutableList()
        val selectionNumbers = uiState.value.selectionNumbers
        var selectedCellRow = uiState.value.selectedCellRow
        var selectedCellColumn = uiState.value.selectedCellColumn
        val grid = uiState.value.matrix
        val gridWithNotes = uiState.value.matrixWithNotes

        val step = steps.last()

        // If the digit and previous digit are both sets, then the step did not include notes and was
        // a normal mode insertion.
        if (step.digit != null && step.previousDigit != null) {
            val digit = (step.digit - 1).coerceAtLeast(0)
            selectionNumbers[digit]++
            grid[selectedCellRow][selectedCellColumn][2].intValue = step.previousDigit

        // Else the step included insertion for the notes enabled mode.
        } else {
            gridWithNotes[selectedCellRow][selectedCellColumn].forEachIndexed { index, _ ->
                gridWithNotes[selectedCellRow][selectedCellColumn][index].intValue = step.previousNotes[index].intValue
            }
        }

        steps.removeLast()

        // If there are still steps available after removing previous step, then set the selected
        // cell to that one.
        if (steps.size > 0) {
            val previousStep = steps.last()
            selectedCellRow = previousStep.xIndex
            selectedCellColumn = previousStep.yIndex

        //  Else set the selected cell to the beginning of the game - the first cell on the grid.
        } else {
            selectedCellRow = 0
            selectedCellColumn = 0
        }

        _uiState.update {
            it.copy(
                selectionNumbers = selectionNumbers,
                matrix = grid,
                steps = steps,
                hasSteps = steps.size > 0,
                matrixWithNotes = gridWithNotes,
                selectedCellRow = selectedCellRow,
                selectedCellColumn = selectedCellColumn,
                selectedDigit = 0,
            )
        }
    }

    /**
     * Toggles notes enabled state on and off.
     */
    fun onNote() {
        _uiState.update {
            it.copy(
                isNotesEnabled = !uiState.value.isNotesEnabled,
                selectedDigit = 0,
            )
        }
    }

    /**
     * Fills the grid fully and then removes some digits randomly based on the level of a game.
     */
    private fun fillGrid() {
        val grid = uiState.value.matrix
        val selectionNumbers = uiState.value.selectionNumbers
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

    /**
     * Fills 3x3 box diagonally.
     */
    private fun fillDiagonally(grid: Array<Array<Array<MutableIntState>>>) {
        for (i in 0 until GRID_SIZE step GRID_SIZE_SQUARE_ROOT) {
            fillBox(i, i, grid)
        }
    }

    /**
     * Fills 3x3 box making sure that no number is duplicated.
     */
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

    /**
     * After filling the 3x3 boxes, there is a chance that not every cell was filled due to the checks
     * and skips. Hence this goes over and fills all.
     */
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

    /**
     * Uses the following functions to see if it is safe to insert a digit into a cell.
     * @see isUnusedInRow
     * @see isUnusedInColumn
     * @see isUnusedInBox
     */
    private fun checkIfSafe(row: Int, column: Int, digit: Int, grid: Array<Array<Array<MutableIntState>>>, useUserGrid: Boolean = false) : Boolean {
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

    /**
     * Goes over the whole board vertically and returns a random digit by getting a random cell.
     * The harder the level, the more digits might be removed as the range goes up. There is still
     * a chance that the hardest level might be easier than medium.
     * @see getRandomUniqueCell how the random x value is determined
     */
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

    /**
     * Get a random row value when removing a digit. The value is returned if the cell with that row
     * and column do not result in 0.
     */
    private fun getRandomUniqueCell(column: Int, grid: Array<Array<Array<MutableIntState>>>): Int {
        val cellId = getRandomNumber(GRID_SIZE * GRID_SIZE) - 1
        val i = cellId / GRID_SIZE
        if (grid[i][column][0].intValue == 0) {
            getRandomUniqueCell(column, grid)
        }

        return i
    }

    /**
     * Records current step by adding to the steps list. After the first step is added, the button
     * on UI activates to indicate that a user can go back to the previous step.
     */
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

    /**
     * Recursively gets a random cell value until that cell value is 0 in both initial and user's grids.
     * To make sure that we are only animating cells that a user didn't fill and values that are
     * possible to fill by a user.
     */
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

    /**
     * Inserts a selected digit into user's grid only if the selected cell's value in the initial grid
     * is 0. Records a step after that.
     */
    private fun insertDigit(selectedDigit: Int = 0) {
        val grid = uiState.value.matrix
        val row = uiState.value.selectedCellRow
        val column = uiState.value.selectedCellColumn
        val selectionNumbers = uiState.value.selectionNumbers
        var mistakesNum = uiState.value.mistakesNum
        val previousCellDigit: Int?
        val isNotCurrentValue = grid[row][column][2].intValue != selectedDigit
        if (grid[row][column][0].intValue == 0 && isNotCurrentValue) {
            previousCellDigit = grid[row][column][2].intValue
            if (grid[row][column][1].intValue != selectedDigit) {
                mistakesNum++
            }

            val isEmptyCell = grid[row][column][2].intValue != 0
            if (isEmptyCell) {
                selectionNumbers[grid[row][column][2].intValue - 1]++
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
                    hasStepsToGo = selectionNumbers.any { number: Int -> number != 0 }
                )
            }
        }
    }

    /**
     * Inserts a selected digit into user's grid as note only if the selected cell's value in the initial grid
     * is 0. Records a step after that.
     *
     * Note: only values that are possible to insert for notes are going to reach this function
     * @see canInsert
     */
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


    /**
     * Gets a random number to help with randomising the cell when filling.
     */
    private fun getRandomNumber(multiplyBy: Int): Int {
        return floor((Math.random() * multiplyBy + 1)).toInt()
    }
}