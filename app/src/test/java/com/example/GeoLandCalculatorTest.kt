package com.example

import com.example.util.GeoLandCalculator
import com.example.util.GeoPoint
import com.example.util.PolygonValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos

class GeoLandCalculatorTest {

    @Test
    fun testHaversineDistance_KnownDistance() {
        // Distance along meridian from 25.0°N to 25.001°N at 85.0°E (Patna / Bihar)
        // 0.001 degree lat = (0.001 * pi / 180) * 6371008.8 ~ 111.19 meters
        val p1 = GeoPoint(25.0, 85.0)
        val p2 = GeoPoint(25.001, 85.0)
        val distance = GeoLandCalculator.computeHaversineDistance(p1, p2)
        assertEquals(111.19, distance, 0.5)
    }

    @Test
    fun testPerimeterAndPolygonClosure() {
        val lat = 25.0
        val lng = 85.0
        val dLat = 100.0 / 111195.0
        val dLng = 100.0 / (111195.0 * cos(Math.toRadians(lat)))

        val points = listOf(
            GeoPoint(lat, lng),
            GeoPoint(lat, lng + dLng),
            GeoPoint(lat + dLat, lng + dLng),
            GeoPoint(lat + dLat, lng)
        )

        // 100m x 100m square has 400m perimeter
        val perimeter = GeoLandCalculator.computePerimeter(points, closeLoop = true)
        assertEquals(400.0, perimeter, 2.0)

        // Without loop closure (3 sides only) ~ 300m
        val openPerimeter = GeoLandCalculator.computePerimeter(points, closeLoop = false)
        assertEquals(300.0, openPerimeter, 2.0)
    }

    @Test
    fun testSyntheticPolygonArea_OneHectareSquare() {
        // Synthetic 100m x 100m square field in Bihar (10,000 sq meters = 1 Hectare)
        val lat = 25.5941 // Patna latitude
        val lng = 85.1376
        val metersPerDegreeLat = (Math.PI / 180.0) * GeoLandCalculator.EARTH_RADIUS_METERS
        val metersPerDegreeLng = metersPerDegreeLat * cos(Math.toRadians(lat))

        val dLat = 100.0 / metersPerDegreeLat
        val dLng = 100.0 / metersPerDegreeLng

        val squareField = listOf(
            GeoPoint(lat, lng),
            GeoPoint(lat, lng + dLng),
            GeoPoint(lat + dLat, lng + dLng),
            GeoPoint(lat + dLat, lng)
        )

        val area = GeoLandCalculator.computeSphericalPolygonArea(squareField)
        assertEquals(10000.0, area, 15.0) // Within 0.15%

        val validation = GeoLandCalculator.validatePolygon(squareField)
        assertTrue("Expected valid polygon", validation is PolygonValidation.Valid)
    }

    @Test
    fun testSyntheticPolygonArea_Triangle() {
        // Right-angled triangle: base = 60m, height = 40m -> Area = 1200 sq meters
        val lat = 25.0
        val lng = 85.0
        val metersPerDegreeLat = (Math.PI / 180.0) * GeoLandCalculator.EARTH_RADIUS_METERS
        val metersPerDegreeLng = metersPerDegreeLat * cos(Math.toRadians(lat))

        val baseM = 60.0
        val heightM = 40.0
        val triangle = listOf(
            GeoPoint(lat, lng),
            GeoPoint(lat, lng + (baseM / metersPerDegreeLng)),
            GeoPoint(lat + (heightM / metersPerDegreeLat), lng)
        )

        val area = GeoLandCalculator.computeSphericalPolygonArea(triangle)
        assertEquals(1200.0, area, 5.0)
    }

    @Test
    fun testInvalidPolygon_TooFewPoints() {
        val points = listOf(
            GeoPoint(25.0, 85.0),
            GeoPoint(25.001, 85.001)
        )
        val result = GeoLandCalculator.validatePolygon(points)
        assertTrue(result is PolygonValidation.TooFewPoints)
    }

    @Test
    fun testInvalidPolygon_SelfIntersectingFigureEight() {
        // Bowtie / Hourglass shape where opposite sides cross
        val points = listOf(
            GeoPoint(25.000, 85.000), // (0, 0)
            GeoPoint(25.002, 85.002), // (2, 2)
            GeoPoint(25.002, 85.000), // (0, 2)
            GeoPoint(25.000, 85.002)  // (2, 0)
        )
        val result = GeoLandCalculator.validatePolygon(points)
        assertTrue("Self intersecting shape should be detected", result is PolygonValidation.SelfIntersecting)
    }

    @Test
    fun testBiharUnitConversions() {
        // Test with 6 हाथ लग्गी (1 धूर = 81 वर्ग फीट, 1 कट्ठा = 1,620 वर्ग फीट, 1 बीघा = 32,400 वर्ग फीट)
        // 32,400 sq ft ~ 3010.05 sq meters
        val areaSqM = 32400.0 / GeoLandCalculator.SQ_METERS_TO_SQ_FEET
        val breakdown = GeoLandCalculator.convertToBiharUnits(areaSqM, laggiHaath = 6.0)

        assertEquals(1, breakdown.bigha)
        assertEquals(0, breakdown.katha)
        assertEquals(0, breakdown.dhur)
        assertEquals(32400.0, breakdown.sqFeet, 1.0)

        // 1 Acre = 43560 sq ft = 100 Dismil
        val oneAcreSqM = 43560.0 / GeoLandCalculator.SQ_METERS_TO_SQ_FEET
        val acreBreakdown = GeoLandCalculator.convertToBiharUnits(oneAcreSqM, laggiHaath = 6.0)
        assertEquals(1.0, acreBreakdown.acres, 0.001)
        assertEquals(100.0, acreBreakdown.dismil, 0.01)

        // Round-trip biharUnitsToSqM
        val calculatedSqM = GeoLandCalculator.biharUnitsToSqM(1.0, 5.0, 10.0, laggiHaath = 6.0)
        val roundTrip = GeoLandCalculator.convertToBiharUnits(calculatedSqM, laggiHaath = 6.0)
        assertEquals(1, roundTrip.bigha)
        assertEquals(5, roundTrip.katha)
        assertEquals(10, roundTrip.dhur)
    }
}
