package com.example.socialconnect

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import androidx.core.graphics.toColorInt
import com.example.socialconnect.adapters.StoryViewersAdapter

class StoryViewer : AppCompatActivity() {

    private lateinit var imgStoryFull: ImageView
    private lateinit var storyProgress: View
    private lateinit var progressTrack: View
    private lateinit var imgViewerAvatar: CircleImageView
    private lateinit var tvViewerFullName: TextView
    private lateinit var tvViewerTime: TextView
    private lateinit var btnMoreOptions: ImageView
    private lateinit var btnViewerBack: ImageView
    private lateinit var tapLeft: View
    private lateinit var tapRight: View
    private lateinit var tvStoryTextOverlay: TextView
    private lateinit var progressDividers: FrameLayout
    private lateinit var layoutViewers: android.widget.LinearLayout
    private lateinit var tvViewCount: TextView

    // Multi-story support: list of stories for this user
    private var stories: ArrayList<StoryBundle> = arrayListOf()
    private var currentIndex = 0
    private var dividersDrawn = false


    private var progressAnimator: ValueAnimator? = null
    private val storyDuration = 5000L
    private var isPaused = false

    data class StoryBundle(
        val storyId: String,
        val userId: String,
        val username: String,
        val fullName: String,
        val createdAt: Long,
        val imageBase64: String,
        val avatarBase64: String,
        val storyText: String,
        val viewerCount: Int = 0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Fix issue 2: prevent window resize when soft keyboard / popups appear ──
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_story_viewer)

        // Immersive fullscreen — hide status bar but don't shift layout when popup opens
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        initViews()
        loadStoriesFromIntent()
        setupClickListeners()
    }

    private fun initViews() {
        imgStoryFull       = findViewById(R.id.imgStoryFull)
        storyProgress      = findViewById(R.id.storyProgress)
        progressTrack      = findViewById(R.id.progressTrack)
        imgViewerAvatar    = findViewById(R.id.imgViewerAvatar)
        tvViewerFullName   = findViewById(R.id.tvViewerFullName)
        tvViewerTime       = findViewById(R.id.tvViewerTime)
        btnMoreOptions     = findViewById(R.id.btnMoreOptions)
        btnViewerBack      = findViewById(R.id.btnViewerBack)
        tapLeft            = findViewById(R.id.tapLeft)
        tapRight           = findViewById(R.id.tapRight)
        tvStoryTextOverlay = findViewById(R.id.tvStoryTextOverlay)
        progressDividers = findViewById(R.id.progressDividers)
        layoutViewers = findViewById(R.id.layoutViewers)
        tvViewCount   = findViewById(R.id.tvViewCount)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MULTI-STORY: receive a list (or fall back to single story from old intent)
    // ─────────────────────────────────────────────────────────────────────────
    private fun loadStoriesFromIntent() {
        val storyModels = StoryCacheHolder.stories
        currentIndex = StoryCacheHolder.startIndex

        if (storyModels.isNotEmpty()) {
            stories = ArrayList(storyModels.map {
                android.util.Log.d("StoryDebug", "storyId=${it.storyId} viewers=${it.viewers} viewerCount=${it.viewers.size}")
                StoryBundle(
                    storyId      = it.storyId,
                    userId       = it.userId,
                    username     = it.username,
                    fullName     = it.fullName,
                    createdAt    = it.createdAt,
                    imageBase64  = it.imageBase64,
                    avatarBase64 = it.userProfileBase64,
                    storyText    = it.storyText ?: "",
                    viewerCount  = it.viewers.size
                )
            })
        } else {
            finishWithAnimation()
            return
        }

        showStory(currentIndex)
    }
    /** Render the story at [index] and (re)start its progress bar segment. */
    private fun showStory(index: Int) {
        if (index < 0 || index >= stories.size) {
            finishWithAnimation()
            return
        }

        val s = stories[index]

        tvViewerFullName.text = s.fullName.ifEmpty { s.username }
        tvViewerTime.text     = getTimeAgo(s.createdAt)

        if (s.imageBase64.isNotEmpty())
            ImageUtils.loadBase64(s.imageBase64, imgStoryFull, null)

        if (s.avatarBase64.isNotEmpty())
            ImageUtils.loadBase64(s.avatarBase64, imgViewerAvatar, getDrawable(R.drawable.ic_avatar))

        if (s.storyText.isNotEmpty()) {
            tvStoryTextOverlay.visibility = View.VISIBLE
            tvStoryTextOverlay.text       = s.storyText
        } else {
            tvStoryTextOverlay.visibility = View.GONE
        }

        if (s.userId != AuthUtil.currentUid) {
            FireStoreUtil.markStoryViewed(s.storyId)
        }

// Show eye icon only for your own stories
        // ✅ Fix — use the actual viewerCount from the bundle
        if (s.userId == AuthUtil.currentUid) {
            layoutViewers.visibility = View.VISIBLE
            tvViewCount.text = s.viewerCount.toString()
            layoutViewers.setOnClickListener {
                progressAnimator?.pause()
                isPaused = true
                showViewersBottomSheet(s.storyId)
            }
        } else {
            layoutViewers.visibility = View.GONE
        }
        startProgress(index)
        drawSegmentDividers()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROGRESS BAR — split total track into equal segments, one per story
    // ─────────────────────────────────────────────────────────────────────────
    private fun startProgress(index: Int) {
        progressTrack.post {
            val trackWidth  = progressTrack.width
            val count       = stories.size
            // Each segment = its fair share of the full track width
            val segmentW    = trackWidth / count

            // The bar starts at the beginning of this segment
            val startX      = (index * segmentW)
            val endX        = ((index + 1) * segmentW)

            // Set bar to start of this segment immediately
            val params = storyProgress.layoutParams
            params.width = startX
            storyProgress.layoutParams = params

            progressAnimator?.cancel()
            progressAnimator = ValueAnimator.ofInt(startX, endX).apply {
                duration = storyDuration
                addUpdateListener { anim ->
                    val p = storyProgress.layoutParams
                    p.width = anim.animatedValue as Int
                    storyProgress.layoutParams = p
                }
                addListener(object : AnimatorListenerAdapter() {
                    private var cancelled = false
                    override fun onAnimationCancel(a: Animator) { cancelled = true }
                    override fun onAnimationEnd(a: Animator) {
                        if (!cancelled) goToNext()
                    }
                })
                start()
            }
        }
    }

    private fun goToNext() {
        if (currentIndex < stories.size - 1) {
            currentIndex++
            showStory(currentIndex)
        } else {
            finishWithAnimation()
        }
    }

    private fun goToPrev() {
        if (currentIndex > 0) {
            currentIndex--
            showStory(currentIndex)
        } else {
            // Already at first story — restart it
            showStory(currentIndex)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLICK / TOUCH LISTENERS
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupClickListeners() {
        btnViewerBack.setOnClickListener { finish() }

        tapLeft.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    progressAnimator?.pause(); isPaused = true; true
                }
                MotionEvent.ACTION_UP -> {
                    if (isPaused) {
                        isPaused = false
                        if (event.eventTime - event.downTime < 300) goToPrev()
                        else progressAnimator?.resume()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    isPaused = false; progressAnimator?.resume(); true
                }
                else -> false
            }
        }

        tapRight.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    progressAnimator?.pause(); isPaused = true; true
                }
                MotionEvent.ACTION_UP -> {
                    if (isPaused) {
                        isPaused = false
                        if (event.eventTime - event.downTime < 300) goToNext()
                        else progressAnimator?.resume()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    isPaused = false; progressAnimator?.resume(); true
                }
                else -> false
            }
        }

        // ── Fix issue 2: use PopupMenu which does NOT trigger window resize ──
        btnMoreOptions.setOnClickListener {
            progressAnimator?.pause()
            isPaused = true
            showMoreOptions()
        }
    }

    private fun showMoreOptions() {
        val popup = android.widget.PopupMenu(this, btnMoreOptions)
        val current = stories.getOrNull(currentIndex)

        if (current?.userId == AuthUtil.currentUid) {
            popup.menu.add(0, 1, 0, "Delete Story")
            popup.menu.add(0, 2, 1, "Save to Gallery")
        } else {
            popup.menu.add(0, 3, 0, "Report Story")
            popup.menu.add(0, 4, 1, "Mute this user")
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> deleteStory()
                2 -> saveToGallery()
                3 -> reportStory()
                4 -> muteUser()
            }
            true
        }
        popup.setOnDismissListener {
            isPaused = false
            progressAnimator?.resume()
        }

        // Get status bar height and offset the popup down by that amount
        try {
            val field = popup::class.java.getDeclaredField("mPopup")
            field.isAccessible = true
            val menuPopupHelper = field.get(popup)
            val classPopupHelper = Class.forName(menuPopupHelper::class.java.name)
            val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
            setForceIcons.invoke(menuPopupHelper, true)
        } catch (_: Exception) {}

        val statusBarHeight = run {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
        }
        popup.gravity = android.view.Gravity.END
        popup.show()

        // Shift popup window down by status bar height to compensate for NO_LIMITS offset
        try {
            val field = popup::class.java.getDeclaredField("mPopup")
            field.isAccessible = true
            val helper = field.get(popup)
            val getPopup = helper::class.java.getDeclaredMethod("getPopup")
            getPopup.isAccessible = true
            val listPopup = getPopup.invoke(helper) as? android.widget.ListPopupWindow
            listPopup?.verticalOffset = statusBarHeight
        } catch (_: Exception) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTIONS
    // ─────────────────────────────────────────────────────────────────────────
    private fun deleteStory() {
        val storyId = stories.getOrNull(currentIndex)?.storyId ?: return
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete Story")
            .setMessage("This story will be deleted for everyone. Sure?")
            .setPositiveButton("Delete") { _, _ ->
                FireStoreUtil.deleteStory(storyId) { success ->
                    android.widget.Toast.makeText(
                        this,
                        if (success) "Story deleted" else "Failed to delete",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    if (success) {
                        stories.removeAt(currentIndex)
                        if (stories.isEmpty()) finishWithAnimation()
                        else {
                            if (currentIndex >= stories.size) currentIndex = stories.size - 1
                            showStory(currentIndex)
                        }
                    } else {
                        isPaused = false; progressAnimator?.resume()
                    }
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                isPaused = false; progressAnimator?.resume()
            }
            .show()
    }

    private fun saveToGallery() {
        try {
            val bitmap = (imgStoryFull.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            if (bitmap != null) {
                android.provider.MediaStore.Images.Media.insertImage(
                    contentResolver, bitmap,
                    "Story_${System.currentTimeMillis()}", "Saved from Social Connect"
                )
                android.widget.Toast.makeText(this, "Saved to gallery", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Failed to save", android.widget.Toast.LENGTH_SHORT).show()
        }
        isPaused = false; progressAnimator?.resume()
    }

    private fun drawSegmentDividers() {
        if (dividersDrawn || stories.size <= 1) return
        dividersDrawn = true

        progressTrack.post {
            val trackWidth = progressTrack.width
            val count = stories.size
            progressDividers.removeAllViews()

            for (i in 1 until count) {
                val divider = View(this)
                val x = (trackWidth * i / count)
                val params = FrameLayout.LayoutParams(6, FrameLayout.LayoutParams.MATCH_PARENT)
                params.leftMargin = x
                divider.layoutParams = params
                divider.setBackgroundColor(android.graphics.Color.parseColor("#000000"))
                divider.alpha  = 1f
                progressDividers.addView(divider)
            }
        }
    }

    private fun showViewersBottomSheet(storyId: String) {
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheet_story_viewers, null)
        bottomSheet.setContentView(view)

        // Black background
        bottomSheet.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor("#1a1a1a".toColorInt())

        val tvCount = view.findViewById<TextView>(R.id.tvViewerCount)
        val rvViewers = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvViewers)
        val tvEmpty = view.findViewById<TextView>(R.id.tvNoViewers)

        rvViewers.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        FireStoreUtil.getStoryViewers(storyId) { users ->
            runOnUiThread {
                tvCount.text = "${users.size} views"
                if (users.isEmpty()) {
                    tvEmpty.visibility  = View.VISIBLE
                    rvViewers.visibility = View.GONE
                } else {
                    tvEmpty.visibility  = View.GONE
                    rvViewers.visibility = View.VISIBLE
                    rvViewers.adapter = StoryViewersAdapter(users)
                }
            }
        }

        bottomSheet.setOnDismissListener {
            isPaused = false
            progressAnimator?.resume()
        }
        bottomSheet.show()
    }

    private fun reportStory() {
        android.widget.Toast.makeText(this, "Story reported", android.widget.Toast.LENGTH_SHORT).show()
        isPaused = false; progressAnimator?.resume()
    }

    private fun muteUser() {
        android.widget.Toast.makeText(this, "User muted", android.widget.Toast.LENGTH_SHORT).show()
        finishWithAnimation()
    }

    private fun finishWithAnimation() {
        progressAnimator?.cancel()
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_down)
    }

    override fun onDestroy() {
        super.onDestroy()
        progressAnimator?.cancel()
        StoryCacheHolder.stories = emptyList()
    }

    private fun getTimeAgo(time: Long): String {
        val diff = System.currentTimeMillis() - time
        return when {
            diff < 60_000        -> "Just now"
            diff < 3_600_000     -> "${diff / 60_000}m ago"
            diff < 86_400_000    -> "${diff / 3_600_000}h ago"
            else                 -> "${diff / 86_400_000}d ago"
        }
    }
}