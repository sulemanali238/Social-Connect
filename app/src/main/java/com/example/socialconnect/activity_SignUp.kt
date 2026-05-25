package com.example.socialconnect

import android.content.Intent
import android.os.Bundle
import android.view.View
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

class activity_SignUp : AppCompatActivity() {

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
        val root = findViewById<View>(R.id.rootLayout) // give your RelativeLayout this id
        root.background = TilePatternDrawable(this)

        initViews()
        setupFocusListeners()
        setupClickListeners()
    }

    private fun initViews() {
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
            clearErrors()
            if (validateInputs()) {
                val fullName = etFullName.text.toString().trim()
                val username = etUsername.text.toString().trim().lowercase()
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()

                setLoadingState(true)

                FireStoreUtil.isUsernameTaken(username) { isTaken ->
                    if (isTaken) {
                        tilUsername.error = "Username already taken"
                        setLoadingState(false)
                    } else {
                        createAccount(fullName, username, email, password)
                    }
                }
            }
        }
    }

    private fun createAccount(
        fullName: String,
        username: String,
        email: String,
        password: String
    ) {
        AuthUtil.signUp(email, password) { success, error ->
            if (!success) {
                setLoadingState(false)
                Toast.makeText(this, "Sign up failed: $error", Toast.LENGTH_LONG).show()
                return@signUp
            }

            // update display name in Auth
            AuthUtil.updateDisplayName(fullName) { }

            // create Firestore document
            val userModel = UserModel(
                uid = AuthUtil.currentUid,
                fullName = fullName,
                username = username,
                email = email,
                bio = "",
                website = "",
                profileImageBase64 = "",
                followerCount = 0,
                followingCount = 0,
                postCount = 0,
                createdAt = System.currentTimeMillis()
            )

            FireStoreUtil.createUser(userModel) { saved, saveError ->
                if (!saved) {
                    // delete auth account if Firestore fails
                    AuthUtil.deleteAccount { }
                    setLoadingState(false)
                    Toast.makeText(
                        this,
                        "Failed to save user: $saveError",
                        Toast.LENGTH_LONG
                    ).show()
                    return@createUser
                }

                // send verification email
                AuthUtil.sendEmailVerification { sent, _ ->
                    AuthUtil.logout()
                    setLoadingState(false)
                    if (sent) {
                        showVerificationDialog(email)
                    } else {
                        Toast.makeText(
                            this,
                            "Account created but verification email failed.",
                            Toast.LENGTH_LONG
                        ).show()
                        goToLogin()
                    }
                }
            }
        }
    }

    private fun showVerificationDialog(email: String) {
        AlertDialog.Builder(this)
            .setTitle("Verify Your Email")
            .setMessage(
                "A verification link has been sent to:\n\n$email\n\n" +
                        "Please verify your email before logging in."
            )
            .setCancelable(false)
            .setPositiveButton("Go to Login") { _, _ -> goToLogin() }
            .show()
    }

    private fun goToLogin() {
        startActivity(Intent(this, activity_Login::class.java))
        finish()
    }

    private fun clearErrors() {
        tilFullName.error = null
        tilUsername.error = null
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null
    }

    private fun validateInputs(): Boolean {
        val name = etFullName.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirm = etConfirmPassword.text.toString().trim()

        if (name.isEmpty()) { tilFullName.error = "Enter your name"; return false }
        if (username.isEmpty()) { tilUsername.error = "Enter a username"; return false }
        if (username.length < 3) { tilUsername.error = "Min 3 characters"; return false }
        if (username.contains(" ")) { tilUsername.error = "No spaces allowed"; return false }
        if (email.isEmpty()) { tilEmail.error = "Enter your email"; return false }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email"; return false
        }
        if (password.isEmpty()) { tilPassword.error = "Enter a password"; return false }
        if (password.length < 6) { tilPassword.error = "Min 6 characters"; return false }
        if (confirm != password) { tilConfirmPassword.error = "Passwords do not match"; return false }
        if (!cbTerms.isChecked) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "Please accept the Terms of Service",
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