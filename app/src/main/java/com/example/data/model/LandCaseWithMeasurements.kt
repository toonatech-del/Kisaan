package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.util.GeoPoint
import org.json.JSONArray
import org.json.JSONObject

data class LandCaseWithMeasurements(
    @Embedded
    val landCase: LandCaseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "landCaseId"
    )
    val measurements: List<LandMeasurementEntity>
)

object PointsConverter {
    fun toJson(points: List<GeoPoint>): String {
        val array = JSONArray()
        points.forEach { pt ->
            val obj = JSONObject()
            obj.put("lat", pt.lat)
            obj.put("lng", pt.lng)
            obj.put("acc", pt.accuracy.toDouble())
            obj.put("time", pt.timestamp)
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(json: String?): List<GeoPoint> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<GeoPoint>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GeoPoint(
                        lat = obj.getDouble("lat"),
                        lng = obj.getDouble("lng"),
                        accuracy = obj.optDouble("acc", 0.0).toFloat(),
                        timestamp = obj.optLong("time", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
