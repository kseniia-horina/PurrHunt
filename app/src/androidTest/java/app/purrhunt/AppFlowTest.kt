package app.purrhunt

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import app.purrhunt.data.SettingsRepository
import app.purrhunt.data.db.PurrDatabase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * End-to-end UI flows. The game and the live location previews request frames forever, so the Compose
 * clock is driven manually: every wait advances it frame by frame.
 */
@HiltAndroidTest
class AppFlowTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var store: DataStore<Preferences>
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var db: PurrDatabase

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        hilt.inject()
        runBlocking { store.edit { it.clear() } }
        db.clearAllTables()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
        compose.mainClock.autoAdvance = false
    }

    @After
    fun tearDown() {
        // A test that ends inside the game leaves the screen pinned: unpin before the next test.
        scenario?.onActivity { app.purrhunt.ui.game.ScreenPinning.stop(it) }
        scenario?.close()
    }

    private fun launch(onboarded: Boolean) {
        if (onboarded) runBlocking {
            settings.setOnboardingDone()
            settings.setLockTaskHintShown()
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun waitFor(tag: String) {
        try {
            compose.waitUntil(15_000) {
                compose.mainClock.advanceTimeByFrame()
                compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Throwable) {
            compose.onRoot().printToLog("PurrHuntTest")
            throw e
        }
    }

    private fun settle() = compose.mainClock.advanceTimeBy(1_000)

    private fun click(tag: String) {
        waitFor(tag)
        // Let a running navigation transition finish: clicks during it are ignored.
        settle()
        compose.onNodeWithTag(tag).performClick()
        settle()
    }

    @Test
    fun onboardingLeadsToMenuAndIsShownOnce() {
        launch(onboarded = false)
        click("onboarding_next")
        click("onboarding_next")
        click("onboarding_next")
        waitFor("menu_play")
        scenario?.recreate()
        waitFor("menu_play")
    }

    @Test
    fun menuNavigatesToEveryOwnerScreen() {
        launch(onboarded = true)
        for (tag in listOf("menu_journal", "menu_achievements", "menu_settings", "menu_modes", "menu_album")) {
            click(tag)
            compose.waitUntil(5_000) {
                compose.mainClock.advanceTimeByFrame()
                compose.onAllNodesWithTag("menu_play").fetchSemanticsNodes().isEmpty()
            }
            scenario?.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor("menu_play")
        }
    }

    @Test
    fun journalTabsOpen() {
        launch(onboarded = true)
        click("menu_journal")
        for (i in 0..3) click("journal_tab_$i")
    }

    @Test
    fun settingsAreRemembered() {
        launch(onboarded = true)
        click("menu_settings")
        waitFor("settings_contrast")
        compose.onNodeWithTag("settings_contrast").assertIsOff().performClick()
        settle()
        compose.waitUntil(5_000) {
            compose.mainClock.advanceTimeByFrame()
            runCatching { compose.onNodeWithTag("settings_contrast").assertIsOn() }.isSuccess
        }
        scenario?.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        click("menu_settings")
        waitFor("settings_contrast")
        compose.onNodeWithTag("settings_contrast").performScrollTo().assertIsOn()
    }

    @Test
    fun ownerCornerHoldPausesTheGameAndBackDoesNothing() {
        launch(onboarded = true)
        click("menu_play")
        click("location_BASEMENT_false")
        waitFor("game_surface")
        compose.mainClock.advanceTimeBy(2_000)

        // Back during play is swallowed: still in the game, no pause screen.
        scenario?.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("game_surface").assertExists()
        compose.onNodeWithTag("pause_overlay").assertDoesNotExist()

        // A finger held in the top right corner for three seconds opens the owner's pause.
        compose.onNodeWithTag("game_surface").performTouchInput { down(Offset(width - 10f, 10f)) }
        repeat(220) { compose.mainClock.advanceTimeByFrame() }
        compose.onNodeWithTag("game_surface").performTouchInput { up() }
        waitFor("pause_overlay")

        click("pause_resume")
        compose.onNodeWithTag("pause_overlay").assertDoesNotExist()
        compose.onNodeWithTag("game_surface").performTouchInput { down(Offset(width - 10f, 10f)) }
        repeat(220) { compose.mainClock.advanceTimeByFrame() }
        compose.onNodeWithTag("game_surface").performTouchInput { up() }
        click("pause_menu")
        waitFor("menu_play")
    }

    @Test
    fun aShortTouchInTheCornerDoesNotPause() {
        launch(onboarded = true)
        click("menu_play")
        click("location_GARDEN_false")
        waitFor("game_surface")
        compose.onNodeWithTag("game_surface").performTouchInput { down(Offset(width - 10f, 10f)) }
        repeat(90) { compose.mainClock.advanceTimeByFrame() }
        compose.onNodeWithTag("game_surface").performTouchInput { up() }
        repeat(60) { compose.mainClock.advanceTimeByFrame() }
        compose.onNodeWithTag("pause_overlay").assertDoesNotExist()
    }
}
