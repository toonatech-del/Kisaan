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
    val selectedLaggiHaath: Double = 6.0,
    val linkedLandCase: LandCaseEntity? = null,
    val wasManuallyEdited: Boolean = false,
    val isStabilizing: Boolean = false,
    val goodFixCount: Int = 0,
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
    private val surveyEngine = GpsLandSurveyEngine()
    private val cornerSamples = mutableListOf<Location>()
    private var goodFixCount = 0

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

    fun addCornerRequested() {
        if (_uiState.value.isPaused) return
        _uiState.update { it.copy(isStabilizing = true, userNotice = "कोना स्थिर हो रहा है... [0/3 samples]") }
        cornerSamples.clear()
    }

    fun startTracking() {
        checkGpsStatus()
        if (!_uiState.value.isGpsEnabled) {
            _uiState.update { it.copy(warningMessage = "कृपया फोन की लोकेशन/GPS सेवा चालू करें") }
            return
        }

        sessionStartTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                isTracking = true,
                isPaused = false,
                isClosed = false,
                recordedPoints = emptyList(),
                startPointRecorded = false,
                isWaitingForFreshGps = true,
                userNotice = "GPS चालू है..."
            )
        }
        startLocationUpdates()
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
        val nowNanos = SystemClock.elapsedRealtimeNanos()
        val locNanos = location.elapsedRealtimeNanos
        val ageSeconds = (nowNanos - locNanos) / 1_000_000_000.0
        
        if (ageSeconds > 3.0) return

        val accuracy = location.accuracy
        
        if (accuracy > 3.5f) {
            _uiState.update { it.copy(warningMessage = "GPS सटीकता कम है: ±${String.format(java.util.Locale.US, "%.1f", accuracy)}m", goodFixCount = 0) }
            return
        } else {
            _uiState.update { it.copy(warningMessage = null) }
            if (goodFixCount < 3) {
                goodFixCount++
                _uiState.update { it.copy(goodFixCount = goodFixCount) }
            }
        }

        val state = _uiState.value

        if (state.isStabilizing) {
            cornerSamples.add(location)
            _uiState.update { it.copy(userNotice = "कोना स्थिर हो रहा है... [${cornerSamples.size}/3 samples]") }
            if (cornerSamples.size >= 3) {
                val meanLat = cornerSamples.map { it.latitude }.average()
                val meanLng = cornerSamples.map { it.longitude }.average()
                val meanAcc = cornerSamples.map { it.accuracy }.average().toFloat()
                
                val stabilizedPoint = GeoPoint(meanLat, meanLng, meanAcc)
                
                val newPoints = state.recordedPoints + stabilizedPoint
                recalculateAndValidate(newPoints, state.isClosed, edited = true)

                _uiState.update { 
                    it.copy(
                        isStabilizing = false,
                        userNotice = "कोना दर्ज हो गया।"
                    ) 
                }
                cornerSamples.clear()
            }
            return
        }
        
        // ... (existing logic for start/moving location marker)
        val currentGeo = GeoPoint(
            lat = location.latitude,
            lng = location.longitude,
            accuracy = accuracy
        )
        _uiState.update { it.copy(currentLocation = currentGeo, currentAccuracy = accuracy) }
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
        val points = _uiState.value.recordedPoints
        if (points.size < 3) return
        recalculateAndValidate(points, isClosed = true, edited = _uiState.value.wasManuallyEdited)
        _uiState.update { it.copy(isClosed = true, userNotice = "बाउंड्री बंद की गई") }
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
    
    fun clearNotice() {
        _uiState.update { it.copy(userNotice = null, warningMessage = null) }
    }

    fun finishMeasurement() {
        val points = _uiState.value.recordedPoints
        if (points.size < 3) return
        stopTracking()
        _uiState.update { it.copy(isReadyForReview = true) }
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
        val validation = GeoLandCalculator.validatePolygon(points)
        val area = surveyEngine.calculateAreaSqMeters(points)
        val perimeter = surveyEngine.calculatePerimeterMeters(points)

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
    
    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}
