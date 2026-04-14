package com.example.socialconnect

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class activity_ReportProblem : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_problem)

        // 1. Initialize Views
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSubmit = findViewById<MaterialButton>(R.id.btnSubmit)
        val etProblemDescription = findViewById<EditText>(R.id.etProblemDescription)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)

        // 2. Setup Category Spinner (Dropdown)
        val categories = arrayOf("Select Issue Category", "App Crash", "Bug Report", "Account Issue", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = adapter

        // 3. Back Button
        btnBack.setOnClickListener {
            finish()
        }

        // 4. Submit Logic
        btnSubmit.setOnClickListener {
            val description = etProblemDescription.text.toString().trim()
            val selectedCategory = spinnerCategory.selectedItem.toString()

            if (selectedCategory == categories[0]) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            } else if (description.isEmpty()) {
                etProblemDescription.error = "Please describe the problem"
            } else {
                sendReport(selectedCategory, description)
            }
        }
    }

    private fun sendReport(category: String, message: String) {
        Toast.makeText(this, "Report submitted. Thank you!", Toast.LENGTH_LONG).show()

        finish()
    }
}