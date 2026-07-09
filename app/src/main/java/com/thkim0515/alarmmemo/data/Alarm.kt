package com.thkim0515.alarmmemo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val isEnabled: Boolean = true
) {
    val timeText: String
        get() = "%02d:%02d".format(hour, minute)
}
