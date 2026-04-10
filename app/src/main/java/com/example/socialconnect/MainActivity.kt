package com.example.socialconnect

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import android.widget.TextView

class  MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get Started → goes to SignUpActivity
        findViewById<MaterialButton>(R.id.btnGetStarted).setOnClickListener {
            startActivity(Intent(this, activity_SignUp::class.java))
        }

        // Log In → goes to LoginActivity
        findViewById<TextView>(R.id.btnGoToLogin).setOnClickListener {
            startActivity(Intent(this, activity_Login::class.java))
        }
    }
}