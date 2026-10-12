package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "land_measurements",
    foreignKeys = [
        ForeignKey(
            entity = LandCaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["landCaseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("landCaseId")]
)
data class LandMeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val landCaseId: Long? = null,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val pointsJson: String, // Stored as JSON array of GeoPoint
    val pointCount: Int,
    val areaSqM: Double,
    val perimeterM: Double,
    val laggiHaath: Double = 6.0,
    val averageAccuracyM: Float = 0f,
    val bestAccuracyM: Float = 0f,
    val gpsQualitySummary: String,
    val notes: String = "",
    val wasManuallyEdited: Boolean = false,
    val isConfirmed: Boolean = true
)
