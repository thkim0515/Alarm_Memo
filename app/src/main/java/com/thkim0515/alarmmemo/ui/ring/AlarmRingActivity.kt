package com.thkim0515.alarmmemo.ui.ring

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.thkim0515.alarmmemo.R
import com.thkim0515.alarmmemo.alarm.AlarmRingService
import com.thkim0515.alarmmemo.data.Alarm
import com.thkim0515.alarmmemo.data.AlarmRepository
import com.thkim0515.alarmmemo.databinding.ActivityAlarmRingBinding
import com.thkim0515.alarmmemo.util.Constants
import kotlinx.coroutines.launch

class AlarmRingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmRingBinding
    private var alarmId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowFlags()

        binding = ActivityAlarmRingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alarmId = intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) {
            finish()
            return
        }

        loadAlarm(alarmId)
        setupSlideToDismiss()

        binding.buttonSnooze.setOnClickListener {
            sendServiceAction(Constants.ACTION_SNOOZE)
            finish()
        }
    }

    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun loadAlarm(id: Long) {
        lifecycleScope.launch {
            val alarm = AlarmRepository.getInstance(applicationContext).getAlarm(id)
            if (alarm == null) {
                finish()
                return@launch
            }
            bindAlarm(alarm)
        }
    }

    private fun bindAlarm(alarm: Alarm) {
        binding.textTime.text = alarm.timeText
        binding.textMemo.text = alarm.memo

        val remaining = alarm.snoozeMaxCount - AlarmRingService.usedSnoozeCount(alarm.id)
        if (alarm.snoozeEnabled && remaining > 0) {
            binding.buttonSnooze.visibility = android.view.View.VISIBLE
            binding.textSnoozeRemaining.visibility = android.view.View.VISIBLE
            binding.textSnoozeRemaining.text = getString(
                R.string.snooze_remaining_format,
                AlarmRingService.usedSnoozeCount(alarm.id),
                alarm.snoozeMaxCount
            )
        } else {
            binding.buttonSnooze.visibility = android.view.View.GONE
            binding.textSnoozeRemaining.visibility = android.view.View.GONE
        }
    }

    private fun setupSlideToDismiss() {
        binding.seekBarDismiss.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (progress >= 92) {
                    dismissAlarm()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (seekBar.progress < 92) {
                    seekBar.progress = 0
                }
            }
        })
    }

    private fun dismissAlarm() {
        sendServiceAction(Constants.ACTION_DISMISS)
        finish()
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(this, AlarmRingService::class.java).apply {
            this.action = action
            putExtra(Constants.EXTRA_ALARM_ID, alarmId)
        }
        startService(intent)
    }

    override fun onBackPressed() {
        // Alarm must be dismissed or snoozed explicitly; ignore back button.
    }
}
