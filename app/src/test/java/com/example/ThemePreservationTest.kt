package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.KisanZameenTheme
import com.example.ui.theme.NeutralBackground
import com.example.ui.theme.NeutralOnBackground
import com.example.ui.theme.GreenPrimary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThemePreservationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testThemeAlwaysUsesLightColorSchemeRegardlessOfParameter() {
        var observedBackground: Color? = null
        var observedOnBackground: Color? = null
        var observedPrimary: Color? = null

        // Even if darkTheme = true is passed or system requests dark theme
        composeTestRule.setContent {
            KisanZameenTheme(darkTheme = true) {
                observedBackground = MaterialTheme.colorScheme.background
                observedOnBackground = MaterialTheme.colorScheme.onBackground
                observedPrimary = MaterialTheme.colorScheme.primary
                Text("Kisan Zameen Saathi")
            }
        }

        // Verify that it maintains the Light Theme colors (NeutralBackground & NeutralOnBackground)
        assertEquals(NeutralBackground, observedBackground)
        assertEquals(NeutralOnBackground, observedOnBackground)
        assertEquals(GreenPrimary, observedPrimary)
    }
}
