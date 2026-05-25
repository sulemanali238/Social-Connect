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

class activity_LikedPost : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var rvLikedPosts: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: View
    private lateinit var tvLikedCount: TextView

    private val postList = mutableListOf<PostModel>()
    private val pendingCounterSet = mutableSetOf<String>()
    private lateinit var postAdapter: PostAdapter
    private lateinit var postHandler: PostInteractionHandler

    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liked_post)

        btnBack      = findViewById(R.id.btnBack)
        rvLikedPosts = findViewById(R.id.rvLikedPosts)
        progressBar  = findViewById(R.id.progressBar)
        layoutEmpty  = findViewById(R.id.layoutEmpty)
        tvLikedCount = findViewById(R.id.tvLikedCount)

        btnBack.setOnClickListener { finish() }
        rvLikedPosts.layoutManager = LinearLayoutManager(this)

        setupAdapter()
        loadLikedPosts()
    }

    private fun setupAdapter() {
        postAdapter = PostAdapter(
            posts           = postList,
            onLikeClick     = { post, pos -> handleUnlike(post, pos) }, // custom — removes item
            onCommentClick  = { post -> postHandler.handleComment(post) },
            onBookmarkClick = { post, pos -> postHandler.handleBookmark(post, pos) },
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
        rvLikedPosts.adapter = postAdapter
        postHandler = PostInteractionHandler(this, postList, postAdapter, pendingCounterSet)
    }

    private fun loadLikedPosts() {
        showLoading()
        db.collectionGroup("likes")
            .whereEqualTo("userId", currentUid)
            .get()
            .addOnSuccessListener { result ->
                val postIds = result.documents.mapNotNull { doc ->
                    doc.reference.parent.parent?.id
                }.distinct()

                if (postIds.isEmpty()) { showEmpty(); return@addOnSuccessListener }
                fetchPostsByIds(postIds)
            }
            .addOnFailureListener {
                showEmpty()
                Toast.makeText(this, "Failed to load liked posts", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchPostsByIds(postIds: List<String>) {
        var fetched = 0
        val rawPosts = mutableListOf<PostModel>()

        postIds.forEach { postId ->
            db.collection("posts").document(postId).get()
                .addOnSuccessListener { doc ->
                    doc.toObject(PostModel::class.java)?.let { rawPosts.add(it) }
                    fetched++
                    if (fetched == postIds.size) {
                        val sorted = rawPosts.sortedByDescending { it.createdAt }
                        // ── fetch bookmark status in parallel before showing ──
                        postHandler.fetchStatusesInParallel(sorted) { readyPosts ->
                            runOnUiThread { showPosts(readyPosts) }
                        }
                    }
                }
                .addOnFailureListener {
                    fetched++
                    if (fetched == postIds.size) {
                        val sorted = rawPosts.sortedByDescending { it.createdAt }
                        postHandler.fetchStatusesInParallel(sorted) { readyPosts ->
                            runOnUiThread { showPosts(readyPosts) }
                        }
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

        rvLikedPosts.visibility = View.VISIBLE
        layoutEmpty.visibility  = View.GONE
        updateHeaderCount()
    }

    // custom — unlike removes item from the liked posts screen
    private fun handleUnlike(post: PostModel, position: Int) {
        FireStoreUtil.likePost(post.postId, true) { success ->
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
                Toast.makeText(this, "Failed to unlike", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateHeaderCount() {
        val count = postList.size
        tvLikedCount.text = "$count liked ${if (count == 1) "post" else "posts"}"
        if (count == 0) showEmpty()
    }

    private fun showLoading() {
        progressBar.visibility  = View.VISIBLE
        rvLikedPosts.visibility = View.GONE
        layoutEmpty.visibility  = View.GONE
    }

    private fun showEmpty() {
        progressBar.visibility  = View.GONE
        rvLikedPosts.visibility = View.GONE
        layoutEmpty.visibility  = View.VISIBLE
        tvLikedCount.text       = "0 liked posts"
    }
}