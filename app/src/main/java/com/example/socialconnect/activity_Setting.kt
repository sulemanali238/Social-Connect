package com.example.socialconnect

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial

class activity_Setting : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var switchNotifications: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        switchNotifications = findViewById(R.id.switchNotifications)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        findViewById<android.view.View>(R.id.settingEditProfile).setOnClickListener {
            val intent = Intent(this, activity_EditProfile::class.java)
            startActivity(intent)
        }

        findViewById<android.view.View>(R.id.settingChangePassword).setOnClickListener {
            val intent = Intent(this, activity_ChangePassword::class.java)
            startActivity(intent)

        }

        findViewById<android.view.View>(R.id.settingPrivacy).setOnClickListener {
            val intent = Intent(this, activity_Privacy::class.java)
            startActivity(intent)

        }
        findViewById<android.view.View>(R.id.settingLinkedEmail).setOnClickListener {
            val intent = Intent(this, activity_LinkedEmail::class.java)
            startActivity(intent)
        }

        findViewById<android.view.View>(R.id.settingSavedBookmarks).setOnClickListener {
            val intent = Intent(this, activity_SavedBookmark::class.java)
            startActivity(intent)
        }

        findViewById<android.view.View>(R.id.settingLikedPosts).setOnClickListener {
            val intent = Intent(this, activity_LikedPost::class.java)
            startActivity(intent)
        }

        findViewById<android.view.View>(R.id.settingHelp).setOnClickListener {
            val intent = Intent(this, activity_HelpSupport::class.java)
            startActivity(intent)
        }
        findViewById<android.view.View>(R.id.settingRateApp).setOnClickListener {
            val intent = Intent(this, activity_RateApp::class.java)
            startActivity(intent)
        }

        findViewById<android.view.View>(R.id.settingLogout).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out") { _, _ ->
                    AuthUtil.logout()
                    CurrentUserCache.clear() // clear cache on logout
                    val intent = Intent(this, activity_Launchers::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}