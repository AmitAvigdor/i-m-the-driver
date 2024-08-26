
package com.imthedriver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.imthedriver.R
import com.imthedriver.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var cameraAdjustmentButton: Button
    private lateinit var eyeClosureSoundToggle: Switch
    private lateinit var yawnSoundToggle: Switch
    private lateinit var emergencyCallNumber: EditText
    private lateinit var saveSettingsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize views
        cameraAdjustmentButton = findViewById(R.id.cameraAdjustmentButton)
        eyeClosureSoundToggle = findViewById(R.id.eyeClosureSoundToggle)
        yawnSoundToggle = findViewById(R.id.yawnSoundToggle)
        emergencyCallNumber = findViewById(R.id.emergencyCallNumber)
        saveSettingsButton = findViewById(R.id.saveSettingsButton)

        // Load saved settings
        loadSettings()

        setupBottomNavigation()

        // Camera adjustment button click listener
        cameraAdjustmentButton.setOnClickListener {
            // Open camera adjustment activity
            val intent = Intent(this, CameraAdjustmentActivity::class.java)
            startActivity(intent)
        }

        // Save button click listener
        saveSettingsButton.setOnClickListener {
            saveSettings()
            finish() // Close settings screen after saving
        }

        // Disable save button if no new settings
        saveSettingsButton.isEnabled = false
        emergencyCallNumber.addTextChangedListener { checkForChanges() }
        eyeClosureSoundToggle.setOnCheckedChangeListener { _, _ -> checkForChanges() }
        yawnSoundToggle.setOnCheckedChangeListener { _, _ -> checkForChanges() }
    }

    private fun loadSettings() {
        // Load saved preferences (assuming you're using SharedPreferences)
        val sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        eyeClosureSoundToggle.isChecked = sharedPref.getBoolean("eyeClosureSound", true)
        yawnSoundToggle.isChecked = sharedPref.getBoolean("yawnSound", true)
        emergencyCallNumber.setText(sharedPref.getString("emergencyCallNumber", ""))
    }

    private fun saveSettings() {
        // Save preferences
        val sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("eyeClosureSound", eyeClosureSoundToggle.isChecked)
            putBoolean("yawnSound", yawnSoundToggle.isChecked)
            putString("emergencyCallNumber", emergencyCallNumber.text.toString())
            apply()
        }
        Log.d("SettingsActivity", "Settings saved: eyeClosureSound=${eyeClosureSoundToggle.isChecked}")
    }

    private fun checkForChanges() {
        val sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        val newSettingsExist =
            eyeClosureSoundToggle.isChecked != sharedPref.getBoolean("eyeClosureSound", true) ||
                    yawnSoundToggle.isChecked != sharedPref.getBoolean("yawnSound", true) ||
                    emergencyCallNumber.text.toString() != sharedPref.getString("emergencyCallNumber", "")

        saveSettingsButton.isEnabled = newSettingsExist
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    finish() // Go back to the main activity
                    true
                }
                R.id.nav_settings -> {
                    true
                }
                else -> false
            }
        }
    }
}