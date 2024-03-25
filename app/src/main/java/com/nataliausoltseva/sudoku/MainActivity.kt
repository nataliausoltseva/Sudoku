package com.nataliausoltseva.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.PartySystem
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainApp()
        }
    }
}

@Composable
fun MainApp(
    sudokuViewModel: SudokuViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val sudokuUIState by sudokuViewModel.uiState.collectAsState()
    val hasStarted = sudokuUIState.hasStarted.value
    val stepsToGo = sudokuUIState.stepsToGo.value
    val showSettings = remember { mutableStateOf(false) }
    val hasCounter by settingsViewModel.hasMistakeCounter.collectAsState()
    val theme by settingsViewModel.theme.collectAsState()
    val hasTimer by settingsViewModel.hasTimer.collectAsState()

    SudokuTheme(
        darkTheme = theme == "dark" || (theme == "system" && isSystemInDarkTheme())
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!hasStarted) {
                WelcomeDialog()
            } else {
                if (stepsToGo == 0) {
                    EndScreen()
                    KonfettiUI()
                } else {
                    Column(
                        horizontalAlignment = Alignment.End,
                    ) {
                        Surface(
                            onClick = { showSettings.value = !showSettings.value },
                            modifier = Modifier.padding(0.dp, 20.dp, 0.dp, 20.dp)
                        ) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Settings icon"
                            )
                        }
                        Settings(showSettings.value, onCancel = { showSettings.value = false })
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LevelIndicator()
                                if (hasCounter) {
                                    MistakeCounter()
                                }
                                if (hasTimer) {
                                    Timer()
                                }
                            }
                            if (stepsToGo > 0) {
                                SudokuGrid()
                                SelectionNumbers()
                                Row (
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RestartButton()
                                    Hints()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
fun KonfettiUI(viewModel: KonfettiViewModel = KonfettiViewModel()) {
    val state: KonfettiViewModel.State by viewModel.state.observeAsState(
        KonfettiViewModel.State.Idle,
    )

    viewModel.rain()

    when (val newState = state) {
        KonfettiViewModel.State.Idle -> {}
        is KonfettiViewModel.State.Started ->
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = newState.party,
                updateListener =
                object : OnParticleSystemUpdateListener {
                    override fun onParticleSystemEnded(
                        system: PartySystem,
                        activeSystems: Int,
                    ) {
                        if (activeSystems == 0) viewModel.ended()
                    }
                },
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeDialog(
    sudokuViewModel: SudokuViewModel = viewModel(),
    timerViewModel: TimerViewModel = viewModel(),
) {
    BasicAlertDialog(
        onDismissRequest = {},
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onPrimary)
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
                    onClick = {
                        sudokuViewModel.onStart(i)
                        timerViewModel.stopTimer()
                        timerViewModel.startTimer()
                    },
                ) {
                    Text(LEVELS[i])
                }
            }
        }
    }
}

@Composable
fun EndScreen(
    sudokuViewModel: SudokuViewModel = viewModel(),
    timerViewModel: TimerViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val sudokuUIState by sudokuViewModel.uiState.collectAsState()
    val timer = sudokuUIState.timer.value
    val hasTimer by settingsViewModel.hasTimer.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "You Won!",
            fontWeight = FontWeight.Bold
        )

        Row {
            Text(text = "Difficulty: ")
            LevelIndicator()
        }

        if (hasTimer) {
            Row {
                Text(text = "Time: ")
                Text(text = timerViewModel.formatTime(timer))
            }
        }

        TextButton(
            onClick = { sudokuViewModel.onRegenerate() },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("New Game")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    showSettings: Boolean,
    onCancel: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
    timerViewModel: TimerViewModel = viewModel(),
    ) {
    if (showSettings) {
        val showMistakes = remember { mutableStateOf(settingsViewModel.showMistakes.value) }
        val hasMistakesCount = remember { mutableStateOf(settingsViewModel.hasMistakeCounter.value) }
        val theme = remember { mutableStateOf(settingsViewModel.theme.value) }
        val hasHighlightSameNumbers = remember { mutableStateOf(settingsViewModel.hasHighlightSameNumbers.value) }
        val hasRowHighlight = remember { mutableStateOf(settingsViewModel.hasRowHighlight.value) }
        val hasTimer = remember { mutableStateOf(settingsViewModel.hasTimer.value) }

        BasicAlertDialog(
            onDismissRequest = {},
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onPrimary)
        ) {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(6.dp)
            ) {
                Column (
                    horizontalAlignment = Alignment.Start,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Show mistakes")
                        Switch(checked = showMistakes.value, onCheckedChange = { isChecked -> showMistakes.value = isChecked })
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Count mistakes")
                        Switch(checked = hasMistakesCount.value, onCheckedChange = { isChecked -> hasMistakesCount.value = isChecked})
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Highlight Same Numbers")
                        Switch(checked = hasHighlightSameNumbers.value, onCheckedChange = { isChecked -> hasHighlightSameNumbers.value = isChecked})
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Highlight Rows")
                        Switch(checked = hasRowHighlight.value, onCheckedChange = { isChecked -> hasRowHighlight.value = isChecked})
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Include Timer")
                        Switch(checked = hasTimer.value, onCheckedChange = { isChecked -> hasTimer.value = isChecked})
                    }
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "Theme:")
                        Column {
                            for (i in THEMES.indices) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = THEMES[i] == theme.value,
                                        onClick = { theme.value = THEMES[i]}
                                    )
                                    Text(text = THEMES[i])
                                }
                            }
                        }
                    }

                }
                Row {
                    TextButton(
                        onClick = { onCancel() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            settingsViewModel.onSave(
                                showMistakes.value,
                                hasMistakesCount.value,
                                theme.value,
                                hasHighlightSameNumbers.value,
                                hasRowHighlight.value,
                                hasTimer.value,
                            )
                            onCancel()

                            if (!hasTimer.value) {
                                timerViewModel.pauseTimer()
                            } else {
                                timerViewModel.startTimer()
                            }
                        },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun LevelIndicator(
    sudokuViewModel: SudokuViewModel = viewModel()
) {
    val sudokuUIState by sudokuViewModel.uiState.collectAsState()
    val currentLevel: MutableState<String> = sudokuUIState.selectedLevel
    Text(text = currentLevel.value)
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
        BasicAlertDialog(
            onDismissRequest = {},
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onPrimary)
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
fun Hints(
    sudokuViewModel: SudokuViewModel = viewModel(),
) {
    val sudokuUIState by sudokuViewModel.uiState.collectAsState()
    val hintNum: Int = sudokuUIState.hintNum.intValue
    Surface {
        Button(
            onClick = { sudokuViewModel.useHint() },
            modifier = Modifier.padding(8.dp),
            enabled = hintNum > 0,
        ) {
            Icon (
                Icons.Rounded.Lightbulb,
                contentDescription = "Hint icon"
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .absoluteOffset(55.dp, 0.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Text(
                text = hintNum.toString(),
                modifier = Modifier
                    .padding(10.dp, 3.dp, 10.dp, 3.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Timer(
    timerViewModel: TimerViewModel = viewModel(),
    sudokuViewModel: SudokuViewModel = viewModel()
) {
    val sudokuUIState by sudokuViewModel.uiState.collectAsState()
    val isPaused = sudokuUIState.isPaused.value
    val timerValue by timerViewModel.timer.collectAsState()
    sudokuViewModel.setTimer(timerValue)

    if (isPaused) {
        BasicAlertDialog(
            onDismissRequest = {},
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onPrimary)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(6.dp)
            ) {
                Text(
                    text = "Paused",
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        sudokuViewModel.onStart()
                        timerViewModel.startTimer()
                    },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("Continue")
                }
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = timerViewModel.formatTime(timerValue))
        Surface(
            onClick = {
                if (!isPaused) {
                    timerViewModel.pauseTimer()
                    sudokuViewModel.onPause()
                }
            }
        ) {
            val icon = if (isPaused) Icons.Filled.PlayArrow
            else Icons.Filled.Pause
            val iconDescription = if (isPaused) "Play icon" else "Pause icon"
            Icon(
                icon,
                contentDescription = iconDescription
            )
        }
    }
}

@Composable
fun SudokuGrid(
    sudokuViewModel: SudokuViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val sudokuUIState by sudokuViewModel.uiState.collectAsState()
    val grid: Array<Array<MutableIntState>> = sudokuUIState.usersMatrix
    val filledGrid: Array<Array<MutableIntState>> = sudokuUIState.filledMatrix
    val initialGrid: Array<Array<MutableIntState>> = sudokuUIState.matrix
    val selectedCellRow: Int? = sudokuUIState.selectedCellRow;
    val selectedCellColumn: Int? = sudokuUIState.selectedCellColumn;
    val isPaused = sudokuUIState.isPaused.value;
    val unlockedCell = sudokuUIState.unlockedCell;

    val showMistakes by settingsViewModel.showMistakes.collectAsState()
    val hasRowHighlight by settingsViewModel.hasRowHighlight.collectAsState()
    val hasHighlightSameNumbers by settingsViewModel.hasHighlightSameNumbers.collectAsState()

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
                    else if (gridValue > 0 && currentGridCellValue == gridValue && hasHighlightSameNumbers) MaterialTheme.colorScheme.tertiaryContainer
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
                            if (hasVerticalOutline && !ignoreVerticalOutline && hasRowHighlight) {
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
                            if (hasHorizontalOutline && !ignoreHorizontalOutline && hasRowHighlight) {
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
                    if (gridValue != 0 && !isPaused) {
                        displayValue = gridValue.toString()
                    }

                    val expectedValue = filledGrid[rowIndex][columnIndex].intValue
                    val initialGridValue = initialGrid[rowIndex][columnIndex].intValue

                    val colour =  if (gridValue != 0 && expectedValue != gridValue && showMistakes) Color.Red
                    else if (isCurrentCell) MaterialTheme.colorScheme.onTertiary
                    else if (initialGridValue == 0) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSecondaryContainer

                    val scale = remember { Animatable(1f) }
                    val isUnlockedCell = unlockedCell[0].value == rowIndex && unlockedCell[1].value == columnIndex
                    if (isUnlockedCell) {
                        LaunchedEffect(true) {
                            scale.animateTo(1f, animationSpec = tween(0))
                            scale.animateTo(3f, animationSpec = tween(350))
                            scale.animateTo(1f, animationSpec = tween(350))
                        }
                    }

                    Text(
                        text = displayValue,
                        Modifier
                            .padding(20.dp)
                            .graphicsLayer {
                                scaleX = scale.value
                                scaleY = scale.value
                                transformOrigin = TransformOrigin.Center
                            },
                        color = colour
                    )
                }
            }
        }
    )
}

@Composable
fun RestartButton(
    sudokuViewModel: SudokuViewModel = viewModel(),
    timerViewModel: TimerViewModel = viewModel(),
) {
    Button(
        onClick = {
            sudokuViewModel.onRegenerate()
            timerViewModel.stopTimer()
        },
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
