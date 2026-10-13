package com.example.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.filled.Warning
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import com.example.util.GeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

@Composable
fun OsmLandMapView(
    points: List<GeoPoint>,
    currentLocation: GeoPoint?,
    isClosed: Boolean,
    modifier: Modifier = Modifier,
    onPointMoved: (index: Int, lat: Double, lng: Double) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize osmdroid configuration
    remember {
        val basePath = context.applicationContext.filesDir
        Configuration.getInstance().osmdroidBasePath = basePath
        Configuration.getInstance().osmdroidTileCache = java.io.File(basePath, "osmdroid/tiles")
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val googleSatellite = remember {
        object : org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase(
            "GoogleSatellite",
            0, 20, 256, ".jpg",
            arrayOf(
                "https://mt0.google.com/vt/lyrs=y&x={x}&y={y}&z={z}",
                "https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}",
                "https://mt2.google.com/vt/lyrs=y&x={x}&y={y}&z={z}",
                "https://mt3.google.com/vt/lyrs=y&x={x}&y={y}&z={z}"
            )
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
                val x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
                val y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)
                val baseUrl = baseUrl
                return baseUrl.replace("{x}", x.toString())
                              .replace("{y}", y.toString())
                              .replace("{z}", zoom.toString())
            }
        }
    }

    var isSatelliteMode by remember { mutableStateOf(true) }
    var isHeadingFollowMode by remember { mutableStateOf(false) }
    var smoothedAzimuth by remember { mutableFloatStateOf(0f) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(googleSatellite)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(19.0)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
        }
    }

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    val sensorListener = remember {
        object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientation = FloatArray(3)
            private var lastAccel: FloatArray? = null
            private var lastMag: FloatArray? = null
            private val alpha = 0.18f // Low-pass filter smoothing coefficient
            private var smoothedSin = 0f
            private var smoothedCos = 1f

            override fun onSensorChanged(event: SensorEvent) {
                var azimuthRad: Float? = null

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    azimuthRad = orientation[0]
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    lastAccel = event.values.clone()
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    lastMag = event.values.clone()
                }

                if (azimuthRad == null && lastAccel != null && lastMag != null) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, lastAccel, lastMag)) {
                        SensorManager.getOrientation(r, orientation)
                        azimuthRad = orientation[0]
                    }
                }

                azimuthRad?.let { rad ->
                    // Circular low-pass filter on unit vector to prevent 0°/360° jump jitter
                    val s = sin(rad.toDouble()).toFloat()
                    val c = cos(rad.toDouble()).toFloat()
                    smoothedSin = (alpha * s) + ((1f - alpha) * smoothedSin)
                    smoothedCos = (alpha * c) + ((1f - alpha) * smoothedCos)
                    val deg = ((Math.toDegrees(atan2(smoothedSin.toDouble(), smoothedCos.toDouble())).toFloat()) + 360f) % 360f
                    smoothedAzimuth = deg

                    if (isHeadingFollowMode) {
                        mapView.mapOrientation = -deg
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    LaunchedEffect(isSatelliteMode) {
        mapView.setTileSource(if (isSatelliteMode) googleSatellite else TileSourceFactory.MAPNIK)
    }

    // Handle MapView and Sensor lifecycle (onResume / onPause to save battery)
    DisposableEffect(lifecycleOwner, mapView) {
        val rotSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        fun registerSensors() {
            if (sensorManager == null) return
            if (rotSensor != null) {
                sensorManager.registerListener(sensorListener, rotSensor, SensorManager.SENSOR_DELAY_UI)
            } else {
                accSensor?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI) }
                magSensor?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI) }
            }
        }

        fun unregisterSensors() {
            sensorManager?.unregisterListener(sensorListener)
        }

        registerSensors()

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    registerSensors()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    unregisterSensors()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    unregisterSensors()
                    mapView.onDetach()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            unregisterSensors()
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Auto-center map on first point or current location
    LaunchedEffect(currentLocation, points.size) {
        if (points.isNotEmpty()) {
            val lastPt = points.last()
            mapView.controller.animateTo(OsmGeoPoint(lastPt.lat, lastPt.lng))
        } else if (currentLocation != null) {
            mapView.controller.animateTo(OsmGeoPoint(currentLocation.lat, currentLocation.lng))
        }
    }

    // Redraw markers, polylines, edge annotations, and polygon whenever points, location, or heading change
    LaunchedEffect(points, currentLocation, isClosed, isHeadingFollowMode, (smoothedAzimuth.toInt() / 4)) {
        mapView.overlays.clear()

        val displayPoints = if (isClosed && points.size >= 3) com.example.util.GeoLandCalculator.orderPointsCircular(points) else points

        // 1. Draw recorded points polygon / polyline
        if (displayPoints.size >= 3) {
            if (isClosed) {
                val polygon = Polygon(mapView).apply {
                    outlinePaint.color = AndroidColor.parseColor("#15803D") // Dark Green
                    outlinePaint.strokeWidth = 6f
                    fillPaint.color = AndroidColor.parseColor("#3316A34A") // 20% Alpha Green
                    
                    val osmPoints = displayPoints.map { OsmGeoPoint(it.lat, it.lng) }
                    // Mathematically close boundary by connecting the last corner back to Point #1
                    setPoints(osmPoints + osmPoints.first())
                    title = "Field Boundary"
                }
                mapView.overlays.add(polygon)
            } else {
                // While still recording corners (4, 5, 6, N corners), only draw open perimeter line
                val polyline = Polyline(mapView).apply {
                    outlinePaint.color = AndroidColor.parseColor("#15803D")
                    outlinePaint.strokeWidth = 6f
                    setPoints(displayPoints.map { OsmGeoPoint(it.lat, it.lng) })
                }
                mapView.overlays.add(polyline)
            }
        } else if (displayPoints.size == 2) {
            val polyline = Polyline(mapView).apply {
                outlinePaint.color = AndroidColor.parseColor("#15803D")
                outlinePaint.strokeWidth = 6f
                setPoints(displayPoints.map { OsmGeoPoint(it.lat, it.lng) })
            }
            mapView.overlays.add(polyline)
        }

        // 2. Draw live walking guidance line from last recorded corner to current live location
        if (!isClosed && points.isNotEmpty() && currentLocation != null) {
            val isCrossing = com.example.util.GeoLandCalculator.checkLiveSegmentCrosses(points, currentLocation)
            val walkingLine = Polyline(mapView).apply {
                outlinePaint.color = if (isCrossing) AndroidColor.parseColor("#DC2626") else AndroidColor.parseColor("#0284C7")
                outlinePaint.strokeWidth = if (isCrossing) 8f else 5f
                outlinePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(12f, 12f), 0f)
                setPoints(
                    listOf(
                        OsmGeoPoint(points.last().lat, points.last().lng),
                        OsmGeoPoint(currentLocation.lat, currentLocation.lng)
                    )
                )
            }
            mapView.overlays.add(walkingLine)
        }

        // 3. Side Length Labels on Map Boundary Lines (Edge Annotations at midpoints)
        if (displayPoints.size >= 2) {
            for (i in 0 until displayPoints.size - 1) {
                val p1 = displayPoints[i]
                val p2 = displayPoints[i + 1]
                val midLat = (p1.lat + p2.lat) / 2.0
                val midLng = (p1.lng + p2.lng) / 2.0
                val edgeResults = FloatArray(1)
                android.location.Location.distanceBetween(p1.lat, p1.lng, p2.lat, p2.lng, edgeResults)
                val distM = edgeResults[0]
                val distFt = distM * 3.28084f
                val label = "${String.format(java.util.Locale.US, "%.1f", distM)} m | ${String.format(java.util.Locale.US, "%.1f", distFt)} ft"

                val edgeMarker = Marker(mapView).apply {
                    position = OsmGeoPoint(midLat, midLng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "भुजा #${i + 1}"
                    snippet = label
                    icon = createEdgeBadgeDrawable(context, label, isLive = false)
                    infoWindow = null
                    isDraggable = false
                }
                mapView.overlays.add(edgeMarker)
            }
        }

        // If boundary closed, also annotate the closing edge from last point back to Point #1
        if (isClosed && displayPoints.size >= 3) {
            val pLast = displayPoints.last()
            val pFirst = displayPoints.first()
            val midLat = (pLast.lat + pFirst.lat) / 2.0
            val midLng = (pLast.lng + pFirst.lng) / 2.0
            val closeResults = FloatArray(1)
            android.location.Location.distanceBetween(pLast.lat, pLast.lng, pFirst.lat, pFirst.lng, closeResults)
            val distM = closeResults[0]
            val distFt = distM * 3.28084f
            val label = "${String.format(java.util.Locale.US, "%.1f", distM)} m | ${String.format(java.util.Locale.US, "%.1f", distFt)} ft"

            val closeMarker = Marker(mapView).apply {
                position = OsmGeoPoint(midLat, midLng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "भुजा #${displayPoints.size}"
                snippet = label
                icon = createEdgeBadgeDrawable(context, label, isLive = false)
                infoWindow = null
                isDraggable = false
            }
            mapView.overlays.add(closeMarker)
        }

        // Live walking distance label on current in-progress segment (Point #N to current location)
        if (!isClosed && points.isNotEmpty() && currentLocation != null) {
            val pLast = points.last()
            val midLat = (pLast.lat + currentLocation.lat) / 2.0
            val midLng = (pLast.lng + currentLocation.lng) / 2.0
            val liveResults = FloatArray(1)
            android.location.Location.distanceBetween(pLast.lat, pLast.lng, currentLocation.lat, currentLocation.lng, liveResults)
            val distM = liveResults[0]
            val distFt = distM * 3.28084f
            val isCrossing = com.example.util.GeoLandCalculator.checkLiveSegmentCrosses(points, currentLocation)
            val liveLabel = if (isCrossing) "काट रही है! ${String.format(java.util.Locale.US, "%.1f", distM)}m" else "चालू: ${String.format(java.util.Locale.US, "%.1f", distM)} m | ${String.format(java.util.Locale.US, "%.1f", distFt)} ft"

            val liveEdgeMarker = Marker(mapView).apply {
                position = OsmGeoPoint(midLat, midLng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "वर्तमान दूरी"
                snippet = liveLabel
                icon = createEdgeBadgeDrawable(context, liveLabel, isLive = true, isWarning = isCrossing)
                infoWindow = null
                isDraggable = false
            }
            mapView.overlays.add(liveEdgeMarker)
        }

        // 4. Add draggable markers for every recorded corner
        displayPoints.forEachIndexed { index, pt ->
            val origIndex = points.indexOf(pt).let { if (it >= 0) it else index }
            val marker = Marker(mapView).apply {
                position = OsmGeoPoint(pt.lat, pt.lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = if (index == 0) "पहला कोना (START)" else "कोना #${index + 1}"
                snippet = "Lat: ${String.format(java.util.Locale.US, "%.6f", pt.lat)}, Lng: ${String.format(java.util.Locale.US, "%.6f", pt.lng)}"
                isDraggable = true
                
                // Custom circular drawable marker
                icon = createCornerMarkerDrawable(context, index)

                setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                    override fun onMarkerDragStart(m: Marker?) {}
                    override fun onMarkerDrag(m: Marker?) {}
                    override fun onMarkerDragEnd(m: Marker?) {
                        m?.position?.let { newPos ->
                            onPointMoved(origIndex, newPos.latitude, newPos.longitude)
                        }
                    }
                })
            }
            mapView.overlays.add(marker)
        }

        // 5. Current Live GPS Marker with Directional Flashlight Cone / Beam
        if (currentLocation != null) {
            val beamAngle = if (isHeadingFollowMode) 0f else smoothedAzimuth
            val liveMarker = Marker(mapView).apply {
                position = OsmGeoPoint(currentLocation.lat, currentLocation.lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "आपकी लाइव लोकेशन"
                snippet = "Accuracy: ±${String.format(java.util.Locale.US, "%.1f", currentLocation.accuracy)}m"
                isDraggable = false
                icon = createLiveLocationDrawable(context, beamAngle)
            }
            mapView.overlays.add(liveMarker)
        }

        mapView.invalidate()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .testTag("osmdroid_map_view")
    ) {
        AndroidView(
            factory = {
                mapView.apply {
                    clipToOutline = true
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
        )

        // Floating Live Distance Badge (From Last Recorded Corner) - Top-Start
        if (points.isNotEmpty() && currentLocation != null) {
            val distResults = FloatArray(1)
            android.location.Location.distanceBetween(
                points.last().lat, points.last().lng,
                currentLocation.lat, currentLocation.lng,
                distResults
            )
            val distM = distResults[0]
            val distFt = distM * 3.28084f

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .testTag("floating_live_distance_badge"),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xF2FFFFFF),
                border = BorderStroke(1.5.dp, Color(0xFF16A34A)),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📏 वर्तमान दूरी: ${String.format(java.util.Locale.US, "%.1f", distM)} m (${String.format(java.util.Locale.US, "%.1f", distFt)} फ़ीट)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534)
                    )
                }
            }
        }

        // Top North Compass Indicator - Top-End
        Surface(
            onClick = {
                isHeadingFollowMode = !isHeadingFollowMode
                if (!isHeadingFollowMode) {
                    mapView.mapOrientation = 0f
                    currentLocation?.let {
                        mapView.controller.animateTo(OsmGeoPoint(it.lat, it.lng))
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .testTag("north_compass_badge"),
            shape = RoundedCornerShape(8.dp),
            color = if (isHeadingFollowMode) Color(0xFFEFF6FF) else Color(0xF2FFFFFF),
            border = BorderStroke(1.dp, if (isHeadingFollowMode) Color(0xFF3B82F6) else Color(0xFFE0E0E0)),
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "उत्तर दिशा (North)",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(if (isHeadingFollowMode) smoothedAzimuth else 0f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isHeadingFollowMode) "लाइव दिशा" else "उत्तर (N)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }

        var showLegend by remember { mutableStateOf(false) }

        // Unobtrusive Legend Button & Popup - Bottom-Start
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            if (!showLegend) {
                Surface(
                    onClick = { showLegend = true },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xF2FFFFFF),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    shadowElevation = 3.dp,
                    modifier = Modifier.testTag("legend_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "संकेत सूची (Legend)",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "संकेत / Legend",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xF7FFFFFF),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.testTag("legend_overlay_popup")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "मानचित्र संकेत (Legend)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { showLegend = false },
                                tint = Color.Gray
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF16A34A), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("START (पहला कोना)", fontSize = 10.sp, color = Color(0xFF1E293B))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF15803D), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("कोना / Vertex", fontSize = 10.sp, color = Color(0xFF1E293B))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF0284C7), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("लाइव GPS लोकेशन", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        }
                    }
                }
            }
        }

        // Floating Map Controls (Satellite Layer Toggle & Center My Location) - Bottom-End
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Layer Switcher (Satellite vs Mapnik)
            SmallFloatingActionButton(
                onClick = {
                    isSatelliteMode = !isSatelliteMode
                    Toast.makeText(
                        context,
                        if (isSatelliteMode) "सैटेलाइट व्यू (Satellite View)" else "मानचित्र व्यू (Map View)",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.testTag("toggle_map_layer_button"),
                containerColor = if (isSatelliteMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (isSatelliteMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Toggle Satellite Layer",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Recenter My Location Button
            SmallFloatingActionButton(
                onClick = {
                    val target = when {
                        currentLocation != null -> OsmGeoPoint(currentLocation.lat, currentLocation.lng)
                        points.isNotEmpty() -> OsmGeoPoint(points.last().lat, points.last().lng)
                        else -> null
                    }
                    target?.let {
                        mapView.controller.animateTo(it)
                        mapView.controller.setZoom(19.0)
                    } ?: run {
                        Toast.makeText(context, "GPS location not available yet", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.testTag("recenter_map_button"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun createCornerMarkerDrawable(context: Context, index: Int): android.graphics.drawable.Drawable {
    val sizePx = (32 * context.resources.displayMetrics.density).toInt()
    val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val colorHex = if (index == 0) "#16A34A" else "#15803D"
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor(colorHex)
        style = android.graphics.Paint.Style.FILL
    }
    val whitePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 4f
    }
    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = (13 * context.resources.displayMetrics.density)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = android.graphics.Paint.Align.CENTER
    }

    val radius = sizePx / 2f - 4f
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, radius, paint)
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, radius, whitePaint)

    val label = (index + 1).toString()
    val yPos = (sizePx / 2f - (textPaint.descent() + textPaint.ascent()) / 2)
    canvas.drawText(label, sizePx / 2f, yPos, textPaint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

private fun createLiveLocationDrawable(context: Context, heading: Float = 0f): android.graphics.drawable.Drawable {
    val sizePx = (36 * context.resources.displayMetrics.density).toInt()
    val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    canvas.save()
    canvas.rotate(heading, sizePx / 2f, sizePx / 2f)

    val conePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(70, 2, 132, 199)
        style = android.graphics.Paint.Style.FILL
    }
    val path = android.graphics.Path().apply {
        moveTo(sizePx / 2f, sizePx / 2f)
        lineTo(sizePx / 2f - 12f, 4f)
        lineTo(sizePx / 2f + 12f, 4f)
        close()
    }
    canvas.drawPath(path, conePaint)
    canvas.restore()

    val bluePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#0284C7")
        style = android.graphics.Paint.Style.FILL
    }
    val whitePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = android.graphics.Paint.Style.FILL
    }
    val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f
    }

    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val r = 10f * context.resources.displayMetrics.density
    canvas.drawCircle(cx, cy, r, bluePaint)
    canvas.drawCircle(cx, cy, r, strokePaint)
    canvas.drawCircle(cx, cy, 3.5f * context.resources.displayMetrics.density, whitePaint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

private fun createEdgeBadgeDrawable(context: Context, text: String, isLive: Boolean = false, isWarning: Boolean = false): android.graphics.drawable.Drawable {
    val density = context.resources.displayMetrics.density
    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 10.5f * density
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = android.graphics.Paint.Align.CENTER
    }

    val textWidth = textPaint.measureText(text)
    val textHeight = textPaint.descent() - textPaint.ascent()

    val padH = 6f * density
    val padV = 3f * density

    val totalWidth = (textWidth + padH * 2).toInt().coerceAtLeast((36 * density).toInt())
    val totalHeight = (textHeight + padV * 2).toInt().coerceAtLeast((20 * density).toInt())

    val bitmap = android.graphics.Bitmap.createBitmap(totalWidth, totalHeight, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = when {
            isWarning -> AndroidColor.parseColor("#E6DC2626")
            isLive -> AndroidColor.parseColor("#E60284C7")
            else -> AndroidColor.parseColor("#E61E5631")
        }
        style = android.graphics.Paint.Style.FILL
    }
    val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    val rect = android.graphics.RectF(
        borderPaint.strokeWidth / 2f,
        borderPaint.strokeWidth / 2f,
        totalWidth - borderPaint.strokeWidth / 2f,
        totalHeight - borderPaint.strokeWidth / 2f
    )
    val cornerRadius = 5f * density
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

    val yPos = totalHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(text, totalWidth / 2f, yPos, textPaint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}
