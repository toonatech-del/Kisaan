package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.GeoLandCalculator
import com.example.util.GeoPoint
import com.example.util.Point2D
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

@Composable
fun SchematicPlotView(
    points: List<GeoPoint>,
    currentLocation: GeoPoint?,
    isClosed: Boolean,
    modifier: Modifier = Modifier,
    selectedPointIndex: Int? = null,
    onPointSelected: (Int) -> Unit = {}
) {
    val greenColor = MaterialTheme.colorScheme.primary
    val blueColor = Color(0xFF1976D2)
    val amberColor = Color(0xFFF59E0B)
    val gridColor = Color(0xFFE2E8F0)

    // State for legend visibility and auto-dismiss timer
    var isLegendVisible by remember { mutableStateOf(true) }
    var userInteractingWithLegend by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var autoHideJob by remember { mutableStateOf<Job?>(null) }

    fun scheduleAutoHide(delayMs: Long = 2000L) {
        autoHideJob?.cancel()
        autoHideJob = coroutineScope.launch {
            delay(delayMs)
            if (!userInteractingWithLegend) {
                isLegendVisible = false
            }
        }
    }

    // When the screen opens, show the legend briefly for 2 seconds, then automatically hide it
    LaunchedEffect(Unit) {
        scheduleAutoHide(2000L)
    }

    // Trigger auto-hide if points list changes or isClosed changes (if legend was opened due to an action)
    val pointCount = points.size
    LaunchedEffect(pointCount, isClosed) {
        if (isLegendVisible && !userInteractingWithLegend) {
            scheduleAutoHide(2000L)
        }
    }

    Box(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .background(Color(0xFFFBFDF9), RoundedCornerShape(16.dp))
            .testTag("schematic_plot_view")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .pointerInput(points) {
                    detectTapGestures { tapOffset ->
                        // Hit detection for points
                        if (points.isNotEmpty()) {
                            val allPoints = if (currentLocation != null) points + currentLocation else points
                            val localPoints = GeoLandCalculator.projectToLocalPlane(allPoints)
                            if (localPoints.isNotEmpty()) {
                                val (minX, maxX, minY, maxY) = computeBounds(localPoints)
                                val widthSpan = max(maxX - minX, 10.0)
                                val heightSpan = max(maxY - minY, 10.0)
                                val scale = min((size.width - 80f) / widthSpan, (size.height - 80f) / heightSpan).toFloat()
                                val offsetX = ((size.width - widthSpan.toFloat() * scale) / 2f) - (minX.toFloat() * scale)
                                val offsetY = ((size.height - heightSpan.toFloat() * scale) / 2f) + (maxY.toFloat() * scale)

                                points.forEachIndexed { index, _ ->
                                    val pt2d = localPoints[index]
                                    val px = pt2d.x.toFloat() * scale + offsetX
                                    val py = offsetY - pt2d.y.toFloat() * scale
                                    val dist = (Offset(px, py) - tapOffset).getDistance()
                                    if (dist < 32f) {
                                        onPointSelected(index)
                                        return@detectTapGestures
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw Agricultural survey background grid
            drawPlotGrid(gridColor, canvasW, canvasH)

            if (points.isEmpty()) {
                // Draw helpful empty state guidance directly on canvas
                return@Canvas
            }

            // Project all coordinates to local metric plane
            val allForProjection = if (currentLocation != null) points + currentLocation else points
            val projected = GeoLandCalculator.projectToLocalPlane(allForProjection)
            val boundaryProjected = projected.take(points.size)
            val currentProjected = if (currentLocation != null) projected.last() else null

            val (minX, maxX, minY, maxY) = computeBounds(projected)
            val spanX = max(maxX - minX, 12.0)
            val spanY = max(maxY - minY, 12.0)

            val padding = 48f
            val availableW = canvasW - padding * 2
            val availableH = canvasH - padding * 2

            val scale = min(availableW / spanX, availableH / spanY).toFloat()
            val drawOffsetX = padding + ((availableW - spanX.toFloat() * scale) / 2f) - (minX.toFloat() * scale)
            val drawOffsetY = canvasH - padding - ((availableH - spanY.toFloat() * scale) / 2f) + (minY.toFloat() * scale)

            fun toCanvasOffset(p: Point2D): Offset {
                val cx = p.x.toFloat() * scale + drawOffsetX
                val cy = drawOffsetY - p.y.toFloat() * scale
                return Offset(cx, cy)
            }

            // 2. Draw polygon fill if 3+ points
            if (boundaryProjected.size >= 3) {
                val polyPath = Path().apply {
                    val first = toCanvasOffset(boundaryProjected.first())
                    moveTo(first.x, first.y)
                    for (i in 1 until boundaryProjected.size) {
                        val pt = toCanvasOffset(boundaryProjected[i])
                        lineTo(pt.x, pt.y)
                    }
                    if (isClosed) {
                        close()
                    }
                }
                drawPath(
                    path = polyPath,
                    color = greenColor.copy(alpha = if (isClosed) 0.22f else 0.12f),
                    style = Fill
                )
            }

            // 3. Draw boundary lines
            if (boundaryProjected.size >= 2) {
                for (i in 0 until boundaryProjected.size - 1) {
                    val p1 = toCanvasOffset(boundaryProjected[i])
                    val p2 = toCanvasOffset(boundaryProjected[i + 1])
                    drawLine(
                        color = greenColor,
                        start = p1,
                        end = p2,
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }

                // If closed, draw closing segment
                if (isClosed && boundaryProjected.size >= 3) {
                    val pLast = toCanvasOffset(boundaryProjected.last())
                    val pFirst = toCanvasOffset(boundaryProjected.first())
                    drawLine(
                        color = amberColor,
                        start = pLast,
                        end = pFirst,
                        strokeWidth = 5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 4. Draw vertices
            boundaryProjected.forEachIndexed { index, p2d ->
                val offset = toCanvasOffset(p2d)
                val isSelected = selectedPointIndex == index
                val isFirst = index == 0
                val isLast = isClosed && index == boundaryProjected.size - 1

                val pointColor = when {
                    isSelected -> Color(0xFFEF4444)
                    isFirst -> Color(0xFF16A34A) // START green
                    isLast -> Color(0xFFD97706)  // END amber
                    else -> greenColor
                }

                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 15f else 12f,
                    center = offset
                )
                drawCircle(
                    color = pointColor,
                    radius = if (isSelected) 13f else 10f,
                    center = offset
                )

                // Label text / badge
                val labelText = when {
                    isFirst -> "START"
                    isLast -> "END"
                    else -> "${index + 1}"
                }

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 22f
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText(
                    labelText,
                    offset.x,
                    offset.y + 7f,
                    paint
                )

                // Sub-label below vertex
                val subPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 22f
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                val subText = when {
                    isFirst -> "शुरू (START)"
                    isLast -> "अंतिम (END)"
                    else -> "बिंदु ${index + 1}"
                }
                drawContext.canvas.nativeCanvas.drawText(
                    subText,
                    offset.x,
                    offset.y - 18f,
                    subPaint
                )
            }

            // 5. Draw current live GPS person marker with accuracy radius
            if (currentProjected != null && currentLocation != null) {
                val currentOffset = toCanvasOffset(currentProjected)
                val accuracyPixels = max(currentLocation.accuracy * scale, 14f)
                val personColor = Color(0xFF0284C7)

                // Accuracy circle halo
                drawCircle(
                    color = personColor.copy(alpha = 0.12f),
                    radius = accuracyPixels,
                    center = currentOffset
                )
                drawCircle(
                    color = personColor.copy(alpha = 0.35f),
                    radius = accuracyPixels,
                    center = currentOffset,
                    style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                )

                // Person marker base circle
                drawCircle(
                    color = Color.White,
                    radius = 16f,
                    center = currentOffset
                )
                drawCircle(
                    color = personColor,
                    radius = 14f,
                    center = currentOffset
                )

                // Person silhouette (Head + Body)
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(currentOffset.x, currentOffset.y - 4f)
                )
                val bodyPath = Path().apply {
                    moveTo(currentOffset.x - 6f, currentOffset.y + 7f)
                    quadraticTo(currentOffset.x, currentOffset.y + 1f, currentOffset.x + 6f, currentOffset.y + 7f)
                }
                drawPath(bodyPath, Color.White, style = Stroke(width = 3f, cap = StrokeCap.Round))

                // Accessible Label for Live Location
                val currentPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#0284C7")
                    textSize = 24f
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "आपकी लाइव लोकेशन / Your live location",
                    currentOffset.x,
                    currentOffset.y + 36f,
                    currentPaint
                )
            }
        }

        // Empty state overlay when 0 points recorded yet
        if (points.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "मेड़ पर खड़े होकर 'नाप शुरू करें' दबाएं",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "पहला बिंदु (START) स्वचालित रूप से दर्ज होगा",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Legend and Info controls container
        // Small unobtrusive Legend / ℹ️ button in map corner (Bottom-Start)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            if (!isLegendVisible) {
                Surface(
                    onClick = {
                        isLegendVisible = true
                        scheduleAutoHide(2000L)
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Animated Legend Overlay popup
        AnimatedVisibility(
            visible = isLegendVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.95f),
            exit = fadeOut() + scaleOut(targetScale = 0.95f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .testTag("legend_overlay_popup")
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                userInteractingWithLegend = true
                                autoHideJob?.cancel()
                                tryAwaitRelease()
                                userInteractingWithLegend = false
                                // Dismiss shortly after interaction ends (e.g. 2s)
                                scheduleAutoHide(2000L)
                            },
                            onTap = {
                                // Tapping inside keeps it open for another 2 seconds
                                scheduleAutoHide(2000L)
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.68f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "मानचित्र संकेत (Legend)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                autoHideJob?.cancel()
                                isLegendVisible = false
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Legend",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF16A34A), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("START / शुरू", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(greenColor, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("बिंदु (Vertex)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFD97706), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("END / अंतिम", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF0284C7), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("आपकी लाइव लोकेशन / Live GPS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }
                    Text(
                        text = "GPS निर्देशांकों पर आधारित रेखाचित्र",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Top North Compass indicator
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "उत्तर दिशा (North)",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "उत्तर (N)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun computeBounds(points: List<Point2D>): Bounds {
    var minX = Double.MAX_VALUE
    var maxX = -Double.MAX_VALUE
    var minY = Double.MAX_VALUE
    var maxY = -Double.MAX_VALUE

    points.forEach { pt ->
        if (pt.x < minX) minX = pt.x
        if (pt.x > maxX) maxX = pt.x
        if (pt.y < minY) minY = pt.y
        if (pt.y > maxY) maxY = pt.y
    }
    return Bounds(minX, maxX, minY, maxY)
}

private data class Bounds(val minX: Double, val maxX: Double, val minY: Double, val maxY: Double)

private fun DrawScope.drawPlotGrid(color: Color, width: Float, height: Float) {
    val step = 40f
    var x = 0f
    while (x <= width) {
        drawLine(color = color, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y <= height) {
        drawLine(color = color, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
        y += step
    }
}
