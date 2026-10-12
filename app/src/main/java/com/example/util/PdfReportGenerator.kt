package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.LandCaseEntity
import com.example.data.model.LandMeasurementEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object PdfReportGenerator {

    /**
     * Generates a high quality PDF report for a land measurement and optionally an attached land case in selected language.
     */
    fun generateMeasurementPdf(
        context: Context,
        measurement: LandMeasurementEntity,
        points: List<GeoPoint>,
        landCase: LandCaseEntity? = null,
        language: AppLanguage = AppLanguage.HINDI
    ): File {
        val isHindi = language == AppLanguage.HINDI
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        val primaryGreen = Color.rgb(46, 125, 50)
        val darkGreen = Color.rgb(27, 94, 32)
        val lightGreenBg = Color.rgb(232, 245, 233)
        val textColor = Color.rgb(33, 33, 33)
        val grayText = Color.rgb(100, 116, 139)
        val borderGray = Color.rgb(203, 213, 225)

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = textColor
            textSize = 11f
            typeface = Typeface.DEFAULT
        }

        val boldPaint = Paint().apply {
            isAntiAlias = true
            color = textColor
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }

        val headerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }

        // 1. Header Banner
        val bgPaint = Paint().apply {
            color = primaryGreen
            style = Paint.Style.FILL
        }
        canvas.drawRect(20f, 20f, 575f, 90f, bgPaint)

        val titleText = if (isHindi) "किसान ज़मीन साथी — भूमि मापन रिपोर्ट" else "Kisan Zameen Saathi — Land Measurement Report"
        val subText = if (isHindi) "खेत नाप एवं रकबा विवरण • बिहार भूमि प्रबंधन" else "Field Measurement & Area Details • Bihar Land Management"
        canvas.drawText(titleText, 35f, 52f, headerPaint)

        val subHeaderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(220, 245, 220)
            textSize = 11f
        }
        canvas.drawText(subText, 35f, 74f, subHeaderPaint)

        val dateStr = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault()).format(Date(measurement.timestamp))
        val rightAlignPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("${if (isHindi) "दिनांक" else "Date"}: $dateStr", 560f, 74f, rightAlignPaint)

        var curY = 110f

        // 2. Land Case Details Box (if linked)
        if (landCase != null) {
            val caseBoxPaint = Paint().apply {
                color = lightGreenBg
                style = Paint.Style.FILL
            }
            canvas.drawRect(20f, curY, 575f, curY + 68f, caseBoxPaint)
            val strokePaint = Paint().apply {
                color = borderGray
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRect(20f, curY, 575f, curY + 68f, strokePaint)

            boldPaint.color = darkGreen
            boldPaint.textSize = 12f
            canvas.drawText(if (isHindi) "संबंधित ज़मीन रिकॉर्ड विवरण:" else "Linked Land Case Details:", 35f, curY + 20f, boldPaint)

            textPaint.textSize = 10.5f
            canvas.drawText("${if (isHindi) "रैयत/किसान" else "Owner"}: ${landCase.farmerName}", 35f, curY + 40f, textPaint)
            canvas.drawText("${if (isHindi) "मौजा" else "Mouza"}: ${landCase.villageMouza} ${if (landCase.thanaNumber.isNotBlank()) "(Thana ${landCase.thanaNumber})" else ""}", 220f, curY + 40f, textPaint)
            canvas.drawText("${if (isHindi) "खाता" else "Khata"}: ${landCase.khataNumber}", 35f, curY + 58f, textPaint)
            canvas.drawText("${if (isHindi) "खेसरा" else "Khesra"}: ${landCase.khesraNumber}", 220f, curY + 58f, textPaint)
            if (landCase.jamabandiNumber.isNotBlank()) {
                canvas.drawText("Jamabandi: ${landCase.jamabandiNumber}", 410f, curY + 58f, textPaint)
            }

            curY += 80f
        }

        // 3. Measurement Metrics Box
        val biharBreakdown = GeoLandCalculator.convertToBiharUnits(measurement.areaSqM, measurement.laggiHaath)
        boldPaint.color = darkGreen
        boldPaint.textSize = 13f
        canvas.drawText(if (isHindi) "मापन परिणाम (Measurement Summary):" else "Measurement Results:", 20f, curY + 12f, boldPaint)
        curY += 24f

        val metricsBoxPaint = Paint().apply {
            color = Color.rgb(250, 250, 250)
            style = Paint.Style.FILL
        }
        canvas.drawRect(20f, curY, 575f, curY + 115f, metricsBoxPaint)
        val strokePaint = Paint().apply {
            color = borderGray
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(20f, curY, 575f, curY + 115f, strokePaint)

        textPaint.textSize = 10.5f
        boldPaint.textSize = 11.5f
        boldPaint.color = textColor

        canvas.drawText(if (isHindi) "अनुमानित रकबा (बिहार इकाई):" else "Estimated Area (Bihar Units):", 35f, curY + 24f, boldPaint)
        val areaHighlightPaint = Paint().apply {
            isAntiAlias = true
            color = darkGreen
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(biharBreakdown.toHindiString(), 35f, curY + 44f, areaHighlightPaint)
        canvas.drawText("(Laggi: ${measurement.laggiHaath} haath)", 35f, curY + 60f, textPaint)

        canvas.drawText(String.format(Locale.US, "Standard: %.3f Acres (%.2f Dismil)", biharBreakdown.acres, biharBreakdown.dismil), 35f, curY + 80f, textPaint)
        canvas.drawText(String.format(Locale.US, "Metric: %.2f Sq. Meters (%.1f Sq. Feet)", measurement.areaSqM, biharBreakdown.sqFeet), 35f, curY + 98f, textPaint)

        val col2X = 320f
        canvas.drawText(if (isHindi) "अनुमानित परिमाप:" else "Estimated Perimeter:", col2X, curY + 24f, boldPaint)
        canvas.drawText(String.format(Locale.US, "%.1f m (%.1f ft)", measurement.perimeterM, measurement.perimeterM * 3.28084), col2X, curY + 44f, textPaint)

        canvas.drawText("${if (isHindi) "कुल बिंदु" else "Total Points"}: ${measurement.pointCount}", col2X, curY + 64f, textPaint)
        canvas.drawText(if (isHindi) "विधि: मोबाइल GPS अनुमान" else "Method: Mobile GPS Estimate", col2X, curY + 80f, textPaint)
        val editStatus = if (measurement.wasManuallyEdited) (if (isHindi) "संशोधित (Edited)" else "Manually Edited") else (if (isHindi) "GPS वॉक" else "GPS Walk")
        canvas.drawText("${if (isHindi) "स्थिति" else "Status"}: $editStatus", col2X, curY + 98f, textPaint)

        curY += 130f

        // 4. Schematic Plot
        boldPaint.color = darkGreen
        boldPaint.textSize = 13f
        canvas.drawText(if (isHindi) "खेत की बाउंड्री का रेखाचित्र (Field Plot Diagram):" else "Field Boundary Plot Diagram:", 20f, curY + 12f, boldPaint)
        curY += 24f

        val plotBoxTop = curY
        val plotBoxHeight = 160f
        val plotBoxBottom = plotBoxTop + plotBoxHeight

        val plotBgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawRect(20f, plotBoxTop, 575f, plotBoxBottom, plotBgPaint)
        canvas.drawRect(20f, plotBoxTop, 575f, plotBoxBottom, strokePaint)

        if (points.size >= 2) {
            val projected = GeoLandCalculator.projectToLocalPlane(points)
            var minX = Double.MAX_VALUE
            var maxX = -Double.MAX_VALUE
            var minY = Double.MAX_VALUE
            var maxY = -Double.MAX_VALUE
            projected.forEach { pt ->
                if (pt.x < minX) minX = pt.x
                if (pt.x > maxX) maxX = pt.x
                if (pt.y < minY) minY = pt.y
                if (pt.y > maxY) maxY = pt.y
            }
            val spanX = max(maxX - minX, 10.0)
            val spanY = max(maxY - minY, 10.0)

            val plotMargin = 28f
            val availW = (575f - 20f) - plotMargin * 2
            val availH = plotBoxHeight - plotMargin * 2
            val scale = min(availW / spanX, availH / spanY).toFloat()

            val offsetX = 20f + plotMargin + ((availW - spanX.toFloat() * scale) / 2f) - (minX.toFloat() * scale)
            val offsetY = plotBoxBottom - plotMargin - ((availH - spanY.toFloat() * scale) / 2f) + (minY.toFloat() * scale)

            fun toPdfPt(p: Point2D) = android.graphics.PointF(
                p.x.toFloat() * scale + offsetX,
                offsetY - p.y.toFloat() * scale
            )

            val polyPath = android.graphics.Path()
            val first = toPdfPt(projected.first())
            polyPath.moveTo(first.x, first.y)
            for (i in 1 until projected.size) {
                val pt = toPdfPt(projected[i])
                polyPath.lineTo(pt.x, pt.y)
            }
            polyPath.close()

            val fillPaint = Paint().apply {
                color = Color.argb(45, 46, 125, 50)
                style = Paint.Style.FILL
            }
            canvas.drawPath(polyPath, fillPaint)

            val linePaint = Paint().apply {
                color = primaryGreen
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
            }
            canvas.drawPath(polyPath, linePaint)

            val vertexPaint = Paint().apply {
                color = darkGreen
                style = Paint.Style.FILL
            }
            val numPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 8.5f
                typeface = Typeface.DEFAULT_BOLD
            }
            projected.forEachIndexed { i, pt ->
                val fpt = toPdfPt(pt)
                canvas.drawCircle(fpt.x, fpt.y, 4f, vertexPaint)
                canvas.drawText("${i + 1}", fpt.x + 6f, fpt.y - 2f, numPaint)
            }
        }

        canvas.drawText("N ↑ North", 520f, plotBoxTop + 20f, boldPaint)
        curY = plotBoxBottom + 16f

        // 5. GPS Summary
        textPaint.color = grayText
        textPaint.textSize = 9.5f
        canvas.drawText("GPS Summary: ${measurement.gpsQualitySummary}", 20f, curY, textPaint)
        if (measurement.notes.isNotBlank()) {
            curY += 14f
            canvas.drawText("Notes: ${measurement.notes}", 20f, curY, textPaint)
        }
        curY += 20f

        // 6. Disclaimer Box at bottom
        val disclaimerTop = 760f
        val disclaimerBgPaint = Paint().apply {
            color = Color.rgb(254, 242, 242)
            style = Paint.Style.FILL
        }
        val redBorderPaint = Paint().apply {
            color = Color.rgb(252, 165, 165)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(20f, disclaimerTop, 575f, 825f, disclaimerBgPaint)
        canvas.drawRect(20f, disclaimerTop, 575f, 825f, redBorderPaint)

        val discTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(185, 28, 28)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(if (isHindi) "वैधानिक अस्वीकरण (Legal Disclaimer):" else "Legal Disclaimer:", 30f, disclaimerTop + 16f, discTitlePaint)

        val discTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(153, 27, 27)
            textSize = 9f
        }
        val line1 = if (isHindi) "यह मोबाइल GPS से प्राप्त अनुमान है। यह सरकारी सीमांकन, अधिकृत सर्वे या कानूनी बंटवारे का प्रमाण नहीं है।"
            else "This is a mobile GPS estimate. It is not official demarcation, survey, title deed, or legal partition proof."
        val line2 = if (isHindi) "पेड़, इमारतें, कमजोर सिग्नल और उपकरण से माप में अंतर संभव है।"
            else "Trees, buildings, weak signal, and device capability may cause measurement differences."
        val line3 = if (isHindi) "आधिकारिक प्रयोजन हेतु राजस्व अमीन या भू-सर्वेक्षण विभाग से संपर्क करें।"
            else "Contact revenue Amin or land survey department for official purposes."

        canvas.drawText(line1, 30f, disclaimerTop + 32f, discTextPaint)
        canvas.drawText(line2, 30f, disclaimerTop + 45f, discTextPaint)
        canvas.drawText(line3, 30f, disclaimerTop + 58f, discTextPaint)

        pdfDoc.finishPage(page)

        val pdfDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "reports")
        if (!pdfDir.exists()) pdfDir.mkdirs()
        val file = File(pdfDir, "Kisan_Zameen_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        return file
    }
}
