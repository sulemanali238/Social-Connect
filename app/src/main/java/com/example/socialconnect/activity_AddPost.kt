package com.example.socialconnect

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.hdodenhof.circleimageview.CircleImageView

class activity_AddPost : AppCompatActivity() {

    private lateinit var btnClose: ImageView
    private lateinit var btnPost: MaterialButton
    private lateinit var imgUserAvatar: CircleImageView
    private lateinit var tvPostUsername: TextView
    private lateinit var etCaption: EditText
    private lateinit var layoutImagePreview: FrameLayout
    private lateinit var imgPreview: ImageView
    private lateinit var btnRemoveImage: FrameLayout
    private lateinit var btnAddImage: LinearLayout
    private lateinit var btnCamera: LinearLayout
    private lateinit var tvCharCount: TextView

    private var selectedImageBase64: String? = null
    private val maxChars = 500

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                processImage(uri)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            imgPreview.setImageBitmap(bitmap)
            layoutImagePreview.visibility = android.view.View.VISIBLE
            // convert bitmap to base64
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            selectedImageBase64 = android.util.Base64.encodeToString(
                bytes, android.util.Base64.DEFAULT
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        initViews()
        loadCurrentUser()
        setupClickListeners()
        setupCharCounter()
    }

    private fun initViews() {
        btnClose = findViewById(R.id.btnClose)
        btnPost = findViewById(R.id.btnPost)
        imgUserAvatar = findViewById(R.id.imgUserAvatar)
        tvPostUsername = findViewById(R.id.tvPostUsername)
        etCaption = findViewById(R.id.etCaption)
        layoutImagePreview = findViewById(R.id.layoutImagePreview)
        imgPreview = findViewById(R.id.imgPreview)
        btnRemoveImage = findViewById(R.id.btnRemoveImage)
        btnAddImage = findViewById(R.id.btnAddImage)
        btnCamera = findViewById(R.id.btnCamera)
        tvCharCount = findViewById(R.id.tvCharCount)
    }

    private fun loadCurrentUser() {
        // show from cache instantly if available
        if (CurrentUserCache.isLoaded()) {
            tvPostUsername.text = CurrentUserCache.username
            if (CurrentUserCache.profileImageBase64.isNotEmpty()) {
                ImageUtils.loadBase64(
                    CurrentUserCache.profileImageBase64,
                    imgUserAvatar,
                    getDrawable(R.drawable.ic_avatar)
                )
            }
            return // no need to fetch from Firestore
        }

        // not cached yet — fetch once then cache
        FireStoreUtil.getCurrentUser { user ->
            if (user == null) return@getCurrentUser
            CurrentUserCache.populate(user)
            tvPostUsername.text = CurrentUserCache.username
            if (CurrentUserCache.profileImageBase64.isNotEmpty()) {
                ImageUtils.loadBase64(
                    CurrentUserCache.profileImageBase64,
                    imgUserAvatar,
                    getDrawable(R.drawable.ic_avatar)
                )
            }
        }
    }

    private fun setupClickListeners() {
        btnClose.setOnClickListener {
            val caption = etCaption.text.toString().trim()
            if (caption.isNotEmpty() || selectedImageBase64 != null) {
                // warn user about losing draft
                MaterialAlertDialogBuilder(this)
                    .setTitle("Discard Post?")
                    .setMessage("Your draft will be lost.")
                    .setPositiveButton("Discard") { _, _ -> finish() }
                    .setNegativeButton("Keep Editing", null)
                    .show()
            } else {
                finish()
            }
        }

        btnAddImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            galleryLauncher.launch(intent)
        }

        btnCamera.setOnClickListener {
            cameraLauncher.launch(null)
        }

        btnRemoveImage.setOnClickListener {
            selectedImageBase64 = null
            layoutImagePreview.visibility = android.view.View.GONE
            imgPreview.setImageDrawable(null)
        }

        btnPost.setOnClickListener {
            val caption = etCaption.text.toString().trim()

            if (caption.isEmpty() && selectedImageBase64 == null) {
                Toast.makeText(
                    this,
                    "Write something or add a photo",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (caption.length > maxChars) {
                Toast.makeText(
                    this,
                    "Caption too long",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            publishPost(caption)
        }
    }

    private fun setupCharCounter() {
        etCaption.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                tvCharCount.text = "$length/$maxChars"
                tvCharCount.setTextColor(
                    if (length > maxChars)
                        getColor(android.R.color.holo_red_light)
                    else
                        getColor(android.R.color.darker_gray)
                )
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun processImage(uri: android.net.Uri) {
        Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show()
        val base64 = ImageUtils.uriToBase64(this, uri)
        if (base64 != null) {
            selectedImageBase64 = base64
            ImageUtils.loadBase64(base64, imgPreview, null)
            layoutImagePreview.visibility = android.view.View.VISIBLE
        } else {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun publishPost(caption: String) {
        setLoadingState(true)

        FireStoreUtil.getCurrentUser { user ->
            if (user == null) {
                setLoadingState(false)
                return@getCurrentUser
            }

            val post = PostModel(
                userId = AuthUtil.currentUid,
                username = user.username,
                userProfileBase64 = user.profileImageBase64,
                caption = caption,
                imageBase64 = selectedImageBase64 ?: "",
                likeCount = 0,
                commentCount = 0,
                isLiked = false,
                isBookmarked = false,
                createdAt = System.currentTimeMillis()
            )

            FireStoreUtil.createPost(post) { success, _ ->
                setLoadingState(false)
                if (success) {
                    Toast.makeText(
                        this,
                        "Post published!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to publish post",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        btnPost.isEnabled = !isLoading
        btnPost.text = if (isLoading) "Posting..." else "Post"
        etCaption.isEnabled = !isLoading
        btnAddImage.isEnabled = !isLoading
        btnCamera.isEnabled = !isLoading
    }
}