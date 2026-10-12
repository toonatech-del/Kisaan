package com.example.ui.measurement

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.LandCaseEntity
import com.example.data.model.LandMeasurementEntity
import com.example.data.model.PointsConverter
import com.example.location.GpsQualityLevel
import com.example.location.LocationTracker
import com.example.util.GeoLandCalculator
import com.example.util.GeoPoint
import com.example.util.PolygonValidation
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
    val recordedPoints: List<GeoPoint> = emptyList(),
    val currentLocation: GeoPoint? = null,
    val currentAccuracy: Float = 0f,
    val gpsQuality: GpsQualityLevel = GpsQualityLevel.UNAVAILABLE,
    val polygonValidation: PolygonValidation = PolygonValidation.TooFewPoints(0),
    val estimatedAreaSqM: Double = 0.0,
    val estimatedPerimeterM: Double = 0.0,
    val selectedLaggiHaath: Double = 6.0,
    val linkedLandCase: LandCaseEntity? = null,
    val wasManuallyEdited: Boolean = false,
    val userNotice: String? = null,
    val warningMessage: String? = null,
    val isReadyForReview: Boolean = false,
    val savedMeasurementId: Long? = null
)

class LiveMeasurementViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val measurementDao = db.landMeasurementDao()
    private val caseDao = db.landCaseDao()
    private val locationTracker = LocationTracker(application)

    private val _uiState = MutableStateFlow(LiveMeasurementUiState())
    val uiState: StateFlow<LiveMeasurementUiState> = _uiState.asStateFlow()

    private var locationJob: Job? = null
    private val minDistanceThresholdM = 2.5 // Minimum distance to record automatic walking point

    init {
        checkGpsStatus()
    }

    fun checkGpsStatus() {
        val enabled = locationTracker.isGpsProviderEnabled()
        _uiState.update { it.copy(isGpsEnabled = enabled) }
    }

    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(isPermissionGranted = granted) }
        if (granted) {
            checkGpsStatus()
        }
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

    fun clearNotice() {
        _uiState.update { it.copy(userNotice = null, warningMessage = null) }
    }

    fun startTracking() {
        checkGpsStatus()
        if (!_uiState.value.isGpsEnabled) {
            _uiState.update { it.copy(warningMessage = "कृपया फोन की लोकेशन/GPS सेवा चालू करें") }
            return
        }

        _uiState.update {
            it.copy(
                isTracking = true,
                isPaused = false,
                userNotice = "नाप शुरू हुई: खेत की मेड़ के साथ धीरे-धीरे चलें"
            )
        }

        startLocationUpdates()
    }

    fun pauseTracking() {
        _uiState.update {
            it.copy(
                isPaused = true,
                userNotice = "नाप रोक दी गई है (Paused)"
            )
        }
    }

    fun resumeTracking() {
        _uiState.update {
            it.copy(
                isPaused = false,
                userNotice = "नाप पुनः चालू (Resumed)"
            )
        }
    }

    private fun startLocationUpdates() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            try {
                locationTracker.getLocationFlow(1200L).collect { location ->
                    processLocationUpdate(location)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(warningMessage = "लोकेशन सेवा में त्रुटि: ${e.message}") }
            }
        }
    }

    private fun processLocationUpdate(location: Location) {
        val accuracy = location.accuracy
        val quality = locationTracker.evaluateQuality(accuracy)
        val currentGeo = GeoPoint(
            lat = location.latitude,
            lng = location.longitude,
            accuracy = accuracy,
            timestamp = location.time
        )

        val currentState = _uiState.value
        val warning = if (accuracy > 15f) {
            "सावधानी: GPS संकेत कमजोर है (±${String.format(java.util.Locale.US, "%.1f", accuracy)} मी.)। खुले आसमान में प्रतीक्षा करें।"
        } else null

        _uiState.update {
            it.copy(
                currentLocation = currentGeo,
                currentAccuracy = accuracy,
                gpsQuality = quality,
                warningMessage = warning
            )
        }

        // Automatic point collection while walking along boundary
        if (currentState.isTracking && !currentState.isPaused && !currentState.isClosed) {
            if (accuracy <= 12f) { // Accept points with reasonable quality
                val lastPoint = currentState.recordedPoints.lastOrNull()
                if (lastPoint == null) {
                    // First point
                    appendPoint(currentGeo, isAuto = true)
                } else {
                    val distFromLast = GeoLandCalculator.computeHaversineDistance(lastPoint, currentGeo)
                    if (distFromLast >= minDistanceThresholdM) {
                        appendPoint(currentGeo, isAuto = true)
                    }
                }
            }
        }
    }

    fun addManualPoint() {
        val current = _uiState.value.currentLocation
        if (current == null) {
            _uiState.update { it.copy(warningMessage = "GPS स्थान अभी उपलब्ध नहीं है, कृपया 2 सेकंड प्रतीक्षा करें") }
            return
        }

        if (current.accuracy > 15f) {
            _uiState.update {
                it.copy(
                    warningMessage = "कमजोर GPS संकेत (±${String.format(java.util.Locale.US, "%.1f", current.accuracy)} मी.)। कृपया बेहतर सिग्नल की प्रतीक्षा करें।"
                )
            }
        }

        appendPoint(current, isAuto = false)
        _uiState.update {
            it.copy(userNotice = "बिंदु #${it.recordedPoints.size} जोड़ा गया")
        }
    }

    private fun appendPoint(point: GeoPoint, isAuto: Boolean) {
        val currentList = _uiState.value.recordedPoints.toMutableList()
        currentList.add(point)
        recalculateAndValidate(currentList, isClosed = _uiState.value.isClosed, edited = !isAuto)
    }

    fun undoLastPoint() {
        val currentList = _uiState.value.recordedPoints.toMutableList()
        if (currentList.isNotEmpty()) {
            currentList.removeAt(currentList.lastIndex)
            recalculateAndValidate(currentList, isClosed = false, edited = true)
            _uiState.update {
                it.copy(
                    isClosed = false,
                    userNotice = "अंतिम बिंदु हटा दिया गया"
                )
            }
        }
    }

    fun removePointAt(index: Int) {
        val currentList = _uiState.value.recordedPoints.toMutableList()
        if (index in 0 until currentList.size) {
            currentList.removeAt(index)
            recalculateAndValidate(currentList, isClosed = _uiState.value.isClosed, edited = true)
            _uiState.update {
                it.copy(userNotice = "बिंदु #${index + 1} हटाया गया")
            }
        }
    }

    fun closeBoundary() {
        val points = _uiState.value.recordedPoints
        if (points.size < 3) {
            _uiState.update { it.copy(warningMessage = "बाउंड्री बंद करने के लिए कम से कम 3 बिंदु आवश्यक हैं") }
            return
        }
        recalculateAndValidate(points, isClosed = true, edited = _uiState.value.wasManuallyEdited)
        _uiState.update {
            it.copy(
                isClosed = true,
                userNotice = "बाउंड्री बंद की गई • अब नाप की समीक्षा करें"
            )
        }
    }

    fun finishMeasurement() {
        val points = _uiState.value.recordedPoints
        if (points.size < 3) {
            _uiState.update { it.copy(warningMessage = "नाप पूरी करने के लिए कम से कम 3 बिंदु आवश्यक हैं") }
            return
        }
        recalculateAndValidate(points, isClosed = true, edited = _uiState.value.wasManuallyEdited)
        stopTracking()
        _uiState.update { it.copy(isReadyForReview = true) }
    }

    fun stopTracking() {
        locationJob?.cancel()
        locationJob = null
        _uiState.update { it.copy(isTracking = false, isPaused = false) }
    }

    fun cancelMeasurement() {
        stopTracking()
        _uiState.update {
            LiveMeasurementUiState(
                isPermissionGranted = it.isPermissionGranted,
                isGpsEnabled = it.isGpsEnabled,
                linkedLandCase = it.linkedLandCase
            )
        }
    }

    private fun recalculateAndValidate(points: List<GeoPoint>, isClosed: Boolean, edited: Boolean) {
        val validation = GeoLandCalculator.validatePolygon(points)
        val area = if (points.size >= 3) GeoLandCalculator.computeSphericalPolygonArea(points) else 0.0
        val perimeter = if (points.size >= 2) GeoLandCalculator.computePerimeter(points, closeLoop = isClosed) else 0.0

        _uiState.update {
            it.copy(
                recordedPoints = points,
                isClosed = isClosed,
                polygonValidation = validation,
                estimatedAreaSqM = area,
                estimatedPerimeterM = perimeter,
                wasManuallyEdited = it.wasManuallyEdited || edited
            )
        }
    }

    fun saveMeasurement(
        title: String,
        notes: String,
        onSuccess: (Long) -> Unit
    ) {
        val state = _uiState.value
        val points = state.recordedPoints
        if (points.size < 3) return

        val avgAccuracy = if (points.isNotEmpty()) {
            points.map { it.accuracy }.average().toFloat()
        } else 0f
        val bestAccuracy = points.minOfOrNull { it.accuracy } ?: 0f

        val qualitySummary = buildString {
            append("औसत सटीकता: ±${String.format(java.util.Locale.US, "%.1f", avgAccuracy)} मी. ")
            append("(${locationTracker.evaluateQuality(avgAccuracy).hindiLabel})")
        }

        val measurementEntity = LandMeasurementEntity(
            landCaseId = state.linkedLandCase?.id,
            title = title.ifBlank { "खेत नाप #${System.currentTimeMillis() % 10000}" },
            timestamp = System.currentTimeMillis(),
            pointsJson = PointsConverter.toJson(points),
            pointCount = points.size,
            areaSqM = state.estimatedAreaSqM,
            perimeterM = state.estimatedPerimeterM,
            laggiHaath = state.selectedLaggiHaath,
            averageAccuracyM = avgAccuracy,
            bestAccuracyM = bestAccuracy,
            gpsQualitySummary = qualitySummary,
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

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}
