package com.nataliausoltseva.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nataliausoltseva.sudoku.ui.theme.SudokuTheme
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SudokuTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp(
    sudokuViewModel: SudokuViewModel = viewModel()
) {
    val sudokuUIState by sudokuViewModel.uiState.collectAsState()
    val hasStarted = sudokuUIState.hasStarted.value

    if (!hasStarted) {
        WelcomeDialog()
    } else {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MistakeCounter()
                Timer()
                RestartButton()
            }
            SudokuGrid()
            SelectionNumbers()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeDialog(
    sudokuViewModel: SudokuViewModel = viewModel()
) {
    AlertDialog(
        onDismissRequest = {},
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onPrimary),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(6.dp)
        ) {
            Text(
                text = "Choose Your Puzzle:",
                fontWeight = FontWeight.Bold
            )
            for (i in LEVELS.indices) {
                TextButton(
                    onClick = { sudokuViewModel.onStart(i) },
                ) {
                    Text(LEVELS[i])
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakeCounter(
    sudokuViewModel: SudokuViewModel = viewModel()
) {
    val sudokuUIState by sudokuViewModel.uiState.collectAsState()
    val mistakesNum: Int = sudokuUIState.mistakesNum.intValue

    Text(text = "Mistakes: $mistakesNum/3")

    if (mistakesNum == 3) {
        AlertDialog(
            onDismissRequest = {},
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onPrimary),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(6.dp)
            ) {
                Text(
                    text = "Game Over",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "You made 3 mistakes and lost this game.",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                )
                TextButton(
                    onClick = { sudokuViewModel.onRegenerate() },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("Restart")
                }
            }
        }
    }
}

@Composable
fun Timer() {
    val timerConverter = remember { mutableStateOf("") }
    val loadedCurrentMillis = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(key1 = timerConverter.value) {
        delay(1000)
        timerConverter.value =
            converting(System.currentTimeMillis() - loadedCurrentMillis.longValue)
    }
    Text(
        text = timerConverter.value,
        Modifier.padding(10.dp)
    )
}

fun converting(millis: Long): String =
    String.format(
        "%02d:%02d:%02d",
        TimeUnit.MILLISECONDS.toHours(millis),
        TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
            TimeUnit.MILLISECONDS.toHours(millis)
        ),
        TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
            TimeUnit.MILLISECONDS.toMinutes(millis)
        )
    )


@Composable
fun SudokuGrid(
    sudokuViewModel: SudokuViewModel = viewModel()
) {
    val sudokuUIState by sudokuViewModel.uiState.collectAsState()
    val grid: Array<Array<MutableIntState>> = sudokuUIState.usersMatrix
    val filledGrid: Array<Array<MutableIntState>> = sudokuUIState.filledMatrix
    val initialGrid: Array<Array<MutableIntState>> = sudokuUIState.matrix
    val selectedCellRow: Int? = sudokuUIState.selectedCellRow;
    val selectedCellColumn: Int? = sudokuUIState.selectedCellColumn;

    LazyVerticalGrid(
        columns = GridCells.Fixed(9),
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant),
        content = {
            items(81) {index ->
                val rowIndex = index / 9
                val columnIndex = index % 9
                val gridValue = grid[rowIndex][columnIndex].intValue
                val cellRowIndex = index % 3
                val isCurrentCell = selectedCellRow == rowIndex && selectedCellColumn == columnIndex
                var currentGridCellValue = 0

                if (selectedCellRow != null && selectedCellColumn != null) {
                    currentGridCellValue = grid[selectedCellRow][selectedCellColumn].intValue
                }

                val backgroundCellColour = if (isCurrentCell) MaterialTheme.colorScheme.tertiary
                else if (gridValue > 0 && currentGridCellValue == gridValue) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.secondaryContainer;

                val outlineColour = MaterialTheme.colorScheme.inversePrimary

                val columnDividerColour = if (cellRowIndex != 0) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.outline

                val rowDividerColour = if (rowIndex % 3 != 0) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.outline

                Surface(
                    color = backgroundCellColour,
                    onClick = { sudokuViewModel.onSelectCell(rowIndex, columnIndex) },
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val canvasWidth = size.width
                            val canvasHeight = size.height

                            val hasVerticalOutline =
                                selectedCellColumn == columnIndex || selectedCellColumn == columnIndex - 1
                            val ignoreVerticalOutline = selectedCellColumn == null ||
                                    isCurrentCell ||
                                    (selectedCellColumn + 1 == columnIndex && rowIndex == selectedCellRow)
                            if (hasVerticalOutline && !ignoreVerticalOutline) {
                                drawLine(
                                    start = Offset(x = 0f, y = canvasHeight),
                                    end = Offset(x = 0f, y = 0f),
                                    color = outlineColour,
                                    strokeWidth = 4.dp.toPx()
                                )
                            } else {
                                drawLine(
                                    start = Offset(x = 0f, y = canvasHeight),
                                    end = Offset(x = 0f, y = 0f),
                                    color = columnDividerColour,
                                    strokeWidth = 2.dp.toPx()
                                )
                            }

                            val hasHorizontalOutline =
                                selectedCellRow == rowIndex || selectedCellRow == rowIndex - 1
                            val ignoreHorizontalOutline = isCurrentCell ||
                                    selectedCellRow == null ||
                                    (selectedCellRow + 1 == rowIndex && columnIndex == selectedCellColumn)
                            if (hasHorizontalOutline && !ignoreHorizontalOutline) {
                                drawLine(
                                    start = Offset(x = canvasWidth - 1.dp.toPx(), y = 0f),
                                    end = Offset(x = 0f, y = 0f),
                                    color = outlineColour,
                                    strokeWidth = 4.dp.toPx()
                                )
                            } else {
                                drawLine(
                                    start = Offset(x = canvasWidth - 1.dp.toPx(), y = 0f),
                                    end = Offset(x = 0f, y = 0f),
                                    color = rowDividerColour,
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        }
                ) {
                    var displayValue = ""
                    if (gridValue != 0) {
                        displayValue = gridValue.toString()
                    }

                    val expectedValue = filledGrid[rowIndex][columnIndex].intValue
                    val initialGridValue = initialGrid[rowIndex][columnIndex].intValue

                    val colour =  if (gridValue != 0 && expectedValue != gridValue) Color.Red
                        else if (isCurrentCell) MaterialTheme.colorScheme.onTertiary
                        else if (initialGridValue == 0) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSecondaryContainer

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
        Icon (
            Icons.Rounded.Refresh,
            contentDescription = "Refresh icon"
        )
    }
}

@Composable
fun SelectionNumbers(
    sudokuViewModel: SudokuViewModel = viewModel()
) {
    val sudokuUIState by sudokuViewModel.uiState.collectAsState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(9),
        modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 0.dp),
        content = {
            items(9) { i ->
                val label = i + 1
                val isAvailable = sudokuUIState.selectionNumbers[i].intValue > 0
                var labelColor = MaterialTheme.colorScheme.inversePrimary
                if (isAvailable) {
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                }

                Surface(
                    onClick = { sudokuViewModel.onSelection(label) },
                    enabled = isAvailable,
                ) {
                    val displayValue = label.toString()
                    Text(
                        text = displayValue,
                        Modifier.padding(20.dp),
                        color = labelColor
                    )
                }
            }
        }
    )
}
