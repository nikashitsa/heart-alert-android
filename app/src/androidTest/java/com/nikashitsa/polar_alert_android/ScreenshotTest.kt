package com.nikashitsa.polar_alert_android

import android.graphics.Color
import androidx.annotation.StringRes
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.nikashitsa.polar_alert_android.lib.AppLanguage
import com.nikashitsa.polar_alert_android.lib.BatteryStatusFeature
import com.nikashitsa.polar_alert_android.lib.DeviceConnectionState
import com.nikashitsa.polar_alert_android.lib.HrFeature
import com.nikashitsa.polar_alert_android.lib.withLanguage
import com.nikashitsa.polar_alert_android.ui.AppFrame
import com.nikashitsa.polar_alert_android.ui.screens.ConnectScreenContent
import com.nikashitsa.polar_alert_android.ui.screens.SettingsScreenContent
import com.nikashitsa.polar_alert_android.ui.screens.TrackingScreenContent
import com.nikashitsa.polar_alert_android.ui.theme.HeartAlertTheme
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy

/**
 * Store screenshots, captured by `bundle exec fastlane screenshots`.
 *
 * Renders the stateless `XScreenContent` composables with fixed values, so no strap,
 * Bluetooth, or Play billing is involved. Screenshot names are prefixed with a number to
 * keep them in store order.
 *
 * screengrab runs the class once per store locale and passes it as the `testLocale`
 * argument (e.g. "de-DE"); the screens are rendered in the app language for it, the same way
 * the app applies its in-app language setting.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    companion object {
        private const val DEVICE_NAME = "Polar H10 A203CC29"
        private const val HR_MIN = 110
        private const val HR_MAX = 140

        // The Google Pixel 4's resolution. frameit picks a device frame by exact screenshot
        // size, and its newest frame without a camera hole over the status bar is the Pixel 4.
        private const val FRAME_RESOLUTION = "1080x2280"

        @JvmStatic
        @BeforeClass
        fun beforeAll() {
            Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
            shell("wm size $FRAME_RESOLUTION")
            // System UI demo mode: a fixed clock and full, notification-free status bar. Sent
            // through the shell rather than screengrab's CleanStatusBar, which needs DUMP.
            shell("settings put global sysui_demo_allowed 1")
            demo("enter")
            demo("clock", "-e hhmm 0900")
            demo("battery", "-e level 100 -e plugged false")
            demo("network", "-e wifi show -e level 4 -e fully true")
            demo("network", "-e mobile hide")
            demo("notifications", "-e visible false")
        }

        @JvmStatic
        @AfterClass
        fun afterAll() {
            demo("exit")
            shell("wm size reset")
        }

        private fun demo(command: String, extras: String = "") =
            shell("am broadcast -a com.android.systemui.demo -e command $command $extras")

        private fun shell(command: String) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand(command)
                .close()
        }
    }

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private val testLocale: String? = InstrumentationRegistry.getArguments().getString("testLocale")

    private val language: AppLanguage =
        testLocale?.let { AppLanguage.fromLocale(Locale.forLanguageTag(it.replace('_', '-'))) }
            ?: AppLanguage.DEFAULT

    init {
        // screengrab files screenshots under Locale.getDefault(), which stays the device's, and
        // then pulls only the folder named after the locale it asked for. Use that name as is.
        testLocale?.let(Screengrab::setLocale)
    }

    private val localizedContext = targetContext.withLanguage(language.tag)

    @Test
    fun connectScreen() = capture("01_connect", button = R.string.connect) {
        ConnectScreenContent()
    }

    @Test
    fun settingsScreen() = capture("02_settings", button = R.string.start) {
        SettingsScreenContent(
            deviceName = DEVICE_NAME,
            batteryStatusFeature = BatteryStatusFeature(isSupported = true, batteryLevel = 70),
            hrMin = HR_MIN,
            hrMax = HR_MAX,
            hasAccess = true,
            freeSessionsLeft = 0,
            language = language,
        )
    }

    @Test
    fun trackingTooLow() = capture("03_tracking_too_low", button = R.string.stop) { Tracking(bpm = 64) }

    @Test
    fun trackingGood() = capture("04_tracking_good", button = R.string.stop) { Tracking(bpm = 117) }

    @Test
    fun trackingTooHigh() = capture("05_tracking_too_high", button = R.string.stop) { Tracking(bpm = 145) }

    /** Tracking with alerts raised on the first sample, so out-of-range states show as alerting. */
    @Composable
    private fun Tracking(bpm: Int) {
        TrackingScreenContent(
            deviceConnectionState = DeviceConnectionState.Connected(),
            hrFeature = HrFeature(isSupported = true),
            hrStreamStart = { _, onHeartRate -> onHeartRate(bpm) },
            hrMin = HR_MIN,
            hrMax = HR_MAX,
            outOfRangeFor = 0,
            initialDelay = 0,
        )
    }

    /** Renders [content], waits until [button] is on the display, then takes the screenshot. */
    private fun capture(name: String, @StringRes button: Int, content: @Composable () -> Unit) {
        composeRule.runOnUiThread {
            // Match MainActivity's system bars; the test activity is not edge-to-edge by default.
            composeRule.activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            )
        }
        composeRule.setContent {
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
            ) {
                HeartAlertTheme {
                    AppFrame(content)
                }
            }
        }
        composeRule.waitForIdle()
        // Compose being idle doesn't mean the frame has reached the display yet; a screen-level
        // capture taken too early shows the test activity's blank window instead.
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val buttonText = localizedContext.getString(button)
        check(device.wait(Until.hasObject(By.text(buttonText)), 5_000)) { "\"$buttonText\" never appeared" }
        device.waitForIdle()
        Screengrab.screenshot(name)
    }
}
