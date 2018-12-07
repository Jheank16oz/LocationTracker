package com.jheank16oz.locationtracker

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

/**
 *
 *  <p>UserDao</p>
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM location")
    fun getAll(): List<Location>

    @Query("SELECT * FROM location WHERE uid IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): List<Location>

    @Insert
    fun insertAll(vararg users: Location)

    @Insert
    fun insert(user: Location)

    @Delete
    fun delete(user: Location)
}