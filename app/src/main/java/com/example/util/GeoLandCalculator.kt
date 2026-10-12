package com.example.util

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Represents a geographical coordinate point recorded during GPS land boundary walking.
 */
data class GeoPoint(
    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val bearing: Float = 0f,
    val speed: Float = 0f
)

/**
 * 2D projected local point in meters.
 */
data class Point2D(val x: Double, val y: Double)

/**
 * Validation result for field boundary polygon.
 */
sealed class PolygonValidation {
    object Valid : PolygonValidation()
    data class TooFewPoints(val count: Int) : PolygonValidation()
    object ZeroArea : PolygonValidation()
    data class SelfIntersecting(val index1: Int, val index2: Int) : PolygonValidation()
    object DuplicateConsecutivePoints : PolygonValidation()
}

/**
 * Detailed breakdown of Bihar land units.
 */
data class BiharLandBreakdown(
    val bigha: Int,
    val katha: Int,
    val dhur: Int,
    val dhurki: Double,
    val totalDhur: Double,
    val acres: Double,
    val dismil: Double,
    val hectares: Double,
    val sqMetres: Double,
    val sqFeet: Double,
    val laggiHaath: Double
) {
    fun toHindiString(): String {
        val parts = mutableListOf<String>()
        if (bigha > 0) parts.add("$bigha बीघा")
        if (katha > 0 || bigha > 0) parts.add("$katha कट्ठा")
        parts.add(String.format(java.util.Locale.US, "%.1f धूर", dhur + dhurki / 20.0))
        return parts.joinToString(" ")
    }

    fun toDetailedSummary(): String {
        return buildString {
            append(toHindiString())
            append(" (")
            append(String.format(java.util.Locale.US, "%.2f एकड़ / %.2f डिसमिल", acres, dismil))
            append(", ")
            append(String.format(java.util.Locale.US, "%.1f वर्ग मी.", sqMetres))
            append(")")
        }
    }
}

/**
 * Core mathematical engine for land measurement, geodesy, and Bihar unit conversions.
 */
object GeoLandCalculator {

    const val EARTH_RADIUS_METERS = 6371008.8 // Authalic radius of Earth (WGS84)
    const val SQ_METERS_TO_SQ_FEET = 10.7639104
    const val ACRE_TO_SQ_FEET = 43560.0
    const val ACRE_TO_SQ_METERS = 4046.8564224
    const val DISMIL_TO_SQ_FEET = 435.6
    const val DISMIL_TO_SQ_METERS = 40.468564
    const val HECTARE_TO_SQ_METERS = 10000.0

    /**
     * Calculates great-circle Haversine distance between two points in meters.
     */
    fun computeHaversineDistance(p1: GeoPoint, p2: GeoPoint): Double {
        val lat1Rad = Math.toRadians(p1.lat)
        val lat2Rad = Math.toRadians(p2.lat)
        val dLat = Math.toRadians(p2.lat - p1.lat)
        val dLng = Math.toRadians(p2.lng - p1.lng)

        val a = sin(dLat / 2.0).pow(2.0) +
                cos(lat1Rad) * cos(lat2Rad) * sin(dLng / 2.0).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calculates the total perimeter of the polygon in meters.
     * When closeLoop is true, includes the distance from the last point back to the first.
     */
    fun computePerimeter(points: List<GeoPoint>, closeLoop: Boolean = true): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += computeHaversineDistance(points[i], points[i + 1])
        }
        if (closeLoop && points.size >= 3) {
            total += computeHaversineDistance(points.last(), points.first())
        }
        return total
    }

    /**
     * Calculates the geodesic area of a closed spherical polygon in square meters.
     * Uses spherical excess / trapezoid integration on the WGS84 sphere.
     */
    fun computeSphericalPolygonArea(points: List<GeoPoint>): Double {
        if (points.size < 3) return 0.0
        var total = 0.0
        val size = points.size
        for (i in 0 until size) {
            val p1 = points[i]
            val p2 = points[(i + 1) % size]
            val lat1 = Math.toRadians(p1.lat)
            val lat2 = Math.toRadians(p2.lat)
            val lng1 = Math.toRadians(p1.lng)
            val lng2 = Math.toRadians(p2.lng)
            total += (lng2 - lng1) * (2.0 + sin(lat1) + sin(lat2))
        }
        val area = abs(total * EARTH_RADIUS_METERS * EARTH_RADIUS_METERS / 2.0)
        return area
    }

    /**
     * Projects spherical coordinates to local Cartesian coordinates (meters)
     * using the polygon centroid as reference origin.
     */
    fun projectToLocalPlane(points: List<GeoPoint>): List<Point2D> {
        if (points.isEmpty()) return emptyList()
        val meanLat = points.map { it.lat }.average()
        val meanLng = points.map { it.lng }.average()
        val meanLatRad = Math.toRadians(meanLat)

        return points.map { pt ->
            val x = EARTH_RADIUS_METERS * Math.toRadians(pt.lng - meanLng) * cos(meanLatRad)
            val y = EARTH_RADIUS_METERS * Math.toRadians(pt.lat - meanLat)
            Point2D(x, y)
        }
    }

    /**
     * Validates whether the given list of points forms a geometrically valid, non-self-intersecting boundary.
     */
    fun validatePolygon(points: List<GeoPoint>): PolygonValidation {
        if (points.size < 3) {
            return PolygonValidation.TooFewPoints(points.size)
        }

        // Check for adjacent duplicates closer than 0.5 meter
        for (i in 0 until points.size - 1) {
            if (computeHaversineDistance(points[i], points[i + 1]) < 0.5) {
                return PolygonValidation.DuplicateConsecutivePoints
            }
        }

        // Check for self-intersections using 2D projection
        val planePoints = projectToLocalPlane(points)
        val n = planePoints.size
        for (i in 0 until n) {
            val p1 = planePoints[i]
            val p2 = planePoints[(i + 1) % n]

            for (j in i + 1 until n) {
                // Adjacent segments share a vertex; skip them and the closing segment
                if (abs(i - j) <= 1 || (i == 0 && j == n - 1)) {
                    continue
                }
                val q1 = planePoints[j]
                val q2 = planePoints[(j + 1) % n]

                if (doLineSegmentsIntersect(p1, p2, q1, q2)) {
                    return PolygonValidation.SelfIntersecting(i, j)
                }
            }
        }

        val area = computeSphericalPolygonArea(points)
        if (area < 0.1) {
            return PolygonValidation.ZeroArea
        }

        return PolygonValidation.Valid
    }

    /**
     * Tests if line segment (p1->p2) and (q1->q2) strictly intersect.
     */
    private fun doLineSegmentsIntersect(p1: Point2D, p2: Point2D, q1: Point2D, q2: Point2D): Boolean {
        fun orientation(a: Point2D, b: Point2D, c: Point2D): Int {
            val valProduct = (b.y - a.y) * (c.x - b.x) - (b.x - a.x) * (c.y - b.y)
            return when {
                abs(valProduct) < 1e-9 -> 0 // Collinear
                valProduct > 0 -> 1        // Clockwise
                else -> 2                  // Counter-Clockwise
            }
        }

        fun onSegment(p: Point2D, q: Point2D, r: Point2D): Boolean {
            return q.x <= max(p.x, r.x) + 1e-9 && q.x >= min(p.x, r.x) - 1e-9 &&
                    q.y <= max(p.y, r.y) + 1e-9 && q.y >= min(p.y, r.y) - 1e-9
        }

        val o1 = orientation(p1, p2, q1)
        val o2 = orientation(p1, p2, q2)
        val o3 = orientation(q1, q2, p1)
        val o4 = orientation(q1, q2, p2)

        // General case (proper crossing)
        if (o1 != o2 && o3 != o4) return true

        // Special Collinear cases
        if (o1 == 0 && onSegment(p1, q1, p2)) return true
        if (o2 == 0 && onSegment(p1, q2, p2)) return true
        if (o3 == 0 && onSegment(q1, p1, q2)) return true
        if (o4 == 0 && onSegment(q1, p2, q2)) return true

        return false
    }

    /**
     * Converts square meters into Bihar local land units according to Laggi size (हाथ).
     * Standard Bihar laggi sizes: 5.5, 6.0, 6.5, 7.0 हाथ.
     * 1 हाथ = 1.5 फीट.
     * 1 धूर = (Laggi * 1.5 ft)^2
     * 1 कट्ठा = 20 धूर
     * 1 बीघा = 20 कट्ठा = 400 धूर
     */
    fun convertToBiharUnits(areaSqM: Double, laggiHaath: Double = 6.0): BiharLandBreakdown {
        val sqFeet = areaSqM * SQ_METERS_TO_SQ_FEET
        val acres = sqFeet / ACRE_TO_SQ_FEET
        val dismil = sqFeet / DISMIL_TO_SQ_FEET
        val hectares = areaSqM / HECTARE_TO_SQ_METERS

        val laggiInFeet = laggiHaath * 1.5
        val oneDhurSqFeet = laggiInFeet * laggiInFeet
        val totalDhur = if (oneDhurSqFeet > 0) sqFeet / oneDhurSqFeet else 0.0

        val totalDhurFloor = totalDhur.toLong()
        val bigha = (totalDhurFloor / 400).toInt()
        val remAfterBigha = totalDhurFloor % 400
        val katha = (remAfterBigha / 20).toInt()
        val dhur = (remAfterBigha % 20).toInt()
        val dhurki = (totalDhur - totalDhurFloor) * 20.0

        return BiharLandBreakdown(
            bigha = bigha,
            katha = katha,
            dhur = dhur,
            dhurki = dhurki,
            totalDhur = totalDhur,
            acres = acres,
            dismil = dismil,
            hectares = hectares,
            sqMetres = areaSqM,
            sqFeet = sqFeet,
            laggiHaath = laggiHaath
        )
    }

    /**
     * Converts Bihar local input (बीघा, कट्ठा, धूर) to square meters.
     */
    fun biharUnitsToSqM(bigha: Double, katha: Double, dhur: Double, laggiHaath: Double = 6.0): Double {
        val totalDhur = (bigha * 400.0) + (katha * 20.0) + dhur
        val laggiInFeet = laggiHaath * 1.5
        val oneDhurSqFeet = laggiInFeet * laggiInFeet
        val sqFeet = totalDhur * oneDhurSqFeet
        return sqFeet / SQ_METERS_TO_SQ_FEET
    }
}
