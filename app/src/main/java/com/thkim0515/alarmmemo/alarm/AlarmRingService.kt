package com.thkim0515.alarmmemo.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.thkim0515.alarmmemo.R
import com.thkim0515.alarmmemo.data.Alarm
import com.thkim0515.alarmmemo.data.AlarmRepository
import com.thkim0515.alarmmemo.data.VibrationLevel
import com.thkim0515.alarmmemo.ui.ring.AlarmRingActivity
import com.thkim0515.alarmmemo.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AlarmRingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var repository: AlarmRepository? = null
    private var scheduler: AlarmScheduler? = null

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    private var currentAlarm: Alarm? = null
    private var currentJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = AlarmRepository.getInstance(applicationContext)
        scheduler = AlarmScheduler(applicationContext)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val alarmId = intent?.getLongExtra(Constants.EXTRA_ALARM_ID, -1L) ?: -1L

        when (action) {
            Constants.ACTION_DISMISS -> {
                onDismiss()
                return START_NOT_STICKY
            }
            Constants.ACTION_SNOOZE -> {
                onSnooze()
                return START_NOT_STICKY
            }
            else -> {
                if (alarmId < 0) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                val isSnooze = intent?.getBooleanExtra(Constants.EXTRA_IS_SNOOZE, false) ?: false
                startRinging(alarmId, isSnooze)
            }
        }
        return START_STICKY
    }

    private fun startRinging(alarmId: Long, isSnooze: Boolean) {
        acquireWakeLock()
        if (!isSnooze) {
            snoozeCounts.remove(alarmId)
        }

        currentJob?.cancel()
        currentJob = serviceScope.launch {
            val alarm = repository?.getAlarm(alarmId) ?: run { stopSelf(); return@launch }
            if (!alarm.isEnabled) { stopSelf(); return@launch }
            currentAlarm = alarm

            startForeground(Constants.NOTIFICATION_ID_ALARM, buildNotification(alarm))
            if (alarm.soundEnabled) {
                playSound(alarm.soundUri)
            }
            startVibration(VibrationLevel.fromValue(alarm.vibrationLevel))
            scheduleAutoTimeout(alarm)
        }
    }

    private fun buildNotification(alarm: Alarm): Notification {
        val fullScreenIntent = Intent(this, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(Constants.EXTRA_ALARM_ID, alarm.id)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarm.id.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = PendingIntent.getService(
            this,
            (alarm.id * 10 + 1).toInt(),
            Intent(this, AlarmRingService::class.java).apply {
                action = Constants.ACTION_DISMISS
                putExtra(Constants.EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (alarm.memo.isBlank()) getString(R.string.app_name) else alarm.memo

        val builder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(alarm.timeText)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(R.drawable.ic_close, getString(R.string.slide_to_dismiss), dismissIntent)

        if (alarm.snoozeEnabled && remainingSnoozeCount(alarm) > 0) {
            val snoozeIntent = PendingIntent.getService(
                this,
                (alarm.id * 10 + 2).toInt(),
                Intent(this, AlarmRingService::class.java).apply {
                    action = Constants.ACTION_SNOOZE
                    putExtra(Constants.EXTRA_ALARM_ID, alarm.id)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_snooze, getString(R.string.snooze_button), snoozeIntent)
        }

        return builder.build()
    }

    private fun playSound(customSoundUri: String?) {
        stopSound()
        val customUri = customSoundUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (customUri == null || !tryPlay(customUri)) {
            val defaultUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            tryPlay(defaultUri)
        }
    }

    private fun tryPlay(uri: Uri): Boolean {
        return try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingService, uri)
                isLooping = true
                prepare()
                start()
            }
            true
        } catch (_: Exception) {
            // If sound fails to play, alarm still rings via vibration.
            stopSound()
            false
        }
    }

    private fun stopSound() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    private fun startVibration(level: VibrationLevel) {
        val v = vibrator ?: return
        if (level == VibrationLevel.OFF || !v.hasVibrator()) return

        val pattern = when (level) {
            VibrationLevel.WEAK -> longArrayOf(0, 200, 800)
            VibrationLevel.MEDIUM -> longArrayOf(0, 500, 500)
            VibrationLevel.STRONG -> longArrayOf(0, 900, 200)
            VibrationLevel.OFF -> return
        }
        val amplitude = when (level) {
            VibrationLevel.WEAK -> 80
            VibrationLevel.MEDIUM -> 160
            VibrationLevel.STRONG -> 255
            VibrationLevel.OFF -> 0
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitudes = intArrayOf(0, amplitude, 0)
            val effect = VibrationEffect.createWaveform(pattern, amplitudes, 0)
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(pattern, 0)
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    private fun scheduleAutoTimeout(alarm: Alarm) {
        cancelAutoTimeout()
        timeoutRunnable = Runnable {
            if (alarm.snoozeEnabled && remainingSnoozeCount(alarm) > 0) {
                onSnooze()
            } else {
                onDismiss()
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable!!, Constants.AUTO_SNOOZE_TIMEOUT_MS)
    }

    private fun cancelAutoTimeout() {
        timeoutRunnable?.let { timeoutHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun remainingSnoozeCount(alarm: Alarm): Int {
        val used = snoozeCounts[alarm.id] ?: 0
        return (alarm.snoozeMaxCount - used).coerceAtLeast(0)
    }

    private fun onSnooze() {
        val alarm = currentAlarm ?: return
        val used = (snoozeCounts[alarm.id] ?: 0) + 1
        snoozeCounts[alarm.id] = used
        scheduler?.scheduleSnooze(alarm.id, alarm.snoozeIntervalMinutes)
        stopRingingAndFinish()
    }

    private fun onDismiss() {
        val alarm = currentAlarm
        if (alarm != null) {
            snoozeCounts.remove(alarm.id)
            if (alarm.isRepeating) {
                if (alarm.isEnabled) {
                    scheduler?.schedule(alarm)
                }
            } else {
                serviceScope.launch { repository?.update(alarm.copy(isEnabled = false)) }
            }
        }
        stopRingingAndFinish()
    }

    private fun stopRingingAndFinish() {
        cancelAutoTimeout()
        stopSound()
        stopVibration()
        releaseWakeLock()
        currentJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlarmMemo:AlarmRingWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        stopRingingAndFinish()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private val snoozeCounts = mutableMapOf<Long, Int>()

        fun usedSnoozeCount(alarmId: Long): Int = snoozeCounts[alarmId] ?: 0
    }
}
