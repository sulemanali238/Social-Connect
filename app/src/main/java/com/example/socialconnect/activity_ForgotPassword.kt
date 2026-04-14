package com.example.socialconnect

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class activity_ForgotPassword : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var btnSendReset: MaterialButton
    private lateinit var tvBackToLogin: TextView
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initViews()
        setupFocusListeners()
        setupClickListeners()
    }

    private fun initViews() {
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        btnSendReset = findViewById(R.id.btnSendReset)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupFocusListeners() {
        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilEmail.error = null
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        tvBackToLogin.setOnClickListener { finish() }

        btnSendReset.setOnClickListener {
            tilEmail.error = null

            if (validateInputs()) {
                val email = etEmail.text.toString().trim()

                setLoadingState(true)

                AuthUtil.sendPasswordResetEmail(email) { success, error ->
                    setLoadingState(false)
                    if (success) {
                        Toast.makeText(
                            this,
                            "If this email is registered, a reset link has been sent",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Failed: $error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            tilEmail.error = "Enter your email"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email"
            return false
        }
        return true
    }

    private fun setLoadingState(isLoading: Boolean) {
        btnSendReset.isEnabled = !isLoading
        tilEmail.isEnabled = !isLoading
        btnSendReset.text = if (isLoading) "Sending..." else "Send Reset Link"
    }
}