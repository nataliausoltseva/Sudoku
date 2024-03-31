package com.nataliausoltseva.sudoku

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val THEMES = arrayOf("system", "light", "dark")

class SettingsViewModel(
    private val context: Context
): ViewModel() {
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val THEME_KEY = stringPreferencesKey("theme")

    private var _showMistakes = MutableStateFlow(true)
    private var _hasMistakeCounter = MutableStateFlow(true)
    private var _theme = MutableStateFlow("")
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

        viewModelScope.launch {
            saveTheme(theme)
        }
    }

    private suspend fun readTheme(): String {
        val settings = context.dataStore.data.first()
        return settings[THEME_KEY] ?: ""
    }
    private suspend fun saveTheme(theme: String) {
        context.dataStore.edit { settings ->
            settings[THEME_KEY] = theme
        }
    }

    init {
        viewModelScope.launch {
            _theme.value = readTheme()
        }
    }
}