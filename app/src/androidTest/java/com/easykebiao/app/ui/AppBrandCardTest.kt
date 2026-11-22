package com.easykebiao.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.easykebiao.app.ui.component.AppBrandCard
import com.easykebiao.app.ui.theme.SleepyThemeProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AppBrandCardTest {
    @get:Rule val compose = createComposeRule()
    @Test fun aboutLetterStaysHiddenUntilSecretEnvelopeIsOpenedAndSurvivesRestore() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val title = context.getString(com.easykebiao.app.R.string.about_letter_title)
        val body = context.resources.getStringArray(com.easykebiao.app.R.array.about_heart_paragraphs)[0]
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            SleepyThemeProvider { com.easykebiao.app.ui.screen.mine.AboutScreen(onBack = {}) }
        }
        compose.onNodeWithText(title).assertDoesNotExist()
        compose.onNodeWithTag("classy-brand-card").performTouchInput {
            repeat(5) { click(center); advanceEventTime(120) }
        }
        compose.onNodeWithTag("brand-secret-letter").assertIsDisplayed()
        compose.onNodeWithText(title).assertDoesNotExist()
        compose.onNodeWithTag("brand-secret-letter").performClick()
        compose.onNodeWithText(body).assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(body).assertIsDisplayed()
        compose.onNodeWithText(title).performClick()
        compose.onNodeWithText(body).assertDoesNotExist()
    }

    @Test fun fiveQuickTapsRevealLetterWithoutOpeningItAutomatically() {
        var opened = 0
        compose.setContent { SleepyThemeProvider { AppBrandCard(onOpenLetter = { opened++ }) } }
        val card = compose.onNodeWithTag("classy-brand-card")
        card.performTouchInput {
            repeat(5) { click(center); advanceEventTime(120) }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("brand-secret-letter").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, opened) }
        capture("brand-secret-discovered")
        compose.onNodeWithTag("brand-secret-letter").performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test fun confettiBurstIsVisibleFiniteAndRepeatable() {
        compose.setContent { SleepyThemeProvider { Box(Modifier.padding(20.dp)) { AppBrandCard() } } }
        compose.mainClock.autoAdvance = false
        val card = compose.onNodeWithTag("classy-brand-card")
        card.performTouchInput { repeat(5) { click(center); advanceEventTime(100) } }
        compose.mainClock.advanceTimeBy(350)
        val burst = capture("brand-confetti-burst")
        compose.mainClock.advanceTimeBy(2000)
        compose.onNodeWithTag("brand-secret-letter").assertExists()
        val settled = capture("brand-confetti-settled")
        assertFalse(burst.sameAs(settled))
        card.performTouchInput { repeat(5) { click(topCenter + androidx.compose.ui.geometry.Offset(0f, 40f)); advanceEventTime(100) } }
        compose.mainClock.advanceTimeBy(350)
        assertFalse(settled.sameAs(capture("brand-confetti-repeat")))
    }

    @Test fun continuedTapsBuildCelebrationThenSettle() {
        compose.setContent { SleepyThemeProvider { Box(Modifier.padding(20.dp)) { AppBrandCard() } } }
        compose.mainClock.autoAdvance = false
        val card = compose.onNodeWithTag("classy-brand-card")
        card.performTouchInput { repeat(5) { click(topCenter + androidx.compose.ui.geometry.Offset(0f, 40f)); advanceEventTime(100) } }
        compose.mainClock.advanceTimeBy(250)
        val first = capture("brand-continuous-first")
        repeat(12) {
            card.performTouchInput { click(topCenter + androidx.compose.ui.geometry.Offset(0f, 40f)); advanceEventTime(90) }
            compose.mainClock.advanceTimeBy(90)
        }
        val peak = capture("brand-continuous-peak")
        assertFalse(first.sameAs(peak))
        compose.mainClock.advanceTimeBy(2400)
        val settled = capture("brand-continuous-stop")
        compose.mainClock.advanceTimeBy(2400)
        assertTrue(settled.sameAs(capture("brand-continuous-idle")))
        compose.onNodeWithTag("brand-secret-letter").assertExists()
    }

    @Test fun spacedTapsAndDragsDoNotUnlockLetter() {
        compose.setContent { SleepyThemeProvider { AppBrandCard() } }
        val card = compose.onNodeWithTag("classy-brand-card")
        card.performTouchInput {
            repeat(5) { click(center); advanceEventTime(1000) }
            repeat(5) {
                down(center); moveBy(androidx.compose.ui.geometry.Offset(50f, 0f)); up()
                advanceEventTime(100)
            }
        }
        compose.onNodeWithTag("brand-secret-letter").assertDoesNotExist()
    }

    private fun capture(name: String): Bitmap {
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return image
    }
    @Test fun pressReleaseAndCancelReturnToRest() {
        compose.setContent { SleepyThemeProvider { Box(Modifier.padding(20.dp).widthIn(max = 480.dp)) { AppBrandCard() } } }
        compose.mainClock.autoAdvance = false
        val card = compose.onNodeWithTag("classy-brand-card")
        val before = capture("brand-card-rest")
        card.performTouchInput { down(androidx.compose.ui.geometry.Offset(width * .8f, height * .3f)) }
        compose.mainClock.advanceTimeBy(600)
        val pressed = capture("brand-card-pressed")
        assertFalse(before.sameAs(pressed))
        card.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(1600)
        assertTrue(before.sameAs(capture("brand-card-released")))
        card.performTouchInput { down(centerLeft + androidx.compose.ui.geometry.Offset(25f, 0f)) }
        compose.mainClock.advanceTimeBy(300)
        card.performTouchInput { cancel() }
        compose.mainClock.advanceTimeBy(1600)
        // Neutral tilt and resting glow must recover after pointer cancellation.
        assertTrue(before.sameAs(capture("brand-card-cancelled")))
    }
    @Test fun developerLetterStartsClosedAndCanBeOpenedAndClosed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val body = context.resources.getStringArray(com.easykebiao.app.R.array.about_heart_paragraphs)[0]
        compose.setContent {
            SleepyThemeProvider {
                Column(Modifier.verticalScroll(rememberScrollState())) { com.easykebiao.app.ui.component.DeveloperLetter() }
            }
        }
        compose.onNodeWithText(body).assertDoesNotExist()
        val title = context.getString(com.easykebiao.app.R.string.about_letter_title)
        compose.onNodeWithText(title).performClick()
        compose.onNodeWithText(body).assertIsDisplayed()
        capture("developer-letter-open")
        compose.onNodeWithText(title).performClick()
        compose.onNodeWithText(body).assertDoesNotExist()
    }

    @Test fun cardDoesNotStealPageScroll() {
        lateinit var scroll: androidx.compose.foundation.ScrollState
        compose.setContent {
            SleepyThemeProvider {
                scroll = rememberScrollState()
                Column(Modifier.verticalScroll(scroll)) {
                    AppBrandCard()
                    Spacer(Modifier.height(1500.dp))
                    Text("Bottom")
                }
            }
        }
        compose.onNodeWithTag("classy-brand-card").performTouchInput { swipeUp() }
        compose.runOnIdle { assertTrue(scroll.value > 0) }
    }
}
