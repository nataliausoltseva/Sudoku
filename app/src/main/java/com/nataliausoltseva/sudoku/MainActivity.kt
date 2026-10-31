package com.nataliausoltseva.sudoku

import android.content.res.Configuration
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nataliausoltseva.sudoku.konfettiData.KonfettiViewModel
import com.nataliausoltseva.sudoku.settingsData.SettingsViewModel
import com.nataliausoltseva.sudoku.sudokaData.LEVELS
import com.nataliausoltseva.sudoku.sudokaData.SudokuViewModel
import com.nataliausoltseva.sudoku.timerData.TimerViewModel
import com.nataliausoltseva.sudoku.ui.theme.NotesTextDark
import com.nataliausoltseva.sudoku.ui.theme.NotesTextLight
import com.nataliausoltseva.sudoku.ui.theme.SudokuTheme
import kotlinx.coroutines.delay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.PartySystem

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current.applicationContext
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(context))
            val sudokuViewModel: SudokuViewModel = viewModel()
            val timerViewModel: TimerViewModel = viewModel()
            val orientation = LocalConfiguration.current.orientation
            MainApp(
                settingsViewModel,
                sudokuViewModel,
                timerViewModel,
                orientation == Configuration.ORIENTATION_LANDSCAPE
            )
        }
    }
}

@androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.Q)
@Suppress("NewApi")
private fun vibrateApi29(vibrator: Vibrator) {
    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
}

@androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.O)
@Suppress("NewApi")
private fun vibrateApi26(vibrator: Vibrator) {
    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
}

@Suppress("DEPRECATION")
private fun vibrateLegacy(vibrator: Vibrator) {
    vibrator.vibrate(100L)
}

private fun triggerVibration(vibrator: Vibrator?) {
    if (vibrator == null) return
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        vibrateApi29(vibrator)
    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        vibrateApi26(vibrator)
    } else {
        vibrateLegacy(vibrator)
    }
}

private fun formatThemeName(themeName: String): String = when (themeName.lowercase()) {
    "light" -> "Light"
    "dark" -> "Dark"
    "system" -> "System"
    else -> themeName.replaceFirstChar { it.uppercase() }
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
    val focusRequester = remember { FocusRequester() }

    val view = LocalView.current
    LaunchedEffect(sudokuUIState.mistakesNum) {
        if (sudokuUIState.mistakesNum > 0 && settingsUIState.hapticFeedbackEnabled && settingsUIState.showMistakes) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)

    SudokuTheme(
        darkTheme = settingsUIState.theme == "dark" || (settingsUIState.theme == "system" && isSystemInDarkTheme())
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .displayCutoutPadding()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && sudokuUIState.hasStarted && !sudokuUIState.isPaused) {
                            when (event.key) {
                                Key.One, Key.NumPad1 -> { sudokuViewModel.onSelection(1, sudokuViewModel.canInsert(1), settingsUIState.showMistakes); true }
                                Key.Two, Key.NumPad2 -> { sudokuViewModel.onSelection(2, sudokuViewModel.canInsert(2), settingsUIState.showMistakes); true }
                                Key.Three, Key.NumPad3 -> { sudokuViewModel.onSelection(3, sudokuViewModel.canInsert(3), settingsUIState.showMistakes); true }
                                Key.Four, Key.NumPad4 -> { sudokuViewModel.onSelection(4, sudokuViewModel.canInsert(4), settingsUIState.showMistakes); true }
                                Key.Five, Key.NumPad5 -> { sudokuViewModel.onSelection(5, sudokuViewModel.canInsert(5), settingsUIState.showMistakes); true }
                                Key.Six, Key.NumPad6 -> { sudokuViewModel.onSelection(6, sudokuViewModel.canInsert(6), settingsUIState.showMistakes); true }
                                Key.Seven, Key.NumPad7 -> { sudokuViewModel.onSelection(7, sudokuViewModel.canInsert(7), settingsUIState.showMistakes); true }
                                Key.Eight, Key.NumPad8 -> { sudokuViewModel.onSelection(8, sudokuViewModel.canInsert(8), settingsUIState.showMistakes); true }
                                Key.Nine, Key.NumPad9 -> { sudokuViewModel.onSelection(9, sudokuViewModel.canInsert(9), settingsUIState.showMistakes); true }
                                Key.DirectionUp -> {
                                    val nextRow = (sudokuUIState.selectedCellRow - 1 + 9) % 9
                                    sudokuViewModel.onSelectCell(nextRow, sudokuUIState.selectedCellColumn)
                                    true
                                }
                                Key.DirectionDown -> {
                                    val nextRow = (sudokuUIState.selectedCellColumn + 1) % 9
                                    sudokuViewModel.onSelectCell(nextRow, sudokuUIState.selectedCellColumn)
                                    true
                                }
                                Key.DirectionLeft -> {
                                    val nextCol = (sudokuUIState.selectedCellColumn - 1 + 9) % 9
                                    sudokuViewModel.onSelectCell(sudokuUIState.selectedCellRow, nextCol)
                                    true
                                }
                                Key.DirectionRight -> {
                                    val nextCol = (sudokuUIState.selectedCellColumn + 1) % 9
                                    sudokuViewModel.onSelectCell(sudokuUIState.selectedCellRow, nextCol)
                                    true
                                }
                                Key.N -> { sudokuViewModel.onNote(); true }
                                Key.Backspace, Key.Delete -> { sudokuViewModel.onErase(); true }
                                Key.U -> { if (sudokuUIState.hasSteps) sudokuViewModel.onUndo(); true }
                                Key.H -> { if (sudokuUIState.hintNum > 0) sudokuViewModel.useHint(); true }
                                else -> false
                            }
                        } else false
                    }
            ) {
            if (sudokuUIState.isPaused) {
                BasicAlertDialog(
                    onDismissRequest = {},
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Paused",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = {
                                timerViewModel.startTimer()
                                sudokuViewModel.onStart()
                            },
                        ) {
                            Text("Continue", style = MaterialTheme.typography.bodyLarge)
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
                if (!sudokuUIState.hasStepsToGo) {
                    timerViewModel.pauseTimer()
                    EndScreen(
                        hasTimer = settingsUIState.hasTimer,
                        level = sudokuUIState.selectedLevel,
                        formattedTime = timerViewModel.getTimer(),
                        onRegenerate = { sudokuViewModel.onRegenerate() }
                    )
                    KonfettiUI()
                    LaunchedEffect(true) {
                        if (settingsUIState.hapticFeedbackEnabled && vibrator != null) {
                            triggerVibration(vibrator)
                            delay(500)
                            triggerVibration(vibrator)
                            delay(1000)
                            triggerVibration(vibrator)
                            delay(500)
                            triggerVibration(vibrator)
                        }
                    }
                } else {
                    if (showSettings.value) {
                        Settings(
                            onDismiss = { showSettings.value = false },
                            showMistakes = settingsUIState.showMistakes,
                            hasMistakeCounter = settingsUIState.hasMistakeCounter,
                            mistakesNum = sudokuUIState.mistakesNum,
                            hasHighlightSameNumbers = settingsUIState.hasHighlightSameNumbers,
                            hasRowHighlight = settingsUIState.hasRowHighlight,
                            hasTimer = settingsUIState.hasTimer,
                            hapticFeedbackEnabled = settingsUIState.hapticFeedbackEnabled,
                            theme = settingsUIState.theme,
                            onSave = {
                                    newShowMistakes: Boolean,
                                    newHasMistakesCount: Boolean,
                                    newTheme: String,
                                    newHasHighlightSameNumbers: Boolean,
                                    newHasRowHighlight: Boolean,
                                    newHasTimer: Boolean,
                                    newHapticFeedbackEnabled: Boolean,
                                ->
                                settingsViewModel.onSave(
                                    newShowMistakes,
                                    if (newShowMistakes) newHasMistakesCount else false,
                                    newTheme,
                                    newHasHighlightSameNumbers,
                                    newHasRowHighlight,
                                    newHasTimer,
                                    newHapticFeedbackEnabled
                                )
                                if (newHasTimer) {
                                    timerViewModel.startTimer()
                                } else {
                                    timerViewModel.pauseTimer()
                                }
                            },
                        )
                    }
                    val currentCell = sudokuUIState.matrix[sudokuUIState.selectedCellRow][sudokuUIState.selectedCellColumn][2].intValue
                    if (isLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                SudokuGrid(
                                    grid = sudokuUIState.matrix,
                                    selectedCellRow = sudokuUIState.selectedCellRow,
                                    selectedCellColumn = sudokuUIState.selectedCellColumn,
                                    hasHighlightSameNumbers = settingsUIState.hasHighlightSameNumbers,
                                    onSelectCell = { row: Int, column: Int -> sudokuViewModel.onSelectCell(row, column) },
                                    hasRowHighlight = settingsUIState.hasRowHighlight,
                                    isPaused = sudokuUIState.isPaused,
                                    showMistakes = settingsUIState.showMistakes,
                                    unlockedCell = sudokuUIState.unlockedCell,
                                    gridWithNotes = sudokuUIState.matrixWithNotes,
                                    selectedDigit = sudokuUIState.selectedDigit,
                                    onBoxCheck = { sudokuViewModel.isUnusedInBox() },
                                    onRowCheck = { sudokuViewModel.isUnusedInRow() },
                                    onColumnCheck = { sudokuViewModel.isUnusedInColumn() },
                                    isNotesEnabled = sudokuUIState.isNotesEnabled,
                                    isLandscape = true
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1.1f)
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                GameHeaderBar(
                                    selectedLevel = sudokuUIState.selectedLevel,
                                    hasMistakeCounter = settingsUIState.hasMistakeCounter && settingsUIState.showMistakes,
                                    mistakesNum = sudokuUIState.mistakesNum,
                                    onRegenerate = { sudokuViewModel.onRegenerate() },
                                    hasTimer = settingsUIState.hasTimer,
                                    isPaused = sudokuUIState.isPaused,
                                    onPause = {
                                        sudokuViewModel.onPause()
                                        timerViewModel.pauseTimer()
                                    },
                                    timerViewModel = timerViewModel,
                                    onShowSettings = { showSettings.value = true },
                                    onRestart = {
                                        timerViewModel.stopTimer()
                                        isRestartClicked.value = true
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SelectionNumbersGrid3x3(
                                    selectionNumbers = sudokuUIState.selectionNumbers,
                                    onSelection = { digit: Int, isInsertable: Boolean -> sudokuViewModel.onSelection(digit, isInsertable, settingsUIState.showMistakes) },
                                    cannotInsert = sudokuUIState.isNotesEnabled && currentCell != 0,
                                    canInsert = { sudokuViewModel.canInsert(it) },
                                    isNotesEnabled = sudokuUIState.isNotesEnabled,
                                    hapticFeedbackEnabled = settingsUIState.hapticFeedbackEnabled,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ActionToolbar(
                                    hasSteps = sudokuUIState.hasSteps,
                                    onUndo = { sudokuViewModel.onUndo() },
                                    onErase = { sudokuViewModel.onErase() },
                                    isNotesEnabled = sudokuUIState.isNotesEnabled,
                                    onNote = { sudokuViewModel.onNote() },
                                    useHint = { sudokuViewModel.useHint() },
                                    hintNum = sudokuUIState.hintNum
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            GameHeaderBar(
                                selectedLevel = sudokuUIState.selectedLevel,
                                hasMistakeCounter = settingsUIState.hasMistakeCounter && settingsUIState.showMistakes,
                                mistakesNum = sudokuUIState.mistakesNum,
                                onRegenerate = { sudokuViewModel.onRegenerate() },
                                hasTimer = settingsUIState.hasTimer,
                                isPaused = sudokuUIState.isPaused,
                                onPause = {
                                    sudokuViewModel.onPause()
                                    timerViewModel.pauseTimer()
                                },
                                timerViewModel = timerViewModel,
                                onShowSettings = { showSettings.value = true },
                                onRestart = {
                                    timerViewModel.stopTimer()
                                    isRestartClicked.value = true
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                SudokuGrid(
                                    grid = sudokuUIState.matrix,
                                    selectedCellRow = sudokuUIState.selectedCellRow,
                                    selectedCellColumn = sudokuUIState.selectedCellColumn,
                                    hasHighlightSameNumbers = settingsUIState.hasHighlightSameNumbers,
                                    onSelectCell = { row: Int, column: Int -> sudokuViewModel.onSelectCell(row, column) },
                                    hasRowHighlight = settingsUIState.hasRowHighlight,
                                    isPaused = sudokuUIState.isPaused,
                                    showMistakes = settingsUIState.showMistakes,
                                    unlockedCell = sudokuUIState.unlockedCell,
                                    gridWithNotes = sudokuUIState.matrixWithNotes,
                                    selectedDigit = sudokuUIState.selectedDigit,
                                    onBoxCheck = { sudokuViewModel.isUnusedInBox() },
                                    onRowCheck = { sudokuViewModel.isUnusedInRow() },
                                    onColumnCheck = { sudokuViewModel.isUnusedInColumn() },
                                    isNotesEnabled = sudokuUIState.isNotesEnabled,
                                    isLandscape = false
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SelectionNumbersRow(
                                    selectionNumbers = sudokuUIState.selectionNumbers,
                                    onSelection = { digit: Int, isInsertable: Boolean -> sudokuViewModel.onSelection(digit, isInsertable, settingsUIState.showMistakes) },
                                    cannotInsert = sudokuUIState.isNotesEnabled && currentCell != 0,
                                    canInsert = { sudokuViewModel.canInsert(it) },
                                    isNotesEnabled = sudokuUIState.isNotesEnabled,
                                    hapticFeedbackEnabled = settingsUIState.hapticFeedbackEnabled,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                ActionToolbar(
                                    hasSteps = sudokuUIState.hasSteps,
                                    onUndo = { sudokuViewModel.onUndo() },
                                    onErase = { sudokuViewModel.onErase() },
                                    isNotesEnabled = sudokuUIState.isNotesEnabled,
                                    onNote = { sudokuViewModel.onNote() },
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

    LaunchedEffect(Unit) {
        viewModel.rain()
    }

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
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            if (hasClosingButton) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { onDismiss() }) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Close icon"
                        )
                    }
                }
            }
            Text(
                text = "Choose Your Puzzle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            for (i in LEVELS.indices) {
                Surface(
                    onClick = { onStartGame(i) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = LEVELS[i],
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
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
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "You Won!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = "Difficulty: $level",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        if (hasTimer) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Time: $formattedTime",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onRegenerate() },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Text("New Game", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    onDismiss: () -> Unit,
    showMistakes: Boolean,
    hasMistakeCounter: Boolean,
    mistakesNum: Int = 0,
    hasHighlightSameNumbers: Boolean,
    hasRowHighlight: Boolean,
    hasTimer: Boolean,
    hapticFeedbackEnabled: Boolean,
    theme: String,
    onSave: (
        showMistakes: Boolean,
        hasMistakesCount: Boolean,
        theme: String,
        hasHighlightSameNumbers: Boolean,
        hasRowHighlight: Boolean,
        hasTimer: Boolean,
        hapticFeedbackEnabled: Boolean,
    ) -> Unit
) {
    val currentShowMistakes = remember { mutableStateOf(showMistakes) }
    val currentHasMistakeCounter = remember { mutableStateOf(if (showMistakes) hasMistakeCounter else false) }
    val currentHasHighlightSameNumbers = remember { mutableStateOf(hasHighlightSameNumbers) }
    val currentHasRowHighlight = remember { mutableStateOf(hasRowHighlight) }
    val currentHasTimer = remember { mutableStateOf(hasTimer) }
    val currentHapticFeedback = remember { mutableStateOf(hapticFeedbackEnabled) }
    val currentTheme = remember { mutableStateOf(theme) }

    val canDisableMistakes = mistakesNum == 0

    fun applySettings() {
        onSave(
            currentShowMistakes.value,
            if (currentShowMistakes.value) currentHasMistakeCounter.value else false,
            currentTheme.value,
            currentHasHighlightSameNumbers.value,
            currentHasRowHighlight.value,
            currentHasTimer.value,
            currentHapticFeedback.value
        )
    }

    BasicAlertDialog(
        onDismissRequest = { onDismiss() },
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = { onDismiss() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Close icon",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                val showMistakesSwitchEnabled = if (currentShowMistakes.value) canDisableMistakes else true

                // Auto-check mistakes
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Auto-check mistakes",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (showMistakesSwitchEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                    Switch(
                        enabled = showMistakesSwitchEnabled,
                        checked = currentShowMistakes.value,
                        onCheckedChange = { isChecked ->
                            currentShowMistakes.value = isChecked
                            if (!isChecked) {
                                currentHasMistakeCounter.value = false
                            }
                            applySettings()
                        }
                    )
                }

                val mistakeCounterSwitchEnabled = currentShowMistakes.value && (if (currentHasMistakeCounter.value) canDisableMistakes else true)

                // Mistake limit (3 strikes)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Mistake limit (3 strikes)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (mistakeCounterSwitchEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                    Switch(
                        enabled = mistakeCounterSwitchEnabled,
                        checked = currentShowMistakes.value && currentHasMistakeCounter.value,
                        onCheckedChange = { isChecked ->
                            currentHasMistakeCounter.value = isChecked
                            applySettings()
                        }
                    )
                }

                if (!canDisableMistakes && (currentShowMistakes.value || currentHasMistakeCounter.value)) {
                    Text(
                        text = "Mistake settings cannot be disabled during an active puzzle with mistakes.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Highlight same numbers
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(text = "Highlight same numbers", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = currentHasHighlightSameNumbers.value,
                        onCheckedChange = { isChecked ->
                            currentHasHighlightSameNumbers.value = isChecked
                            applySettings()
                        }
                    )
                }

                // Highlight row & column
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(text = "Highlight row & column", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = currentHasRowHighlight.value,
                        onCheckedChange = { isChecked ->
                            currentHasRowHighlight.value = isChecked
                            applySettings()
                        }
                    )
                }

                // Show timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(text = "Show timer", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = currentHasTimer.value,
                        onCheckedChange = { isChecked ->
                            currentHasTimer.value = isChecked
                            applySettings()
                        }
                    )
                }

                // Haptic feedback
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(text = "Haptic feedback", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = currentHapticFeedback.value,
                        onCheckedChange = { isChecked ->
                            currentHapticFeedback.value = isChecked
                            applySettings()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Theme Segmented Control
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(21.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val themes = listOf("system", "light", "dark")
                    themes.forEach { t ->
                        val isSelected = currentTheme.value.equals(t, ignoreCase = true)
                        val segmentBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        val segmentTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(21.dp))
                                .background(segmentBg)
                                .clickable {
                                    currentTheme.value = t
                                    applySettings()
                                }
                        ) {
                            Text(
                                text = formatThemeName(t),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = segmentTextColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onDismiss() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameOverDialog(
    onRegenerate: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = {},
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Game Over",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You made 3 mistakes and lost this game.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onRegenerate() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Restart")
            }
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
    onBoxCheck: () -> IntArray?,
    onRowCheck: () -> IntArray?,
    onColumnCheck: () -> IntArray?,
    isNotesEnabled: Boolean,
    isLandscape: Boolean = false
) {
    val darkBorderColor = MaterialTheme.colorScheme.outline
    val innerHairlineColor = MaterialTheme.colorScheme.outlineVariant
    val errorColor = MaterialTheme.colorScheme.error

    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, darkBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .aspectRatio(1f, matchHeightConstraintsFirst = isLandscape)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .drawWithContent {
                // 1. Draw cell backgrounds and content first
                drawContent()

                // 2. Foreground pass: Draw entire 9x9 grid overlay lines on top
                val cellWidth = size.width / 9f
                val cellHeight = size.height / 9f

                // Vertical grid lines (index 1..8)
                for (i in 1 until 9) {
                    val isBlockBorder = i % 3 == 0
                    val color = if (isBlockBorder) darkBorderColor else innerHairlineColor
                    val strokeWidth = if (isBlockBorder) 2.dp.toPx() else 1.dp.toPx()
                    val x = cellWidth * i
                    drawLine(
                        color = color,
                        start = Offset(x = x, y = 0f),
                        end = Offset(x = x, y = size.height),
                        strokeWidth = strokeWidth
                    )
                }

                // Horizontal grid lines (index 1..8)
                for (i in 1 until 9) {
                    val isBlockBorder = i % 3 == 0
                    val color = if (isBlockBorder) darkBorderColor else innerHairlineColor
                    val strokeWidth = if (isBlockBorder) 2.dp.toPx() else 1.dp.toPx()
                    val y = cellHeight * i
                    drawLine(
                        color = color,
                        start = Offset(x = 0f, y = y),
                        end = Offset(x = size.width, y = y),
                        strokeWidth = strokeWidth
                    )
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0 until 9) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        for (index in 0 until 9) {
                            val isCurrentCell = selectedCellRow == row && selectedCellColumn == index
                            var displayValue = ""
                            val gridValue = grid[row][index][2].intValue
                            if (gridValue != 0 && !isPaused) {
                                displayValue = gridValue.toString()
                            }

                            val currentGridCellValue = grid[selectedCellRow][selectedCellColumn][2].intValue
                            val expectedValue = grid[row][index][1].intValue
                            val initialGridValue = grid[row][index][0].intValue
                            val isMistake = gridValue != 0 && expectedValue != gridValue && showMistakes

                            val isSameNumber = gridValue > 0 && currentGridCellValue == gridValue && hasHighlightSameNumbers
                            val isRelated = (row == selectedCellRow || index == selectedCellColumn || (row / 3 == selectedCellRow / 3 && index / 3 == selectedCellColumn / 3)) && hasRowHighlight

                            val backgroundCellColour = when {
                                isMistake -> MaterialTheme.colorScheme.errorContainer
                                isCurrentCell -> MaterialTheme.colorScheme.primary
                                isSameNumber -> MaterialTheme.colorScheme.tertiaryContainer
                                isRelated -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }

                            val cellDescription = buildString {
                                append("Row ${row + 1}, Column ${index + 1}. ")
                                if (gridValue != 0) {
                                    append("Value $gridValue. ")
                                    if (initialGridValue != 0) {
                                        append("Given digit.")
                                    } else {
                                        append("Entered digit.")
                                    }
                                } else {
                                    val activeNotes = gridWithNotes[row][index].filter { it.intValue > 0 }.map { it.intValue }
                                    if (activeNotes.isNotEmpty()) {
                                        append("Notes: ${activeNotes.joinToString(", ")}.")
                                    } else {
                                        append("Empty cell.")
                                    }
                                }
                                if (isCurrentCell) {
                                    append(" Selected.")
                                }
                            }

                            val isDark = isSystemInDarkTheme()
                            val notesTextColor = if (isDark) NotesTextDark else NotesTextLight

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(backgroundCellColour)
                                    .semantics { contentDescription = cellDescription }
                                    .clickable { onSelectCell(row, index) }
                                    .drawBehind {
                                        // Highlight active selected cell border if selected cell has an error
                                        if (isCurrentCell && isMistake) {
                                            drawRect(
                                                color = errorColor,
                                                style = Stroke(width = 2.dp.toPx())
                                            )
                                        }
                                    }
                            ) {
                                val scale = remember { Animatable(1f) }
                                var shouldAnimate = unlockedCell[0] == row && unlockedCell[1] == index

                                if (isNotesEnabled && selectedDigit != 0) {
                                    val repeatedInBox = onBoxCheck()
                                    val repeatedInRow = onRowCheck()
                                    val repeatedInColumn = onColumnCheck()

                                    shouldAnimate = (repeatedInBox != null && row == repeatedInBox[0] && index == repeatedInBox[1]) ||
                                            (repeatedInRow != null && row == repeatedInRow[0] && index == repeatedInRow[1]) ||
                                            (repeatedInColumn != null && row == repeatedInColumn[0] && index == repeatedInColumn[1])
                                }

                                if (shouldAnimate) {
                                    LaunchedEffect(true) {
                                        scale.animateTo(1f, animationSpec = tween(0))
                                        scale.animateTo(2.5f, animationSpec = tween(350))
                                        scale.animateTo(1f, animationSpec = tween(350))
                                    }
                                } else {
                                    LaunchedEffect(true) {
                                        scale.animateTo(1f, animationSpec = tween(0))
                                    }
                                }

                                val gridWithNoteCell = gridWithNotes[row][index]
                                val hasNotesInCurrentCell = gridWithNoteCell.any { it.intValue > 0 }

                                if (hasNotesInCurrentCell && displayValue == "" && !isPaused) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        var noteIdx = 0
                                        repeat(3) {
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                repeat(3) {
                                                    val noteVal = gridWithNoteCell[noteIdx].intValue
                                                    val noteDisplay = if (noteVal == 0) "" else noteVal.toString()
                                                    Text(
                                                        text = noteDisplay,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = notesTextColor,
                                                        textAlign = TextAlign.Center,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    noteIdx++
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val textColour = when {
                                        isMistake -> MaterialTheme.colorScheme.error
                                        isCurrentCell -> MaterialTheme.colorScheme.onPrimary
                                        initialGridValue != 0 -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.secondary
                                    }
                                    val textWeight = if (initialGridValue != 0) FontWeight.Bold else FontWeight.Medium

                                    Text(
                                        text = displayValue,
                                        fontSize = 22.sp,
                                        fontWeight = textWeight,
                                        color = textColour,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = scale.value
                                            scaleY = scale.value
                                            transformOrigin = TransformOrigin.Center
                                        }
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
fun SelectionNumbersRow(
    selectionNumbers: Array<Int>,
    onSelection: (number: Int, canInsert: Boolean) -> Unit,
    cannotInsert: Boolean,
    canInsert: (number: Int) -> Boolean,
    isNotesEnabled: Boolean,
    hapticFeedbackEnabled: Boolean = true,
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
        if (isClicked.value && hapticFeedbackEnabled) {
            triggerVibration(vibrator)
            isClicked.value = false
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (i in 0 until 9) {
            val label = i + 1
            val remaining = selectionNumbers[i]
            val isCompleted = remaining == 0
            val isAvailable = remaining > 0 || isNotesEnabled

            val containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }

            val contentColor = if (isCompleted) {
                MaterialTheme.colorScheme.outline
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Surface(
                onClick = { onSelect(label) },
                enabled = isAvailable,
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = label.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (remaining > 0) remaining.toString() else "",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 3.dp, end = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SelectionNumbersGrid3x3(
    selectionNumbers: Array<Int>,
    onSelection: (number: Int, canInsert: Boolean) -> Unit,
    cannotInsert: Boolean,
    canInsert: (number: Int) -> Boolean,
    isNotesEnabled: Boolean,
    hapticFeedbackEnabled: Boolean = true,
    modifier: Modifier = Modifier
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
        if (isClicked.value && hapticFeedbackEnabled) {
            triggerVibration(vibrator)
            isClicked.value = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (col in 0 until 3) {
                    val idx = row * 3 + col
                    val label = idx + 1
                    val remaining = selectionNumbers[idx]
                    val isCompleted = remaining == 0
                    val isAvailable = remaining > 0 || isNotesEnabled

                    val containerColor = if (isCompleted) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }

                    val contentColor = if (isCompleted) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }

                    Surface(
                        onClick = { onSelect(label) },
                        enabled = isAvailable,
                        shape = RoundedCornerShape(12.dp),
                        color = containerColor,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = label.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (remaining > 0) remaining.toString() else "",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = contentColor.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 3.dp, end = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionToolbar(
    hasSteps: Boolean,
    onUndo: () -> Unit,
    onErase: () -> Unit,
    isNotesEnabled: Boolean,
    onNote: () -> Unit,
    useHint: () -> Unit,
    hintNum: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Undo Button
        Surface(
            onClick = { onUndo() },
            enabled = hasSteps,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 0.dp,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo icon",
                    tint = if (hasSteps) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Undo",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (hasSteps) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
            }
        }

        // Erase Button
        Surface(
            onClick = { onErase() },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 0.dp,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Erase cell icon",
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Erase",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Notes Button (Explicit ON / OFF active state)
        val notesContainerColor = if (isNotesEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        val notesContentColor = if (isNotesEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

        Surface(
            onClick = { onNote() },
            shape = RoundedCornerShape(12.dp),
            color = notesContainerColor,
            border = BorderStroke(1.dp, if (isNotesEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 0.dp,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Notes button",
                    tint = notesContentColor
                )
                Text(
                    text = if (isNotesEnabled) "Notes ON" else "Notes",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = notesContentColor
                )
            }
        }

        // Hints Button
        Surface(
            onClick = { if (hintNum > 0) useHint() },
            enabled = hintNum > 0,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 0.dp,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.Lightbulb,
                        contentDescription = "Hint icon",
                        tint = if (hintNum > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Hint",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (hintNum > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                ) {
                    Text(
                        text = hintNum.toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GameHeaderBar(
    selectedLevel: String,
    hasMistakeCounter: Boolean,
    mistakesNum: Int,
    onRegenerate: () -> Unit,
    hasTimer: Boolean,
    isPaused: Boolean,
    onPause: () -> Unit,
    timerViewModel: TimerViewModel,
    onShowSettings: () -> Unit,
    onRestart: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level Pill Badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shadowElevation = 0.dp
            ) {
                Text(
                    text = selectedLevel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Mistakes Pill Badge
            if (hasMistakeCounter) {
                Surface(
                    shape = CircleShape,
                    color = if (mistakesNum > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (mistakesNum > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = "Mistakes: $mistakesNum/3",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (mistakesNum > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                if (mistakesNum == 3) {
                    GameOverDialog(onRegenerate = onRegenerate)
                }
            }

            // Timer Pill Badge
            if (hasTimer) {
                val timerValue by timerViewModel.timer.collectAsState()
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = timerViewModel.formatTime(timerValue),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = onPause,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (isPaused) "Play icon" else "Pause icon",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Action icons (Restart, Settings)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                onClick = onRestart,
                shadowElevation = 0.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "Restart puzzle icon",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                onClick = onShowSettings,
                shadowElevation = 0.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings icon",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
