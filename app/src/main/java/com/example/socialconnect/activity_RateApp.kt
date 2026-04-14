package com.example.socialconnect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class activity_RateApp : AppCompatActivity() {

    private var currentRating = 4 // Default rating from XML
    private lateinit var stars: Array<ImageView>
    private lateinit var tvRatingLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rate_app)

        // 1. Initialize Views
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val etReview = findViewById<EditText>(R.id.etReview)
        val rowRatePlayStore = findViewById<LinearLayout>(R.id.rowRatePlayStore)
        val rowSubmitFeedback = findViewById<LinearLayout>(R.id.rowSubmitFeedback)
        tvRatingLabel = findViewById(R.id.tvRatingLabel)

        stars = arrayOf(
            findViewById(R.id.star1),
            findViewById(R.id.star2),
            findViewById(R.id.star3),
            findViewById(R.id.star4),
            findViewById(R.id.star5)
        )

        // 2. Set Star Click Listeners
        stars.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                updateRating(index + 1)
            }
        }

        // 3. Back Button
        btnBack.setOnClickListener { finish() }

        // 4. Action: Play Store
        rowRatePlayStore.setOnClickListener {
            Toast.makeText(this, "Coming soon...", Toast.LENGTH_LONG).show()
        }

        // 5. Action: Submit Feedback
        rowSubmitFeedback.setOnClickListener {
            val reviewText = etReview.text.toString()
            submitFeedback(currentRating, reviewText)
            val intent = Intent(this, activity_RateThanks::class.java)
            startActivity(intent)
        }
    }

    private fun updateRating(rating: Int) {
        currentRating = rating

        // Update Star Visuals
        for (i in stars.indices) {
            if (i < rating) {
                stars[i].setImageResource(R.drawable.ic_star)
                stars[i].setColorFilter(ContextCompat.getColor(this, android.R.color.holo_orange_light))
            } else {
                stars[i].setImageResource(R.drawable.ic_star_outline)
                stars[i].setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
            }
        }

        // Update Label
        tvRatingLabel.text = when (rating) {
            1 -> "Poor"
            2 -> "Fair"
            3 -> "Good"
            4 -> "Very Good"
            5 -> "Excellent!"
            else -> ""
        }
    }

    private fun submitFeedback(rating: Int, feedback: String) {
        // Here you would typically send data to your API/Firebase
        val message = "Thank you! You rated us $rating stars."
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        if (feedback.isNotEmpty()) {
            // Logic to send text feedback
        }
    }
}