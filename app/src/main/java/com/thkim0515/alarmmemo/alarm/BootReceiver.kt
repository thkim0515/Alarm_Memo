package com.thkim0515.alarmmemo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.thkim0515.alarmmemo.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = AlarmRepository.getInstance(appContext)
                val scheduler = AlarmScheduler(appContext)
                repository.getEnabledAlarms().forEach { alarm ->
                    scheduler.schedule(alarm)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
