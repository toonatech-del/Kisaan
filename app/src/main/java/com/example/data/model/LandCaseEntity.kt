package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "land_cases")
data class LandCaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val farmerName: String,         // रैयत / किसान का नाम
    val villageMouza: String,       // मौजा / गांव
    val thanaNumber: String = "",   // थाना संख्या
    val khataNumber: String,        // खाता संख्या
    val khesraNumber: String,       // खेसरा / प्लॉट संख्या
    val jamabandiNumber: String = "", // जमाबंदी संख्या
    val recordedRakba: String = "", // खतियानी / दस्तावेजी रकबा
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
