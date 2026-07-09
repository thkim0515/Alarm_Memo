package com.thkim0515.alarmmemo.ui.edit

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.thkim0515.alarmmemo.R
import com.thkim0515.alarmmemo.alarm.AlarmScheduler
import com.thkim0515.alarmmemo.data.Alarm
import com.thkim0515.alarmmemo.data.AlarmRepository
import com.thkim0515.alarmmemo.data.VibrationLevel
import com.thkim0515.alarmmemo.databinding.ActivityAlarmEditBinding
import com.thkim0515.alarmmemo.util.Constants
import com.thkim0515.alarmmemo.util.DayOfWeek
import kotlinx.coroutines.launch

class AlarmEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmEditBinding
    private lateinit var repository: AlarmRepository
    private lateinit var scheduler: AlarmScheduler

    private var editingAlarm: Alarm? = null
    private val alarmId: Long by lazy { intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L) }

    private val snoozeIntervalOptions = listOf(5, 10, 15, 30)
    private val snoozeCountOptions = listOf(1, 2, 3, 5, 10)

    private val dayToggles = mutableMapOf<Int, TextView>()
    private var repeatDays = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AlarmRepository.getInstance(applicationContext)
        scheduler = AlarmScheduler(applicationContext)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.timePicker.setIs24HourView(true)

        setupDayToggles()
        setupSpinners()
        setupSnoozeToggle()
        updateRepeatSummary()

        binding.toolbar.title = getString(
            if (alarmId >= 0) R.string.edit_alarm_title_edit else R.string.edit_alarm_title_new
        )

        if (alarmId >= 0) {
            loadAlarm(alarmId)
        }

        binding.buttonSave.setOnClickListener { save() }
    }

    private fun setupDayToggles() {
        val size = (40 * resources.displayMetrics.density).toInt()
        DayOfWeek.ORDERED_DAYS.forEach { calendarDay ->
            val toggle = TextView(this).apply {
                text = DayOfWeek.shortLabel(this@AlarmEditActivity, calendarDay)
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(ContextCompat.getColorStateList(this@AlarmEditActivity, R.color.day_toggle_text))
                background = ContextCompat.getDrawable(this@AlarmEditActivity, R.drawable.bg_day_toggle)
                isSelected = false
                setOnClickListener {
                    isSelected = !isSelected
                    repeatDays = if (isSelected) {
                        repeatDays or Alarm.dayBit(calendarDay)
                    } else {
                        repeatDays and Alarm.dayBit(calendarDay).inv()
                    }
                    updateRepeatSummary()
                }
            }
            val params = LinearLayout.LayoutParams(size, size).apply {
                marginStart = (4 * resources.displayMetrics.density).toInt()
                marginEnd = (4 * resources.displayMetrics.density).toInt()
            }
            binding.dayToggleRow.addView(toggle, params)
            dayToggles[calendarDay] = toggle
        }
    }

    private fun setSelectedDays(mask: Int) {
        repeatDays = mask
        dayToggles.forEach { (day, view) -> view.isSelected = (mask and Alarm.dayBit(day)) != 0 }
        updateRepeatSummary()
    }

    private fun updateRepeatSummary() {
        binding.textRepeatSummary.text = DayOfWeek.repeatSummary(this, repeatDays)
    }

    private fun setupSpinners() {
        binding.spinnerSnoozeInterval.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            snoozeIntervalOptions.map { getString(R.string.snooze_interval_format, it) }
        )
        binding.spinnerSnoozeCount.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            snoozeCountOptions.map { getString(R.string.snooze_count_format, it) }
        )
    }

    private fun setupSnoozeToggle() {
        binding.switchSnooze.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutSnoozeOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun loadAlarm(id: Long) {
        lifecycleScope.launch {
            val alarm = repository.getAlarm(id) ?: return@launch
            editingAlarm = alarm
            bindAlarm(alarm)
        }
    }

    private fun bindAlarm(alarm: Alarm) {
        binding.timePicker.hour = alarm.hour
        binding.timePicker.minute = alarm.minute
        binding.editMemo.setText(alarm.memo)
        setSelectedDays(alarm.repeatDays)

        val radioId = when (VibrationLevel.fromValue(alarm.vibrationLevel)) {
            VibrationLevel.OFF -> R.id.radioVibrationOff
            VibrationLevel.WEAK -> R.id.radioVibrationWeak
            VibrationLevel.MEDIUM -> R.id.radioVibrationMedium
            VibrationLevel.STRONG -> R.id.radioVibrationStrong
        }
        binding.radioGroupVibration.check(radioId)

        binding.switchSnooze.isChecked = alarm.snoozeEnabled
        binding.layoutSnoozeOptions.visibility = if (alarm.snoozeEnabled) View.VISIBLE else View.GONE

        val intervalIndex = snoozeIntervalOptions.indexOf(alarm.snoozeIntervalMinutes).coerceAtLeast(0)
        binding.spinnerSnoozeInterval.setSelection(intervalIndex)
        val countIndex = snoozeCountOptions.indexOf(alarm.snoozeMaxCount).coerceAtLeast(0)
        binding.spinnerSnoozeCount.setSelection(countIndex)
    }

    private fun save() {
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute

        val vibrationLevel = when (binding.radioGroupVibration.checkedRadioButtonId) {
            R.id.radioVibrationOff -> VibrationLevel.OFF
            R.id.radioVibrationWeak -> VibrationLevel.WEAK
            R.id.radioVibrationStrong -> VibrationLevel.STRONG
            else -> VibrationLevel.MEDIUM
        }

        val snoozeEnabled = binding.switchSnooze.isChecked
        val snoozeInterval = snoozeIntervalOptions[binding.spinnerSnoozeInterval.selectedItemPosition]
        val snoozeCount = snoozeCountOptions[binding.spinnerSnoozeCount.selectedItemPosition]

        val alarm = (editingAlarm ?: Alarm(hour = hour, minute = minute)).copy(
            hour = hour,
            minute = minute,
            memo = binding.editMemo.text?.toString()?.trim().orEmpty(),
            vibrationLevel = vibrationLevel.value,
            snoozeEnabled = snoozeEnabled,
            snoozeIntervalMinutes = snoozeInterval,
            snoozeMaxCount = snoozeCount,
            isEnabled = true,
            repeatDays = repeatDays
        )

        lifecycleScope.launch {
            val id = repository.save(alarm)
            val saved = alarm.copy(id = if (alarm.id != 0L) alarm.id else id)
            scheduler.schedule(saved)
            finish()
        }
    }
}
