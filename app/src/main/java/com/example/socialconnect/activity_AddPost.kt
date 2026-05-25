package com.example.socialconnect

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.hdodenhof.circleimageview.CircleImageView

class activity_AddPost : AppCompatActivity() {

    // ── Views ────────────────────────────────────────────────────────────────
    private lateinit var btnClose: ImageView
    private lateinit var btnPost: MaterialButton
    private lateinit var imgUserAvatar: CircleImageView
    private lateinit var tvPostFullName: TextView
    private lateinit var etCaption: EditText
    private lateinit var layoutImageTray: LinearLayout
    private lateinit var llImageThumbnails: LinearLayout
    private lateinit var btnAddImage: LinearLayout
    private lateinit var btnCamera: LinearLayout
    private lateinit var tvCharCount: TextView
    private lateinit var tvImageCountBadge: TextView

    // ── State ────────────────────────────────────────────────────────────────
    private val selectedImages = mutableListOf<String>()
    private val maxImages = 5
    private val maxChars = 500
    private var isEditMode = false
    private var editPostId = ""
    private var editCreatedAt = 0L
    private var editLikeCount = 0
    private var editCommentCount = 0

    // ── Gallery picker (multiple) ────────────────────────────────────────────
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult

        val data = result.data ?: return@registerForActivityResult
        val uris = mutableListOf<android.net.Uri>()

        if (data.clipData != null) {
            // Multiple selected
            val clip = data.clipData!!
            for (i in 0 until clip.itemCount) {
                uris.add(clip.getItemAt(i).uri)
            }
        } else {
            data.data?.let { uris.add(it) }
        }

        val remaining = maxImages - selectedImages.size
        if (remaining <= 0) {
            Toast.makeText(this, "Maximum $maxImages photos allowed", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        val toProcess = uris.take(remaining)
        if (uris.size > remaining) {
            Toast.makeText(
                this,
                "Only $remaining more photo(s) can be added",
                Toast.LENGTH_SHORT
            ).show()
        }

        toProcess.forEach { uri ->
            val base64 = ImageUtils.uriToBase64(this, uri)
            if (base64 != null) {
                selectedImages.add(base64)
            }
        }
        refreshImageTray()
    }

    // ── Camera ───────────────────────────────────────────────────────────────
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap == null) return@registerForActivityResult

        if (selectedImages.size >= maxImages) {
            Toast.makeText(this, "Maximum $maxImages photos allowed", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        val out = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
        val base64 = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
        selectedImages.add(base64)
        refreshImageTray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        editPostId = intent.getStringExtra("POST_ID") ?: ""

        initViews()
        loadCurrentUser()
        setupClickListeners()
        setupCharCounter()
        setupBackHandler()

        if (isEditMode) setupEditMode()
    }

    private fun initViews() {
        btnClose           = findViewById(R.id.btnClose)
        btnPost            = findViewById(R.id.btnPost)
        imgUserAvatar      = findViewById(R.id.imgUserAvatar)
        tvPostFullName         = findViewById(R.id.tvPostFullName)
        etCaption          = findViewById(R.id.etCaption)
        layoutImageTray    = findViewById(R.id.layoutImageTray)
        llImageThumbnails  = findViewById(R.id.llImageThumbnails)
        btnAddImage        = findViewById(R.id.btnAddImage)
        btnCamera          = findViewById(R.id.btnCamera)
        tvCharCount        = findViewById(R.id.tvCharCount)
        tvImageCountBadge  = findViewById(R.id.tvImageCountBadge)
    }

    private fun loadCurrentUser() {
        if (CurrentUserCache.isLoaded()) {
            tvPostFullName.text = CurrentUserCache.fullName
            if (CurrentUserCache.profileImageBase64.isNotEmpty()) {
                ImageUtils.loadBase64(
                    CurrentUserCache.profileImageBase64,
                    imgUserAvatar,
                    getDrawable(R.drawable.ic_avatar)
                )
            }
            return
        }
        FireStoreUtil.getCurrentUser { user ->
            if (user == null) return@getCurrentUser
            CurrentUserCache.populate(user)
            runOnUiThread {
                tvPostFullName.text = CurrentUserCache.fullName
                if (CurrentUserCache.profileImageBase64.isNotEmpty()) {
                    ImageUtils.loadBase64(
                        CurrentUserCache.profileImageBase64,
                        imgUserAvatar,
                        getDrawable(R.drawable.ic_avatar)
                    )
                }
            }
        }
    }

    private fun setupBackHandler() {
        // Hardware back / gesture
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                maybeShowDiscardDialog()
            }
        })
        // X button in top bar
        btnClose.setOnClickListener { maybeShowDiscardDialog() }
    }

    private fun hasDraft(): Boolean =
        etCaption.text.toString().trim().isNotEmpty() || selectedImages.isNotEmpty()

    private fun maybeShowDiscardDialog() {
        if (!hasDraft()) {
            finish()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Discard post?")
            .setMessage("Your draft will be lost if you go back.")
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Keep editing", null)
            .show()
    }

    // ── Click listeners ──────────────────────────────────────────────────────
    private fun setupClickListeners() {
        btnAddImage.setOnClickListener {
            if (selectedImages.size >= maxImages) {
                Toast.makeText(this, "Maximum $maxImages photos allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            galleryLauncher.launch(intent)
        }

        btnCamera.setOnClickListener {
            if (selectedImages.size >= maxImages) {
                Toast.makeText(this, "Maximum $maxImages photos allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            cameraLauncher.launch(null)
        }

        btnPost.setOnClickListener {
            val caption = etCaption.text.toString().trim()
            when {
                caption.isEmpty() && selectedImages.isEmpty() ->
                    Toast.makeText(this, "Write something or add a photo", Toast.LENGTH_SHORT).show()
                caption.length > maxChars ->
                    Toast.makeText(this, "Caption too long", Toast.LENGTH_SHORT).show()
                else -> publishPost(caption)
            }
        }
    }

    private fun setupCharCounter() {
        etCaption.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                val len = s?.length ?: 0
                tvCharCount.text = "$len/$maxChars"
                tvCharCount.setTextColor(
                    if (len > maxChars) getColor(android.R.color.holo_red_light)
                    else getColor(android.R.color.darker_gray)
                )
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun refreshImageTray() {
        llImageThumbnails.removeAllViews()

        if (selectedImages.isEmpty()) {
            layoutImageTray.visibility = View.GONE
            tvImageCountBadge.visibility = View.GONE
            return
        }

        layoutImageTray.visibility = View.VISIBLE
        tvImageCountBadge.visibility = View.VISIBLE
        tvImageCountBadge.text = "${selectedImages.size}/$maxImages photos"

        val sizePx  = resources.getDimensionPixelSize(R.dimen.thumbnail_size)   // 90dp
        val marginPx = resources.getDimensionPixelSize(R.dimen.thumbnail_margin) // 6dp
        val radiusPx = resources.getDimensionPixelSize(R.dimen.thumbnail_radius) // 10dp

        selectedImages.forEachIndexed { index, base64 ->
            // Container frame
            val frame = android.widget.FrameLayout(this).apply {
                layoutParams = ViewGroup.MarginLayoutParams(sizePx, sizePx).apply {
                    marginEnd = marginPx
                }
            }

            // Thumbnail image
            val imgView = ImageView(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP

                // Rounded corners via outline
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radiusPx.toFloat())
                    }
                }
                clipToOutline = true
            }

            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imgView.setImageBitmap(bmp)
            } catch (e: Exception) {
                imgView.setBackgroundColor(0xFFE2E8F0.toInt())
            }

            val removeBtn = TextView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    (sizePx * 0.32f).toInt(),
                    (sizePx * 0.32f).toInt(),
                    android.view.Gravity.TOP or android.view.Gravity.END
                )
                text = "✕"
                textSize = 10f
                gravity = android.view.Gravity.CENTER
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xCC000000.toInt())
                // Circular shape via outline
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
                clipToOutline = true
                elevation = 4f
                setOnClickListener {
                    selectedImages.removeAt(index)
                    refreshImageTray()
                }
            }

            val orderBadge = TextView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    (sizePx * 0.32f).toInt(),
                    (sizePx * 0.28f).toInt(),
                    android.view.Gravity.BOTTOM or android.view.Gravity.START
                ).apply { setMargins(4, 0, 0, 4) }
                text = "${index + 1}"
                textSize = 10f
                gravity = android.view.Gravity.CENTER
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xBB00BFA5.toInt())
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, 6f)
                    }
                }
                clipToOutline = true
            }

            frame.addView(imgView)
            frame.addView(removeBtn)
            frame.addView(orderBadge)
            llImageThumbnails.addView(frame)
        }

        if (selectedImages.size < maxImages) {
            val addMore = android.widget.FrameLayout(this).apply {
                layoutParams = ViewGroup.MarginLayoutParams(sizePx, sizePx).apply {
                    marginEnd = marginPx
                }
                setBackgroundColor(0xFFF1F5F9.toInt())
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radiusPx.toFloat())
                    }
                }
                clipToOutline = true
                isClickable = true
                isFocusable = true
                foreground = getDrawable(android.R.drawable.list_selector_background)
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_PICK).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    galleryLauncher.launch(intent)
                }
            }

            val plusIcon = TextView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.Gravity.CENTER
                )
                text = "+"
                textSize = 26f
                setTextColor(0xFF00BFA5.toInt())
                gravity = android.view.Gravity.CENTER
            }

            val countLabel = TextView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                ).apply { setMargins(0, 0, 0, 8) }
                text = "${maxImages - selectedImages.size} left"
                textSize = 9f
                setTextColor(0xFF94A3B8.toInt())
                gravity = android.view.Gravity.CENTER
            }

            addMore.addView(plusIcon)
            addMore.addView(countLabel)
            llImageThumbnails.addView(addMore)
        }
    }
    private fun setupEditMode() {
        btnPost.text = "Save"

        val caption = intent.getStringExtra("EDIT_CAPTION") ?: ""
        etCaption.setText(caption)
        etCaption.setSelection(caption.length)

        val images = intent.getStringArrayListExtra("EDIT_IMAGES") ?: arrayListOf()
        selectedImages.clear()
        selectedImages.addAll(images)
        refreshImageTray()

        FireStoreUtil.getPost(editPostId) { post ->
            if (post != null) {
                editCreatedAt    = post.createdAt
                editLikeCount    = post.likeCount
                editCommentCount = post.commentCount
            }
        }
    }

    private fun publishPost(caption: String) {
        setLoadingState(true)

        FireStoreUtil.getCurrentUser { user ->
            if (user == null) { runOnUiThread { setLoadingState(false) }; return@getCurrentUser }

            if (isEditMode) {
                val updates = mapOf(
                    "caption" to caption,
                    "images"  to selectedImages.toList(),
                    "imageBase64" to (selectedImages.firstOrNull() ?: "")
                )
                FireStoreUtil.db.collection("posts")
                    .document(editPostId)
                    .update(updates)
                    .addOnSuccessListener {
                        runOnUiThread {
                            setLoadingState(false)
                            Toast.makeText(this, "Post updated!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    .addOnFailureListener {
                        runOnUiThread {
                            setLoadingState(false)
                            Toast.makeText(this, "Failed to update post", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                val post = PostModel(
                    userId            = AuthUtil.currentUid,
                    username          = user.username,
                    fullName          = user.fullName,
                    userProfileBase64 = user.profileImageBase64,
                    caption           = caption,
                    images            = selectedImages.toList(),
                    imageBase64       = "",
                    likeCount         = 0,
                    commentCount      = 0,
                    isLiked           = false,
                    isBookmarked      = false,
                    createdAt         = System.currentTimeMillis()
                )
                FireStoreUtil.createPost(post) { success, _ ->
                    runOnUiThread {
                        setLoadingState(false)
                        if (success) {
                            Toast.makeText(this, "Post published!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to publish post", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        btnPost.isEnabled     = !isLoading
        btnPost.text          = if (isLoading) "Saving…" else if (isEditMode) "Save" else "Post"
        etCaption.isEnabled   = !isLoading
        btnAddImage.isEnabled = !isLoading
        btnCamera.isEnabled   = !isLoading
    }
}