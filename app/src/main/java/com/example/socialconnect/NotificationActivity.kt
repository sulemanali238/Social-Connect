package com.example.socialconnect

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.socialconnect.adapters.NotificationAdapter

class NotificationActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutEmptyState: View
    private lateinit var btnBack: View
    private lateinit var btnMarkAllRead: View
    private lateinit var shimmerNotifications: com.facebook.shimmer.ShimmerFrameLayout
    private lateinit var adapter: NotificationAdapter
    private val notifList = mutableListOf<NotificationModel>()
    private val userMap = mutableMapOf<String, UserModel?>()


    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)
        bindViews()
        setupRecyclerView()
        setupClickListeners()
        setupSwipeRefresh()
        listenForNotifications()
    }

    private fun bindViews() {
        rvNotifications       = findViewById(R.id.rvNotifications)
        swipeRefresh          = findViewById(R.id.swipeRefresh)
        layoutEmptyState      = findViewById(R.id.layoutEmptyState)
        btnBack               = findViewById(R.id.btnBack)
        btnMarkAllRead        = findViewById(R.id.btnMarkAllRead)
        shimmerNotifications  = findViewById(R.id.shimmerNotifications)
        swipeRefresh.setColorSchemeResources(R.color.teal_green)

        // Start shimmer immediately
        shimmerNotifications.startShimmer()
    }

    private fun setupRecyclerView() {
        // KEY FIX: these two lines kill the flash on scroll
        rvNotifications.itemAnimator = null
        (rvNotifications.itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)
            ?.supportsChangeAnimations = false

        rvNotifications.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(
            notifications = notifList,
            onClick       = { notif -> handleNotifClick(notif) },
            userMap       = userMap,
            onMenuClick   = { notif, view -> showNotifMenu(notif, view) }
        )
        rvNotifications.adapter = adapter
    }

    private fun showShimmer() {
        shimmerNotifications.startShimmer()
        shimmerNotifications.visibility = View.VISIBLE
        swipeRefresh.visibility         = View.GONE
    }

    private fun hideShimmer() {
        shimmerNotifications.stopShimmer()
        shimmerNotifications.visibility = View.GONE
        swipeRefresh.visibility         = View.VISIBLE
    }

    private fun showNotifMenu(notif: NotificationModel, anchor: View) {
        val items = listOf("Mark as Read", "Delete")

        val listPopup = androidx.appcompat.widget.ListPopupWindow(this)
        listPopup.anchorView = anchor
        listPopup.setAdapter(
            android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        )
        listPopup.width = 400
        listPopup.isModal = true
        listPopup.setBackgroundDrawable(
            android.graphics.Color.WHITE.toDrawable()
        )
        listPopup.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> markSingleSeen(notif)
                1 -> deleteNotification(notif)
            }
            listPopup.dismiss()
        }
        listPopup.show()
    }

    private fun deleteNotification(notif: NotificationModel) {
        FireStoreUtil.deleteNotification(notif.notificationId) { success ->
            if (success) runOnUiThread {
                adapter.removeNotification(notif)
                layoutEmptyState.visibility =
                    if (notifList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun markSingleSeen(notif: NotificationModel) {
        FireStoreUtil.markSingleNotificationSeen(notif.notificationId) {
            runOnUiThread { adapter.markSingleSeen(notif) }
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        btnMarkAllRead.setOnClickListener {
            FireStoreUtil.markNotificationsSeen { success ->
                if (success) runOnUiThread { adapter.markAllSeen() }
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            isFirstLoad = true
            userMap.clear()
            FireStoreUtil.markNotificationsSeen { success ->
                runOnUiThread {
                    if (success) adapter.markAllSeen()
                    swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun listenForNotifications() {
        showShimmer()
        FireStoreUtil.getNotifications { list ->
            runOnUiThread {
                if (isFirstLoad) {
                    isFirstLoad = false
                    adapter.replaceAll(list)

                    val uniqueIds = list.map { it.fromUserId }.distinct()
                    if (uniqueIds.isEmpty()) {
                        layoutEmptyState.visibility = View.VISIBLE
                        hideShimmer()
                        return@runOnUiThread
                    }

                    // ── Fetch all users, THEN hide shimmer ────────────
                    var remaining = uniqueIds.size
                    uniqueIds.forEach { userId ->
                        FireStoreUtil.getUser(userId) { user ->
                            userMap[userId] = user
                            remaining--
                            if (remaining == 0) {
                                runOnUiThread {
                                    adapter.notifyDataSetChanged()
                                    layoutEmptyState.visibility =
                                        if (list.isEmpty()) View.VISIBLE else View.GONE
                                    hideShimmer()
                                    if (list.any { !it.seen }) {
                                        FireStoreUtil.markNotificationsSeen {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private fun handleNotifClick(notif: NotificationModel) {
        when (notif.type) {
            "follow" -> {
                val intent = Intent(this, activity_Main::class.java).apply {
                    putExtra("OPEN_PROFILE_UID", notif.fromUserId)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }
            "like", "comment" -> {
                if (notif.postId.isEmpty()) {
                    android.widget.Toast.makeText(this, "Post not available", android.widget.Toast.LENGTH_SHORT).show()
                    return
                }
                val intent = Intent(this, PostDetail::class.java).apply {
                    putExtra("POST_ID", notif.postId)
                }
                startActivity(intent)
            }
        }
    }
}
