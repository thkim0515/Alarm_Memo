package com.thkim0515.alarmmemo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.thkim0515.alarmmemo.util.Constants

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) return
        val isSnooze = intent.getBooleanExtra(Constants.EXTRA_IS_SNOOZE, false)

        val serviceIntent = Intent(context, AlarmRingService::class.java).apply {
            action = Constants.ACTION_RING
            putExtra(Constants.EXTRA_ALARM_ID, alarmId)
            putExtra(Constants.EXTRA_IS_SNOOZE, isSnooze)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
