package com.thkim0515.alarmmemo.ui.edit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
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
    private var soundUri: String? = null

    private val chooseSoundLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                soundUri = uri.toString()
                updateSelectedSoundText()
            }
        }

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
        setupSoundToggle()
        setupSnoozeToggle()
        updateRepeatSummary()
        updateSelectedSoundText()

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

    private fun setupSoundToggle() {
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutSoundOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        binding.buttonChooseSound.setOnClickListener {
            chooseSoundLauncher.launch(arrayOf("audio/*"))
        }
        binding.buttonResetSound.setOnClickListener {
            soundUri = null
            updateSelectedSoundText()
        }
    }

    private fun updateSelectedSoundText() {
        binding.textSelectedSound.text = soundUri?.let { displayNameFor(Uri.parse(it)) }
            ?: getString(R.string.sound_default)
    }

    private fun displayNameFor(uri: Uri): String {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: getString(R.string.sound_default)
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

        binding.switchSound.isChecked = alarm.soundEnabled
        binding.layoutSoundOptions.visibility = if (alarm.soundEnabled) View.VISIBLE else View.GONE
        soundUri = alarm.soundUri
        updateSelectedSoundText()

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
            repeatDays = repeatDays,
            soundEnabled = binding.switchSound.isChecked,
            soundUri = soundUri
        )

        lifecycleScope.launch {
            val id = repository.save(alarm)
            val saved = alarm.copy(id = if (alarm.id != 0L) alarm.id else id)
            scheduler.schedule(saved)
            finish()
        }
    }
}
