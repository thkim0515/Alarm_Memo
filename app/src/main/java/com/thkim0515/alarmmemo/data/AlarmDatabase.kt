package com.thkim0515.alarmmemo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Alarm::class], version = 3, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var instance: AlarmDatabase? = null

        fun getInstance(context: Context): AlarmDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarm_memo.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
