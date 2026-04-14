package com.example.socialconnect

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class activity_ChangePassword : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tilCurrentPassword: TextInputLayout
    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnChangePassword: MaterialButton

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnBack             = findViewById(R.id.btnBack)
        tilCurrentPassword  = findViewById(R.id.tilCurrentPassword)
        tilNewPassword      = findViewById(R.id.tilNewPassword)
        tilConfirmPassword  = findViewById(R.id.tilConfirmPassword)
        etCurrentPassword   = findViewById(R.id.etCurrentPassword)
        etNewPassword       = findViewById(R.id.etNewPassword)
        etConfirmPassword   = findViewById(R.id.etConfirmPassword)
        btnChangePassword   = findViewById(R.id.btnChangePassword)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnChangePassword.setOnClickListener {
            if (validateInputs()) {
                changePassword()
            }
        }
    }

    // ─── VALIDATION ─────────────────────────────────────────────────

    private fun validateInputs(): Boolean {
        val current = etCurrentPassword.text.toString().trim()
        val newPass  = etNewPassword.text.toString().trim()
        val confirm  = etConfirmPassword.text.toString().trim()

        // clear old errors
        tilCurrentPassword.error = null
        tilNewPassword.error     = null
        tilConfirmPassword.error = null

        var isValid = true

        if (current.isEmpty()) {
            tilCurrentPassword.error = "Enter your current password"
            isValid = false
        }

        if (newPass.isEmpty()) {
            tilNewPassword.error = "Enter a new password"
            isValid = false
        } else if (newPass.length < 6) {
            tilNewPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else if (newPass == current) {
            tilNewPassword.error = "New password must be different from current password"
            isValid = false
        }

        if (confirm.isEmpty()) {
            tilConfirmPassword.error = "Confirm your new password"
            isValid = false
        } else if (confirm != newPass) {
            tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        return isValid
    }

    // ─── CHANGE PASSWORD ─────────────────────────────────────────────

    private fun changePassword() {
        val currentPassword = etCurrentPassword.text.toString().trim()
        val newPassword     = etNewPassword.text.toString().trim()

        val user = auth.currentUser
        if (user == null || user.email.isNullOrEmpty()) {
            showToast("No user logged in")
            return
        }

        setLoading(true)

        // Re-authenticate first (required by Firebase before sensitive operations)
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Now update the password
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        setLoading(false)
                        showToast("Password changed successfully")
                        finish()
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        showToast(e.message ?: "Failed to update password")
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                // Most likely wrong current password
                tilCurrentPassword.error = "Current password is incorrect"
                showToast(e.message ?: "Re-authentication failed")
            }
    }

    // ─── HELPERS ────────────────────────────────────────────────────

    private fun setLoading(loading: Boolean) {
        btnChangePassword.isEnabled = !loading
        btnChangePassword.text = if (loading) "Updating..." else "Change Password"
        tilCurrentPassword.isEnabled = !loading
        tilNewPassword.isEnabled     = !loading
        tilConfirmPassword.isEnabled = !loading
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}