package com.thkim0515.alarmmemo.ui.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thkim0515.alarmmemo.R
import com.thkim0515.alarmmemo.data.Alarm
import com.thkim0515.alarmmemo.data.VibrationLevel
import com.thkim0515.alarmmemo.databinding.ItemAlarmBinding
import com.thkim0515.alarmmemo.util.DayOfWeek

class AlarmListAdapter(
    private val onClick: (Alarm) -> Unit,
    private val onLongClick: (Alarm) -> Unit,
    private val onToggle: (Alarm, Boolean) -> Unit
) : ListAdapter<Alarm, AlarmListAdapter.AlarmViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val binding = ItemAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlarmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlarmViewHolder(private val binding: ItemAlarmBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(alarm: Alarm) {
            val context = binding.root.context
            binding.textTime.text = alarm.timeText
            binding.textMemo.text = alarm.memo.ifBlank { context.getString(R.string.no_memo) }

            val vibrationLabel = when (VibrationLevel.fromValue(alarm.vibrationLevel)) {
                VibrationLevel.OFF -> context.getString(R.string.vibration_off)
                VibrationLevel.WEAK -> context.getString(R.string.vibration_weak)
                VibrationLevel.MEDIUM -> context.getString(R.string.vibration_medium)
                VibrationLevel.STRONG -> context.getString(R.string.vibration_strong)
            }
            val repeatSummary = DayOfWeek.repeatSummary(context, alarm.repeatDays, short = true)
            binding.textDetail.text = "$repeatSummary · ${context.getString(R.string.vibration_label)} $vibrationLabel"

            binding.switchEnabled.setOnCheckedChangeListener(null)
            binding.switchEnabled.isChecked = alarm.isEnabled
            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggle(alarm, isChecked)
            }

            binding.root.alpha = if (alarm.isEnabled) 1f else 0.55f

            binding.root.setOnClickListener { onClick(alarm) }
            binding.root.setOnLongClickListener {
                onLongClick(alarm)
                true
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Alarm>() {
            override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm) = oldItem == newItem
        }
    }
}
