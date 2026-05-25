package com.example.socialconnect

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

@Suppress("DEPRECATION")
class activity_LinkedEmail : AppCompatActivity() {

    // ── Views ────────────────────────────────────────────────────────────────
    private lateinit var btnBack: ImageView
    private lateinit var tvCurrentEmailHero: TextView
    private lateinit var tvCurrentEmail: TextView
    private lateinit var tvVerifiedBadge: TextView
    private lateinit var tvVerificationStatus: TextView
    private lateinit var ivVerificationIcon: ImageView
    private lateinit var rowChangeEmail: LinearLayout
    private lateinit var rowResendVerification: LinearLayout

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_linked_email)

        // Toolbar back button
        supportActionBar?.apply {
            title = "Linked Email"
            setDisplayHomeAsUpEnabled(true)
        }

        bindViews()
        loadEmailInfo()
        setListeners()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    private fun bindViews() {
        btnBack               = findViewById(R.id.btnBack)
        tvCurrentEmailHero    = findViewById(R.id.tvCurrentEmailHero)
        tvCurrentEmail        = findViewById(R.id.tvCurrentEmail)
        tvVerifiedBadge       = findViewById(R.id.tvVerifiedBadge)
        tvVerificationStatus  = findViewById(R.id.tvVerificationStatus)
        ivVerificationIcon    = findViewById(R.id.ivVerificationIcon)
        rowChangeEmail        = findViewById(R.id.rowChangeEmail)
        rowResendVerification = findViewById(R.id.rowResendVerification)
    }

    private fun loadEmailInfo() {
        val user = AuthUtil.currentUser
        if (user == null) {
            showToast("No logged in user found.")
            finish()
            return
        }

        // Wait for fresh data, then update UI once
        user.reload().addOnCompleteListener {
            val email    = user.email ?: "No email linked"
            val verified = AuthUtil.isEmailVerified

            runOnUiThread {
                tvCurrentEmailHero.text = email
                tvCurrentEmail.text     = email
                updateVerificationUI(verified)
            }
        }
    }

    private fun updateVerificationUI(isVerified: Boolean) {
        if (isVerified) {
            // Verified state
            tvVerifiedBadge.visibility = View.VISIBLE
            tvVerificationStatus.text = "Email verified"
            tvVerificationStatus.setTextColor(
                ContextCompat.getColor(this, R.color.teal_green)
            )
            ivVerificationIcon.setColorFilter(
                ContextCompat.getColor(this, R.color.teal_green)
            )
            ivVerificationIcon.setImageResource(R.drawable.shape_story_ring_unseen)
            // Hide resend row if already verified
            rowResendVerification.visibility = View.GONE
        } else {
            // Not verified
            tvVerifiedBadge.visibility = View.GONE
            tvVerificationStatus.text = "Email not verified — please check your inbox"
            tvVerificationStatus.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_orange_dark)
            )
            ivVerificationIcon.clearColorFilter()
            ivVerificationIcon.setColorFilter(
                ContextCompat.getColor(this, android.R.color.holo_orange_dark)
            )
            ivVerificationIcon.setImageResource(R.drawable.ic_info)
            rowResendVerification.visibility = View.VISIBLE
        }
    }


    private fun setListeners() {

        btnBack.setOnClickListener { finish() }

        rowChangeEmail.setOnClickListener {
            showChangeEmailDialog()
        }

        rowResendVerification.setOnClickListener {
            resendVerificationEmail()
        }
    }

    private fun showChangeEmailDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_change_email, null)

        val etNewEmail    = dialogView.findViewById<EditText>(R.id.etNewEmail)
        val etPassword    = dialogView.findViewById<EditText>(R.id.etPassword)

        AlertDialog.Builder(this)
            .setTitle("Change Email")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newEmail  = etNewEmail.text.toString().trim()
                val password  = etPassword.text.toString().trim()

                if (newEmail.isEmpty() || password.isEmpty()) {
                    showToast("Please fill in all fields.")
                    return@setPositiveButton
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                    showToast("Enter a valid email address.")
                    return@setPositiveButton
                }

                updateEmail(newEmail, password)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Update Email (re-authenticate → update) ───────────────────────────────

    private fun updateEmail(newEmail: String, password: String) {
        val currentEmail = AuthUtil.currentUser?.email ?: return

        // Step 1: re-authenticate with current credentials
        AuthUtil.login(currentEmail, password) { success, error ->
            if (!success) {
                showToast("Authentication failed: $error")
                return@login
            }

            // Step 2: update email in Firebase Auth
            AuthUtil.currentUser?.updateEmail(newEmail)
                ?.addOnSuccessListener {
                    // Step 3: sync new email to Firestore user doc
                    FireStoreUtil.updateUser(mapOf("email" to newEmail)) { updated, msg ->
                        runOnUiThread {
                            if (updated) {
                                showToast("Email updated. Please verify your new email.")
                                loadEmailInfo()   // refresh UI
                                // Auto-send verification to new address
                                AuthUtil.sendEmailVerification { _, _ -> }
                            } else {
                                showToast("Firestore update failed: $msg")
                            }
                        }
                    }
                }
                ?.addOnFailureListener { e ->
                    runOnUiThread {
                        showToast("Failed to update email: ${e.message}")
                    }
                }
        }
    }

    // ── Resend Verification Email ─────────────────────────────────────────────

    private fun resendVerificationEmail() {
        AuthUtil.sendEmailVerification { success, error ->
            runOnUiThread {
                if (success) {
                    showToast("Verification email sent! Check your inbox.")
                } else {
                    showToast("Failed to send: $error")
                }
            }
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}