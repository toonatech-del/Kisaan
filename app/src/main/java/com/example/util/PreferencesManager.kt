package com.example.util

import android.content.Context
import android.content.SharedPreferences

enum class AreaDisplayUnit(val key: String, val hindiLabel: String, val englishLabel: String) {
    BIHAR("BIHAR", "बीघा - कट्ठा - धूर", "Bigha - Katha - Dhur"),
    DISMIL("DISMIL", "डिसमिल", "Dismil / Decimal"),
    SQ_METER("SQ_METER", "वर्ग मीटर", "Square Meters"),
    SQ_FEET("SQ_FEET", "वर्ग फीट", "Square Feet"),
    ACRE("ACRE", "एकड़", "Acre"),
    HECTARE("HECTARE", "हेक्टेयर", "Hectare")
}

object PreferencesManager {
    private const val PREFS_NAME = "kisan_zameen_settings_prefs"
    private const val KEY_DEFAULT_LAGGI = "default_laggi_haath"
    private const val KEY_DEFAULT_UNIT = "default_display_unit"

    const val DEFAULT_LAGGI = 6.0 // Standard Bihar laggi in haath (9 feet per laggi)
    val STANDARD_LAGGI_OPTIONS = listOf(4.0, 4.5, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0)

    fun getDefaultLaggi(context: Context): Double {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getString(KEY_DEFAULT_LAGGI, DEFAULT_LAGGI.toString()) ?: DEFAULT_LAGGI.toString()
        return value.toDoubleOrNull() ?: DEFAULT_LAGGI
    }

    fun saveDefaultLaggi(context: Context, laggi: Double) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DEFAULT_LAGGI, laggi.toString()).apply()
    }

    fun getDefaultDisplayUnit(context: Context): AreaDisplayUnit {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = prefs.getString(KEY_DEFAULT_UNIT, AreaDisplayUnit.BIHAR.key) ?: AreaDisplayUnit.BIHAR.key
        return AreaDisplayUnit.values().find { it.key == key } ?: AreaDisplayUnit.BIHAR
    }

    fun saveDefaultDisplayUnit(context: Context, unit: AreaDisplayUnit) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DEFAULT_UNIT, unit.key).apply()
    }

    /**
     * Formats area in square meters according to the chosen display unit and laggi.
     */
    fun formatAreaWithUnit(
        areaSqM: Double,
        unit: AreaDisplayUnit,
        laggiHaath: Double,
        language: AppLanguage
    ): String {
        return when (unit) {
            AreaDisplayUnit.BIHAR -> {
                val breakdown = GeoLandCalculator.convertToBiharUnits(areaSqM, laggiHaath)
                val dhurVal = breakdown.dhur + breakdown.dhurki / 20.0
                if (language == AppLanguage.HINDI) {
                    "${breakdown.bigha} बीघा ${breakdown.katha} कट्ठा ${String.format(java.util.Locale.US, "%.1f", dhurVal)} धूर"
                } else {
                    "${breakdown.bigha} Bigha ${breakdown.katha} Katha ${String.format(java.util.Locale.US, "%.1f", dhurVal)} Dhur"
                }
            }
            AreaDisplayUnit.DISMIL -> {
                val dismil = areaSqM / GeoLandCalculator.DISMIL_TO_SQ_METERS
                val label = if (language == AppLanguage.HINDI) "डिसमिल" else "Dismil"
                "${String.format(java.util.Locale.US, "%.2f", dismil)} $label"
            }
            AreaDisplayUnit.SQ_METER -> {
                val label = if (language == AppLanguage.HINDI) "वर्ग मीटर" else "sq.m"
                "${String.format(java.util.Locale.US, "%.2f", areaSqM)} $label"
            }
            AreaDisplayUnit.SQ_FEET -> {
                val sqFt = areaSqM * GeoLandCalculator.SQ_METERS_TO_SQ_FEET
                val label = if (language == AppLanguage.HINDI) "वर्ग फीट" else "sq.ft"
                "${String.format(java.util.Locale.US, "%.1f", sqFt)} $label"
            }
            AreaDisplayUnit.ACRE -> {
                val acre = areaSqM / GeoLandCalculator.ACRE_TO_SQ_METERS
                val label = if (language == AppLanguage.HINDI) "एकड़" else "Acre"
                "${String.format(java.util.Locale.US, "%.4f", acre)} $label"
            }
            AreaDisplayUnit.HECTARE -> {
                val hectare = areaSqM / GeoLandCalculator.HECTARE_TO_SQ_METERS
                val label = if (language == AppLanguage.HINDI) "हेक्टेयर" else "Hectare"
                "${String.format(java.util.Locale.US, "%.4f", hectare)} $label"
            }
        }
    }
}
