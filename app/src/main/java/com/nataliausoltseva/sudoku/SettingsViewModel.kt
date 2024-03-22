package com.nataliausoltseva.sudoku

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

val THEMES = arrayOf("system", "light", "dark")

class SettingsViewModel: ViewModel() {
    private var _showMistakes = MutableStateFlow(true)
    private var _hasMistakeCounter = MutableStateFlow(true)
    private var _theme = MutableStateFlow(THEMES[0])

    var showMistakes = _showMistakes.asStateFlow()
    var hasMistakeCounter = _hasMistakeCounter.asStateFlow()
    var theme = _theme.asStateFlow()

    fun onSave(showMistakes: Boolean, hasMistakesCounter: Boolean, theme: String) {
        _showMistakes.value = showMistakes
        _hasMistakeCounter.value = hasMistakesCounter
        _theme.value = theme
    }
}