package com.thkim0515.alarmmemo.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class AlarmRepository(context: Context) {

    private val dao = AlarmDatabase.getInstance(context).alarmDao()

    fun observeAlarms(): Flow<List<Alarm>> = dao.observeAlarms()

    suspend fun getAlarm(id: Long): Alarm? = dao.getAlarm(id)

    suspend fun getEnabledAlarms(): List<Alarm> = dao.getEnabledAlarms()

    suspend fun save(alarm: Alarm): Long = dao.upsert(alarm)

    suspend fun update(alarm: Alarm) = dao.update(alarm)

    suspend fun delete(alarm: Alarm) = dao.delete(alarm)

    companion object {
        @Volatile
        private var instance: AlarmRepository? = null

        fun getInstance(context: Context): AlarmRepository {
            return instance ?: synchronized(this) {
                instance ?: AlarmRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
