package com.aceyan.mytodo.checklist

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aceyan.mytodo.utils.TimeUtil
import com.ysw.android.extensions.currentTimeMillis

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey
    val id: Long = currentTimeMillis,
    var updateTime: Long = currentTimeMillis,
    var content: String = "",
    var reminderTime: Long = -1L,
    var done: Boolean = false,
    var picturePath: String = "",
    var audioPath: String = ""
) : Parcelable, Comparable<Todo?> {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readBoolean(),
        parcel.readString()!!,
        parcel.readString()!!
    ) {
    }

    fun hasPicture() = picturePath.isNotEmpty()
    fun hasContent() = content.isNotEmpty()
    fun hasReminder() = reminderTime > -1L
    fun hasAudio() = audioPath.isNotEmpty()

    fun getReminderTimeFormat() = TimeUtil.getTimeStrFromTime(reminderTime)
    fun getUpdateTimeFormat() = "更新于${TimeUtil.getTimeStrFromTime(updateTime)}"

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeLong(updateTime)
        parcel.writeString(content)
        parcel.writeLong(reminderTime)
        parcel.writeBoolean(done)
        parcel.writeString(picturePath)
        parcel.writeString(audioPath)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Todo

        if (id != other.id) return false
        if (updateTime != other.updateTime) return false
        if (content != other.content) return false
        if (reminderTime != other.reminderTime) return false
        if (done != other.done) return false

        return true
    }

    override fun compareTo(other: Todo?): Int {
        if (this == other) {
            return 0
        }
        return -1
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + updateTime.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + reminderTime.hashCode()
        result = 31 * result + done.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<Todo> {
        override fun createFromParcel(parcel: Parcel): Todo {
            return Todo(parcel)
        }

        override fun newArray(size: Int): Array<Todo?> {
            return arrayOfNulls(size)
        }
    }
}
