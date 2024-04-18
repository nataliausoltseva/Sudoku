package com.nataliausoltseva.sudoku.timerData

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerViewModel: ViewModel() {
    private val _timer = MutableStateFlow(0L)
    var timer = _timer.asStateFlow()

    private var timerJob: Job? = null

    /**
     * Starts the timer. This can be used as initial or resuming state,
     */
    fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _timer.value++
            }
        }
    }

    /**
     * Pauses the timer so that it would not go on, if the game is paused or finished.
     */
    fun pauseTimer() {
        timerJob?.cancel()
    }

    /**
     * Resets the timer to 0 so that it can be started again once the game has started.
     */
    fun stopTimer() {
        _timer.value = 0
        timerJob?.cancel()
    }

    /**
     * Gets formatted timer based on the state.
     */
    fun getTimer(): String {
        return formatTime(_timer.value)
    }

    /**
     * Returns formatted string for UI.
     */
    fun formatTime(timer: Long): String {
        val hours = timer / 3600
        val minutes = (timer % 3600) / 60
        val remainingSeconds = timer % 60
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    init {
        startTimer()
    }
}