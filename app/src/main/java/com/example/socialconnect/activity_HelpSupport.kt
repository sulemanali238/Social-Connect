package com.example.socialconnect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class activity_HelpSupport : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var rowFAQ: LinearLayout
    private lateinit var rowGettingStarted: LinearLayout
    private lateinit var rowPrivacySafety: LinearLayout
    private lateinit var rowEmailSupport: LinearLayout
    private lateinit var rowReportProblem: LinearLayout




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_support)

        // 1. Initialize Views
        btnBack = findViewById(R.id.btnBack)
        rowFAQ = findViewById(R.id.rowFAQ)
        rowGettingStarted = findViewById(R.id.rowGettingStarted)
        rowPrivacySafety = findViewById(R.id.rowPrivacySafety)
        rowEmailSupport = findViewById(R.id.rowEmailSupport)
        rowReportProblem = findViewById(R.id.rowReportProblem)

        // 2. Set Click Listeners

        // Back Button
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // FAQ Row
        rowFAQ.setOnClickListener {
            val intent = Intent(this, activity_Faqs::class.java)
            startActivity(intent)
        }

        // Getting Started Row
        rowGettingStarted.setOnClickListener {
            val intent = Intent(this, activity_Guide::class.java)
            startActivity(intent)
        }

        // Privacy & Safety Row
        rowPrivacySafety.setOnClickListener {
            val intent = Intent(this, activity_Privacy::class.java)
            startActivity(intent)
        }

        // Email Support Row (Opens Email Client)
        rowEmailSupport.setOnClickListener {
            composeEmail("socialConnect@gmail.com", "Support Request")
        }

        // Report a Problem Row
        rowReportProblem.setOnClickListener {
            val intent = Intent(this, activity_ReportProblem::class.java)
            startActivity(intent)
        }
    }

    /**
     * Helper function to open the user's preferred email app
     */
    private fun composeEmail(address: String, subject: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // only email apps should handle this
            putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }
}