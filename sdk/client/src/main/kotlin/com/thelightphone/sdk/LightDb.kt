package com.thelightphone.sdk

import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Build a Room database.
 *
 * @param dbName the on-device filename for the database.
 * @param fromAsset if set, seeds the database from this file in the tool's `assets/`
 *   directory on first launch (see Room's `createFromAsset`). Use this to ship a
 *   prebuilt, read-only database. Requires a non-null [dbName].
 */
fun <T : RoomDatabase> SealedLightContext.buildDatabase(
    dbClass: Class<T>,
    dbName: String?,
    fromAsset: String? = null,
): T {
    return Room.databaseBuilder(androidContext.applicationContext, dbClass, dbName)
        .apply { fromAsset?.let { createFromAsset(it) } }
        .build()
}