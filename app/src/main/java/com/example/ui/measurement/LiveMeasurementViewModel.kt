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
            _uiState.update { it.copy(warningMessage = "कृपया फोन की लोकेशन/GPS सेवा चालू करें / Please enable GPS") }
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
                userNotice = "GPS चालू है: कृपया खेत के पहले कोने (START) पर खड़े रहें। ताज़ा सिग्नल मिलते ही START बिंदु दर्ज होगा..."
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
                _uiState.update { it.copy(warningMessage = "लोकेशन त्रुटि: ${e.message}") }
            }
        }
    }

    private fun processLocationUpdate(location: Location) {
        val accuracy = location.accuracy
        val quality = locationTracker.evaluateQuality(accuracy)
        val bearing = if (location.hasBearing()) location.bearing else 0f
        val speed = if (location.hasSpeed()) location.speed else 0f
        val currentGeo = GeoPoint(
            lat = location.latitude,
            lng = location.longitude,
            accuracy = accuracy,
            timestamp = location.time,
            bearing = bearing,
            speed = speed
        )

        val warning = if (accuracy > 15f) {
            "सावधानी: GPS संकेत कमजोर है (±${String.format(java.util.Locale.US, "%.1f", accuracy)} मी.)। खुले आसमान में आएं।"
        } else null

        val now = System.currentTimeMillis()
        val isFresh = (now - location.time < 8000L) && (location.time >= sessionStartTime - 1000L)

        val state = _uiState.value
        val shouldRecordStart = state.isTracking &&
                !state.isPaused &&
                !state.startPointRecorded &&
                state.recordedPoints.isEmpty() &&
                isFresh &&
                accuracy <= 15.0f

        val updatedPoints = if (shouldRecordStart) {
            listOf(currentGeo)
        } else {
            state.recordedPoints
        }

        val notice = when {
            shouldRecordStart ->
                "पहला कोना (START / शुरू) दर्ज हो गया! अब खेत की मेड़ के साथ अगले कोने (Corner 2) पर जाएं और 'बिंदु जोड़ें' दबाएं।"
            state.isWaitingForFreshGps && accuracy > 15f ->
                "START की प्रतीक्षा: GPS सटीकता ±${String.format(java.util.Locale.US, "%.1f", accuracy)}m है (≤15m होने पर START दर्ज होगा)..."
            else -> state.userNotice
        }

        _uiState.update {
            it.copy(
                currentLocation = currentGeo,
                currentAccuracy = accuracy,
                gpsQuality = quality,
                warningMessage = warning,
                userNotice = notice,
                startPointRecorded = state.startPointRecorded || shouldRecordStart,
                isWaitingForFreshGps = if (shouldRecordStart) false else state.isWaitingForFreshGps
            )
        }

        if (updatedPoints.size != state.recordedPoints.size) {
            recalculateAndValidate(updatedPoints, isClosed = state.isClosed, edited = true)
        }
    }

    fun addManualPoint() {
        val state = _uiState.value
        if (state.isPaused) {
            _uiState.update { it.copy(warningMessage = "नाप रुकी हुई है। पहले 'जारी रखें' दबाएं।") }
            return
        }
        val current = state.currentLocation
        if (current == null) {
            _uiState.update { it.copy(warningMessage = "GPS स्थान अभी प्राप्त हो रहा है, कृपया 2 सेकंड प्रतीक्षा करें") }
            return
        }

        // If no START point recorded yet (user explicitly adds first corner manually):
        if (state.recordedPoints.isEmpty()) {
            appendPoint(current)
            _uiState.update {
                it.copy(
                    startPointRecorded = true,
                    isWaitingForFreshGps = false,
                    userNotice = "पहला कोना (START / शुरू) दर्ज हो गया! अब अगले कोने पर जाएं और 'बिंदु जोड़ें' दबाएं।"
                )
            }
            return
        }

        // Prevent duplicate consecutive points within 2 meters
        val lastPoint = state.recordedPoints.last()
        val distToLast = GeoLandCalculator.computeHaversineDistance(lastPoint, current)
        if (distToLast < 2.0) {
            _uiState.update {
                it.copy(userNotice = "आप पिछले दर्ज कोने के बहुत करीब हैं (${String.format(java.util.Locale.US, "%.1f", distToLast)}m)। अगले कोने पर बढ़ें।")
            }
            return
        }

        // Prevent re-inserting START as ordinary vertex if user has completed loop back near START
        val startPoint = state.recordedPoints.first()
        val distToStart = GeoLandCalculator.computeHaversineDistance(startPoint, current)
        if (state.recordedPoints.size >= 3 && distToStart < 3.5) {
            _uiState.update {
                it.copy(userNotice = "आप शुरुआती कोने (START) पर पहुंच चुके हैं! बाउंड्री बंद करने के लिए 'नाप पूरी करें' दबाएं।")
            }
            return
        }

        appendPoint(current)
    }

    private fun appendPoint(point: GeoPoint) {
        val currentList = _uiState.value.recordedPoints.toMutableList()
        currentList.add(point)
        recalculateAndValidate(currentList, isClosed = _uiState.value.isClosed, edited = true)
        val cornerNumber = currentList.size
        _uiState.update {
            it.copy(userNotice = "कोना #${cornerNumber} (Corner $cornerNumber) दर्ज किया गया ✓")
        }
    }

    fun undoLastPoint() {
        val currentList = _uiState.value.recordedPoints.toMutableList()
        if (currentList.size > 1) {
            val removedIndex = currentList.size
            currentList.removeAt(currentList.lastIndex)
            recalculateAndValidate(currentList, isClosed = false, edited = true)
            _uiState.update {
                it.copy(
                    isClosed = false,
                    userNotice = "अंतिम कोना #${removedIndex} हटा दिया गया (Undo)"
                )
            }
        } else if (currentList.size == 1) {
            _uiState.update {
                it.copy(warningMessage = "START बिंदु को Undo से नहीं हटाया जा सकता। पूरी नाप रीसेट करने के लिए 'रद्द करें' (Cancel) दबाएं।")
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
        var points = _uiState.value.recordedPoints
        if (points.size < 3) {
            _uiState.update { it.copy(warningMessage = "नाप पूरी करने के लिए कम से कम 3 बिंदु आवश्यक हैं। खेत के अगले कोने पर जाएं और 'बिंदु जोड़ें' दबाएं।") }
            return
        }

        // If user accidentally tapped Add Point right at START at the very end, drop the duplicate vertex:
        if (points.size > 3 && GeoLandCalculator.computeHaversineDistance(points.first(), points.last()) < 2.5) {
            points = points.dropLast(1)
        }

        recalculateAndValidate(points, isClosed = true, edited = _uiState.value.wasManuallyEdited)
        stopTracking()
        _uiState.update {
            it.copy(
                isReadyForReview = true,
                userNotice = "नाप पूरी हुई • कुल ${points.size} कोने दर्ज किए गए"
            )
        }
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
                linkedLandCase = it.linkedLandCase,
                selectedLaggiHaath = it.selectedLaggiHaath
            )
        }
    }

    private fun recalculateAndValidate(points: List<GeoPoint>, isClosed: Boolean, edited: Boolean) {
        val validation = GeoLandCalculator.validatePolygon(points)
        // Area is calculated only when closed or when user has 3+ points and requests review
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
            append("Avg Accuracy: ±${String.format(java.util.Locale.US, "%.1f", avgAccuracy)} m ")
            append("(${locationTracker.evaluateQuality(avgAccuracy).hindiLabel})")
        }

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
