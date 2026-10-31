package com.nataliausoltseva.sudoku.settingsData

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsViewModel(
    context: Context
) : ViewModel() {
    private val appContext = context.applicationContext
    private val THEME_KEY = stringPreferencesKey("theme")
    private val HAPTIC_KEY = booleanPreferencesKey("haptic_feedback")

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    fun onSave(
        showMistakes: Boolean,
        hasMistakeCounter: Boolean,
        theme: String,
        hasHighlightSameNumbers: Boolean,
        hasRowHighlight: Boolean,
        hasTimer: Boolean,
        hapticFeedbackEnabled: Boolean,
    ) {
        _uiState.update {
            it.copy(
                showMistakes = showMistakes,
                hasMistakeCounter = hasMistakeCounter,
                theme = theme,
                hasHighlightSameNumbers = hasHighlightSameNumbers,
                hasRowHighlight = hasRowHighlight,
                hasTimer = hasTimer,
                hapticFeedbackEnabled = hapticFeedbackEnabled
            )
        }

        viewModelScope.launch {
            saveThemeAndHaptics(theme, hapticFeedbackEnabled)
        }
    }

    private suspend fun readSettings(): Pair<String, Boolean> {
        val settings = appContext.dataStore.data.first()
        val theme = settings[THEME_KEY] ?: THEMES[0]
        val haptic = settings[HAPTIC_KEY] ?: true
        return Pair(theme, haptic)
    }

    private suspend fun saveThemeAndHaptics(theme: String, haptic: Boolean) {
        appContext.dataStore.edit { settings ->
            settings[THEME_KEY] = theme
            settings[HAPTIC_KEY] = haptic
        }
    }

    init {
        viewModelScope.launch {
            val (savedTheme, savedHaptic) = readSettings()
            _uiState.update {
                it.copy(
                    theme = savedTheme,
                    hapticFeedbackEnabled = savedHaptic
                )
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(context) as T
        }
    }
}