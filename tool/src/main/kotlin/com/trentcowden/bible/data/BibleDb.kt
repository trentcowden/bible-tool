package com.trentcowden.bible.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The Room database. `version` must match `PRAGMA user_version` baked into the
 * prebuilt bible.db (currently 1). exportSchema is off because this DB is
 * read-only and shipped prebuilt — there are no migrations to track.
 */
@Database(entities = [Verse::class], version = 1, exportSchema = false)
abstract class BibleDb : RoomDatabase() {
    abstract fun verseDao(): VerseDao
}
