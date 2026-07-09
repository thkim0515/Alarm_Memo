package com.thkim0515.alarmmemo.ui.list

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.thkim0515.alarmmemo.R
import com.thkim0515.alarmmemo.data.Alarm
import com.thkim0515.alarmmemo.databinding.ActivityAlarmListBinding
import com.thkim0515.alarmmemo.ui.edit.AlarmEditActivity
import com.thkim0515.alarmmemo.util.Constants

class AlarmListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmListBinding
    private lateinit var viewModel: AlarmListViewModel
    private lateinit var adapter: AlarmListAdapter

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        viewModel = ViewModelProvider(this)[AlarmListViewModel::class.java]

        adapter = AlarmListAdapter(
            onClick = { alarm -> openEdit(alarm.id) },
            onLongClick = { alarm -> confirmDelete(alarm) },
            onToggle = { alarm, enabled -> viewModel.setEnabled(alarm, enabled) }
        )
        binding.recyclerView.adapter = adapter

        binding.fabAdd.setOnClickListener { openEdit(null) }

        viewModel.alarms.observe(this) { alarms ->
            adapter.submitList(alarms)
            binding.emptyState.visibility = if (alarms.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        checkExactAlarmPermission()
    }

    private fun openEdit(alarmId: Long?) {
        val intent = Intent(this, AlarmEditActivity::class.java)
        if (alarmId != null) {
            intent.putExtra(Constants.EXTRA_ALARM_ID, alarmId)
        }
        startActivity(intent)
    }

    private fun confirmDelete(alarm: Alarm) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_alarm)
            .setMessage(R.string.delete_alarm_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.delete(alarm) }
            .show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                Snackbar.make(binding.root, R.string.permission_exact_alarm_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.grant_permission) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    }
                    .show()
            }
        }
    }
}
