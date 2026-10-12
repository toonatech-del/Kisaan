package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.util.GeoPoint
import com.example.engine.GpsLandSurveyEngine

@Composable
fun PlotCanvasView(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier
) {
    val engine = GpsLandSurveyEngine()
    
    Canvas(modifier = modifier.fillMaxSize()) {
        if (points.size < 2) return@Canvas
        
        val cartesianPoints = engine.projectToLocalPlane(points)
        
        // Find bounds for auto-fit
        val minX = cartesianPoints.minOf { it.first }
        val maxX = cartesianPoints.maxOf { it.first }
        val minY = cartesianPoints.minOf { it.second }
        val maxY = cartesianPoints.maxOf { it.second }
        
        val padding = 100f
        val plotWidth = maxX - minX
        val plotHeight = maxY - minY
        val scale = minOf(
            (size.width - 2 * padding) / plotWidth,
            (size.height - 2 * padding) / plotHeight
        )
        
        val path = Path()
        cartesianPoints.forEachIndexed { index, p ->
            val x = padding + (p.first - minX) * scale
            val y = size.height - (padding + (p.second - minY) * scale)
            if (index == 0) path.moveTo(x.toFloat(), y.toFloat())
            else path.lineTo(x.toFloat(), y.toFloat())
        }
        path.close()
        
        drawPath(path, Color.Green.copy(alpha = 0.3f))
        drawPath(path, Color.Green, style = Stroke(width = 5f))
    }
}
