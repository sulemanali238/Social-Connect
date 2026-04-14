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

class activity_SavedBookmark : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var rvBookmarks: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: View
    private lateinit var tvBookmarkCount: TextView

    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_bookmark)

        supportActionBar?.apply {
            title = "Saved Bookmarks"
            setDisplayHomeAsUpEnabled(true)
        }

        btnBack = findViewById(R.id.btnBack)
        rvBookmarks = findViewById(R.id.rvBookmarks)
        progressBar = findViewById(R.id.progressBar)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        tvBookmarkCount = findViewById(R.id.tvBookmarkCount)

        btnBack.setOnClickListener { finish() }
        rvBookmarks.layoutManager = LinearLayoutManager(this)

        loadBookmarkedPosts()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun loadBookmarkedPosts() {
        showLoading()

        db.collection("users")
            .document(currentUid)
            .collection("bookmarks")
            .get()
            .addOnSuccessListener { result ->
                val postIds = result.documents.mapNotNull { it.id }

                if (postIds.isEmpty()) {
                    showEmpty()
                    return@addOnSuccessListener
                }

                fetchPostsByIds(postIds)
            }
            .addOnFailureListener {
                showEmpty()
                Toast.makeText(this, "Failed to load bookmarks", Toast.LENGTH_SHORT).show()
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
                        // Sort newest first
                        val sorted = posts.sortedByDescending { it.createdAt }
                        showPosts(sorted, isBookmarkMode = true)
                    }
                }
                .addOnFailureListener {
                    fetched++
                    if (fetched == postIds.size) {
                        val sorted = posts.sortedByDescending { it.createdAt }
                        showPosts(sorted, isBookmarkMode = true)
                    }
                }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────
    private fun showPosts(posts: List<PostModel>, isBookmarkMode: Boolean) {
        progressBar.visibility = View.GONE

        if (posts.isEmpty()) {
            showEmpty()
            return
        }

        val count = posts.size
        tvBookmarkCount.text = "$count saved ${if (count == 1) "bookmark" else "bookmarks"}"

        rvBookmarks.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE

//        rvBookmarks.adapter = PostAdapter(
//            posts = posts.toMutableList(),
//            actionIcon = R.drawable.ic_bookmark_filled,         // filled bookmark
//            actionTint = "#00BFA5",
//            onActionClick = { post, position ->
//                // Unbookmark
//                FireStoreUtil.bookmarkPost(post, isBookmarked = true) { success ->
//                    if (success) {
//                        (rvBookmarks.adapter as PostAdapter).removeAt(position)
//                        val newCount = (rvBookmarks.adapter as PostAdapter).itemCount
//                        tvBookmarkCount.text =
//                            "$newCount saved ${if (newCount == 1) "bookmark" else "bookmarks"}"
//                        if (newCount == 0) showEmpty()
//                    }
//                }
//            }
//        )
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