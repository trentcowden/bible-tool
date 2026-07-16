package com.trentcowden.bible.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.trentcowden.bible.BibleRef
import kotlinx.coroutines.flow.first

/**
 * Persists the last chapter the user was reading, using the SDK-provided Preferences
 * DataStore (get it from lightContext.dataStore). This is the pattern to reuse for
 * other small settings later — font size, theme, etc.
 */
class ReadingPositionStore(private val dataStore: DataStore<Preferences>) {

    /** The saved position, or Genesis 1 the first time the tool is ever opened. */
    suspend fun load(): BibleRef {
        val prefs = dataStore.data.first()
        return BibleRef(
            book = prefs[BOOK_KEY] ?: DEFAULT_BOOK,
            chapter = prefs[CHAPTER_KEY] ?: DEFAULT_CHAPTER,
        )
    }

    suspend fun save(book: Int, chapter: Int) {
        dataStore.edit { prefs ->
            prefs[BOOK_KEY] = book
            prefs[CHAPTER_KEY] = chapter
        }
    }

    private companion object {
        val BOOK_KEY = intPreferencesKey("reading_book")
        val CHAPTER_KEY = intPreferencesKey("reading_chapter")
        const val DEFAULT_BOOK = 1      // Genesis
        const val DEFAULT_CHAPTER = 1
    }
}
