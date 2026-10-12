package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.LanguageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LanguagePreferenceAndStringsTest {

    @Test
    fun `test language persistence in SharedPreferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Initial / default should be Hindi
        LanguageManager.saveLanguage(context, AppLanguage.HINDI)
        assertEquals(AppLanguage.HINDI, LanguageManager.getSavedLanguage(context))

        // Switch to English
        LanguageManager.saveLanguage(context, AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, LanguageManager.getSavedLanguage(context))

        // Switch back to Hindi
        LanguageManager.saveLanguage(context, AppLanguage.HINDI)
        assertEquals(AppLanguage.HINDI, LanguageManager.getSavedLanguage(context))
    }

    @Test
    fun `test complete Hindi and English strings availability`() {
        val hindi = AppStrings.get(AppLanguage.HINDI)
        val english = AppStrings.get(AppLanguage.ENGLISH)

        // Titles and headers
        assertEquals("किसान ज़मीन साथी", hindi.appTitle)
        assertEquals("Kisan Zameen Saathi", english.appTitle)

        // Live measurement controls
        assertEquals("नाप शुरू करें →", hindi.startMeasurementBtn)
        assertEquals("Start Measurement →", english.startMeasurementBtn)

        assertEquals("बिंदु जोड़ें", hindi.addPoint)
        assertEquals("Add Point", english.addPoint)

        assertEquals("अंतिम हटाएं", hindi.undoLast)
        assertEquals("Undo Last", english.undoLast)

        // Partition calculator
        assertEquals("पारिवारिक ज़मीन बंटवारा", hindi.familyPartitionTitle)
        assertEquals("Family Land Partition", english.familyPartitionTitle)

        // Converter
        assertEquals("भूमि इकाई परिवर्तक (रकबा)", hindi.unitConverterTitle)
        assertEquals("Land Unit Converter (Rakba)", english.unitConverterTitle)

        // Help and Info
        assertEquals("मदद और जानकारी", hindi.helpAndInfo)
        assertEquals("Help & Information", english.helpAndInfo)
    }
}
