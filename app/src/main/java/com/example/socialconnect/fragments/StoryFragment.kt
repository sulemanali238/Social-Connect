package com.example.socialconnect.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.AuthUtil
import com.example.socialconnect.CurrentUserCache
import com.example.socialconnect.FireStoreUtil
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.R
import com.example.socialconnect.SeenStoryCache
import com.example.socialconnect.StoryCacheHolder
import com.example.socialconnect.StoryEditorActivity
import com.example.socialconnect.StoryModel
import com.example.socialconnect.StoryViewer
import com.example.socialconnect.adapters.StoryAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream
import java.io.File

class StoryFragment : Fragment() {

    private lateinit var rvStories: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    private lateinit var imgMyAvatar: CircleImageView
    private lateinit var tvMyStoryStatus: TextView
    private lateinit var storyLoadingBar: View
    private lateinit var layoutStoryLoading: View
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout

    private var hasLoadedOnce = false
    private lateinit var tvRecentLabel: TextView

    private lateinit var layoutEmptyStories: View
    private lateinit var myStoryRingUnseen: View
    private lateinit var myStoryRingSeen: View
    private lateinit var myStoryRingDashed: View
    private lateinit var badgePlus: View
    private lateinit var fabPhotoStory: FloatingActionButton
    private val userMap = mutableMapOf<String, com.example.socialconnect.UserModel>()

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { openEditor(it) }
        }
    }
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap == null) return@registerForActivityResult
        try {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            val tempFile = File(requireContext().cacheDir, "story_cam_${System.currentTimeMillis()}.jpg")
            tempFile.writeBytes(out.toByteArray())
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                tempFile
            )
            openEditor(uri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to process photo", Toast.LENGTH_SHORT).show()
        }
    }

    private val groupedStories = mutableListOf<List<StoryModel>>()
    private var myStories: List<StoryModel> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_story, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        setupClickListeners(view)
        loadMyAvatar()
        loadStories()
    }

    override fun onResume() {
        super.onResume()
        loadStories()
    }

    private fun initViews(view: View) {
        rvStories          = view.findViewById(R.id.rvStories)
        imgMyAvatar        = view.findViewById(R.id.imgMyAvatar)
        tvMyStoryStatus    = view.findViewById(R.id.tvMyStoryStatus)
        storyLoadingBar = view.findViewById(R.id.storyLoadingBar)
        layoutStoryLoading = view.findViewById(R.id.layoutStoryLoading)
        swipeRefresh = view.findViewById(R.id.swipeRefreshStory)
        tvRecentLabel      = view.findViewById(R.id.tvRecentLabel)
        layoutEmptyStories = view.findViewById(R.id.layoutEmptyStories)
        myStoryRingUnseen  = view.findViewById(R.id.myStoryRingUnseen)
        myStoryRingSeen    = view.findViewById(R.id.myStoryRingSeen)
        myStoryRingDashed  = view.findViewById(R.id.myStoryRingDashed)
        badgePlus          = view.findViewById(R.id.badgePlus)
        fabPhotoStory      = view.findViewById(R.id.fabPhotoStory)
    }

    private fun setupRecyclerView() {
        rvStories.layoutManager = LinearLayoutManager(requireContext())
        storyAdapter = StoryAdapter(
            stories = mutableListOf(),
            onClick = { _, position -> openGroupedStoryViewer(position) }
        )
        rvStories.adapter = storyAdapter
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.layoutMyStory).setOnClickListener {
            if (myStories.isNotEmpty()) openMyStoryViewer()
            else showStoryOptions()
        }
        fabPhotoStory.setOnClickListener {
            showStoryOptions()
        }
    }

    private fun loadMyAvatar() {
        if (CurrentUserCache.isLoaded() && CurrentUserCache.profileImageBase64.isNotEmpty()) {
            ImageUtils.loadBase64(
                CurrentUserCache.profileImageBase64,
                imgMyAvatar,
                requireContext().getDrawable(R.drawable.ic_avatar)
            )
            return
        }
        FireStoreUtil.getCurrentUser { user ->
            if (user == null || !isAdded) return@getCurrentUser
            CurrentUserCache.populate(user)
            if (user.profileImageBase64.isNotEmpty())
                ImageUtils.loadBase64(
                    user.profileImageBase64,
                    imgMyAvatar,
                    requireContext().getDrawable(R.drawable.ic_avatar)
                )
        }
    }

    private fun loadStories() {
        if (!hasLoadedOnce) {
            layoutStoryLoading.visibility = View.VISIBLE
        }

        FireStoreUtil.getAllStories { stories ->
            if (!isAdded) return@getAllStories

            val fresh = stories.filter { System.currentTimeMillis() - it.createdAt < 86_400_000L }
            SeenStoryCache.clearOldStories(requireContext(), fresh.map { it.storyId })

            myStories = fresh.filter { it.userId == AuthUtil.currentUid }.sortedBy { it.createdAt }

            val grouped = fresh
                .filter { it.userId != AuthUtil.currentUid }
                .groupBy { it.userId }
                .values
                .map { it.sortedBy { s -> s.createdAt } }
                .sortedByDescending { it.last().createdAt }

            groupedStories.clear()
            groupedStories.addAll(grouped)

                val representatives = grouped.map { group ->
                val allSeen = group.all { SeenStoryCache.isSeen(requireContext(), it.storyId) }
                group.last().copy(seen = allSeen)
            }.toMutableList()

            hasLoadedOnce = true
            layoutStoryLoading.visibility = View.GONE
            updateMyStorySection(myStories)

            if (representatives.isEmpty()) {
                storyAdapter.replaceAll(mutableListOf())
                rvStories.visibility = View.GONE
                tvRecentLabel.visibility = View.GONE
                layoutEmptyStories.visibility = View.VISIBLE
                return@getAllStories
            }

            rvStories.visibility = View.VISIBLE
            tvRecentLabel.visibility = View.VISIBLE
            layoutEmptyStories.visibility = View.GONE
            storyAdapter.replaceAll(representatives)

            userMap.clear()
            val uniqueUserIds = (grouped.map { it.first().userId } + AuthUtil.currentUid).distinct()

            uniqueUserIds.forEach { uid ->
                FireStoreUtil.getUser(uid) { user ->
                    if (user == null || !isAdded) return@getUser
                    userMap[uid] = user

                    val position = grouped.indexOfFirst { it.first().userId == uid }
                    if (position != -1) {
                        storyAdapter.enrichStory(
                            position,
                            fullName = user.fullName,
                            profileBase64 = user.profileImageBase64
                        )
                    }
                    if (uid == AuthUtil.currentUid) {
                        updateMyStorySection(myStories)
                    }
                }
            }
        }
    }
    private fun updateMyStorySection(myStories: List<StoryModel>) {
        if (myStories.isNotEmpty()) {
            val latest = myStories.last()
            myStoryRingDashed.visibility = View.GONE
            badgePlus.visibility         = View.GONE
            val seen = SeenStoryCache.isSeen(requireContext(), latest.storyId)
            myStoryRingUnseen.visibility = if (seen) View.GONE else View.VISIBLE
            myStoryRingSeen.visibility   = if (seen) View.VISIBLE else View.GONE

            ImageUtils.loadBase64(
                latest.imageBase64,
                imgMyAvatar,
                requireContext().getDrawable(R.drawable.ic_avatar)
            )
            tvMyStoryStatus.text = "Tap to view your story"
        } else {
            myStoryRingUnseen.visibility = View.GONE
            myStoryRingSeen.visibility   = View.GONE
            myStoryRingDashed.visibility = View.VISIBLE
            badgePlus.visibility         = View.VISIBLE
            if (CurrentUserCache.profileImageBase64.isNotEmpty())
                ImageUtils.loadBase64(
                    CurrentUserCache.profileImageBase64,
                    imgMyAvatar,
                    requireContext().getDrawable(R.drawable.ic_avatar)
                )
            tvMyStoryStatus.text = "Tap to add story"
        }
    }

    private fun showStoryOptions() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_story_options, null)

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.optCamera).setOnClickListener {
            dialog.dismiss()
            cameraLauncher.launch(null)
        }

        dialogView.findViewById<View>(R.id.optGallery).setOnClickListener {
            dialog.dismiss()
            galleryLauncher.launch(Intent(Intent.ACTION_PICK).apply { type = "image/*" })
        }

        dialogView.findViewById<View>(R.id.optCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openEditor(uri: Uri) {
        startActivity(
            Intent(requireContext(), StoryEditorActivity::class.java)
                .putExtra("imageUri", uri.toString())
        )
    }

    private fun openGroupedStoryViewer(position: Int) {
        val group = groupedStories.getOrNull(position) ?: return
        group.forEach { SeenStoryCache.markSeen(requireContext(), it.storyId) }
        storyAdapter.markAsSeen(position)

        val user = userMap[group.first().userId]
        val freshGroup = group.map { story ->
            story.copy(
                fullName          = user?.fullName ?: story.fullName,
                username          = user?.username ?: story.username,
                userProfileBase64 = user?.profileImageBase64 ?: story.userProfileBase64
            )
        }

        StoryCacheHolder.stories = freshGroup
        StoryCacheHolder.startIndex = 0

        startActivity(Intent(requireContext(), StoryViewer::class.java))
        requireActivity().overridePendingTransition(R.anim.slide_up, 0)
    }

    private fun openMyStoryViewer() {
        myStories.forEach { SeenStoryCache.markSeen(requireContext(), it.storyId) }

        val user = userMap[AuthUtil.currentUid]
        val freshStories = myStories.map { story ->
            story.copy(
                fullName          = user?.fullName ?: story.fullName,
                username          = user?.username ?: story.username,
                userProfileBase64 = user?.profileImageBase64 ?: story.userProfileBase64
            )
        }

        StoryCacheHolder.stories = freshStories
        StoryCacheHolder.startIndex = 0

        startActivity(Intent(requireContext(), StoryViewer::class.java))
        requireActivity().overridePendingTransition(R.anim.slide_up, 0)
    }
}