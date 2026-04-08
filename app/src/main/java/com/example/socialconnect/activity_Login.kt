package com.example.socialconnect

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class activity_Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var signUpText: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupClickListeners()
        setupFocusListeners()
    }
    private fun initViews() {
        auth = FirebaseAuth.getInstance()

        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        signUpText = findViewById(R.id.signUpText)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupFocusListeners() {
        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilEmail.error = null
        }
        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilPassword.error = null
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        signUpText.setOnClickListener {
            startActivity(Intent(this, activity_SignUp::class.java))
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, activity_ForgotPassword::class.java))
        }

        btnLogin.setOnClickListener {

            tilEmail.error    = null
            tilPassword.error = null

            if (validateInputs()) {

                val email    = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()

                setLoadingState(true)

                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        val user = auth.currentUser

                        if (user != null && !user.isEmailVerified) {
                            // Not verified — sign out and prompt
                            auth.signOut()
                            setLoadingState(false)
                            AlertDialog.Builder(this)
                                .setTitle("Email Not Verified")
                                .setMessage(
                                    "Please verify your email before logging in.\n\n" +
                                            "Check your inbox for the verification link."
                                )
                                .setNegativeButton("Cancel") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton("Resend Email") { _, _ ->
                                    user.sendEmailVerification()
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Verification email resent!", Toast.LENGTH_LONG).show()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this, "Failed to resend: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }.show()

                        } else {
                            // All good — go to Home
                            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, activity_Home::class.java))
                            finish()
                        }
                    }
                    .addOnFailureListener {
                        setLoadingState(false)
                        Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }

    }
    private fun validateInputs(): Boolean {
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            tilEmail.error = "Enter your email"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email"
            return false
        }
        if (password.isEmpty()) {
            tilPassword.error = "Enter your password"
            return false
        }
        if (password.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"
            return false
        }
        return true
    }
    private fun setLoadingState(isLoading: Boolean) {
        btnLogin.isEnabled = !isLoading
        tilEmail.isEnabled = !isLoading
        tilPassword.isEnabled = !isLoading
        btnLogin.text = if (isLoading) "Logging in..." else "Log In"
    }
}