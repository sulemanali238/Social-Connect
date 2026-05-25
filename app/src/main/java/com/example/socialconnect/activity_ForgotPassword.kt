package com.example.socialconnect

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class activity_ForgotPassword : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var btnSendReset: MaterialButton
    private lateinit var btnBack: ImageView
    private lateinit var cardSuccess: MaterialCardView
    private lateinit var cardError: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val root = findViewById<View>(R.id.rootLayout)
        root.background = TilePatternDrawable(this)

        initViews()
        setupFocusListeners()
        setupClickListeners()
    }

    private fun initViews() {
        tilEmail     = findViewById(R.id.tilEmail)
        etEmail      = findViewById(R.id.etEmail)
        btnSendReset = findViewById(R.id.btnSendReset)
        btnBack      = findViewById(R.id.btnBack)
        cardSuccess  = findViewById(R.id.cardSuccess)
        cardError = findViewById(R.id.cardError)
    }

    private fun setupFocusListeners() {
        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilEmail.error         = null
                cardSuccess.visibility = View.GONE
                cardError.visibility   = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnSendReset.setOnClickListener {
            tilEmail.error = null

            if (!validateInputs()) return@setOnClickListener

            val email = etEmail.text.toString().trim()
            setLoadingState(true)

            FireStoreUtil.checkEmailExists(email) { exists ->
                if (!exists) {
                    runOnUiThread {
                        setLoadingState(false)
                        cardError.visibility   = View.VISIBLE
                        cardSuccess.visibility = View.GONE
                    }
                    return@checkEmailExists
                }

                AuthUtil.sendPasswordResetEmail(email) { success, error ->
                    runOnUiThread {
                        setLoadingState(false)
                        if (success) {
                            cardSuccess.visibility = View.VISIBLE
                            cardError.visibility   = View.GONE
                            tilEmail.visibility    = View.GONE
                            btnSendReset.text      = "Resend Link"
                        } else {
                            cardError.visibility   = View.VISIBLE
                            cardSuccess.visibility = View.GONE
                        }
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
            tilEmail.error = "Enter a valid email address"
            return false
        }
        return true
    }

    private fun setLoadingState(isLoading: Boolean) {
        btnSendReset.isEnabled = !isLoading
        tilEmail.isEnabled     = !isLoading
        btnSendReset.text      = if (isLoading) "Sending..." else "Send Reset Link"
    }
}