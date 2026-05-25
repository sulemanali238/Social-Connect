package com.example.socialconnect.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.socialconnect.AuthUtil
import com.example.socialconnect.DiscoverActivity
import com.example.socialconnect.FireStoreUtil
import com.example.socialconnect.NotificationActivity
import com.example.socialconnect.PostInteractionHandler
import com.example.socialconnect.PostModel
import com.example.socialconnect.R
import com.example.socialconnect.activity_Main
import com.example.socialconnect.adapters.PostAdapter

class HomeFragment : Fragment() {

    private lateinit var rvFeed: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<PostModel>()
    private val pendingCounterSet = mutableSetOf<String>()
    private lateinit var postHandler: PostInteractionHandler
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView(view)

        swipeRefresh = view.findViewById(R.id.swipeRefreshHome)
        swipeRefresh.setColorSchemeResources(R.color.teal_green)
        swipeRefresh.setOnRefreshListener {
            postList.clear()
            postAdapter.notifyDataSetChanged()
            loadPosts()
            loadSuggestedUsers()
            swipeRefresh.postDelayed({
                if (isAdded) swipeRefresh.isRefreshing = false
            }, 1500)
        }

        loadPosts()
        view.findViewById<ImageView>(R.id.btnSearch).setOnClickListener {
            (requireActivity() as activity_Main).openSearch()
        }
        view.findViewById<ImageView>(R.id.btnNotifications).setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }
    }

    private fun setupRecyclerView(view: View) {
        rvFeed = view.findViewById(R.id.rvFeed)
        rvFeed.layoutManager = LinearLayoutManager(requireContext())

        postAdapter = PostAdapter(
            posts           = postList,
            onLikeClick     = { post, position -> postHandler.handleLike(post, position) },
            onCommentClick  = { post -> postHandler.handleComment(post) },
            onBookmarkClick = { post, position -> postHandler.handleBookmark(post, position) },
            onShareClick    = { post -> postHandler.handleShare(post) },
            onAvatarClick   = { post -> navigateToProfile(post.userId) },
            onDeleteClick   = { post ->
                postHandler.showDeleteDialog(post) { deleted ->
                    val idx = postList.indexOfFirst { it.postId == deleted.postId }
                    if (idx != -1) {
                        postList.removeAt(idx)
                        postAdapter.notifyItemRemoved(idx)
                    }
                }
            }
        )

        // ── MUST be before rvFeed.adapter and loadSuggestedUsers ─────
        postHandler = PostInteractionHandler(this, postList, postAdapter, pendingCounterSet)

        rvFeed.adapter = postAdapter
        loadSuggestedUsers()
    }

    private fun navigateToProfile(userId: String) {
        val activity = requireActivity() as activity_Main
        if (userId == AuthUtil.currentUid) activity.openMyProfile()
        else activity.openOtherProfile(userId)
    }

    private fun loadPosts() {
        FireStoreUtil.getAllPosts { rawPosts ->
            if (!isAdded) return@getAllPosts
            val serverIds = rawPosts.map { it.postId }.toSet()
            postList.removeAll { it.postId !in serverIds }
            val existingIds = postList.map { it.postId }.toSet()
            val newPosts = rawPosts.filter { it.postId !in existingIds }

            rawPosts.forEach { serverPost ->
                val index = postList.indexOfFirst { it.postId == serverPost.postId }
                if (index != -1) {
                    val existing = postList[index]
                    val countTruth = if (pendingCounterSet.contains(serverPost.postId))
                        existing.likeCount else serverPost.likeCount
                    if (existing.likeCount != countTruth) {
                        postList[index] = existing.copy(likeCount = countTruth)
                        postAdapter.notifyItemChanged(index, PostAdapter.PAYLOAD_COUNTS)
                    }
                }
            }

            if (newPosts.isEmpty()) return@getAllPosts
            postHandler.fetchStatusesInParallel(newPosts) { readyPosts ->
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    val existingIds = postList.map { it.postId }.toSet()
                    val toAdd = readyPosts.filter { it.postId !in existingIds }
                    if (toAdd.isNotEmpty()) {
                        postList.addAll(toAdd)
                        postList.sortByDescending { it.createdAt }
                        postAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun loadSuggestedUsers() {
        FireStoreUtil.getSuggestedUsers { users ->
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                postAdapter.setSuggestedUsers(
                    users = users,
                    onFollow = { user, position ->
                        postHandler.handleFollow(user.uid, user.isFollowing) { isNowFollowing ->
                            activity?.runOnUiThread {
                                postAdapter.updateSuggestedFollowState(position, isNowFollowing)
                            }
                        }
                    },
                    onSeeAll = {
                        startActivity(Intent(requireContext(), DiscoverActivity::class.java))
                    },
                    onUserClick = { user ->
                        navigateToProfile(user.uid)
                    }
                )
            }
        }
    }


    override fun onResume() {
        super.onResume()
        checkNotifBadge()
    }

    private fun checkNotifBadge() {
        FireStoreUtil.getUnseenNotificationCount { count ->
            activity?.runOnUiThread {
                view?.findViewById<View>(R.id.notifBadge)?.visibility =
                    if (count > 0) View.VISIBLE else View.GONE
            }
        }
    }
}