package com.jheank16oz.locationtracker

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable

/**
 *
 *  <p>Location</p>
 */
@Entity
data class Location(
        @ColumnInfo(name = "latitude") var latitude: String?,
        @ColumnInfo(name = "longitude") var longitude: String?,
        @ColumnInfo(name = "datetime") var date: String?,
        @ColumnInfo(name = "type") var type: String?) : Parcelable {
    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString()) {
        uid = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(latitude)
        parcel.writeString(longitude)
        parcel.writeString(date)
        parcel.writeString(type)
        parcel.writeInt(uid)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "$date $latitude/$longitude timeOff=$type \n\n"
    }

    companion object CREATOR : Parcelable.Creator<Location> {
        override fun createFromParcel(parcel: Parcel): Location {
            return Location(parcel)
        }

        override fun newArray(size: Int): Array<Location?> {
            return arrayOfNulls(size)
        }
    }


}

