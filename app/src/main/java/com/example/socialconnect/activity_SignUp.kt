package com.example.socialconnect

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

class activity_SignUp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var tilFullName: TextInputLayout
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etFullName: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var cbTerms: MaterialCheckBox
    private lateinit var btnSignUp: MaterialButton
    private lateinit var btnBack: ImageView
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        initViews()
        setupFocusListeners()
        setupClickListeners()
    }

    private fun initViews() {
        auth = FirebaseAuth.getInstance()

        tilFullName = findViewById(R.id.tilFullName)
        tilUsername = findViewById(R.id.tilUsername)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etFullName = findViewById(R.id.etFullName)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        cbTerms = findViewById(R.id.cbTerms)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnBack = findViewById(R.id.btnBack)
        tvLogin = findViewById(R.id.tvLogin)
    }

    private fun setupFocusListeners() {
        etFullName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilFullName.error = null
        }
        etUsername.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilUsername.error = null
        }
        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilEmail.error = null
        }
        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilPassword.error = null
        }
        etConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilConfirmPassword.error = null
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, activity_Login::class.java))
            finish()
        }

        btnSignUp.setOnClickListener {
            tilFullName.error = null
            tilUsername.error = null
            tilEmail.error = null
            tilPassword.error = null
            tilConfirmPassword.error = null

            if (validateInputs()) {
                val name     = etFullName.text.toString().trim()
                val email    = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()

                setLoadingState(true)

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val user = result.user

                        val profileUpdates = userProfileChangeRequest {
                            displayName = name
                        }
                        user?.updateProfile(profileUpdates)

                        user?.sendEmailVerification()
                            ?.addOnSuccessListener {
                                auth.signOut()
                                setLoadingState(false)

                                AlertDialog.Builder(this)
                                    .setTitle("Verify Your Email")
                                    .setMessage(
                                        "A verification link has been sent to:\n\n$email\n\n" +
                                                "Please check your inbox and verify your email " +
                                                "before logging in."
                                    )
                                    .setCancelable(false)
                                    .setPositiveButton("Go to Login") { _, _ ->
                                        startActivity(Intent(this, activity_Login::class.java))
                                        finish()
                                    }
                                    .show()
                            }
                            ?.addOnFailureListener {
                                auth.signOut()
                                setLoadingState(false)
                                Toast.makeText(
                                    this,
                                    "Account created but verification email could not be sent. " +
                                            "Try logging in and resend verification.",
                                    Toast.LENGTH_LONG
                                ).show()
                                startActivity(Intent(this, activity_Login::class.java))
                                finish()
                            }
                    }
                    .addOnFailureListener {

                        setLoadingState(false)

                        Toast.makeText(
                            this,
                            "Sign up failed: ${it.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val name     = etFullName.text.toString().trim()
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirm  = etConfirmPassword.text.toString().trim()

        if (name.isEmpty()) {
            tilFullName.error = "Enter your name"
            return false
        }
        if (email.isEmpty()) {
            tilEmail.error = "Enter your email"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email"
            return false
        }
        if (password.isEmpty()) {
            tilPassword.error = "Enter a password"
            return false
        }
        if (password.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"
            return false
        }
        if (confirm != password) {
            tilConfirmPassword.error = "Passwords do not match"
            return false
        }
        if (!cbTerms.isChecked) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "Please accept the Terms of Service and Privacy Policy",
                Snackbar.LENGTH_LONG
            ).show()
            return false
        }
        return true
    }

    private fun setLoadingState(isLoading: Boolean) {
        btnSignUp.isEnabled = !isLoading
        tilFullName.isEnabled = !isLoading
        tilUsername.isEnabled = !isLoading
        tilEmail.isEnabled = !isLoading
        tilPassword.isEnabled = !isLoading
        tilConfirmPassword.isEnabled = !isLoading
        cbTerms.isEnabled = !isLoading
        btnSignUp.text = if (isLoading) "Creating account..." else "Create Account"
    }
}