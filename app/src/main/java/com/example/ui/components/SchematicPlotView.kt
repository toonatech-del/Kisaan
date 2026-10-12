package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

                // Outer circle ring
                val pointColor = when {
                    isSelected -> Color(0xFFEF4444)
                    isFirst -> Color(0xFF16A34A)
                    else -> greenColor
                }

                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 14f else 11f,
                    center = offset
                )
                drawCircle(
                    color = pointColor,
                    radius = if (isSelected) 12f else 9f,
                    center = offset
                )

                // Label point number
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 28f
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "${index + 1}",
                    offset.x,
                    offset.y - 14f,
                    paint
                )
            }

            // 5. Draw current live GPS location with accuracy radius
            if (currentProjected != null && currentLocation != null) {
                val currentOffset = toCanvasOffset(currentProjected)
                val accuracyPixels = max(currentLocation.accuracy * scale, 12f)

                // Accuracy circle
                drawCircle(
                    color = blueColor.copy(alpha = 0.15f),
                    radius = accuracyPixels,
                    center = currentOffset
                )
                drawCircle(
                    color = blueColor.copy(alpha = 0.4f),
                    radius = accuracyPixels,
                    center = currentOffset,
                    style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                )

                // Pulse dot
                drawCircle(
                    color = Color.White,
                    radius = 12f,
                    center = currentOffset
                )
                drawCircle(
                    color = blueColor,
                    radius = 9f,
                    center = currentOffset
                )

                // Label
                val currentPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLUE
                    textSize = 26f
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "आप यहाँ हैं",
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
                    text = "मेड़ पर खड़े होकर 'शुरू करें' दबाएं",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "खेत के कोने-कोने पर बिंदु जोड़ें और रेखाचित्र देखें",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

        // Offline notice banner at bottom
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFF1F5F9).copy(alpha = 0.92f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ऑफलाइन रेखाचित्र • GPS निर्देशांकों पर आधारित",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
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
