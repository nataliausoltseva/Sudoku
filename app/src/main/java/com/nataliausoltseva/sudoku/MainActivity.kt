package com.nataliausoltseva.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nataliausoltseva.sudoku.ui.theme.SudokuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SudokuTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SudokuGrid()
                        RestartButton()
                        SelectionNumbers()
                    }
                }
            }
        }
    }
}

@Composable
fun SudokuGrid(
    sudokuViewModel: SudokuViewModel = viewModel()
) {
    val sudokuUIState by sudokuViewModel.uiState.collectAsState()
    val grid: Array<Array<MutableIntState>> = sudokuUIState.usersMatrix
    val filledGrid: Array<Array<MutableIntState>> = sudokuUIState.filledMatrix
    val initialGrid: Array<Array<MutableIntState>> = sudokuUIState.matrix

    LazyVerticalGrid(
        columns = GridCells.Fixed(9),
        verticalArrangement = Arrangement.Center,
        content = {
            items(81) {index ->
                val rowIndex = index / 9
                val columnIndex = index % 9
                val gridValue = grid[rowIndex][columnIndex].intValue
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    onClick = { sudokuViewModel.onSelectCell(rowIndex, columnIndex) }
                ) {
                    var displayValue = ""
                    if (gridValue != 0) {
                        displayValue = gridValue.toString()
                    }

                    val expectedValue = filledGrid[rowIndex][columnIndex].intValue
                    val initialGridValue = initialGrid[rowIndex][columnIndex].intValue

                    var colour = Color.White

                    // If inserted value is not the expected value then a user made a mistake
                    if (gridValue != 0 && expectedValue != gridValue) {
                        colour = Color.Red

                    // Apply different colour to all editable values
                    } else if (initialGridValue == 0) {
                        colour = Color.Cyan
                    }

                    Text(
                        text = displayValue,
                        Modifier.padding(20.dp),
                        color = colour
                    )
                }
            }
        }
    )
}

@Composable
fun RestartButton(
    sudokuViewModel: SudokuViewModel = viewModel()
) {
    Button(
        onClick = { sudokuViewModel.onRegenerate() },
    ) {
        Text("Regenerate")
    }
}

@Composable
fun SelectionNumbers(
    sudokuViewModel: SudokuViewModel = viewModel()
) {
    val sudokuUIState by sudokuViewModel.uiState.collectAsState()
    Row {
        for (i in 0 until 9) {
            val label = i + 1
            val isAvailable = sudokuUIState.selectionNumbers[i].intValue > 0
            var labelColor = MaterialTheme.colorScheme.secondaryContainer
            if (isAvailable) {
                labelColor = MaterialTheme.colorScheme.tertiaryContainer
            }
            Surface(
                color = labelColor,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                onClick = { sudokuViewModel.onSelection(label) },
                enabled = isAvailable
            ) {
                val displayValue = label.toString()
                Text(
                    text = displayValue,
                    Modifier.padding(20.dp)
                )
            }
        }
    }
}
