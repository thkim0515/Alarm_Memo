package com.thkim0515.alarmmemo.util

object Constants {
    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_IS_SNOOZE = "extra_is_snooze"

    const val NOTIFICATION_CHANNEL_ALARM = "channel_alarm"
    const val NOTIFICATION_ID_ALARM = 1001

    const val ACTION_DISMISS = "com.thkim0515.alarmmemo.action.DISMISS"
    const val ACTION_SNOOZE = "com.thkim0515.alarmmemo.action.SNOOZE"
    const val ACTION_RING = "com.thkim0515.alarmmemo.action.RING"

    const val AUTO_SNOOZE_TIMEOUT_MS = 2 * 60 * 1000L
}
