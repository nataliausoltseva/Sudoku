package com.nataliausoltseva.sudoku

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

val THEMES = arrayOf("system", "light", "dark")

class SettingsViewModel: ViewModel() {
    private var _showMistakes = MutableStateFlow(true)
    private var _hasMistakeCounter = MutableStateFlow(true)
    private var _theme = MutableStateFlow(THEMES[0])
    private var _hasHighlightSameNumbers = MutableStateFlow(true)
    private var _hasRowHighlight = MutableStateFlow(true)
    private var _hasTimer = MutableStateFlow(true)

    var showMistakes = _showMistakes.asStateFlow()
    var hasMistakeCounter = _hasMistakeCounter.asStateFlow()
    var theme = _theme.asStateFlow()
    var hasHighlightSameNumbers = _hasHighlightSameNumbers.asStateFlow()
    var hasRowHighlight = _hasRowHighlight.asStateFlow()
    var hasTimer = _hasTimer.asStateFlow()

    fun onSave(
        showMistakes: Boolean,
        hasMistakesCounter: Boolean,
        theme: String,
        hasHighlightSameNumbers: Boolean,
        hasRowHighlight: Boolean,
        hasTimer: Boolean,
    ) {
        _showMistakes.value = showMistakes
        _hasMistakeCounter.value = hasMistakesCounter
        _theme.value = theme
        _hasHighlightSameNumbers.value = hasHighlightSameNumbers
        _hasRowHighlight.value = hasRowHighlight
        _hasTimer.value = hasTimer
    }
}