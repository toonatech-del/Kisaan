package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.LandCaseEntity
import com.example.data.model.LandCaseWithMeasurements
import com.example.data.model.LandMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LandMeasurementDao {
    @Query("SELECT * FROM land_measurements ORDER BY timestamp DESC")
    fun getAllMeasurements(): Flow<List<LandMeasurementEntity>>

    @Query("SELECT * FROM land_measurements WHERE id = :id")
    suspend fun getMeasurementById(id: Long): LandMeasurementEntity?

    @Query("SELECT * FROM land_measurements WHERE landCaseId = :caseId ORDER BY timestamp DESC")
    fun getMeasurementsForCase(caseId: Long): Flow<List<LandMeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: LandMeasurementEntity): Long

    @Update
    suspend fun updateMeasurement(measurement: LandMeasurementEntity)

    @Delete
    suspend fun deleteMeasurement(measurement: LandMeasurementEntity)

    @Query("DELETE FROM land_measurements WHERE id = :id")
    suspend fun deleteMeasurementById(id: Long)

    @Query("DELETE FROM land_measurements")
    suspend fun clearAllMeasurements()

    @Query("SELECT COUNT(*) FROM land_measurements")
    fun getMeasurementCount(): Flow<Int>
}

@Dao
interface LandCaseDao {
    @Query("SELECT * FROM land_cases ORDER BY updatedAt DESC")
    fun getAllCases(): Flow<List<LandCaseEntity>>

    @Query("SELECT * FROM land_cases WHERE id = :id")
    suspend fun getCaseById(id: Long): LandCaseEntity?

    @Transaction
    @Query("SELECT * FROM land_cases WHERE id = :id")
    fun getCaseWithMeasurements(id: Long): Flow<LandCaseWithMeasurements?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(landCase: LandCaseEntity): Long

    @Update
    suspend fun updateCase(landCase: LandCaseEntity)

    @Delete
    suspend fun deleteCase(landCase: LandCaseEntity)

    @Query("DELETE FROM land_cases")
    suspend fun clearAllCases()

    @Query("SELECT COUNT(*) FROM land_cases")
    fun getCaseCount(): Flow<Int>
}
