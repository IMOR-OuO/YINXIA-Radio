package com.example.soundbox.ui

import com.example.soundbox.data.AppSettings
import com.example.soundbox.data.MusicBox

sealed interface AppScreen {
    data object Home : AppScreen
    data object BoxManager : AppScreen
    data object BoxEdit : AppScreen
    data class PadEdit(val padId: String) : AppScreen
    data object About : AppScreen
}

data class AppUiState(
    val isLoading: Boolean = true,
    val boxes: List<MusicBox> = emptyList(),
    val currentBoxId: String? = null,
    val settings: AppSettings = AppSettings(),
    val screen: AppScreen = AppScreen.Home,
    val activeVoices: Map<String, Int> = emptyMap(),
    val busyMessage: String? = null,
    val message: String? = null
) {
    val currentBox: MusicBox?
        get() = boxes.firstOrNull { it.id == currentBoxId } ?: boxes.firstOrNull()

    val activeVoiceCount: Int
        get() = activeVoices.values.sum()
}
