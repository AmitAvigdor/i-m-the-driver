package com.imthedriver

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.imthedriver.R

class DrivingTipsActivity : AppCompatActivity() {

    private lateinit var tipTextView: TextView
    private lateinit var nextTipButton: Button
    private val drivingTips = listOf(
        "Take regular breaks during long drives.",
        "Stay hydrated and avoid heavy meals before driving.",
        "Keep your car well-ventilated to stay alert.",
        "Listen to music or podcasts to keep your mind active.",
        "Avoid driving when you're feeling drowsy or fatigued.",
        "If you start feeling sleepy, pull over and take a short nap.",
        "Chew gum or have a snack to stay awake.",
        "Keep the temperature cool inside the car to stay awake.",
        "Engage in light conversation with a passenger to stay alert.",
        "Plan your route ahead of time to avoid unnecessary stress."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving_tips)

        tipTextView = findViewById(R.id.tipTextView)
        nextTipButton = findViewById(R.id.nextTipButton)

        // Show the first random tip
        showRandomTip()

        // Set click listener for the next tip button
        nextTipButton.setOnClickListener {
            showRandomTip()
        }
    }

    private fun showRandomTip() {
        val randomTip = drivingTips.random()
        tipTextView.text = randomTip
        tipTextView.textSize = 24f // Make the tip big and bold
        tipTextView.setTypeface(null, android.graphics.Typeface.BOLD)
    }
}
