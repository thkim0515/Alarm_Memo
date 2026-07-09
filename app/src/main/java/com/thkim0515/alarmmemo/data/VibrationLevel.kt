package com.thkim0515.alarmmemo.data

enum class VibrationLevel(val value: Int) {
    OFF(0),
    WEAK(1),
    MEDIUM(2),
    STRONG(3);

    companion object {
        fun fromValue(value: Int): VibrationLevel = entries.firstOrNull { it.value == value } ?: MEDIUM
    }
}
