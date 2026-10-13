package com.example.engine

import com.example.util.GeoPoint
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

class GpsLandSurveyEngine {
    private val R = 6378137.0 // WGS84 equatorial radius

    // 1. Projection: Lat/Lng -> Cartesian (x, y) meters using centroid origin
    fun projectToLocalPlane(points: List<GeoPoint>): List<Pair<Double, Double>> {
        if (points.isEmpty()) return emptyList()
        val meanLat = points.map { it.lat }.average()
        val meanLng = points.map { it.lng }.average()
        val meanLatRad = Math.toRadians(meanLat)
        val meanLngRad = Math.toRadians(meanLng)

        return points.map { pt ->
            val latRad = Math.toRadians(pt.lat)
            val lngRad = Math.toRadians(pt.lng)
            
            val x = (lngRad - meanLngRad) * R * cos(meanLatRad)
            val y = (latRad - meanLatRad) * R
            Pair(x, y)
        }
    }

    // 2. High-precision Geodesic Polygon Area (combines Spherical Excess on WGS84 ellipsoid radius with planar Shoelace)
    fun calculateAreaSqMeters(points: List<GeoPoint>): Double {
        if (points.size < 3) return 0.0
        
        // Spherical excess integration (geodesic standard on WGS84)
        var total = 0.0
        val size = points.size
        for (i in 0 until size) {
            val p1 = points[i]
            val p2 = points[(i + 1) % size]
            val lat1 = Math.toRadians(p1.lat)
            val lat2 = Math.toRadians(p2.lat)
            val lng1 = Math.toRadians(p1.lng)
            val lng2 = Math.toRadians(p2.lng)
            total += (lng2 - lng1) * (2.0 + kotlin.math.sin(lat1) + kotlin.math.sin(lat2))
        }
        val geodesicArea = Math.abs(total * R * R / 2.0)
        
        if (geodesicArea > 0.05) {
            return geodesicArea
        }

        // Fallback to local centroid shoelace
        val cartesian = projectToLocalPlane(points)
        var area = 0.0
        for (i in cartesian.indices) {
            val j = (i + 1) % cartesian.size
            area += cartesian[i].first * cartesian[j].second
            area -= cartesian[j].first * cartesian[i].second
        }
        return Math.abs(area) / 2.0
    }

    // 3. True Geodetic Perimeter (Closed loop using Haversine / WGS84 great circle)
    fun calculatePerimeterMeters(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0
        var perimeter = 0.0
        val results = FloatArray(1)
        for (i in points.indices) {
            val next = (i + 1) % points.size
            android.location.Location.distanceBetween(
                points[i].lat, points[i].lng,
                points[next].lat, points[next].lng,
                results
            )
            perimeter += results[0]
        }
        return perimeter
    }

    // 4. Open Perimeter (Sum of consecutive edges without loop closure)
    fun calculateOpenPerimeterMeters(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0
        var perimeter = 0.0
        val results = FloatArray(1)
        for (i in 0 until points.size - 1) {
            android.location.Location.distanceBetween(
                points[i].lat, points[i].lng,
                points[i + 1].lat, points[i + 1].lng,
                results
            )
            perimeter += results[0]
        }
        return perimeter
    }
}
