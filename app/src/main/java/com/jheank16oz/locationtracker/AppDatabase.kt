package com.jheank16oz.locationtracker

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

/**
 *
 *  <p>AppDatabase</p>
 */
@Database(entities = arrayOf(Location::class), version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}