package com.example.socialconnect.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.AuthUtil
import com.example.socialconnect.FireStoreUtil
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.R
import com.example.socialconnect.StoryModel
import com.example.socialconnect.adapters.StoryAdapter

class StoryFragment : Fragment() {

    private lateinit var rvStories: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    private val storyList = mutableListOf<StoryModel>()

    // image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                uploadStory(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_story, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        setupClickListeners(view)
        loadStories()
    }

    private fun setupRecyclerView(view: View) {
        rvStories = view.findViewById(R.id.rvStories)
        rvStories.layoutManager = LinearLayoutManager(requireContext())

        storyAdapter = StoryAdapter(storyList) { story ->
            markStorySeen(story)
        }
        rvStories.adapter = storyAdapter
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.btnAddStory).setOnClickListener {
            openImagePicker()
        }
    }

    private fun loadStories() {
        FireStoreUtil.getAllStories { stories ->
            if (!isAdded) return@getAllStories

            storyList.clear()
            storyList.addAll(stories)
            storyAdapter.notifyDataSetChanged()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }

    private fun uploadStory(uri: Uri) {
        // show loading
        Toast.makeText(requireContext(), "Uploading story...", Toast.LENGTH_SHORT).show()

        // convert image to base64
        val base64 = ImageUtils.uriToBase64(requireContext(), uri)
        if (base64 == null) {
            Toast.makeText(
                requireContext(),
                "Failed to process image",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // get current user info first
        FireStoreUtil.getCurrentUser { user ->
            if (user == null || !isAdded) return@getCurrentUser

            val story = StoryModel(
                userId = AuthUtil.currentUid,
                username = user.username,
                userProfileBase64 = user.profileImageBase64,
                imageBase64 = base64,
                seen = false,
                createdAt = System.currentTimeMillis()
            )

            FireStoreUtil.createStory(story) { success ->
                if (!isAdded) return@createStory
                if (success) {
                    Toast.makeText(
                        requireContext(),
                        "Story uploaded!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to upload story",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun markStorySeen(story: StoryModel) {
        val index = storyList.indexOfFirst { it.storyId == story.storyId }
        if (index != -1) {
            storyAdapter.markAsSeen(index)
        }

        // update seen in Firestore if it belongs to someone else
        if (story.userId != AuthUtil.currentUid && !story.seen) {
            FireStoreUtil.db.collection("stories")
                .document(story.storyId)
                .update("seen", true)
        }
    }
}