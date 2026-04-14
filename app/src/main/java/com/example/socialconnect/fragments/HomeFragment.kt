package com.example.socialconnect.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.AuthUtil
import com.example.socialconnect.FireStoreUtil
import com.example.socialconnect.PostModel
import com.example.socialconnect.R
import com.example.socialconnect.adapters.PostAdapter

class HomeFragment : Fragment() {

    private lateinit var rvFeed: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<PostModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        setupClickListeners(view)
        loadPosts()
    }

    private fun setupRecyclerView(view: View) {
        rvFeed = view.findViewById(R.id.rvFeed)
        rvFeed.layoutManager = LinearLayoutManager(requireContext())

        postAdapter = PostAdapter(
            posts = postList,
            onLikeClick = { post, position -> handleLike(post, position) },
            onCommentClick = { post -> },
            onBookmarkClick = { post, position -> handleBookmark(post, position) },
            onShareClick = { post -> handleShare(post) },
            onAvatarClick = { post -> }
        )
        rvFeed.adapter = postAdapter
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.btnNotifications).setOnClickListener {
            // open notifications later
        }
    }

    private fun loadPosts() {
        FireStoreUtil.getAllPosts { posts ->
            if (!isAdded) return@getAllPosts

            // for each post check if current user liked and bookmarked it
            val uid = AuthUtil.currentUid
            val updatedPosts = mutableListOf<PostModel>()
            var checkedCount = 0

            if (posts.isEmpty()) {
                postList.clear()
                postAdapter.notifyDataSetChanged()
                return@getAllPosts
            }

            for (post in posts) {
                FireStoreUtil.isPostLiked(post.postId) { isLiked ->
                    FireStoreUtil.isPostBookmarked(post.postId) { isBookmarked ->
                        updatedPosts.add(
                            post.copy(
                                isLiked = isLiked,
                                isBookmarked = isBookmarked
                            )
                        )
                        checkedCount++

                        // once all posts are checked update the list
                        if (checkedCount == posts.size && isAdded) {
                            // sort randomly
                            val shuffled = updatedPosts.shuffled()
                            postList.clear()
                            postList.addAll(shuffled)
                            postAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    private fun handleLike(post: PostModel, position: Int) {
        // prevent multiple likes — isLiked check happens here
        FireStoreUtil.likePost(post.postId, post.isLiked) { success ->
            if (!success || !isAdded) return@likePost

            val updatedPost = post.copy(
                isLiked = !post.isLiked,
                likeCount = if (post.isLiked) post.likeCount - 1
                else post.likeCount + 1
            )
            postAdapter.updatePost(position, updatedPost)
        }
    }

    private fun handleBookmark(post: PostModel, position: Int) {
        FireStoreUtil.bookmarkPost(post.postId, post.isBookmarked) { success ->
            if (!success || !isAdded) return@bookmarkPost

            val updatedPost = post.copy(isBookmarked = !post.isBookmarked)
            postAdapter.updatePost(position, updatedPost)
        }
    }

    private fun handleShare(post: PostModel) {
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(
                android.content.Intent.EXTRA_TEXT,
                "${post.username} shared a post on Social Connect"
            )
            type = "text/plain"
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "Share via"))
    }
}