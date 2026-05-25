package com.example.socialconnect

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class StoryEditorActivity : AppCompatActivity() {

    private lateinit var imgStoryPreview: ImageView
    private lateinit var etStoryCaption: TextInputEditText
    private lateinit var btnShareStory: FrameLayout
    private lateinit var btnEditorBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_story_editor)

        imgStoryPreview = findViewById(R.id.imgStoryPreview)
        etStoryCaption  = findViewById(R.id.etStoryCaption)
        btnShareStory   = findViewById(R.id.btnShareStory)
        btnEditorBack   = findViewById(R.id.btnEditorBack)

        // Load image
        val uriString = intent.getStringExtra("imageUri")
        val uri = uriString?.let { Uri.parse(it) }
        if (uri != null) {
            imgStoryPreview.setImageURI(uri)
        } else {
            Toast.makeText(this, "Image not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnEditorBack.setOnClickListener { finish() }

        btnShareStory.setOnClickListener { postStory(uri) }
    }

    private fun postStory(uri: Uri) {
        btnShareStory.isEnabled = false

        val caption = etStoryCaption.text?.toString()?.trim() ?: ""

        val base64 = ImageUtils.uriToBase64(this, uri)
        if (base64 == null) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
            btnShareStory.isEnabled = true
            return
        }

        FireStoreUtil.getCurrentUser { user ->
            if (user == null) {
                runOnUiThread {
                    Toast.makeText(this, "Could not load profile", Toast.LENGTH_SHORT).show()
                    btnShareStory.isEnabled = true
                }
                return@getCurrentUser
            }

            val story = StoryModel(
                userId            = AuthUtil.currentUid,
                username          = user.username,
                fullName          = user.fullName,
                userProfileBase64 = user.profileImageBase64,
                imageBase64       = base64,
                storyText         = caption,
                seen              = false,
                createdAt         = System.currentTimeMillis()
            )

            FireStoreUtil.createStory(story) { success ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        if (success) "Story shared!" else "Failed to share",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (success) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        btnShareStory.isEnabled = true
                    }
                }
            }
        }
    }
}