package com.thkim0515.alarmmemo.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.thkim0515.alarmmemo.data.Alarm
import com.thkim0515.alarmmemo.ui.list.AlarmListActivity
import com.thkim0515.alarmmemo.util.Constants
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) return
        val triggerAtMillis = nextTriggerTime(alarm.hour, alarm.minute)
        scheduleAt(alarm.id, triggerAtMillis, isSnooze = false)
    }

    fun scheduleSnooze(alarmId: Long, delayMinutes: Int) {
        val triggerAtMillis = System.currentTimeMillis() + delayMinutes * 60_000L
        scheduleAt(alarmId, triggerAtMillis, isSnooze = true)
    }

    fun cancel(alarmId: Long) {
        val pendingIntent = buildPendingIntent(alarmId, isSnooze = false)
        alarmManager.cancel(pendingIntent)
        val snoozePendingIntent = buildPendingIntent(alarmId, isSnooze = true)
        alarmManager.cancel(snoozePendingIntent)
    }

    private fun scheduleAt(alarmId: Long, triggerAtMillis: Long, isSnooze: Boolean) {
        val pendingIntent = buildPendingIntent(alarmId, isSnooze)
        val showIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            Intent(context, AlarmListActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
        alarmManager.setAlarmClock(info, pendingIntent)
    }

    private fun buildPendingIntent(alarmId: Long, isSnooze: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Constants.ACTION_RING
            putExtra(Constants.EXTRA_ALARM_ID, alarmId)
            putExtra(Constants.EXTRA_IS_SNOOZE, isSnooze)
        }
        val requestCode = requestCodeFor(alarmId, isSnooze)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCodeFor(alarmId: Long, isSnooze: Boolean): Int {
        return (alarmId * 2 + if (isSnooze) 1 else 0).toInt()
    }

    private fun nextTriggerTime(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }
}
