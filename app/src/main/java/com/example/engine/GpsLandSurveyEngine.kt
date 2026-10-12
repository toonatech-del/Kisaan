package com.example.engine

import com.example.util.GeoPoint
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

class GpsLandSurveyEngine {
    private val R = 6378137.0 // WGS84 equatorial radius

    // 1. Projection: Lat/Lng -> Cartesian (x, y) meters
    fun projectToLocalPlane(points: List<GeoPoint>): List<Pair<Double, Double>> {
        if (points.isEmpty()) return emptyList()
        val p0 = points.first()
        val lat0 = Math.toRadians(p0.lat)
        
        return points.map { pt ->
            val latRad = Math.toRadians(pt.lat)
            val lngRad = Math.toRadians(pt.lng)
            val lng0Rad = Math.toRadians(p0.lng)
            
            val x = (lngRad - lng0Rad) * R * cos(lat0)
            val y = (latRad - lat0) * R
            Pair(x, y)
        }
    }

    // 2. Shoelace Formula for Area
    fun calculateAreaSqMeters(points: List<GeoPoint>): Double {
        val cartesian = projectToLocalPlane(points)
        if (cartesian.size < 3) return 0.0
        
        var area = 0.0
        for (i in cartesian.indices) {
            val j = (i + 1) % cartesian.size
            area += cartesian[i].first * cartesian[j].second
            area -= cartesian[j].first * cartesian[i].second
        }
        return Math.abs(area) / 2.0
    }

    // 3. Perimeter
    fun calculatePerimeterMeters(points: List<GeoPoint>): Double {
        val cartesian = projectToLocalPlane(points)
        if (cartesian.size < 2) return 0.0
        
        var perimeter = 0.0
        for (i in cartesian.indices) {
            val j = (i + 1) % cartesian.size
            val dx = cartesian[j].first - cartesian[i].first
            val dy = cartesian[j].second - cartesian[i].second
            perimeter += sqrt(dx.pow(2) + dy.pow(2))
        }
        return perimeter
    }
}
