package com.example.socialconnect

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*

class activity_Setting : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var imgProfilePic: de.hdodenhof.circleimageview.CircleImageView

    private lateinit var tvAvatarInitials: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileHandle: TextView
    private lateinit var tvBookmarkCount: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        initViews()
        loadUserProfile()
        setupClickListeners()
        loadBookmarkCount()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        switchNotifications = findViewById(R.id.switchNotifications)
        imgProfilePic = findViewById(R.id.imgSettingProfilePic)
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileHandle = findViewById(R.id.tvProfileHandle)
        tvBookmarkCount = findViewById(R.id.tvBookmarkCount)
        swipeRefresh = findViewById(R.id.swipeRefreshSettings)

    }

    private fun loadUserProfile(forceRefresh: Boolean = false) {
        if (!forceRefresh && CurrentUserCache.isLoaded()) {
            renderProfile()
        } else {
            FireStoreUtil.getCurrentUser { user ->
                user?.let {
                    CurrentUserCache.populate(it)
                    runOnUiThread { renderProfile() }
                }
            }
        }
    }

    private fun renderProfile() {
        tvProfileName.text = CurrentUserCache.fullName
        tvProfileHandle.text = buildUsernameJoinedText()

        if (CurrentUserCache.profileImageBase64.isNotEmpty()) {
            ImageUtils.loadBase64(
                base64 = CurrentUserCache.profileImageBase64,
                imageView = imgProfilePic
            )
            imgProfilePic.visibility = View.VISIBLE
            tvAvatarInitials.visibility = View.GONE
        } else {
            val initials = CurrentUserCache.fullName
                .split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
            tvAvatarInitials.text = initials.ifEmpty { "?" }
            imgProfilePic.visibility = View.GONE
            tvAvatarInitials.visibility = View.VISIBLE
        }
    }

    private fun buildUsernameJoinedText(): String {
        val username = "@${CurrentUserCache.username}"
        return if (CurrentUserCache.joinedAt > 0L) {
            val date = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                .format(Date(CurrentUserCache.joinedAt))
            "$username · Joined $date"
        } else {
            username
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        findViewById<View>(R.id.settingEditProfile).setOnClickListener {
            startActivity(Intent(this, activity_EditProfile::class.java))
        }
        findViewById<View>(R.id.settingChangePassword).setOnClickListener {
            startActivity(Intent(this, activity_ChangePassword::class.java))
        }
        findViewById<View>(R.id.settingPrivacy).setOnClickListener {
            startActivity(Intent(this, activity_Privacy::class.java))
        }
        findViewById<View>(R.id.settingLinkedEmail).setOnClickListener {
            startActivity(Intent(this, activity_LinkedEmail::class.java))
        }
        findViewById<View>(R.id.settingSavedBookmarks).setOnClickListener {
            startActivity(Intent(this, activity_SavedBookmark::class.java))
        }
        findViewById<View>(R.id.settingLikedPosts).setOnClickListener {
            startActivity(Intent(this, activity_LikedPost::class.java))
        }
        findViewById<View>(R.id.settingHelp).setOnClickListener {
            startActivity(Intent(this, activity_HelpSupport::class.java))
        }
        findViewById<View>(R.id.settingRateApp).setOnClickListener {
            startActivity(Intent(this, activity_RateApp::class.java))
        }
        findViewById<View>(R.id.settingAccountInsights).setOnClickListener {
            startActivity(Intent(this, AccountInsights::class.java))
        }

        findViewById<View>(R.id.settingAbout).setOnClickListener {
            showAboutBottomSheet()
        }
        findViewById<View>(R.id.settingLogout).setOnClickListener {
            showLogoutDialog()
        }

        findViewById<View>(R.id.settingDeleteAccount).setOnClickListener {
            showDeleteAccountDialog()
        }
        swipeRefresh.setColorSchemeResources(R.color.teal_green)
        swipeRefresh.setOnRefreshListener {
            loadUserProfile(forceRefresh = true)
            loadBookmarkCount()
            swipeRefresh.postDelayed({ swipeRefresh.isRefreshing = false }, 1500)
        }
    }

    private fun showAboutBottomSheet() {
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.RoundedBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_about, null)
        bottomSheet.setContentView(view)
        bottomSheet.show()
    }
    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ ->
                FireStoreUtil.clearFcmToken {
                    AuthUtil.logout()
                    CurrentUserCache.clear()
                    val intent = Intent(this, activity_Launchers::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Account")
            .setMessage("This will permanently delete your account, posts, stories, and all your data. This cannot be undone.")
            .setPositiveButton("Delete Permanently") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Deleting Account")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        FireStoreUtil.deleteAccount { success, error ->
            runOnUiThread {
                loadingDialog.dismiss()
                if (success) {
                    CurrentUserCache.clear()
                    val intent = Intent(this, activity_Launchers::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Error")
                        .setMessage("Failed to delete account: $error\n\nPlease try again.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }
    private fun loadBookmarkCount() {
        FireStoreUtil.getBookmarkedPostIds { postIds ->
            if (postIds.isEmpty()) {
                runOnUiThread { tvBookmarkCount.text = "0" }
                return@getBookmarkedPostIds
            }
            FireStoreUtil.getBookMarkedPosts(postIds) { posts ->
                runOnUiThread { tvBookmarkCount.text = posts.size.toString() }
            }
        }
    }
}