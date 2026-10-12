package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.model.LandCaseEntity
import com.example.data.model.LandMeasurementEntity
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.KisanZameenTheme
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.AreaDisplayUnit
import com.example.util.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsAndPreferencesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test default laggi and unit persistence in PreferencesManager`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Default should be 6.0
        PreferencesManager.saveDefaultLaggi(context, 6.0)
        assertEquals(6.0, PreferencesManager.getDefaultLaggi(context), 0.001)

        // Change laggi to 6.5
        PreferencesManager.saveDefaultLaggi(context, 6.5)
        assertEquals(6.5, PreferencesManager.getDefaultLaggi(context), 0.001)

        // Default unit should be BIHAR
        PreferencesManager.saveDefaultDisplayUnit(context, AreaDisplayUnit.BIHAR)
        assertEquals(AreaDisplayUnit.BIHAR, PreferencesManager.getDefaultDisplayUnit(context))

        // Change unit to ACRE
        PreferencesManager.saveDefaultDisplayUnit(context, AreaDisplayUnit.ACRE)
        assertEquals(AreaDisplayUnit.ACRE, PreferencesManager.getDefaultDisplayUnit(context))
    }

    @Test
    fun `test area formatting across all display units in Hindi and English`() {
        val areaSqM = 2529.285 // approx 1 Bigha at 6-haath laggi (or ~0.625 Acre)

        val biharHi = PreferencesManager.formatAreaWithUnit(areaSqM, AreaDisplayUnit.BIHAR, 6.0, AppLanguage.HINDI)
        val biharEn = PreferencesManager.formatAreaWithUnit(areaSqM, AreaDisplayUnit.BIHAR, 6.0, AppLanguage.ENGLISH)
        assertTrue(biharHi.contains("बीघा") && biharHi.contains("कट्ठा"))
        assertTrue(biharEn.contains("Bigha") && biharEn.contains("Katha"))

        val dismilHi = PreferencesManager.formatAreaWithUnit(areaSqM, AreaDisplayUnit.DISMIL, 6.0, AppLanguage.HINDI)
        val dismilEn = PreferencesManager.formatAreaWithUnit(areaSqM, AreaDisplayUnit.DISMIL, 6.0, AppLanguage.ENGLISH)
        assertTrue(dismilHi.contains("डिसमिल"))
        assertTrue(dismilEn.contains("Dismil"))

        val acreHi = PreferencesManager.formatAreaWithUnit(areaSqM, AreaDisplayUnit.ACRE, 6.0, AppLanguage.HINDI)
        val acreEn = PreferencesManager.formatAreaWithUnit(areaSqM, AreaDisplayUnit.ACRE, 6.0, AppLanguage.ENGLISH)
        assertTrue(acreHi.contains("एकड़"))
        assertTrue(acreEn.contains("Acre"))

        val sqFtHi = PreferencesManager.formatAreaWithUnit(areaSqM, AreaDisplayUnit.SQ_FEET, 6.0, AppLanguage.HINDI)
        val sqFtEn = PreferencesManager.formatAreaWithUnit(areaSqM, AreaDisplayUnit.SQ_FEET, 6.0, AppLanguage.ENGLISH)
        assertTrue(sqFtHi.contains("वर्ग फीट"))
        assertTrue(sqFtEn.contains("sq.ft"))
    }

    @Test
    fun `test settings and legal safety strings completeness in Hindi and English`() {
        val hindi = AppStrings.get(AppLanguage.HINDI)
        val english = AppStrings.get(AppLanguage.ENGLISH)

        // Settings headings
        assertEquals("सेटिंग्स एवं सुरक्षा", hindi.settingsTitle)
        assertEquals("Settings & Safety", english.settingsTitle)

        // Storage & Clear data
        assertEquals("स्थानीय डेटा भंडारण स्थिति (Local Storage)", hindi.localStorageStatusTitle)
        assertEquals("Local Storage Status", english.localStorageStatusTitle)
        assertTrue(hindi.clearDataBtn.contains("स्थानीय डेटा मिटाएं"))
        assertTrue(english.clearDataBtn.contains("Clear All Local Data"))

        // Legal & Safety
        assertEquals("कानूनी अस्वीकरण (Legal Advisory)", hindi.legalAdvisoryTitle)
        assertEquals("Legal Advisory & Disclaimer", english.legalAdvisoryTitle)
        assertTrue(hindi.legalAdvisoryDesc.contains("आधिकारिक राजस्व सर्वेक्षण"))
        assertTrue(english.legalAdvisoryDesc.contains("NOT official revenue survey"))

        // Safety items
        assertTrue(hindi.safetyWeather.contains("धूप"))
        assertTrue(english.safetyWeather.contains("Sun & Weather"))
        assertTrue(hindi.safetyTerrain.contains("साँप-बिच्छू"))
        assertTrue(english.safetyTerrain.contains("snakes"))

        // Attribution
        assertTrue(hindi.mapAttributionDesc.contains("OpenStreetMap"))
        assertTrue(english.mapAttributionDesc.contains("OpenStreetMap"))
    }

    @Test
    fun `test clearAll data in Room database`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val measurementDao = inMemoryDb.landMeasurementDao()
        val caseDao = inMemoryDb.landCaseDao()

        // Insert dummy data
        val caseId = caseDao.insertCase(
            LandCaseEntity(
                farmerName = "राम कुमार",
                villageMouza = "महुआ",
                thanaNumber = "12",
                khataNumber = "45",
                khesraNumber = "102"
            )
        )
        measurementDao.insertMeasurement(
            LandMeasurementEntity(
                landCaseId = caseId,
                title = "उत्तर की मेड़ नाप",
                areaSqM = 1500.0,
                perimeterM = 160.0,
                pointCount = 4,
                gpsQualitySummary = "EXCELLENT",
                pointsJson = "[]"
            )
        )

        assertEquals(1, caseDao.getCaseCount().first())
        assertEquals(1, measurementDao.getMeasurementCount().first())

        // Clear all
        measurementDao.clearAllMeasurements()
        caseDao.clearAllCases()

        assertEquals(0, caseDao.getCaseCount().first())
        assertEquals(0, measurementDao.getMeasurementCount().first())

        inMemoryDb.close()
    }

    @Test
    fun `test SettingsScreen UI components rendering and interaction`() {
        var selectedLang: AppLanguage = AppLanguage.HINDI
        var navigatedBack = false

        composeTestRule.setContent {
            KisanZameenTheme {
                SettingsScreen(
                    currentLanguage = selectedLang,
                    onLanguageChanged = { selectedLang = it },
                    onNavigateBack = { navigatedBack = true }
                )
            }
        }

        // Verify top components and interaction
        composeTestRule.onNodeWithTag("settings_back_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_lang_hindi").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_lang_english").assertIsDisplayed()

        // Click English while visible at the top
        composeTestRule.onNodeWithTag("settings_lang_english").performClick()
        assertEquals(AppLanguage.ENGLISH, selectedLang)

        // Click Back while visible at the top
        composeTestRule.onNodeWithTag("settings_back_button").performClick()
        assertTrue(navigatedBack)

        // Scroll to and verify items down the list
        composeTestRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("settings_laggi_chip_6_0"))
        composeTestRule.onNodeWithTag("settings_laggi_chip_6_0").assertExists()

        composeTestRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("settings_unit_bihar"))
        composeTestRule.onNodeWithTag("settings_unit_bihar").assertExists()

        composeTestRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("clear_data_button"))
        composeTestRule.onNodeWithTag("clear_data_button").assertExists()
    }
}
