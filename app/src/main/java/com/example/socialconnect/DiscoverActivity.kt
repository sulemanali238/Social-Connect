package com.example.socialconnect

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.socialconnect.adapters.DiscoverUsersAdapter
import com.facebook.shimmer.ShimmerFrameLayout

class DiscoverActivity : AppCompatActivity() {

    private lateinit var rvDiscoverUsers: RecyclerView
    private lateinit var discoverAdapter: DiscoverUsersAdapter
    private lateinit var shimmer: ShimmerFrameLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val userList = mutableListOf<UserModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discover)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        shimmer      = findViewById(R.id.shimmerDiscover)
        swipeRefresh = findViewById(R.id.swipeRefreshDiscover)

        shimmer.startShimmer()

        setupRecyclerView()
        loadUsers()

        swipeRefresh.setColorSchemeResources(R.color.teal_green)
        swipeRefresh.setOnRefreshListener {
            userList.clear()
            discoverAdapter.notifyDataSetChanged()
            loadUsers()
            swipeRefresh.postDelayed({
                swipeRefresh.isRefreshing = false
            }, 2000)
        }
    }

    private fun setupRecyclerView() {
        rvDiscoverUsers = findViewById(R.id.rvDiscoverUsers)
        rvDiscoverUsers.layoutManager = LinearLayoutManager(this)
        rvDiscoverUsers.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        discoverAdapter = DiscoverUsersAdapter(
            users = userList,
            onFollowClick = { user, position -> handleFollow(user, position) },
            onUserClick   = { user -> openProfile(user.uid) }
        )
        rvDiscoverUsers.adapter = discoverAdapter
    }

    private fun loadUsers() {
        FireStoreUtil.db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val currentUid = AuthUtil.currentUid
                val allUsers = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserModel::class.java)?.copy(uid = doc.id)
                }.filter { it.uid != currentUid }

                if (allUsers.isEmpty()) {
                    hideShimmer()
                    return@addOnSuccessListener
                }

                var remaining = allUsers.size
                val result = mutableListOf<UserModel>()

                allUsers.forEach { user ->
                    FireStoreUtil.isFollowing(user.uid) { isFollowing ->
                        synchronized(result) {
                            result.add(user.copy(isFollowing = isFollowing))
                            if (--remaining == 0) {
                                runOnUiThread {
                                    val sorted = result.sortedBy { it.isFollowing }
                                    userList.clear()
                                    userList.addAll(sorted)
                                    discoverAdapter.notifyDataSetChanged()
                                    hideShimmer()
                                }
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
                hideShimmer()
            }
    }

    private fun hideShimmer() {
        shimmer.stopShimmer()
        shimmer.visibility      = android.view.View.GONE
        swipeRefresh.visibility = android.view.View.VISIBLE
    }

    private fun handleFollow(user: UserModel, position: Int) {
        if (user.isFollowing) {
            FireStoreUtil.unfollowUser(user.uid) { success ->
                if (success) runOnUiThread {
                    discoverAdapter.updateFollowState(position, false)
                }
            }
        } else {
            FireStoreUtil.followUser(user.uid) { success ->
                if (success) {
                    if (user.uid != AuthUtil.currentUid) {
                        FireStoreUtil.sendNotification(
                            this,
                            NotificationModel(
                                toUserId              = user.uid,
                                fromUsername          = CurrentUserCache.username,
                                fromFullName          = CurrentUserCache.fullName,
                                fromUserProfileBase64 = CurrentUserCache.profileImageBase64,
                                type                  = "follow",
                                postId                = "",
                                postImageBase64       = ""
                            )
                        ) {}
                    }
                    runOnUiThread {
                        discoverAdapter.updateFollowState(position, true)
                    }
                }
            }
        }
    }

    private fun openProfile(userId: String) {
        val intent = Intent(this, activity_Main::class.java).apply {
            putExtra("OPEN_PROFILE_UID", userId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}