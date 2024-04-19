package com.nataliausoltseva.sudoku.settingsData

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: Context
): ViewModel() {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val THEME_KEY = stringPreferencesKey("theme")

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    /**
     * Saves the state what a user wants to allow for the current game state.
     * Theme is saved to datastore, so that next time a user enters the application, they do not have
     * to select it again. Other settings are dependent on the game and will be reset.
     */
    fun onSave(
        showMistakes: Boolean,
        hasMistakeCounter: Boolean,
        theme: String,
        hasHighlightSameNumbers: Boolean,
        hasRowHighlight: Boolean,
        hasTimer: Boolean,
    ) {
        _uiState.update {
            it.copy(
                showMistakes = showMistakes,
                hasMistakeCounter = hasMistakeCounter,
                theme = theme,
                hasHighlightSameNumbers = hasHighlightSameNumbers,
                hasRowHighlight = hasRowHighlight,
                hasTimer = hasTimer
            )
        }

        viewModelScope.launch {
            saveTheme(theme)
        }
    }

    /**
     * Tries to get a theme from datastore. If datastore was not created, returns system theme.
     */
    private suspend fun readTheme(): String {
        val settings = context.dataStore.data.first()
        return settings[THEME_KEY] ?: THEMES[0]
    }

    /**
     * Saves the selected theme into datastore.
     */
    private suspend fun saveTheme(theme: String) {
        context.dataStore.edit { settings ->
            settings[THEME_KEY] = theme
        }
    }

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    theme = readTheme()
                )
            }
        }
    }
}