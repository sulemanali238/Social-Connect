package com.example.socialconnect

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class activity_RateThanks : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rate_thanks)

        val btnBack = findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }
    }
}