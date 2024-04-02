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

    fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _timer.value++
            }
        }
    }
    fun pauseTimer() {
        timerJob?.cancel()
    }

    fun stopTimer() {
        _timer.value = 0
        timerJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    fun formatTime(timer: Long): String {
        val hours = timer / 3600
        val minutes = (timer % 3600) / 60
        val remainingSeconds = timer % 60
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }

    init {
        startTimer()
    }
}