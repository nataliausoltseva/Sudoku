package com.nataliausoltseva.sudoku

import android.content.res.Configuration
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nataliausoltseva.sudoku.konfettiData.KonfettiViewModel
import com.nataliausoltseva.sudoku.settingsData.SettingsViewModel
import com.nataliausoltseva.sudoku.settingsData.THEMES
import com.nataliausoltseva.sudoku.sudokaData.LEVELS
import com.nataliausoltseva.sudoku.sudokaData.SudokuViewModel
import com.nataliausoltseva.sudoku.timerData.TimerViewModel
import com.nataliausoltseva.sudoku.ui.theme.SudokuTheme
import kotlinx.coroutines.delay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.PartySystem

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val settingsViewModel = remember { SettingsViewModel(context) }
            val sudokuViewModel = remember { SudokuViewModel() }
            val timerViewModel = remember { TimerViewModel() }
            val orientation = LocalConfiguration.current.orientation
            val view = LocalView.current
            view.windowInsetsController?.hide(android.view.WindowInsets.Type.systemBars())
            view.windowInsetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            MainApp(
                settingsViewModel,
                sudokuViewModel,
                timerViewModel,
                orientation == Configuration.ORIENTATION_LANDSCAPE
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    settingsViewModel: SettingsViewModel,
    sudokuViewModel: SudokuViewModel,
    timerViewModel: TimerViewModel,
    isLandscape: Boolean,
) {
    val sudokuUIState by sudokuViewModel.uiState.collectAsState()
    val settingsUIState by settingsViewModel.uiState.collectAsState()
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
            if (sudokuUIState.isPaused) {
                BasicAlertDialog(
                    onDismissRequest = {},
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
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
                                timerViewModel.startTimer()
                                sudokuViewModel.onStart()
                            },
                            modifier = Modifier.padding(8.dp),
                        ) {
                            Text("Continue")
                        }
                    }
                }
            }

            if (!sudokuUIState.hasStarted || isRestartClicked.value) {
                WelcomeDialog(
                    onStartGame = {
                        sudokuViewModel.onStart(it)
                        timerViewModel.stopTimer()
                        timerViewModel.startTimer()
                        isRestartClicked.value = false
                    },
                    onDismiss = {
                        timerViewModel.startTimer()
                        isRestartClicked.value = false
                    },
                    hasClosingButton = isRestartClicked.value,
                )
            } else {
                if (sudokuUIState.stepsToGo == 0) {
                    timerViewModel.pauseTimer()
                    EndScreen(
                        hasTimer = settingsUIState.hasTimer,
                        level = sudokuUIState.selectedLevel,
                        formattedTime = timerViewModel.getTimer(),
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
                    if (showSettings.value) {
                        Settings(
                            onCancel = { showSettings.value = false },
                            showMistakes = settingsUIState.showMistakes,
                            hasMistakeCounter = settingsUIState.hasMistakeCounter,
                            hasHighlightSameNumbers = settingsUIState.hasHighlightSameNumbers,
                            hasRowHighlight = settingsUIState.hasRowHighlight,
                            hasTimer = settingsUIState.hasTimer,
                            theme = settingsUIState.theme,
                            onSave = {
                                    newShowMistakes: Boolean,
                                    newHasMistakesCount: Boolean,
                                    newTheme: String,
                                    newHasHighlightSameNumbers: Boolean,
                                    newHasRowHighlight: Boolean,
                                    newHasTimer: Boolean,
                                ->
                                settingsViewModel.onSave(
                                    newShowMistakes,
                                    newHasMistakesCount,
                                    newTheme,
                                    newHasHighlightSameNumbers,
                                    newHasRowHighlight,
                                    newHasTimer
                                )
                                if (newHasTimer) {
                                    timerViewModel.startTimer()
                                } else {
                                    timerViewModel.pauseTimer()
                                }
                            },
                        )
                    }
                    val currentCell =  sudokuUIState.matrix[sudokuUIState.selectedCellRow][sudokuUIState.selectedCellColumn][2].intValue
                    if (isLandscape) {
                        Row {
                            Row {
                                if (sudokuUIState.stepsToGo > 0) {
                                    Column(
                                        modifier = Modifier.fillMaxHeight(),
                                        verticalArrangement = Arrangement.Center
                                    ) {
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
                                            onBoxCheck = { sudokuViewModel.isUnusedInBox() },
                                            onRowCheck = { sudokuViewModel.isUnusedInRow() },
                                            onColumnCheck = { sudokuViewModel.isUnusedInColumn() },
                                            isNotesEnabled = sudokuUIState.isNotesEnabled
                                        )
                                    }
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            GameIndications(
                                                timerViewModel = timerViewModel,
                                                selectedLevel = sudokuUIState.selectedLevel,
                                                hasMistakeCounter = settingsUIState.hasMistakeCounter,
                                                mistakesNum = sudokuUIState.mistakesNum,
                                                onRegenerate = { sudokuViewModel.onRegenerate() },
                                                hasTimer = settingsUIState.hasTimer,
                                                isPaused = sudokuUIState.isPaused,
                                                onPause = {
                                                    sudokuViewModel.onPause()
                                                    timerViewModel.pauseTimer()
                                                }
                                            )
                                            SettingsBar(onShowSettings = { showSettings.value = true }) {
                                                RestartButton(onRestart = {
                                                    timerViewModel.stopTimer()
                                                    isRestartClicked.value = true
                                                })
                                            }
                                        }
                                        SelectionNumbers(
                                            selectionNumbers = sudokuUIState.selectionNumbers,
                                            onSelection = { digit: Int, isInsertable: Boolean -> sudokuViewModel.onSelection(digit, isInsertable) },
                                            cannotInsert = sudokuUIState.isNotesEnabled && currentCell != 0,
                                            canInsert = { sudokuViewModel.canInsert(it) },
                                            isNotesEnabled = sudokuUIState.isNotesEnabled,
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            UndoButton(sudokuUIState.hasSteps, onUndo = { sudokuViewModel.onUndo() })
                                            EraseButton(onErase = { sudokuViewModel.onErase() })
                                            NotesButton(sudokuUIState.isNotesEnabled, onNote = { sudokuViewModel.onNote() })
                                            HintsButton(
                                                useHint = { sudokuViewModel.useHint() },
                                                hintNum = sudokuUIState.hintNum
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            SettingsBar(onShowSettings = { showSettings.value = true }) {
                                RestartButton(onRestart = {
                                    timerViewModel.stopTimer()
                                    isRestartClicked.value = true
                                })
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (sudokuUIState.stepsToGo > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        GameIndications(
                                            timerViewModel = timerViewModel,
                                            selectedLevel = sudokuUIState.selectedLevel,
                                            hasMistakeCounter = settingsUIState.hasMistakeCounter,
                                            mistakesNum = sudokuUIState.mistakesNum,
                                            onRegenerate = { sudokuViewModel.onRegenerate() },
                                            hasTimer = settingsUIState.hasTimer,
                                            isPaused = sudokuUIState.isPaused,
                                            onPause = {
                                                sudokuViewModel.onPause()
                                                timerViewModel.pauseTimer()
                                            }
                                        )
                                    }
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
                                        onBoxCheck = { sudokuViewModel.isUnusedInBox() },
                                        onRowCheck = { sudokuViewModel.isUnusedInRow() },
                                        onColumnCheck = { sudokuViewModel.isUnusedInColumn() },
                                        isNotesEnabled = sudokuUIState.isNotesEnabled
                                    )
                                    Column {
                                        SelectionNumbers(
                                            selectionNumbers = sudokuUIState.selectionNumbers,
                                            onSelection = { digit: Int, isInsertable: Boolean -> sudokuViewModel.onSelection(digit, isInsertable) },
                                            cannotInsert = sudokuUIState.isNotesEnabled && currentCell != 0,
                                            canInsert = { sudokuViewModel.canInsert(it) },
                                            isNotesEnabled = sudokuUIState.isNotesEnabled,
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            UndoButton(sudokuUIState.hasSteps, onUndo = { sudokuViewModel.onUndo() })
                                            EraseButton(onErase = { sudokuViewModel.onErase() })
                                            NotesButton(sudokuUIState.isNotesEnabled, onNote = { sudokuViewModel.onNote() })
                                            HintsButton(
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
    onDismiss: () -> Unit,
    onStartGame: (index: Int) -> Unit,
    hasClosingButton: Boolean = false
) {
    BasicAlertDialog(
        onDismissRequest = { onDismiss() },
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.onPrimary)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(6.dp, 6.dp, 6.dp, 12.dp)
        ) {
            if (hasClosingButton) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { onDismiss() }
                    ) {
                        Icon(
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
                    onClick = { onStartGame(i) },
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
        theme: String,
        hasHighlightSameNumbers: Boolean,
        hasRowHighlight: Boolean,
        hasTimer: Boolean,
    ) -> Unit
) {
    val newTheme = remember { mutableStateOf(theme) }
    val newShowMistakes = remember { mutableStateOf(showMistakes) }
    val newHasMistakeCounter = remember { mutableStateOf(hasMistakeCounter) }
    val newHasRowHighlight = remember { mutableStateOf(hasRowHighlight) }
    val newHasTimer = remember { mutableStateOf(hasTimer) }
    val newHasHighlightSameNumbers = remember { mutableStateOf(hasHighlightSameNumbers) }

    BasicAlertDialog(
        onDismissRequest = {},
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.onPrimary)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(6.dp)
        ) {
            Column(
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
                    Switch(
                        checked = newShowMistakes.value,
                        onCheckedChange = { isChecked -> newShowMistakes.value = isChecked })
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
                        onCheckedChange = { isChecked -> newHasMistakeCounter.value = isChecked }
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
                        onCheckedChange = { isChecked ->
                            newHasHighlightSameNumbers.value = isChecked
                        }
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
                        onCheckedChange = { isChecked -> newHasRowHighlight.value = isChecked }
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
                        onCheckedChange = { isChecked -> newHasTimer.value = isChecked }
                    )
                }
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp, 0.dp, 10.dp, 0.dp)
                ) {
                    val isExpanded = remember { mutableStateOf(false) }
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

                        ExposedDropdownMenu(
                            expanded = isExpanded.value,
                            onDismissRequest = { isExpanded.value = false }) {
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
                    },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("Save")
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
                .clip(MaterialTheme.shapes.large)
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
fun HintsButton(
    useHint: () -> Unit,
    hintNum: Int,
) {
    Surface {
        Button(
            onClick = { useHint() },
            modifier = Modifier.padding(8.dp),
            enabled = hintNum > 0,
        ) {
            Icon(
                Icons.Rounded.Lightbulb,
                contentDescription = "Hint icon"
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .absoluteOffset(55.dp, 0.dp)
                .clip(MaterialTheme.shapes.large)
        ) {
            Text(
                text = hintNum.toString(),
                modifier = Modifier
                    .padding(10.dp, 3.dp, 10.dp, 3.dp)
            )
        }
    }
}

@Composable
fun Timer(
    timerViewModel: TimerViewModel,
) {
    val timer by timerViewModel.timer.collectAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = timerViewModel.formatTime(timer))
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
    Box {
        Column(
            modifier = Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline))
        ) {
            for (row in 0 until 9) {
                val rowDividerColour = if (row % 3 != 0) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.outline
                val outlineColour = MaterialTheme.colorScheme.inversePrimary
                Row {
                    for (index in 0 until 9) {
                        val isCurrentCell = selectedCellRow == row && selectedCellColumn == index
                        var displayValue = ""
                        val gridValue = grid[row][index][2].intValue
                        if (gridValue != 0 && !isPaused) {
                            displayValue = gridValue.toString()
                        }

                        val currentGridCellValue = grid[selectedCellRow][selectedCellColumn][2].intValue

                        val backgroundCellColour = if (isCurrentCell) MaterialTheme.colorScheme.tertiary
                            else if (gridValue > 0 && currentGridCellValue == gridValue && hasHighlightSameNumbers) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer

                        val columnDividerColour = if (index % 3 == 0) MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.surface
                        val orientation = LocalConfiguration.current.orientation
                        val cellSize = if (orientation == Configuration.ORIENTATION_LANDSCAPE) LocalConfiguration.current.screenHeightDp.dp / (9)
                            else LocalConfiguration.current.screenWidthDp.dp / (9+1)
                        Column (
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .size(cellSize)
                                .background(backgroundCellColour)
                                .clickable { onSelectCell(row, index) }
                                .drawBehind {
                                    val hasVerticalOutline =
                                        selectedCellColumn == index || selectedCellColumn == index - 1

                                    if (!isCurrentCell) {
                                        if (hasVerticalOutline && hasRowHighlight) {
                                            drawLine(
                                                start = Offset(x = 0f, y = size.height),
                                                end = Offset(x = 0f, y = 0f),
                                                color = outlineColour,
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        } else if (index != 0) {
                                            drawLine(
                                                start = Offset(x = 0f, y = size.height),
                                                end = Offset(x = 0f, y = 0f),
                                                color = columnDividerColour,
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }
                                    }

                                    val hasHorizontalOutline =
                                        selectedCellRow == row || selectedCellRow == row - 1
                                    if (!isCurrentCell) {
                                        if (hasHorizontalOutline && hasRowHighlight) {
                                            drawLine(
                                                start = Offset(
                                                    x = size.width - 1.dp.toPx(),
                                                    y = 0f
                                                ),
                                                end = Offset(x = 0f, y = 0f),
                                                color = outlineColour,
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        } else if (row != 0) {
                                            drawLine(
                                                start = Offset(
                                                    x = size.width - 1.dp.toPx(),
                                                    y = 0f
                                                ),
                                                end = Offset(x = 0f, y = 0f),
                                                color = rowDividerColour,
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }
                                    }
                                }
                        ) {
                            val scale = remember { Animatable(1f) }
                            val isUnlockedCell = unlockedCell[0] == row && unlockedCell[1] == index
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

                                if ((repeatedInBox.isNotEmpty() && row == repeatedInBox[0] && index == repeatedInBox[1]) ||
                                    (repeatedInRow.isNotEmpty() && row == repeatedInRow[0] && index == repeatedInRow[1]) ||
                                    (repeatedInColumn.isNotEmpty() && row == repeatedInColumn[0] && index == repeatedInColumn[1])
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
                            val gridWithNoteCell = gridWithNotes[row][index]
                            val hasNotesInCurrentCell = gridWithNoteCell.any { it.intValue > 0 }
                            if (hasNotesInCurrentCell && displayValue == "" && !isPaused) {
                                var actualIndex = 0
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.size(cellSize)
                                ) {
                                    for (rowIndex in 0 until 3) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .heightIn(max = cellSize / 3)
                                                .fillMaxSize()
                                        ) {
                                            for (i in 0 until 3) {
                                                val noteDisplay = if (gridWithNoteCell[actualIndex].intValue == 0) ""
                                                    else gridWithNoteCell[actualIndex].intValue.toString()
                                                Text(
                                                    text = noteDisplay,
                                                    fontSize = 8.sp,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier
                                                        .offset(y = -4.dp)
                                                        .weight(1f)
                                                )
                                                actualIndex++
                                            }
                                        }
                                    }
                                    actualIndex = 0
                                }
                            } else {
                                val expectedValue = grid[row][index][1].intValue
                                val initialGridValue = grid[row][index][0].intValue
                                val colour = if (gridValue != 0 && expectedValue != gridValue && showMistakes) Color.Red
                                    else if (isCurrentCell) MaterialTheme.colorScheme.onTertiary
                                    else if (initialGridValue == 0) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.onSecondaryContainer
                                Text(
                                    text = displayValue,
                                    Modifier
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
            }
        }
    }
}

@Composable
fun RestartButton(
    onRestart: () -> Unit,
) {
    IconButton(
        onClick = { onRestart() }
    ) {
        Icon(
            Icons.Rounded.Refresh,
            contentDescription = "Refresh icon"
        )
    }
}

@Composable
fun EraseButton(
    onErase: () -> Unit
) {
    Button(
        onClick = { onErase() },
        modifier = Modifier.padding(8.dp),
    ) {
        Icon(
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
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        for (i in 0 until 9) {
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
        Icon(
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
        Icon(
            Icons.Filled.Edit,
            contentDescription = "Notes button",
            tint = if (isActive) Color.Green else Color.Unspecified
        )
    }
}

@Composable
fun SettingsBar(
    onShowSettings: () -> Unit,
    restartButton: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        restartButton()
        IconButton(
            onClick = { onShowSettings() },
            modifier = Modifier.padding(0.dp, 20.dp, 0.dp, 20.dp)
        ) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Settings icon"
            )
        }
    }
}

@Composable
fun GameIndications(
    selectedLevel: String,
    hasMistakeCounter: Boolean,
    mistakesNum: Int,
    onRegenerate: () -> Unit,
    hasTimer: Boolean,
    isPaused: Boolean,
    onPause: () -> Unit,
    timerViewModel: TimerViewModel,
) {
    LevelIndicator(level = selectedLevel)
    if (hasMistakeCounter) {
        MistakeCounter(
            mistakesNum = mistakesNum,
            onRegenerate = { onRegenerate() }
        )
    }
    if (hasTimer) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Timer(timerViewModel)
            IconButton(
                onClick = { onPause() }
            ) {
                val icon =
                    if (isPaused) Icons.Filled.PlayArrow
                    else Icons.Filled.Pause
                val iconDescription =
                    if (isPaused) "Play icon" else "Pause icon"
                Icon(
                    icon,
                    contentDescription = iconDescription
                )
            }
        }
    }
}