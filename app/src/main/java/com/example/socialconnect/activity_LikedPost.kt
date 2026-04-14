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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class activity_LikedPost : AppCompatActivity() {

    private lateinit var btnBack: ImageView

    private lateinit var rvLikedPosts: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: View
    private lateinit var tvLikedCount: TextView

    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liked_post)

        supportActionBar?.apply {
            title = "Liked Posts"
            setDisplayHomeAsUpEnabled(true)
        }

        btnBack = findViewById(R.id.btnBack)
        rvLikedPosts = findViewById(R.id.rvLikedPosts)
        progressBar  = findViewById(R.id.progressBar)
        layoutEmpty  = findViewById(R.id.layoutEmpty)
        tvLikedCount = findViewById(R.id.tvLikedCount)

        btnBack.setOnClickListener { finish() }

        rvLikedPosts.layoutManager = LinearLayoutManager(this)

        loadLikedPosts()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    // ── Step 1: query posts where likes sub-collection contains currentUid ───
    // Firestore stores likes as posts/{postId}/likes/{currentUid}
    // We use a collectionGroup query to find all like docs owned by currentUid
    private fun loadLikedPosts() {
        showLoading()

        db.collectionGroup("likes")
            .whereEqualTo("userId", currentUid)
            .get()
            .addOnSuccessListener { result ->
                // Each like doc path is:  posts/{postId}/likes/{currentUid}
                // So parent of parent = the post document id
                val postIds = result.documents.mapNotNull { doc ->
                    doc.reference.parent.parent?.id
                }.distinct()

                if (postIds.isEmpty()) {
                    showEmpty()
                    return@addOnSuccessListener
                }

                fetchPostsByIds(postIds)
            }
            .addOnFailureListener {
                showEmpty()
                Toast.makeText(this, "Failed to load liked posts", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Step 2: fetch each PostModel by its id ───────────────────────────────
    private fun fetchPostsByIds(postIds: List<String>) {
        val posts = mutableListOf<PostModel>()
        var fetched = 0

        for (postId in postIds) {
            db.collection("posts")
                .document(postId)
                .get()
                .addOnSuccessListener { doc ->
                    doc.toObject(PostModel::class.java)?.let { posts.add(it) }
                    fetched++
                    if (fetched == postIds.size) {
                        val sorted = posts.sortedByDescending { it.createdAt }
                        showPosts(sorted)
                    }
                }
                .addOnFailureListener {
                    fetched++
                    if (fetched == postIds.size) {
                        val sorted = posts.sortedByDescending { it.createdAt }
                        showPosts(sorted)
                    }
                }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────
    private fun showPosts(posts: List<PostModel>) {
        progressBar.visibility = View.GONE

        if (posts.isEmpty()) {
            showEmpty()
            return
        }

        val count = posts.size
        tvLikedCount.text = "$count liked ${if (count == 1) "post" else "posts"}"

        rvLikedPosts.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE

//        rvLikedPosts.adapter = PostCardAdapter(
//            posts       = posts.toMutableList(),
//            actionIcon  = R.drawable.ic_heart_filled,    // filled heart
//            actionTint  = "#E57373",
//            onActionClick = { post, position ->
//                // Unlike
//                FireStoreUtil.likePost(post.postId, isLiked = true) { success ->
//                    if (success) {
//                        (rvLikedPosts.adapter as PostCardAdapter).removeAt(position)
//                        val newCount = (rvLikedPosts.adapter as PostCardAdapter).itemCount
//                        tvLikedCount.text =
//                            "$newCount liked ${if (newCount == 1) "post" else "posts"}"
//                        if (newCount == 0) showEmpty()
//                    }
//                }
//            }
//        )
    }

    private fun showLoading() {
        progressBar.visibility  = View.VISIBLE
        rvLikedPosts.visibility = View.GONE
        layoutEmpty.visibility  = View.GONE
    }

    private fun showEmpty() {
        progressBar.visibility = View.GONE
        rvLikedPosts.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE
        tvLikedCount.text = "0 liked posts"
    }
}