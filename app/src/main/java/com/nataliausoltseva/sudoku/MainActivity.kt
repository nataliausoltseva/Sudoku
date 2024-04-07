package com.nataliausoltseva.sudoku

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.nataliausoltseva.sudoku.ui.theme.SudokuTheme
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.PartySystem
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import com.nataliausoltseva.sudoku.konfettiData.KonfettiViewModel
import com.nataliausoltseva.sudoku.settingsData.SettingsViewModel
import com.nataliausoltseva.sudoku.settingsData.THEMES
import com.nataliausoltseva.sudoku.sudokaData.GRID_SIZE_SQUARE_ROOT
import com.nataliausoltseva.sudoku.sudokaData.LEVELS
import com.nataliausoltseva.sudoku.sudokaData.SudokuViewModel
import com.nataliausoltseva.sudoku.timerData.TimerViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val settingsViewModel: SettingsViewModel =  remember { SettingsViewModel(context) }
            val sudokuViewModel = remember { SudokuViewModel() }
            val timerViewModel = remember { TimerViewModel() }
            MainApp(settingsViewModel, sudokuViewModel, timerViewModel)
        }
    }
}

@Composable
fun MainApp(
    settingsViewModel: SettingsViewModel,
    sudokuViewModel: SudokuViewModel,
    timerViewModel: TimerViewModel
) {
    val sudokuUIState by sudokuViewModel.uiState.collectAsState()
    val settingsUIState by settingsViewModel.uiState.collectAsState()
    val timer by timerViewModel.timer.collectAsState()
    val showSettings = remember { mutableStateOf(false) }
    val isRestartClicked = remember { mutableStateOf(false) }

    val view = LocalView.current
    LaunchedEffect(sudokuUIState.mistakesNum) {
        if (sudokuUIState.mistakesNum > 0) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }
    }

    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)

    SudokuTheme(
        darkTheme = settingsUIState.theme == "dark" || (settingsUIState.theme == "system" && isSystemInDarkTheme())
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!sudokuUIState.hasStarted || isRestartClicked.value) {
                WelcomeDialog(
                    isRestartClicked = isRestartClicked,
                    onStartGame = { sudokuViewModel.onStart(it) },
                    onStartTimer = { timerViewModel.startTimer() },
                    onStopTimer = { timerViewModel.stopTimer() },
                )
            } else {
                if (sudokuUIState.stepsToGo == 0) {
                    timerViewModel.pauseTimer()
                    EndScreen(
                        hasTimer = settingsUIState.hasTimer,
                        level = sudokuUIState.selectedLevel,
                        formattedTime = timerViewModel.formatTime(timer),
                        onRegenerate = { sudokuViewModel.onRegenerate() }
                    )
                    KonfettiUI()
                    LaunchedEffect(true) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                        delay(500)
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                        delay(1000)
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                        delay(500)
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.End,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RestartButton(
                                isRestartClicked = isRestartClicked,
                                onPauseTimer = { timerViewModel.pauseTimer() }
                            )
                            Surface(
                                onClick = { showSettings.value = !showSettings.value },
                                modifier = Modifier.padding(0.dp, 20.dp, 0.dp, 20.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = "Settings icon"
                                )
                            }
                        }
                        Settings(
                            showSettings.value,
                            onCancel = { showSettings.value = false },
                            settingsUIState.showMistakes,
                            settingsUIState.hasMistakeCounter,
                            settingsUIState.hasHighlightSameNumbers,
                            settingsUIState.hasRowHighlight,
                            settingsUIState.hasTimer,
                            settingsUIState.theme,
                            onSave = {
                                newShowMistakes: Boolean,
                                newHasMistakesCount: Boolean,
                                newTheme: String,
                                newHasHighlightSameNumbers: Boolean,
                                newHasRowHighlight: Boolean,
                                newHasTimer: Boolean,
                                 -> settingsViewModel.onSave(
                                    newShowMistakes,
                                    newHasMistakesCount,
                                    newTheme,
                                    newHasHighlightSameNumbers,
                                    newHasRowHighlight,
                                    newHasTimer
                                )
                            },
                            onPauseTimer = { timerViewModel.pauseTimer() },
                            onStartTimer = { timerViewModel.startTimer() }
                        )
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LevelIndicator(level = sudokuUIState.selectedLevel)
                                if (settingsUIState.hasMistakeCounter) {
                                    MistakeCounter(
                                        mistakesNum = sudokuUIState.mistakesNum,
                                        onRegenerate = { sudokuViewModel.onRegenerate() }
                                    )
                                }
                                if (settingsUIState.hasTimer) {
                                    Timer(
                                        isPaused = sudokuUIState.isPaused,
                                        onStartTimer = {
                                            timerViewModel.startTimer()
                                            sudokuViewModel.onStart()
                                        },
                                        formattedTime = timerViewModel.formatTime(timer),
                                        onPauseTimer = {
                                            timerViewModel.pauseTimer()
                                            sudokuViewModel.onPause()
                                        }
                                    )
                                }
                            }
                            if (sudokuUIState.stepsToGo > 0) {
                                SudokuGrid(
                                    grid = sudokuUIState.matrix,
                                    selectedCellRow = sudokuUIState.selectedCellRow,
                                    selectedCellColumn = sudokuUIState.selectedCellColumn,
                                    hasHighlightSameNumbers = settingsUIState.hasHighlightSameNumbers,
                                    onSelectCell = { row:Int, column: Int -> sudokuViewModel.onSelectCell(row, column) },
                                    hasRowHighlight = settingsUIState.hasRowHighlight,
                                    isPaused = sudokuUIState.isPaused,
                                    showMistakes = settingsUIState.showMistakes,
                                    unlockedCell = sudokuUIState.unlockedCell,
                                    gridWithNotes = sudokuUIState.matrixWithNotes,
                                    selectedDigit = sudokuUIState.selectedDigit,
                                    onBoxCheck = { sudokuViewModel.isUnusedInBox(
                                        sudokuUIState.selectedCellRow - sudokuUIState.selectedCellRow % GRID_SIZE_SQUARE_ROOT,
                                        sudokuUIState.selectedCellColumn - sudokuUIState.selectedCellColumn % GRID_SIZE_SQUARE_ROOT,
                                        sudokuUIState.selectedDigit,
                                        sudokuUIState.matrix,
                                        true
                                    )},
                                    onRowCheck = { sudokuViewModel.isUnusedInRow(
                                        sudokuUIState.selectedCellRow,
                                        sudokuUIState.selectedDigit,
                                        sudokuUIState.matrix,
                                        true
                                    )},
                                    onColumnCheck = { sudokuViewModel.isUnusedInColumn(
                                        sudokuUIState.selectedCellColumn,
                                        sudokuUIState.selectedDigit,
                                        sudokuUIState.matrix,
                                        true
                                    )},
                                    isNotesEnabled = sudokuUIState.isNotesEnabled
                                )
                                val currentCell =sudokuUIState.matrix[sudokuUIState.selectedCellRow][sudokuUIState.selectedCellColumn][2]
                                SelectionNumbers(
                                    selectionNumbers = sudokuUIState.selectionNumbers,
                                    onSelection = { digit: Int, canInsert: Boolean -> sudokuViewModel.onSelection(digit, canInsert) },
                                    cannotInsert = sudokuUIState.isNotesEnabled &&
                                            currentCell.intValue != 0,
                                    canInsert = { sudokuViewModel.canInsert(it) },
                                    isNotesEnabled = sudokuUIState.isNotesEnabled,
                                )
                                Row (
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UndoButton(sudokuUIState.hasSteps, onUndo = { sudokuViewModel.onUndo() })
                                    Erase(onErase = { sudokuViewModel.onErase() })
                                    NotesButton(sudokuUIState.isNotesEnabled, onNote = { sudokuViewModel.onNote() })
                                    Hints(
                                        useHint = { sudokuViewModel.useHint() },
                                        hintNum = sudokuUIState.hintNum
                                    )
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
    onStartGame: (index: Int) -> Unit,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    isRestartClicked: MutableState<Boolean>,
) {
    BasicAlertDialog(
        onDismissRequest = {
            isRestartClicked.value = false
        },
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onPrimary)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(6.dp, 6.dp, 6.dp, 12.dp)
        ) {
            if (isRestartClicked.value) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        onClick = {
                            isRestartClicked.value = false
                            onStartTimer()
                        }
                    ) {
                        Icon (
                            Icons.Outlined.Close,
                            contentDescription = "Close icon"
                        )
                    }
                }
            }
            Text(
                text = "Choose Your Puzzle:",
                fontWeight = FontWeight.Bold
            )
            for (i in LEVELS.indices) {
                TextButton(
                    onClick = {
                        onStartGame(i)
                        onStopTimer()
                        onStartTimer()
                        isRestartClicked.value = false
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
    hasTimer: Boolean,
    level: String,
    formattedTime: String,
    onRegenerate: () -> Unit
) {
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
            LevelIndicator(level)
        }

        if (hasTimer) {
            Row {
                Text(text = "Time: ")
                Text(text = formattedTime)
            }
        }

        TextButton(
            onClick = { onRegenerate() },
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
    showMistakes: Boolean,
    hasMistakeCounter: Boolean,
    hasHighlightSameNumbers: Boolean,
    hasRowHighlight: Boolean,
    hasTimer: Boolean,
    theme: String,
    onSave: (
        showMistakes: Boolean,
        hasMistakesCount: Boolean,
        theme:String,
        hasHighlightSameNumbers: Boolean,
        hasRowHighlight: Boolean,
        hasTimer: Boolean,
    ) -> Unit,
    onPauseTimer: () -> Unit,
    onStartTimer: () -> Unit
) {
    val newTheme = remember { mutableStateOf(theme) }
    val newShowMistakes = remember { mutableStateOf(showMistakes) }
    val newHasMistakeCounter = remember { mutableStateOf(hasMistakeCounter) }
    val newHasRowHighlight = remember { mutableStateOf(hasRowHighlight) }
    val newHasTimer = remember { mutableStateOf(hasTimer) }
    val newHasHighlightSameNumbers = remember { mutableStateOf(hasHighlightSameNumbers) }

    if (showSettings) {
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp, 0.dp, 10.dp, 0.dp)
                    ) {
                        Text(text = "Show mistakes")
                        Switch(checked = newShowMistakes.value, onCheckedChange = { isChecked -> newShowMistakes.value = isChecked })
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp, 0.dp, 10.dp, 0.dp)
                    ) {
                        Text(text = "Count mistakes")
                        Switch(
                            checked = newHasMistakeCounter.value,
                            onCheckedChange = { isChecked -> newHasMistakeCounter.value = isChecked}
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp, 0.dp, 10.dp, 0.dp)
                    ) {
                        Text(text = "Highlight Same Numbers")
                        Switch(
                            checked = newHasHighlightSameNumbers.value,
                            onCheckedChange = { isChecked -> newHasHighlightSameNumbers.value = isChecked}
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp, 0.dp, 10.dp, 0.dp)
                    ) {
                        Text(text = "Highlight Rows")
                        Switch(
                            checked = newHasRowHighlight.value,
                            onCheckedChange = { isChecked -> newHasRowHighlight.value = isChecked}
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp, 0.dp, 10.dp, 0.dp)
                    ) {
                        Text(text = "Include Timer")
                        Switch(
                            checked = newHasTimer.value,
                            onCheckedChange = { isChecked -> newHasTimer.value = isChecked}
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp, 0.dp, 10.dp, 0.dp)
                    ) {
                        val isExpanded = remember{ mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = isExpanded.value,
                            onExpandedChange = { isExpanded.value = !isExpanded.value }
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = newTheme.value,
                                onValueChange = {},
                                label = { Text(text = "Theme") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded.value)
                                },
                                colors = OutlinedTextFieldDefaults.colors(),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(expanded = isExpanded.value, onDismissRequest = { isExpanded.value = false }) {
                                for (i in THEMES.indices) {
                                    DropdownMenuItem(
                                        text = { 
                                            Text(text = THEMES[i])
                                        },
                                        onClick = {
                                            newTheme.value = THEMES[i]
                                            isExpanded.value = false
                                        },
                                        
                                    )
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
                            onSave(
                                newShowMistakes.value,
                                newHasMistakeCounter.value,
                                newTheme.value,
                                newHasHighlightSameNumbers.value,
                                newHasRowHighlight.value,
                                newHasTimer.value,
                            )
                            onCancel()

                            if (!newHasTimer.value) {
                                onPauseTimer()
                            } else {
                                onStartTimer()
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
    level: String,
) {
    Text(
        text = level,
        modifier = Modifier.padding(15.dp, 0.dp, 0.dp, 0.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakeCounter(
    mistakesNum: Int,
    onRegenerate: () -> Unit
) {
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
                    onClick = { onRegenerate() },
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
    useHint: () -> Unit,
    hintNum: Int,
) {
    Surface {
        Button(
            onClick = { useHint() },
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
    isPaused: Boolean,
    onStartTimer: () -> Unit,
    formattedTime: String,
    onPauseTimer: () -> Unit,
) {
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
                        onStartTimer()
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
        Text(text = formattedTime)
        Surface(
            onClick = {
                if (!isPaused) {
                    onPauseTimer()
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
    grid: Array<Array<Array<MutableIntState>>>,
    selectedCellRow: Int,
    selectedCellColumn: Int,
    hasHighlightSameNumbers: Boolean,
    onSelectCell: (rowIndex: Int, columnIndex: Int) -> Unit,
    hasRowHighlight: Boolean,
    isPaused: Boolean,
    showMistakes: Boolean,
    unlockedCell: Array<Int?>,
    gridWithNotes: Array<Array<Array<MutableIntState>>>,
    selectedDigit: Int,
    onBoxCheck: () -> IntArray,
    onRowCheck: () -> IntArray,
    onColumnCheck: () -> IntArray,
    isNotesEnabled: Boolean
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(9),
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant),
        content = {
            items(81) {index ->
                val rowIndex = index / 9
                val columnIndex = index % 9
                val gridValue = grid[rowIndex][columnIndex][2].intValue
                val cellRowIndex = index % 3
                val isCurrentCell = selectedCellRow == rowIndex && selectedCellColumn == columnIndex
                val currentGridCellValue = grid[selectedCellRow][selectedCellColumn][2].intValue

                val backgroundCellColour = if (isCurrentCell) MaterialTheme.colorScheme.tertiary
                    else if (gridValue > 0 && currentGridCellValue == gridValue && hasHighlightSameNumbers) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer

                val outlineColour = MaterialTheme.colorScheme.inversePrimary

                val columnDividerColour = if (cellRowIndex != 0) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.outline

                val rowDividerColour = if (rowIndex % 3 != 0) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.outline

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val canvasWidth = size.width
                            val canvasHeight = size.height

                            val hasVerticalOutline =
                                selectedCellColumn == columnIndex || selectedCellColumn == columnIndex - 1
                            val ignoreVerticalOutline = isCurrentCell ||
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
                        .clickable { onSelectCell(rowIndex, columnIndex) }
                        .background(backgroundCellColour)
                ) {
                    var displayValue = ""
                    if (gridValue != 0 && !isPaused) {
                        displayValue = gridValue.toString()
                    }

                    val expectedValue = grid[rowIndex][columnIndex][1].intValue
                    val initialGridValue = grid[rowIndex][columnIndex][0].intValue

                    val colour =  if (gridValue != 0 && expectedValue != gridValue && showMistakes) Color.Red
                        else if (isCurrentCell) MaterialTheme.colorScheme.onTertiary
                        else if (initialGridValue == 0) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSecondaryContainer

                    val scale = remember { Animatable(1f) }
                    val isUnlockedCell = unlockedCell[0] == rowIndex && unlockedCell[1] == columnIndex
                    if (isUnlockedCell) {
                        LaunchedEffect(true) {
                            scale.animateTo(1f, animationSpec = tween(0))
                            scale.animateTo(3f, animationSpec = tween(350))
                            scale.animateTo(1f, animationSpec = tween(350))
                        }
                    } else {
                        LaunchedEffect(true) {
                            scale.animateTo(1f, animationSpec = tween(0))
                        }
                    }

                    if (isNotesEnabled && selectedDigit != 0) {
                        val repeatedInBox = onBoxCheck()
                        val repeatedInRow = onRowCheck()
                        val repeatedInColumn = onColumnCheck()

                        if ((repeatedInBox.isNotEmpty() && rowIndex == repeatedInBox[0] && columnIndex == repeatedInBox[1]) ||
                            (repeatedInRow.isNotEmpty() && rowIndex == repeatedInRow[0] && columnIndex == repeatedInRow[1]) ||
                            (repeatedInColumn.isNotEmpty() && rowIndex == repeatedInColumn[0] && columnIndex == repeatedInColumn[1])
                        ) {
                            LaunchedEffect(true) {
                                scale.animateTo(1f, animationSpec = tween(0))
                                scale.animateTo(3f, animationSpec = tween(350))
                                scale.animateTo(1f, animationSpec = tween(350))
                            }
                        } else {
                            LaunchedEffect(true) {
                                scale.animateTo(1f, animationSpec = tween(0))
                            }
                        }
                    }

                    val gridWithNoteCell = gridWithNotes[rowIndex][columnIndex]
                    val hasNotesInCurrentCell = gridWithNoteCell.any { it.intValue > 0 }
                    if (hasNotesInCurrentCell && displayValue == "") {
                        var actualIndex = 0
                        Column(
                            modifier = Modifier
                                .padding(6.dp, 2.dp, 3.dp, 4.dp)
                        ) {
                            for (row in 0 until 3) {
                                Row(
                                    modifier = Modifier.height(19.dp)
                                ) {
                                    for (i in 0 until 3) {
                                        val noteDisplay = if (gridWithNoteCell[actualIndex].intValue == 0) ""
                                            else gridWithNoteCell[actualIndex].intValue.toString()
                                        Text(
                                            text = noteDisplay,
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        actualIndex++
                                    }
                                }
                            }
                            actualIndex = 0
                        }
                    } else {
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
        }
    )
}

@Composable
fun RestartButton(
    isRestartClicked: MutableState<Boolean>,
    onPauseTimer: () -> Unit
) {
    Surface(
        onClick = {
            isRestartClicked.value = true
            onPauseTimer()
        }
    ) {
        Icon (
            Icons.Rounded.Refresh,
            contentDescription = "Refresh icon"
        )
    }
}

@Composable
fun Erase(
    onErase: () -> Unit
) {
    Button(
        onClick = { onErase() },
        modifier = Modifier.padding(8.dp),
    ) {
        Icon (
            Icons.Rounded.Delete,
            contentDescription = "Icon to clear a cell"
        )
    }
}

@Composable
fun SelectionNumbers(
    selectionNumbers: Array<Int>,
    onSelection: (number: Int, canInsert: Boolean) -> Unit,
    cannotInsert: Boolean,
    canInsert: (number: Int) -> Boolean,
    isNotesEnabled: Boolean
) {
    val isClicked = remember { mutableStateOf(false) }
    val isInsertable = remember { mutableStateOf(false) }
    fun onSelect(label: Int) {
        isInsertable.value = canInsert(label)
        if (cannotInsert) {
            isClicked.value = true
        } else {
            onSelection(label, isInsertable.value)
            if (isNotesEnabled && !isInsertable.value) {
                isClicked.value = true
            }
        }
    }

    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)
    LaunchedEffect(isClicked.value) {
        if (isClicked.value) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            isClicked.value = false
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(9),
        modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 0.dp),
        content = {
            items(9) { i ->
                val label = i + 1
                val isAvailable = selectionNumbers[i] > 0
                var labelColor = MaterialTheme.colorScheme.inversePrimary
                if (isAvailable) {
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                }

                Surface(
                    onClick = { onSelect(label) },
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

@Composable
fun UndoButton(
    isEnabled: Boolean = false,
    onUndo: () -> Unit
) {
    Button(
        onClick = { onUndo() },
        enabled = isEnabled,
        modifier = Modifier.padding(8.dp),
        ) {
        Icon (
            Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Undo icon"
        )
    }
}

@Composable
fun NotesButton(
    isActive: Boolean = false,
    onNote: () -> Unit
) {
    Button(
        onClick = { onNote() },
        modifier = Modifier.padding(8.dp)
    ) {
        Icon (
            Icons.Filled.Edit,
            contentDescription = "Notes button",
            tint = if (isActive) Color.Green else Color.Unspecified
        )
    }
}