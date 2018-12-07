package com.jheank16oz.locationtracker

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 *
 *  <p>Location</p>
 */
@Entity
data class Location(
        @ColumnInfo(name = "latitude") var latitude: String?,
        @ColumnInfo(name = "longitude") var longitude: String?,
        @ColumnInfo(name = "datetime") var date: String?,
        @ColumnInfo(name = "type") var type: String?){
    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0
}