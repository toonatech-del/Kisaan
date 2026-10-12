package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.LandCaseDao
import com.example.data.dao.LandMeasurementDao
import com.example.data.model.LandCaseEntity
import com.example.data.model.LandMeasurementEntity

@Database(
    entities = [LandCaseEntity::class, LandMeasurementEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun landCaseDao(): LandCaseDao
    abstract fun landMeasurementDao(): LandMeasurementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kisan_zameen_saathi.db"
                ).fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
