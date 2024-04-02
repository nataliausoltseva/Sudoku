package com.nataliausoltseva.sudoku.settingsData

val THEMES = arrayOf("system", "light", "dark")

data class SettingsState(
    val showMistakes: Boolean = true,
    val hasMistakeCounter: Boolean = true,
    val theme: String = THEMES[0],
    val hasHighlightSameNumbers: Boolean = true,
    val hasRowHighlight: Boolean = true,
    val hasTimer: Boolean = true,
)