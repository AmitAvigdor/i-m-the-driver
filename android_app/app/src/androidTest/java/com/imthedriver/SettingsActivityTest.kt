package com.imthedriver

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Button
import android.widget.Switch
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.imthedriver.R
import junit.framework.TestCase.assertEquals
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var scenario: ActivityScenario<SettingsActivity>

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        scenario = ActivityScenario.launch(SettingsActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
        sharedPreferences.edit().clear().apply()
    }

    @Test
    fun testLoadSettings() {
        // Start the SettingsActivity scenario
        scenario = ActivityScenario.launch(SettingsActivity::class.java)

        scenario.onActivity { activity ->
            // Run the shared preferences check in a separate thread if necessary
            activity.runOnUiThread {
                // Check if the eye closure sound toggle is set correctly
                val sharedPref = activity.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val eyeClosureSoundEnabled = sharedPref.getBoolean("eyeClosureSound", true)

                assert(eyeClosureSoundEnabled == activity.findViewById<Switch>(R.id.eyeClosureSoundToggle).isChecked) {
                    "Eye closure sound setting was not loaded correctly"
                }
            }
        }
    }


    @Test
    fun testSaveSettingsButtonDisabledInitially() {
        // The save button should be disabled initially
        onView(withId(R.id.saveSettingsButton)).check(matches(not(isEnabled())))
    }

    @Test
    fun testEnableSaveButtonOnChange() {
        // Simulate a change in one of the settings
        onView(withId(R.id.emergencyCallNumber)).perform(replaceText("123456789"))

        // The save button should be enabled after a change
        onView(withId(R.id.saveSettingsButton)).check(matches(isEnabled()))
    }

    @Test
    fun testSaveSettings() {
        val scenario = ActivityScenario.launch(SettingsActivity::class.java)

        scenario.onActivity { activity ->
            // Toggle the setting
            val eyeClosureToggle = activity.findViewById<Switch>(R.id.eyeClosureSoundToggle)
            eyeClosureToggle.isChecked = false

            // Save the settings
            val saveButton = activity.findViewById<Button>(R.id.saveSettingsButton)
            saveButton.performClick()

            // Verify that the setting was saved correctly
            val sharedPref = activity.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val savedValue = sharedPref.getBoolean("eyeClosureSound", true)

            // Adding logs for debugging
            Log.d("SettingsActivityTest", "Eye closure sound setting saved value: $savedValue")

            // Perform the assertion
            assertEquals("Eye closure sound setting was not saved correctly", false, savedValue)
        }
    }



    @Test
    fun testCameraAdjustmentButton() {
        // Launch the SettingsActivity
        val scenario = ActivityScenario.launch(SettingsActivity::class.java)

        // Check if the button is displayed
        onView(withId(R.id.cameraAdjustmentButton))
            .check(matches(isDisplayed()))

        // Click the button if necessary for further test steps
        onView(withId(R.id.cameraAdjustmentButton))
            .perform(click())
    }

    @Test
    fun testBottomNavigation() {
        // Click on the home navigation item and check if the activity finishes
        onView(withId(R.id.nav_home)).perform(click())

        scenario.onActivity {
            assert(it.isFinishing) {
                "Activity did not finish when Home navigation item was clicked"
            }
        }
    }
}
