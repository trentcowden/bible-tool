package com.trentcowden.bible

import androidx.lifecycle.viewModelScope
import com.trentcowden.bible.data.SettingsStore
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the settings screen. Exposes each setting as a StateFlow (Compose needs a
 * current value to draw the toggle) and a function to flip it. Writes go to the shared
 * DataStore, so the reader's own observer picks the change up.
 */
class SettingsViewModel(private val settingsStore: SettingsStore) : LightViewModel<Unit>() {

    val hideVerseNumbers: StateFlow<Boolean> =
        settingsStore.hideVerseNumbers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    fun toggleHideVerseNumbers() = viewModelScope.launch {
        settingsStore.setHideVerseNumbers(!hideVerseNumbers.value)
    }
}
