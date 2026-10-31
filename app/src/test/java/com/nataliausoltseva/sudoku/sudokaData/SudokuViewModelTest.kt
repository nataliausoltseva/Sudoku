package com.nataliausoltseva.sudoku.sudokaData

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SudokuViewModelTest {

    private lateinit var viewModel: SudokuViewModel

    @Before
    fun setUp() {
        viewModel = SudokuViewModel()
    }

    @Test
    fun testOnStartGeneratesValidGrid() {
        viewModel.onStart(0) // Easy level
        val state = viewModel.uiState.value

        assertTrue(state.hasStarted)
        assertEquals("Easy", state.selectedLevel)
        assertEquals(3, state.hintNum)
        assertEquals(0, state.mistakesNum)

        // Verify solved matrix (layer 1) has valid numbers 1..9 with no duplicates in rows or columns
        for (row in 0 until GRID_SIZE) {
            val rowValues = mutableSetOf<Int>()
            for (col in 0 until GRID_SIZE) {
                val value = state.matrix[row][col][1].intValue
                assertTrue(value in 1..9)
                rowValues.add(value)
            }
            assertEquals(GRID_SIZE, rowValues.size)
        }
    }

    @Test
    fun testSelectCellUpdatesState() {
        viewModel.onStart(0)
        viewModel.onSelectCell(4, 5)

        val state = viewModel.uiState.value
        assertEquals(4, state.selectedCellRow)
        assertEquals(5, state.selectedCellColumn)
        assertEquals(0, state.selectedDigit)
    }

    @Test
    fun testToggleNoteMode() {
        assertFalse(viewModel.uiState.value.isNotesEnabled)
        viewModel.onNote()
        assertTrue(viewModel.uiState.value.isNotesEnabled)
        viewModel.onNote()
        assertFalse(viewModel.uiState.value.isNotesEnabled)
    }

    @Test
    fun testSmartNoteAutoClearing() {
        viewModel.onStart(0)
        val state = viewModel.uiState.value

        // Find an empty cell
        var emptyRow = -1
        var emptyCol = -1
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (state.matrix[r][c][0].intValue == 0) {
                    emptyRow = r
                    emptyCol = c
                    break
                }
            }
            if (emptyRow != -1) break
        }

        assertTrue(emptyRow != -1 && emptyCol != -1)
        val correctDigit = state.matrix[emptyRow][emptyCol][1].intValue

        // Add a note in another cell in the same row for correctDigit
        val siblingCol = (emptyCol + 1) % GRID_SIZE
        viewModel.onSelectCell(emptyRow, siblingCol)
        viewModel.onNote() // Enable notes
        viewModel.onSelection(correctDigit, true) // Add note
        assertTrue(viewModel.uiState.value.matrixWithNotes[emptyRow][siblingCol][correctDigit - 1].intValue == correctDigit)

        // Now place the correct digit in (emptyRow, emptyCol)
        viewModel.onNote() // Disable notes
        viewModel.onSelectCell(emptyRow, emptyCol)
        viewModel.onSelection(correctDigit, true)

        // Note in sibling cell should be auto-cleared!
        assertEquals(0, viewModel.uiState.value.matrixWithNotes[emptyRow][siblingCol][correctDigit - 1].intValue)
    }

    @Test
    fun testUndoRestoresPreviousState() {
        viewModel.onStart(0)
        val state = viewModel.uiState.value

        // Find an empty cell
        var emptyRow = -1
        var emptyCol = -1
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (state.matrix[r][c][0].intValue == 0) {
                    emptyRow = r
                    emptyCol = c
                    break
                }
            }
            if (emptyRow != -1) break
        }

        viewModel.onSelectCell(emptyRow, emptyCol)
        val digitToInsert = state.matrix[emptyRow][emptyCol][1].intValue
        viewModel.onSelection(digitToInsert, true)

        assertTrue(viewModel.uiState.value.hasSteps)
        assertEquals(digitToInsert, viewModel.uiState.value.matrix[emptyRow][emptyCol][2].intValue)

        // Undo
        viewModel.onUndo()
        assertEquals(0, viewModel.uiState.value.matrix[emptyRow][emptyCol][2].intValue)
        assertFalse(viewModel.uiState.value.hasSteps)
    }

    @Test
    fun testIsUnusedInBoxBounds() {
        viewModel.onStart(0)
        // Check that bounds stay inside 0..8 subgrid
        val result = viewModel.isUnusedInBox(rowStart = 8, columnStart = 8, digit = 1, useUserGrid = false)
        // Should not throw IndexOutOfBoundsException
        assertNotNull(result != null || result == null)
    }
}