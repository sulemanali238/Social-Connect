package com.example.socialconnect

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import de.hdodenhof.circleimageview.CircleImageView

class activity_EditProfile : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnSave: TextView
    private lateinit var tvChangePhoto: TextView
    private lateinit var btnPickImage: android.view.View
    private lateinit var imgEditAvatar: CircleImageView
    private lateinit var tilFullName: TextInputLayout
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilBio: TextInputLayout
    private lateinit var tilWebsite: TextInputLayout
    private lateinit var etFullName: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var etWebsite: TextInputEditText

    private var selectedImageBase64: String? = null
    private var currentUsername: String = ""
    private var uploadProgressDialog: com.google.android.material.dialog.MaterialAlertDialogBuilder? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                processImage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        initViews()
        setupClickListeners()
        loadCurrentUserData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSaveBottom)
        tvChangePhoto = findViewById(R.id.tvChangePhoto)
        btnPickImage = findViewById(R.id.btnPickImage)
        imgEditAvatar = findViewById(R.id.imgEditAvatar)
        tilFullName = findViewById(R.id.tilFullName)
        tilUsername = findViewById(R.id.tilUsername)
        tilBio = findViewById(R.id.tilBio)
        tilWebsite = findViewById(R.id.tilWebsite)
        etFullName = findViewById(R.id.etFullName)
        etUsername = findViewById(R.id.etUsername)
        etBio = findViewById(R.id.etBio)
        etWebsite = findViewById(R.id.etWebsite)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnPickImage.setOnClickListener { openImagePicker() }
        tvChangePhoto.setOnClickListener { openImagePicker() }

        btnSave.setOnClickListener {
            tilFullName.error = null
            tilUsername.error = null
            if (validateInputs()) {
                saveProfile()
            }
        }
    }

    private fun loadCurrentUserData() {
        FireStoreUtil.getCurrentUser { user ->
            if (user == null) return@getCurrentUser

            currentUsername = user.username

            etFullName.setText(user.fullName)
            etUsername.setText(user.username)
            etBio.setText(user.bio)
            etWebsite.setText(user.website)

            if (user.profileImageBase64.isNotEmpty()) {
                ImageUtils.loadBase64(
                    user.profileImageBase64,
                    imgEditAvatar,
                    getDrawable(R.drawable.ic_avatar)
                )
            }
        }
    }

    private fun openImagePicker() {
        val options = arrayOf("Choose from Gallery", "Remove Photo")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Profile Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
                        imagePickerLauncher.launch(intent)
                    }
                    1 -> removeProfilePhoto()
                }
            }
            .show()
    }

    private fun removeProfilePhoto() {
        selectedImageBase64 = ""  // empty string = remove
        imgEditAvatar.setImageDrawable(getDrawable(R.drawable.ic_avatar))
    }

    private fun processImage(uri: Uri) {
        val dialog = showUploadProgress()

        android.os.Handler(mainLooper).postDelayed({
            Thread {
                val base64 = ImageUtils.uriToBase64(this, uri)
                runOnUiThread {
                    dialog.dismiss()
                    if (base64 != null) {
                        selectedImageBase64 = base64
                        ImageUtils.loadBase64(
                            base64,
                            imgEditAvatar,
                            getDrawable(R.drawable.ic_avatar)
                        )
                        Toast.makeText(this, "Photo uploaded successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }, 300)
    }
    private fun validateInputs(): Boolean {
        val name = etFullName.text.toString().trim()
        val username = etUsername.text.toString().trim()

        if (name.isEmpty()) {
            tilFullName.error = "Enter your name"
            return false
        }
        if (username.isEmpty()) {
            tilUsername.error = "Enter a username"
            return false
        }
        if (username.length < 3) {
            tilUsername.error = "Min 3 characters"
            return false
        }
        if (username.contains(" ")) {
            tilUsername.error = "No spaces allowed"
            return false
        }
        return true
    }

    private fun saveProfile() {
        val newUsername = etUsername.text.toString().trim().lowercase()
        val fullName = etFullName.text.toString().trim()
        val bio = etBio.text.toString().trim()
        val website = etWebsite.text.toString().trim()

        setLoadingState(true)

        // if username changed check if taken
        if (newUsername != currentUsername) {
            FireStoreUtil.isUsernameTaken(newUsername) { isTaken ->
                if (isTaken) {
                    tilUsername.error = "Username already taken"
                    setLoadingState(false)
                } else {
                    updateProfile(fullName, newUsername, bio, website)
                }
            }
        } else {
            updateProfile(fullName, newUsername, bio, website)
        }
    }

    private fun updateProfile(
        fullName: String,
        username: String,
        bio: String,
        website: String
    ) {
        val data = mutableMapOf<String, Any>(
            "fullName" to fullName,
            "username" to username,
            "bio" to bio,
            "website" to website
        )

        if (selectedImageBase64 != null) {
            data["profileImageBase64"] = selectedImageBase64!!
        }

        FireStoreUtil.updateUser(data) { success, error ->
            setLoadingState(false)
            if (success) {
                val newFullName = etFullName.text.toString().trim()

                FireStoreUtil.updateUserProfileEverywhere(
                    newFullName = newFullName,
                    newBase64 = selectedImageBase64  // null if not changed, handled inside
                )


                CurrentUserCache.clear()

                android.os.Handler(mainLooper).postDelayed({
                    FireStoreUtil.forceServerFetch = true
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                    finish()
                }, 3000)
            }
        }
    }
    private fun showUploadProgress(): androidx.appcompat.app.AlertDialog {
        val view = layoutInflater.inflate(R.layout.dialog_progress, null)
        return MaterialAlertDialogBuilder(this)
            .setView(view)
            .setCancelable(false)
            .create()
            .also { it.show() }
    }

    private fun setLoadingState(isLoading: Boolean) {
        btnSave.isEnabled = !isLoading
        btnSave.text = if (isLoading) "Saving..." else "Save"
        tilFullName.isEnabled = !isLoading
        tilUsername.isEnabled = !isLoading
        tilBio.isEnabled = !isLoading
        tilWebsite.isEnabled = !isLoading
    }
}