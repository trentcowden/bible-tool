package com.trentcowden.bible.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists the reader's preferences. Backed by the same Preferences DataStore as
 * [ReadingPositionStore] (both come from lightContext.dataStore), so a change made on
 * the settings screen is seen everywhere that observes it.
 *
 * Unlike ReadingPositionStore, which reads once with load(), this exposes a Flow: the
 * value plus every later change. That lets the reader update live when the setting is
 * toggled and you navigate back.
 */
class SettingsStore(private val dataStore: DataStore<Preferences>) {

    /** Whether verse numbers should be hidden. Defaults to false (numbers shown). */
    val hideVerseNumbers: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[HIDE_VERSE_NUMBERS_KEY] ?: false }

    suspend fun setHideVerseNumbers(hide: Boolean) {
        dataStore.edit { prefs -> prefs[HIDE_VERSE_NUMBERS_KEY] = hide }
    }

    private companion object {
        val HIDE_VERSE_NUMBERS_KEY = booleanPreferencesKey("hide_verse_numbers")
    }
}
