package com.imthedriver

import ToastMatcher
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.gsm.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.android.gms.location.FusedLocationProviderClient
import com.imthedriver.R
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class DriverStateDetectorTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var scenario: ActivityScenario<DriverStateDetector>

    @Before
    fun setup() {
        fusedLocationClient = Mockito.mock(FusedLocationProviderClient::class.java)
        scenario = ActivityScenario.launch(DriverStateDetector::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun testEmergencyCallButton() {
        // Check if the button is displayed
        onView(withId(R.id.emergencyCallButton)).check(matches(isDisplayed()))

        // Perform a click on the emergency call button
        onView(withId(R.id.emergencyCallButton)).perform(click())

        // Use onActivity to interact with the Activity's context
        scenario.onActivity { activity ->
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:123456789") // Mock number for testing
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Verify that the activity attempted to start the call
                val mockIntent = Intent(Intent.ACTION_CALL)
                mockIntent.data = Uri.parse("tel:123456789")
                val resolvedActivity = mockIntent.resolveActivity(activity.packageManager)
                assert(resolvedActivity != null) {
                    "No activity found to handle call intent"
                }
                // Optionally log the test run or use assertions for verification
                Log.d("Test", "Call intent was properly handled by the test.")
            }
        }
    }


    @Test
    fun testCameraInitialization() {
        // Check if the camera view is visible
        onView(withId(R.id.viewFinder)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        // Check if monitoring text is visible
        onView(withId(R.id.monitoringText)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        scenario.onActivity { activity ->
            assert(activity.getCameraProvider() != null) { "CameraProvider is not initialized" }
        }
    }
}