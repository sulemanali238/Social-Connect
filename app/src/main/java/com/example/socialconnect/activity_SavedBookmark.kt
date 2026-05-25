package com.example.socialconnect

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.adapters.PostAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class activity_SavedBookmark : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var rvBookmarks: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: View
    private lateinit var tvBookmarkCount: TextView

    private val postList = mutableListOf<PostModel>()
    private val pendingCounterSet = mutableSetOf<String>()
    private lateinit var postAdapter: PostAdapter
    private lateinit var postHandler: PostInteractionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_bookmark)

        btnBack         = findViewById(R.id.btnBack)
        rvBookmarks     = findViewById(R.id.rvBookmarks)
        progressBar     = findViewById(R.id.progressBar)
        layoutEmpty     = findViewById(R.id.layoutEmpty)
        tvBookmarkCount = findViewById(R.id.tvBookmarkCount)

        btnBack.setOnClickListener { finish() }
        rvBookmarks.layoutManager = LinearLayoutManager(this)

        setupAdapter()
        loadBookmarkedPosts()
    }

    private fun setupAdapter() {
        postAdapter = PostAdapter(
            posts           = postList,
            onLikeClick     = { post, pos -> postHandler.handleLike(post, pos) },
            onCommentClick  = { post -> postHandler.handleComment(post) },
            onBookmarkClick = { post, pos -> handleUnbookmark(post, pos) },
            onShareClick    = { post -> postHandler.handleShare(post) },
            onAvatarClick   = { _ -> },
            onDeleteClick   = { post ->
                postHandler.showDeleteDialog(post) { deleted ->
                    val idx = postList.indexOfFirst { it.postId == deleted.postId }
                    if (idx != -1) {
                        postList.removeAt(idx)
                        postAdapter.notifyItemRemoved(idx)
                        updateHeaderCount()
                    }
                }
            }
        )
        rvBookmarks.adapter = postAdapter
        postHandler = PostInteractionHandler(this, postList, postAdapter, pendingCounterSet)
    }

    private fun loadBookmarkedPosts() {
        showLoading()
        FireStoreUtil.getBookmarkedPostIds { postIds ->
            if (postIds.isEmpty()) { showEmpty(); return@getBookmarkedPostIds }
            FireStoreUtil.getBookMarkedPosts(postIds) { posts ->
                postHandler.fetchStatusesInParallel(posts) { readyPosts ->
                    runOnUiThread { showPosts(readyPosts) }
                }
            }
        }
    }

    private fun showPosts(posts: List<PostModel>) {
        progressBar.visibility = View.GONE
        if (posts.isEmpty()) { showEmpty(); return }

        postList.clear()
        postList.addAll(posts)
        postAdapter.notifyDataSetChanged()

        rvBookmarks.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE
        updateHeaderCount()
    }

    // custom — unbookmark removes item from the saved screen
    private fun handleUnbookmark(post: PostModel, position: Int) {
        FireStoreUtil.bookmarkPost(post.postId, true) { success ->
            if (success) {
                runOnUiThread {
                    val idx = postList.indexOfFirst { it.postId == post.postId }
                    if (idx != -1) {
                        postList.removeAt(idx)
                        postAdapter.notifyItemRemoved(idx)
                        updateHeaderCount()
                    }
                }
            } else {
                Toast.makeText(this, "Failed to remove bookmark", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateHeaderCount() {
        val count = postList.size
        tvBookmarkCount.text = "$count saved ${if (count == 1) "bookmark" else "bookmarks"}"
        if (count == 0) showEmpty()
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        rvBookmarks.visibility = View.GONE
        layoutEmpty.visibility = View.GONE
    }

    private fun showEmpty() {
        progressBar.visibility = View.GONE
        rvBookmarks.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE
        tvBookmarkCount.text   = "0 bookmarks"
    }
}