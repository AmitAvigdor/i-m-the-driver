package com.imthedriver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.imthedriver.R
import com.imthedriver.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private val tipChangeInterval = 4000L // 4 seconds interval for auto-loop

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the "Let's Drive" button to start the DriverStateDetector activity
        binding.letsDriveButton.setOnClickListener {
            val intent = Intent(this, DriverStateDetector::class.java)
            startActivity(intent)
        }

        // Set the greeting text based on the current time
        setGreetingText()

        // Setup ViewPager with TipsAdapter
        val tips = listOf(
            Tip("Take regular breaks every 2 hours.", R.drawable.regular_break),
            Tip("Stay hydrated and eat light.", R.drawable.hydrate),
            Tip("Keep the car cool to avoid drowsiness.", R.drawable.drowsiness),
            Tip("Avoid heavy meals before driving.", R.drawable.light_meal),
            Tip("Maintain a good posture.", R.drawable.posture)
        )
        val tipsAdapter = TipsAdapter(tips)
        binding.tipsViewPager.adapter = tipsAdapter

        // Arrow button click listeners
        binding.leftArrow.setOnClickListener {
            val currentItem = binding.tipsViewPager.currentItem
            if (currentItem > 0) {
                binding.tipsViewPager.currentItem = currentItem - 1
            } else {
                binding.tipsViewPager.currentItem = tips.size - 1
            }
        }

        binding.rightArrow.setOnClickListener {
            val currentItem = binding.tipsViewPager.currentItem
            if (currentItem < tips.size - 1) {
                binding.tipsViewPager.currentItem = currentItem + 1
            } else {
                binding.tipsViewPager.currentItem = 0
            }
        }

        // Start auto-looping tips
        startAutoLoopTips()

        // Check permissions
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        setupBottomNavigation()
    }

    private fun setGreetingText() {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val greetingText = when (hourOfDay) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
        binding.greetingTextView.text = greetingText
    }

    private fun startAutoLoopTips() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val currentItem = binding.tipsViewPager.currentItem
                val nextItem = if (currentItem < binding.tipsViewPager.adapter!!.itemCount - 1) {
                    currentItem + 1
                } else {
                    0
                }
                binding.tipsViewPager.currentItem = nextItem
                handler.postDelayed(this, tipChangeInterval)
            }
        }, tipChangeInterval)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true // Already in home, do nothing
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                // Permissions granted, proceed as necessary
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        )
    }
}
