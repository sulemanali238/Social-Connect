package com.example.socialconnect

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class activity_Privacy : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}