package com.example.socialconnect.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.AuthUtil
import com.example.socialconnect.CurrentUserCache
import com.example.socialconnect.FireStoreUtil
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.R
import com.example.socialconnect.adapters.ProfilePostAdapter
import com.example.socialconnect.PostModel
import com.example.socialconnect.activity_EditProfile
import com.example.socialconnect.activity_Setting
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {

    private lateinit var rvProfilePosts: RecyclerView
    private lateinit var profilePostsAdapter: ProfilePostAdapter
    private val profilePostList = mutableListOf<PostModel>()

    private lateinit var imgAvatar: CircleImageView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileUsername: TextView
    private lateinit var tvProfileBio: TextView
    private lateinit var tvPostCount: TextView
    private lateinit var tvPostCount2: TextView
    private lateinit var tvFollowerCount: TextView
    private lateinit var tvFollowingCount: TextView
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnEditAvatar: View
    private lateinit var btnSettings: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        loadUserData()
        loadUserPosts()
    }

    private fun initViews(view: View) {
        imgAvatar = view.findViewById(R.id.imgAvatar)
        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvProfileUsername = view.findViewById(R.id.tvProfileUsername)
        tvProfileBio = view.findViewById(R.id.tvProfileBio)
        tvPostCount = view.findViewById(R.id.tvPostCount)
        tvPostCount2 = view.findViewById(R.id.tvPostCount2)
        tvFollowerCount = view.findViewById(R.id.tvFollowerCount)
        tvFollowingCount = view.findViewById(R.id.tvFollowingCount)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnEditAvatar = view.findViewById(R.id.btnEditAvatar)
        btnSettings = view.findViewById(R.id.btnSettings)
        rvProfilePosts = view.findViewById(R.id.rvProfilePosts)
    }

    private fun setupRecyclerView() {
        rvProfilePosts.layoutManager = GridLayoutManager(requireContext(), 3)
        profilePostsAdapter = ProfilePostAdapter(profilePostList) { post -> }
        rvProfilePosts.adapter = profilePostsAdapter
    }

    private fun setupClickListeners() {
        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), activity_EditProfile::class.java))
        }

        btnEditAvatar.setOnClickListener {
            // image picker later
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), activity_Setting::class.java))
        }

        view?.findViewById<View>(R.id.btnFollowers)?.setOnClickListener {
            // open followers list later
        }

        view?.findViewById<View>(R.id.btnFollowing)?.setOnClickListener {
            // open following list later
        }
    }

    private fun loadUserData() {
        val uid = AuthUtil.currentUid

        // show cache instantly if available
        if (CurrentUserCache.isLoaded()) {
            tvProfileName.text = CurrentUserCache.fullName
            tvProfileUsername.text = "@${CurrentUserCache.username}"
            if (CurrentUserCache.profileImageBase64.isNotEmpty()) {
                ImageUtils.loadBase64(
                    CurrentUserCache.profileImageBase64,
                    imgAvatar,
                    requireContext().getDrawable(R.drawable.ic_avatar)
                )
            }
        }

        // always fetch fresh from Firestore in background
        FireStoreUtil.getUser(uid) { user ->
            if (user == null || !isAdded) return@getUser

            // update cache
            CurrentUserCache.populate(user)

            tvProfileName.text = user.fullName
            tvProfileUsername.text = "@${user.username}"
            tvProfileBio.text = user.bio.ifEmpty { "No bio yet." }
            tvPostCount.text = user.postCount.toString()
            tvPostCount2.text = "${user.postCount} posts"
            tvFollowerCount.text = user.followerCount.toString()
            tvFollowingCount.text = user.followingCount.toString()

            if (user.profileImageBase64.isNotEmpty()) {
                ImageUtils.loadBase64(
                    user.profileImageBase64,
                    imgAvatar,
                    requireContext().getDrawable(R.drawable.ic_avatar)
                )
            }
        }
    }

    private fun loadUserPosts() {
        val uid = AuthUtil.currentUid

        // show cached posts instantly if available
        if (CurrentUserCache.posts.isNotEmpty()) {
            profilePostList.clear()
            profilePostList.addAll(CurrentUserCache.posts)
            profilePostsAdapter.notifyDataSetChanged()
            tvPostCount.text = CurrentUserCache.posts.size.toString()
            tvPostCount2.text = "${CurrentUserCache.posts.size} posts"
        }

        // always fetch fresh in background
        FireStoreUtil.getUserPosts(uid) { posts ->
            if (!isAdded) return@getUserPosts

            // update cache
            CurrentUserCache.posts = posts.toMutableList()

            profilePostList.clear()
            profilePostList.addAll(posts)
            profilePostsAdapter.notifyDataSetChanged()
            tvPostCount.text = posts.size.toString()
            tvPostCount2.text = "${posts.size} posts"
        }
    }

    // called from outside when profile is updated
    fun refreshProfile() {
        CurrentUserCache.clear()
        loadUserData()
        loadUserPosts()
    }
}