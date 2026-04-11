package com.example.socialconnect

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class  activity_Launchers : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launchers)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            startActivity(Intent(this, activity_Main::class.java))
            finish()
            return
        }

        val login = findViewById<MaterialButton>(R.id.btnGoToLogin)
        login.setOnClickListener {
            val intent = Intent(this, activity_Login::class.java)
            startActivity(intent)
            finish()
        }

        val signup = findViewById<MaterialButton>(R.id.btnGetStarted)
        signup.setOnClickListener {
            val intent = Intent(this, activity_SignUp::class.java) // ← fixed class
            startActivity(intent)
            finish()

        }
    }
}