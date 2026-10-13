package com.example.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.util.GeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
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
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(19.0)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
        }
    }

    // Handle MapView lifecycle
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
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

    // Redraw markers, polylines, and polygon whenever points or currentLocation change
    LaunchedEffect(points, currentLocation, isClosed) {
        mapView.overlays.clear()

        // 1. Draw recorded points polygon / polyline
        if (points.size >= 3) {
            val polygon = Polygon(mapView).apply {
                outlinePaint.color = AndroidColor.parseColor("#15803D") // Dark Green
                outlinePaint.strokeWidth = 6f
                fillPaint.color = AndroidColor.parseColor("#3316A34A") // 20% Alpha Green
                
                val osmPoints = points.map { OsmGeoPoint(it.lat, it.lng) }
                // Connect in strict order [P1, P2, ... P_N, P1]
                val closedPoints = if (isClosed) osmPoints + osmPoints.first() else osmPoints
                setPoints(closedPoints)
                title = "Field Boundary"
            }
            mapView.overlays.add(polygon)
        } else if (points.size == 2) {
            val polyline = Polyline(mapView).apply {
                outlinePaint.color = AndroidColor.parseColor("#15803D")
                outlinePaint.strokeWidth = 6f
                setPoints(points.map { OsmGeoPoint(it.lat, it.lng) })
            }
            mapView.overlays.add(polyline)
        }

        // 2. Add draggable markers for every recorded corner
        points.forEachIndexed { index, pt ->
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
                            onPointMoved(index, newPos.latitude, newPos.longitude)
                        }
                    }
                })
            }
            mapView.overlays.add(marker)
        }

        // 3. Current Live GPS Marker
        if (currentLocation != null) {
            val liveMarker = Marker(mapView).apply {
                position = OsmGeoPoint(currentLocation.lat, currentLocation.lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "आपकी लाइव लोकेशन"
                snippet = "Accuracy: ±${String.format(java.util.Locale.US, "%.1f", currentLocation.accuracy)}m"
                isDraggable = false
                icon = createLiveLocationDrawable(context)
            }
            mapView.overlays.add(liveMarker)
        }

        mapView.invalidate()
    }

    Box(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .testTag("osmdroid_map_view")
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Center on Location Button
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
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .testTag("recenter_map_button"),
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

private fun createLiveLocationDrawable(context: Context): android.graphics.drawable.Drawable {
    val sizePx = (24 * context.resources.displayMetrics.density).toInt()
    val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val bluePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#0284C7") // Sky blue
        style = android.graphics.Paint.Style.FILL
    }
    val whitePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = android.graphics.Paint.Style.FILL
    }
    val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 4f
    }

    val radius = sizePx / 2f - 2f
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, radius, bluePaint)
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, radius, strokePaint)
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, 4f * context.resources.displayMetrics.density, whitePaint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}
