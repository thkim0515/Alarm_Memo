package com.thkim0515.alarmmemo.util

import android.content.Context
import com.thkim0515.alarmmemo.R
import com.thkim0515.alarmmemo.data.Alarm
import java.util.Calendar

object DayOfWeek {

    /** Calendar.SUNDAY..Calendar.SATURDAY, in that order. */
    val ORDERED_DAYS = listOf(
        Calendar.SUNDAY,
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY
    )

    fun shortLabel(context: Context, calendarDayOfWeek: Int): String {
        val resId = when (calendarDayOfWeek) {
            Calendar.SUNDAY -> R.string.day_sun
            Calendar.MONDAY -> R.string.day_mon
            Calendar.TUESDAY -> R.string.day_tue
            Calendar.WEDNESDAY -> R.string.day_wed
            Calendar.THURSDAY -> R.string.day_thu
            Calendar.FRIDAY -> R.string.day_fri
            else -> R.string.day_sat
        }
        return context.getString(resId)
    }

    fun repeatSummary(context: Context, repeatDays: Int, short: Boolean = false): String {
        return when (repeatDays) {
            0 -> context.getString(if (short) R.string.repeat_once_short else R.string.repeat_once)
            Alarm.ALL_DAYS -> context.getString(R.string.repeat_every_day)
            Alarm.WEEKDAYS -> context.getString(R.string.repeat_weekdays)
            Alarm.WEEKEND -> context.getString(R.string.repeat_weekend)
            else -> ORDERED_DAYS.filter { (repeatDays and Alarm.dayBit(it)) != 0 }
                .joinToString(" ") { shortLabel(context, it) }
        }
    }
}
