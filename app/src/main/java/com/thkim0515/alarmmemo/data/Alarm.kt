package com.thkim0515.alarmmemo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val memo: String = "",
    val vibrationLevel: Int = VibrationLevel.MEDIUM.value,
    val snoozeEnabled: Boolean = true,
    val snoozeIntervalMinutes: Int = 5,
    val snoozeMaxCount: Int = 3,
    val isEnabled: Boolean = true,
    /** Bitmask of [Calendar.SUNDAY]..[Calendar.SATURDAY] bits. 0 means a one-time alarm. */
    val repeatDays: Int = 0
) {
    val timeText: String
        get() = "%02d:%02d".format(hour, minute)

    val isRepeating: Boolean
        get() = repeatDays != 0

    fun isDaySelected(calendarDayOfWeek: Int): Boolean = (repeatDays and (1 shl calendarDayOfWeek)) != 0

    companion object {
        fun dayBit(calendarDayOfWeek: Int): Int = 1 shl calendarDayOfWeek

        const val ALL_DAYS = (1 shl Calendar.SUNDAY) or (1 shl Calendar.MONDAY) or (1 shl Calendar.TUESDAY) or
            (1 shl Calendar.WEDNESDAY) or (1 shl Calendar.THURSDAY) or (1 shl Calendar.FRIDAY) or
            (1 shl Calendar.SATURDAY)

        const val WEEKDAYS = (1 shl Calendar.MONDAY) or (1 shl Calendar.TUESDAY) or (1 shl Calendar.WEDNESDAY) or
            (1 shl Calendar.THURSDAY) or (1 shl Calendar.FRIDAY)

        const val WEEKEND = (1 shl Calendar.SATURDAY) or (1 shl Calendar.SUNDAY)
    }
}
