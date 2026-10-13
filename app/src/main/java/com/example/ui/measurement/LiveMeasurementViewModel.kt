package com.example.ui.measurement

import android.app.Application
import android.location.Location
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.LandCaseEntity
import com.example.data.model.LandMeasurementEntity
import com.example.data.model.PointsConverter
import com.example.engine.GpsLandSurveyEngine
import com.example.location.GpsQualityLevel
import com.example.location.LocationTracker
import com.example.util.GeoLandCalculator
import com.example.util.GeoPoint
import com.example.util.PolygonValidation
import com.example.util.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveMeasurementUiState(
    val isPermissionGranted: Boolean = false,
    val isGpsEnabled: Boolean = false,
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val isClosed: Boolean = false,
    val startPointRecorded: Boolean = false,
    val isWaitingForFreshGps: Boolean = false,
    val recordedPoints: List<GeoPoint> = emptyList(),
    val currentLocation: GeoPoint? = null,
    val currentAccuracy: Float = 0f,
    val gpsQuality: GpsQualityLevel = GpsQualityLevel.UNAVAILABLE,
    val polygonValidation: PolygonValidation = PolygonValidation.TooFewPoints(0),
    val estimatedAreaSqM: Double = 0.0,
    val estimatedPerimeterM: Double = 0.0,
    val liveWalkingDistanceM: Double = 0.0,
    val liveWalkingDistanceFt: Double = 0.0,
    val selectedLaggiHaath: Double = 6.0,
    val linkedLandCase: LandCaseEntity? = null,
    val wasManuallyEdited: Boolean = false,
    val isStabilizing: Boolean = false,
    val userNotice: String? = null,
    val warningMessage: String? = null,
    val isLineCrossing: Boolean = false,
    val isReadyForReview: Boolean = false,
    val savedMeasurementId: Long? = null,
    val detectedAreaName: String = ""
)

class LiveMeasurementViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val measurementDao = db.landMeasurementDao()
    private val caseDao = db.landCaseDao()
    private val locationTracker = LocationTracker(application)
    private val surveyEngine = GpsLandSurveyEngine()
    private val cornerSamples = mutableListOf<Location>()

    private val _uiState = MutableStateFlow(LiveMeasurementUiState())
    val uiState: StateFlow<LiveMeasurementUiState> = _uiState.asStateFlow()

    private var locationJob: Job? = null
    private var sessionStartTime: Long = 0L

    init {
        checkGpsStatus()
        val defaultLaggi = PreferencesManager.getDefaultLaggi(application)
        _uiState.update { it.copy(selectedLaggiHaath = defaultLaggi) }
    }

    fun checkGpsStatus() {
        val enabled = locationTracker.isGpsProviderEnabled()
        _uiState.update { it.copy(isGpsEnabled = enabled) }
    }

    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(isPermissionGranted = granted) }
        if (granted) checkGpsStatus()
    }

    fun setLinkedCaseId(caseId: Long?) {
        if (caseId == null || caseId <= 0) return
        viewModelScope.launch {
            val caseEntity = caseDao.getCaseById(caseId)
            _uiState.update { it.copy(linkedLandCase = caseEntity) }
        }
    }

    fun setLaggiHaath(laggi: Double) {
        _uiState.update { it.copy(selectedLaggiHaath = laggi) }
    }

    private var latestLocation: Location? = null
    private var lastRecordedPoint: GeoPoint? = null
    
    // ...
    
    fun addCornerRequested() {
        if (_uiState.value.isPaused) return
        
        val loc = latestLocation
        if (loc != null) {
            addPoint(GeoPoint(loc.latitude, loc.longitude, loc.accuracy))
        } else {
            _uiState.update { it.copy(warningMessage = "GPS signal abhi kamzor hai, thoda aur wait karein.") }
        }
    }
    
    private fun addPoint(newPoint: GeoPoint) {
        val last = _uiState.value.recordedPoints.lastOrNull()
        if (last != null) {
            val results = FloatArray(1)
            Location.distanceBetween(last.lat, last.lng, newPoint.lat, newPoint.lng, results)
            if (results[0] < 2.5f) {
                _uiState.update { it.copy(userNotice = "Aap pichle kone ke bohot kareeb hain. Kripya agle kone par chalkar jayein.") }
                return
            }
        }
        
        if (GeoLandCalculator.checkLiveSegmentCrosses(_uiState.value.recordedPoints, newPoint)) {
            _uiState.update { it.copy(userNotice = "चेतावनी: रेखाएं आपस में कट रही हैं, सही कोने की ओर बढ़ें") }
        }

        val newPoints = _uiState.value.recordedPoints + newPoint
        recalculateAndValidate(newPoints, _uiState.value.isClosed, edited = true)
        
        // Vibrate
        val vibrator = getApplication<Application>().getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        if (vibrator?.hasVibrator() == true) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun startTracking() {
        checkGpsStatus()
        if (!_uiState.value.isGpsEnabled) {
            _uiState.update { it.copy(warningMessage = "कृपया फोन की लोकेशन/GPS सेवा चालू करें") }
            return
        }

        sessionStartTime = System.currentTimeMillis()
        hasGeocoded = false
        _uiState.update {
            it.copy(
                isTracking = true,
                isPaused = false,
                isClosed = false,
                recordedPoints = emptyList(),
                startPointRecorded = false,
                isWaitingForFreshGps = true,
                detectedAreaName = "",
                userNotice = "GPS चालू है..."
            )
        }
        startLocationUpdates()
    }

    private var hasGeocoded = false

    private fun detectAreaName(location: Location) {
        if (hasGeocoded) return
        hasGeocoded = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val geocoder = android.location.Geocoder(getApplication(), java.util.Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val address = addresses?.firstOrNull()
                if (address != null) {
                    val areaName = address.subLocality ?: address.locality ?: address.subAdminArea ?: "अज्ञात मौजा"
                    _uiState.update { it.copy(detectedAreaName = areaName) }
                } else {
                    hasGeocoded = false
                }
            } catch (e: Exception) {
                hasGeocoded = false
            }
        }
    }

    fun pauseTracking() {
        _uiState.update { it.copy(isPaused = true, userNotice = "नाप रोक दी गई है (Paused)") }
    }

    fun resumeTracking() {
        _uiState.update { it.copy(isPaused = false, userNotice = "नाप पुनः चालू (Resumed)") }
    }

    private fun startLocationUpdates() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            try {
                locationTracker.getLocationFlow(1200L).collect { processLocationUpdate(it) }
            } catch (e: Exception) {
                _uiState.update { it.copy(warningMessage = "लोकेशन त्रुटि: ${e.message}") }
            }
        }
    }

    private fun processLocationUpdate(location: Location) {
        latestLocation = location
        
        val currentGeo = GeoPoint(
            lat = location.latitude,
            lng = location.longitude,
            accuracy = location.accuracy
        )
        
        val points = _uiState.value.recordedPoints
        var walkingDistM = 0.0
        var livePerimeter = _uiState.value.estimatedPerimeterM

        if (points.isNotEmpty()) {
            val lastPoint = points.last()
            val distResults = FloatArray(1)
            Location.distanceBetween(lastPoint.lat, lastPoint.lng, location.latitude, location.longitude, distResults)
            walkingDistM = distResults[0].toDouble()

            if (!_uiState.value.isClosed) {
                // Sum of recorded edges
                var recordedEdgesSum = 0.0
                for (i in 0 until points.size - 1) {
                    val r = FloatArray(1)
                    Location.distanceBetween(points[i].lat, points[i].lng, points[i + 1].lat, points[i + 1].lng, r)
                    recordedEdgesSum += r[0].toDouble()
                }

                // Current walking distance back to start (Point #1)
                val firstPoint = points.first()
                val backToStartResults = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, firstPoint.lat, firstPoint.lng, backToStartResults)
                val distBackToStart = backToStartResults[0].toDouble()

                livePerimeter = if (points.size == 1) {
                    walkingDistM
                } else {
                    recordedEdgesSum + walkingDistM + distBackToStart
                }
            }
        }

        val walkingDistFt = walkingDistM * 3.28084
        val quality = locationTracker.evaluateQuality(location.accuracy)

        val isLineCrossing = if (points.size >= 2 && !_uiState.value.isClosed) {
            GeoLandCalculator.checkLiveSegmentCrosses(points, currentGeo)
        } else {
            false
        }

        val updatedWarning = if (isLineCrossing) {
            "चेतावनी: रेखाएं आपस में कट रही हैं, सही कोने की ओर बढ़ें"
        } else if (_uiState.value.warningMessage == "चेतावनी: रेखाएं आपस में कट रही हैं, सही कोने की ओर बढ़ें") {
            null
        } else {
            _uiState.value.warningMessage
        }

        _uiState.update {
            it.copy(
                currentLocation = currentGeo,
                currentAccuracy = location.accuracy,
                gpsQuality = quality,
                liveWalkingDistanceM = walkingDistM,
                liveWalkingDistanceFt = walkingDistFt,
                estimatedPerimeterM = if (!it.isClosed && points.isNotEmpty()) livePerimeter else it.estimatedPerimeterM,
                isLineCrossing = isLineCrossing,
                warningMessage = updatedWarning
            )
        }
        detectAreaName(location)
    }

    fun undoLastPoint() {
        val currentList = _uiState.value.recordedPoints.toMutableList()
        if (currentList.size > 1) {
            currentList.removeAt(currentList.lastIndex)
            recalculateAndValidate(currentList, isClosed = false, edited = true)
            _uiState.update { it.copy(isClosed = false, userNotice = "अंतिम कोना हटा दिया गया (Undo)") }
        }
    }

    fun closeBoundary() {
        val rawPoints = _uiState.value.recordedPoints
        if (rawPoints.size < 3) return
        val points = GeoLandCalculator.orderPointsCircular(rawPoints)
        recalculateAndValidate(points, isClosed = true, edited = _uiState.value.wasManuallyEdited)
        _uiState.update { it.copy(recordedPoints = points, isClosed = true, userNotice = "बाउंड्री बंद की गई") }
    }

    fun cancelMeasurement() {
        stopTracking()
        _uiState.update { LiveMeasurementUiState() }
    }
    
    fun removePointAt(index: Int) {
        val currentList = _uiState.value.recordedPoints.toMutableList()
        if (index in 0 until currentList.size) {
            currentList.removeAt(index)
            recalculateAndValidate(currentList, isClosed = _uiState.value.isClosed, edited = true)
        }
    }

    fun updatePointCoordinates(index: Int, lat: Double, lng: Double) {
        val currentList = _uiState.value.recordedPoints.toMutableList()
        if (index in 0 until currentList.size) {
            val oldPt = currentList[index]
            currentList[index] = oldPt.copy(lat = lat, lng = lng)
            recalculateAndValidate(currentList, isClosed = _uiState.value.isClosed, edited = true)
        }
    }
    
    fun clearNotice() {
        _uiState.update { it.copy(userNotice = null, warningMessage = null) }
    }

    fun finishMeasurement() {
        val rawPoints = _uiState.value.recordedPoints
        if (rawPoints.size < 3) return
        val points = GeoLandCalculator.orderPointsCircular(rawPoints)
        // Mathematically close boundary by connecting the last corner back to Point #1
        recalculateAndValidate(points, isClosed = true, edited = _uiState.value.wasManuallyEdited)
        stopTracking()
        _uiState.update { it.copy(recordedPoints = points, isClosed = true, isReadyForReview = true) }
    }

    fun saveMeasurement(title: String, notes: String, onSuccess: (Long) -> Unit) {
        val state = _uiState.value
        val points = state.recordedPoints
        if (points.size < 3) return

        val avgAccuracy = if (points.isNotEmpty()) points.map { it.accuracy }.average().toFloat() else 0f
        
        val measurementEntity = LandMeasurementEntity(
            landCaseId = state.linkedLandCase?.id,
            title = title.ifBlank { "Field Measurement #${System.currentTimeMillis() % 10000}" },
            timestamp = System.currentTimeMillis(),
            pointsJson = PointsConverter.toJson(points),
            pointCount = points.size,
            areaSqM = state.estimatedAreaSqM,
            perimeterM = state.estimatedPerimeterM,
            laggiHaath = state.selectedLaggiHaath,
            averageAccuracyM = avgAccuracy,
            bestAccuracyM = points.minOfOrNull { it.accuracy } ?: 0f,
            gpsQualitySummary = "Avg Accuracy: ±${String.format(java.util.Locale.US, "%.1f", avgAccuracy)} m",
            notes = notes,
            wasManuallyEdited = state.wasManuallyEdited,
            isConfirmed = true
        )

        viewModelScope.launch {
            val insertedId = measurementDao.insertMeasurement(measurementEntity)
            _uiState.update { it.copy(savedMeasurementId = insertedId) }
            onSuccess(insertedId)
        }
    }

    fun stopTracking() {
        locationJob?.cancel()
        locationJob = null
        _uiState.update { it.copy(isTracking = false, isPaused = false) }
    }

    private fun recalculateAndValidate(points: List<GeoPoint>, isClosed: Boolean, edited: Boolean) {
        val effectivePoints = if (isClosed && points.size >= 3) GeoLandCalculator.orderPointsCircular(points) else points
        val validation = GeoLandCalculator.validatePolygon(effectivePoints)
        val area = surveyEngine.calculateAreaSqMeters(effectivePoints)
        
        val currentLoc = latestLocation
        var walkingDistM = 0.0
        if (effectivePoints.isNotEmpty() && currentLoc != null) {
            val d = FloatArray(1)
            Location.distanceBetween(effectivePoints.last().lat, effectivePoints.last().lng, currentLoc.latitude, currentLoc.longitude, d)
            walkingDistM = d[0].toDouble()
        }

        val perimeter = if (isClosed) {
            surveyEngine.calculatePerimeterMeters(effectivePoints)
        } else {
            if (effectivePoints.isNotEmpty() && currentLoc != null) {
                var recordedEdgesSum = 0.0
                for (i in 0 until effectivePoints.size - 1) {
                    val r = FloatArray(1)
                    Location.distanceBetween(effectivePoints[i].lat, effectivePoints[i].lng, effectivePoints[i + 1].lat, effectivePoints[i + 1].lng, r)
                    recordedEdgesSum += r[0].toDouble()
                }
                val firstPoint = effectivePoints.first()
                val backToStartResults = FloatArray(1)
                Location.distanceBetween(currentLoc.latitude, currentLoc.longitude, firstPoint.lat, firstPoint.lng, backToStartResults)
                val distBackToStart = backToStartResults[0].toDouble()

                if (effectivePoints.size == 1) {
                    walkingDistM
                } else {
                    recordedEdgesSum + walkingDistM + distBackToStart
                }
            } else {
                surveyEngine.calculateOpenPerimeterMeters(effectivePoints)
            }
        }

        _uiState.update {
            it.copy(
                recordedPoints = effectivePoints,
                isClosed = isClosed,
                polygonValidation = validation,
                estimatedAreaSqM = area,
                estimatedPerimeterM = perimeter,
                liveWalkingDistanceM = walkingDistM,
                liveWalkingDistanceFt = walkingDistM * 3.28084,
                wasManuallyEdited = it.wasManuallyEdited || edited
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}
