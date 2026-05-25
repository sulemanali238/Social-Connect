package com.example.socialconnect.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.*
import com.example.socialconnect.adapters.PostAdapter
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {

    private lateinit var rvProfilePosts: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val profilePostList = mutableListOf<PostModel>()
    private val pendingCounterSet = mutableSetOf<String>()

    private lateinit var imgAvatar: CircleImageView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileUsername: TextView
    private lateinit var tvProfileBio: TextView
    private lateinit var tvJoinedDate: TextView
    private lateinit var tvPostCount: TextView
    private lateinit var tvFollowerCount: TextView
    private lateinit var tvFollowingCount: TextView

    // My-profile buttons
    private lateinit var layoutMyButtons: View
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnShareProfile: MaterialButton

    // Other-user buttons
    private lateinit var layoutOtherButtons: View
    private lateinit var btnFollowAction: MaterialButton
    private lateinit var btnMessage: MaterialButton

    // App bar
    private lateinit var btnBack: ImageView
    private lateinit var btnMoreOptions: ImageView

    // Layouts
    private lateinit var layoutJoinedDate: View
    private lateinit var layoutNoPosts: View

    // Shimmer
    private lateinit var shimmerProfile: ShimmerFrameLayout
    private lateinit var shimmerPosts: ShimmerFrameLayout
    private lateinit var layoutProfileContent: View
    private lateinit var layoutUsernameChip: View

    private lateinit var postHandler: PostInteractionHandler
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout


    private var targetUserId: String? = null
    private var isMyProfile: Boolean = true
    private var isFollowingUser: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetUserId = arguments?.getString("USER_ID")
        isMyProfile = targetUserId == null || targetUserId == AuthUtil.currentUid
    }

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

        swipeRefresh = view.findViewById(R.id.swipeRefreshProfile)
        swipeRefresh.setColorSchemeResources(R.color.teal_green)
        swipeRefresh.setOnRefreshListener {
            profilePostList.clear()
            postAdapter.notifyDataSetChanged()
            loadUserData()
            loadUserPosts()
            swipeRefresh.postDelayed({
                if (isAdded) swipeRefresh.isRefreshing = false
            }, 2000)
        }
    }

    private fun initViews(view: View) {
        imgAvatar         = view.findViewById(R.id.imgAvatar)
        tvProfileName     = view.findViewById(R.id.tvProfileName)
        tvProfileUsername = view.findViewById(R.id.tvProfileUsername)
        layoutUsernameChip = view.findViewById(R.id.layoutUsernameChip)
        tvProfileBio      = view.findViewById(R.id.tvProfileBio)
        tvJoinedDate      = view.findViewById(R.id.tvJoinedDate)
        layoutJoinedDate  = view.findViewById(R.id.layoutJoinedDate)
        layoutNoPosts     = view.findViewById(R.id.layoutNoPosts)

        btnBack           = view.findViewById(R.id.btnBack)
        btnMoreOptions    = view.findViewById(R.id.btnMoreOptions)

        layoutMyButtons   = view.findViewById(R.id.layoutMyButtons)
        btnEditProfile    = view.findViewById(R.id.btnEditProfile)
        btnShareProfile   = view.findViewById(R.id.btnShareProfile)

        layoutOtherButtons = view.findViewById(R.id.layoutOtherButtons)
        btnFollowAction   = view.findViewById(R.id.btnFollowAction)
        btnMessage        = view.findViewById(R.id.btnMessage)

        shimmerProfile       = view.findViewById(R.id.shimmerProfile)
        shimmerPosts         = view.findViewById(R.id.shimmerPosts)
        layoutProfileContent = view.findViewById(R.id.layoutProfileContent)

        // Start profile shimmer immediately
        shimmerProfile.startShimmer()
        shimmerProfile.visibility       = View.VISIBLE
        layoutProfileContent.visibility = View.GONE

        val tvAppBarTitle = view.findViewById<TextView>(R.id.tvAppBarTitle)

        if (isMyProfile) {
            btnBack.visibility            = View.GONE
            tvAppBarTitle.visibility      = View.VISIBLE
            layoutMyButtons.visibility    = View.VISIBLE
            layoutOtherButtons.visibility = View.GONE
        } else {
            btnBack.visibility            = View.VISIBLE
            tvAppBarTitle.visibility      = View.VISIBLE
            layoutMyButtons.visibility    = View.GONE
            layoutOtherButtons.visibility = View.VISIBLE
            checkFollowStatus()
        }

        tvPostCount      = view.findViewById(R.id.tvPostCount)
        tvFollowerCount  = view.findViewById(R.id.tvFollowerCount)
        tvFollowingCount = view.findViewById(R.id.tvFollowingCount)
    }

    private fun setupStat(includeView: View, label: String, assignTo: (TextView) -> Unit) {
        includeView.findViewById<TextView>(R.id.tvStatLabel).text = label
        assignTo(includeView.findViewById(R.id.tvStatValue))
    }

    private fun showProfileContent() {
        if (!isAdded) return
        shimmerProfile.stopShimmer()
        shimmerProfile.visibility       = View.GONE
        layoutProfileContent.visibility = View.VISIBLE
        // Now start posts shimmer
        shimmerPosts.startShimmer()
        shimmerPosts.visibility = View.VISIBLE
    }

    private fun showPostsContent() {
        if (!isAdded) return
        shimmerPosts.stopShimmer()
        shimmerPosts.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        rvProfilePosts = requireView().findViewById(R.id.rvProfilePosts)
        rvProfilePosts.layoutManager = LinearLayoutManager(requireContext())
        rvProfilePosts.isNestedScrollingEnabled = false

        postAdapter = PostAdapter(
            posts           = profilePostList,
            onLikeClick     = { post, position -> postHandler.handleLike(post, position) },
            onCommentClick  = { post -> postHandler.handleComment(post) },
            onBookmarkClick = { post, position -> postHandler.handleBookmark(post, position) },
            onShareClick    = { post -> postHandler.handleShare(post) },
            onAvatarClick   = { _ -> },
            onDeleteClick   = { post ->
                postHandler.showDeleteDialog(post) { deleted ->
                    val idx = profilePostList.indexOfFirst { it.postId == deleted.postId }
                    if (idx != -1) {
                        profilePostList.removeAt(idx)
                        postAdapter.notifyItemRemoved(idx)
                        tvPostCount.text = profilePostList.size.toString()
                        if (profilePostList.isEmpty()) {
                            layoutNoPosts.visibility  = View.VISIBLE
                            rvProfilePosts.visibility = View.GONE
                        }
                    }
                }
            }
        )
        rvProfilePosts.adapter = postAdapter

// Init handler after adapter is ready
        postHandler = PostInteractionHandler(this, profilePostList, postAdapter, pendingCounterSet)
    }

    private fun setupClickListeners() {

        // Back arrow
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Three-dots menu
        btnMoreOptions.setOnClickListener {
            showOptionsDialog()
        }

        // Edit Profile (my profile)
        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), activity_EditProfile::class.java))
        }

        // Share Profile (my profile)
        btnShareProfile.setOnClickListener {
            shareProfile()
        }

        // Follow / Unfollow (other user)
        btnFollowAction.setOnClickListener {
            toggleFollow()
        }

        btnMessage.setOnClickListener {
            val uid  = targetUserId ?: return@setOnClickListener
            val name = tvProfileName.text.toString()
            val profileImg = ""

            (activity as? activity_Main)?.openChatMessage(uid, name, profileImg)
        }
        layoutUsernameChip.setOnClickListener {
            startActivity(Intent(requireContext(), DiscoverActivity::class.java))
    }
        }

    private fun showOptionsDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_profile_options, null)

        val popupWindow = android.widget.PopupWindow(
            dialogView,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 24f
            isOutsideTouchable = true
            isFocusable = true
            setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            )
        }

        // ── Wire up views ─────────────────────────────────────────────
        val optionSettings    = dialogView.findViewById<View>(R.id.optionSettings)
        val optionShare       = dialogView.findViewById<View>(R.id.optionShareProfile)
        val optionCopyLink    = dialogView.findViewById<View>(R.id.optionCopyLink)
        val divider           = dialogView.findViewById<View>(R.id.dividerOptions)
        val optionReport      = dialogView.findViewById<View>(R.id.optionReport)
        val optionBlock       = dialogView.findViewById<View>(R.id.optionBlock)

        if (isMyProfile) {
            // My profile: Settings, Share, Copy Link
            optionSettings.visibility = View.VISIBLE
            optionShare.visibility    = View.VISIBLE
            optionCopyLink.visibility = View.VISIBLE

            optionSettings.setOnClickListener {
                popupWindow.dismiss()
                startActivity(Intent(requireContext(), activity_Setting::class.java))
            }
            optionShare.setOnClickListener {
                popupWindow.dismiss()
                shareProfile()
            }
            optionCopyLink.setOnClickListener {
                popupWindow.dismiss()
                copyProfileLink()
            }
        } else {
            // Other profile: Share, Copy Link + divider + Report, Block
            optionShare.visibility    = View.VISIBLE
            optionCopyLink.visibility = View.VISIBLE
            divider.visibility        = View.VISIBLE
            optionReport.visibility   = View.VISIBLE
            optionBlock.visibility    = View.VISIBLE

            optionShare.setOnClickListener {
                popupWindow.dismiss()
                shareProfile()
            }
            optionCopyLink.setOnClickListener {
                popupWindow.dismiss()
                copyProfileLink()
            }
            optionReport.setOnClickListener {
                popupWindow.dismiss()
                Toast.makeText(requireContext(), "Report submitted to admin", Toast.LENGTH_SHORT).show()
            }
            optionBlock.setOnClickListener {
                popupWindow.dismiss()
                Toast.makeText(requireContext(), "Coming Soon", Toast.LENGTH_SHORT).show()
            }
        }

        // ── Anchor to btnMoreOptions, show below it aligned to end ────
        val popupWidthPx = 220.dpToPx(requireContext())
        popupWindow.width = popupWidthPx
        popupWindow.showAsDropDown(
            btnMoreOptions,
            -(popupWidthPx - btnMoreOptions.width),
            14
        )
    }

    private fun Int.dpToPx(context: android.content.Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    private fun copyProfileLink() {
        val username = tvProfileUsername.text.toString()
        val clipboard = requireContext()
            .getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
        clipboard.setPrimaryClip(
            android.content.ClipData.newPlainText("profile_link", "socialconnect.app/$username")
        )
        Toast.makeText(requireContext(), "Link copied!", Toast.LENGTH_SHORT).show()
    }

    private fun shareProfile() {
        val username = tvProfileUsername.text.toString()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out $username on SocialConnect!")
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    }

    private fun checkFollowStatus() {
        targetUserId?.let { uid ->
            FireStoreUtil.isFollowing(uid) { following ->
                if (!isAdded) return@isFollowing
                isFollowingUser = following
                updateFollowButtonUI()
            }
        }
    }

    private fun updateFollowButtonUI() {
        if (!isAdded) return
        if (isFollowingUser) {
            btnFollowAction.text = "Following"
            btnFollowAction.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    resources.getColor(R.color.grey_nav, null))
            btnFollowAction.setTextColor(resources.getColor(R.color.text_dark, null))
        } else {
            btnFollowAction.text = "Follow"
            btnFollowAction.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    resources.getColor(R.color.teal_green, null))
            btnFollowAction.setTextColor(resources.getColor(android.R.color.white, null))
        }
    }

    private fun toggleFollow() {
        val uid = targetUserId ?: return
        btnFollowAction.isEnabled = false

        if (isFollowingUser) {
            FireStoreUtil.unfollowUser(uid) { success ->
                btnFollowAction.isEnabled = true
                if (success) {
                    isFollowingUser = false
                    updateFollowButtonUI()
                    val current = tvFollowerCount.text.toString().toIntOrNull() ?: 0
                    tvFollowerCount.text = (current - 1).toString()
                }
            }
        } else {
            FireStoreUtil.followUser(uid) { success ->
                btnFollowAction.isEnabled = true
                if (success) {
                    isFollowingUser = true
                    updateFollowButtonUI()
                    val current = tvFollowerCount.text.toString().toIntOrNull() ?: 0
                    tvFollowerCount.text = (current + 1).toString()

                    FireStoreUtil.sendNotification(requireContext(),
                        NotificationModel(
                            toUserId              = uid,
                            fromUsername          = CurrentUserCache.username,
                            fromFullName          = CurrentUserCache.fullName,
                            fromUserProfileBase64 = CurrentUserCache.profileImageBase64,
                            type                  = "follow"
                        )
                    ) {}
                }
            }
        }
    }

    private fun loadUserData() {
        val uid = if (isMyProfile) AuthUtil.currentUid else targetUserId!!

        if (isMyProfile && CurrentUserCache.isLoaded()) {
            activity?.runOnUiThread {
                updateUI(
                    CurrentUserCache.fullName,
                    CurrentUserCache.username,
                    CurrentUserCache.bio,
                    CurrentUserCache.profileImageBase64
                )
                showProfileContent()
            }
        }

        FireStoreUtil.getUser(uid) { user ->
            if (user == null || !isAdded) return@getUser
            if (isMyProfile) CurrentUserCache.populate(user)
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                updateUI(user.fullName, user.username, user.bio, user.profileImageBase64)
                tvFollowerCount.text  = user.followerCount.toString()
                tvFollowingCount.text = user.followingCount.toString()

                val sdf = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                tvJoinedDate.text = "Joined ${sdf.format(java.util.Date(user.createdAt))}"
                layoutJoinedDate.visibility = View.VISIBLE

                showProfileContent()
            }
        }
    }

    private fun updateUI(name: String, username: String, bio: String, imageBase64: String) {
        tvProfileName.text     = name
        tvProfileUsername.text = "@$username"

        if (bio.isEmpty()) {
            tvProfileBio.visibility = View.GONE
        } else {
            tvProfileBio.visibility = View.VISIBLE
            tvProfileBio.text = bio
        }

        if (imageBase64.isNotEmpty()) {
            ImageUtils.loadBase64(imageBase64, imgAvatar,
                requireContext().getDrawable(R.drawable.ic_avatar))
        }
    }

    private fun loadUserPosts() {
        val uid = if (isMyProfile) AuthUtil.currentUid else targetUserId!!

        FireStoreUtil.getUserPosts(uid) { posts ->
            if (!isAdded) return@getUserPosts

            val sorted = posts.sortedByDescending { it.createdAt }

            if (sorted.isEmpty()) {
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    showPostsContent()
                    displayPosts(emptyList())
                }
                return@getUserPosts
            }

            postHandler.fetchStatusesInParallel(sorted) { readyPosts ->
                val finalList = readyPosts.sortedByDescending { it.createdAt }
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    showPostsContent()
                    displayPosts(finalList)
                }
            }
        }
    }

    private fun displayPosts(posts: List<PostModel>) {
        profilePostList.clear()
        profilePostList.addAll(posts)
        postAdapter.notifyDataSetChanged()
        tvPostCount.text          = posts.size.toString()
        layoutNoPosts.visibility  = if (posts.isEmpty()) View.VISIBLE else View.GONE
        rvProfilePosts.visibility = if (posts.isEmpty()) View.GONE else View.VISIBLE
    }
}