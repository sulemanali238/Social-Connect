package com.example.socialconnect

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.socialconnect.adapters.CommentAdapter
import com.example.socialconnect.adapters.PostAdapter
import com.google.firebase.firestore.ListenerRegistration
import de.hdodenhof.circleimageview.CircleImageView

class PostDetail : AppCompatActivity() {

    private lateinit var shimmer: com.facebook.shimmer.ShimmerFrameLayout
    private lateinit var scrollPost: androidx.core.widget.NestedScrollView
    private lateinit var imgAvatar: CircleImageView
    private lateinit var tvName: TextView
    private lateinit var tvTime: TextView
    private lateinit var layoutImages: FrameLayout
    private lateinit var vpImages: ViewPager2
    private lateinit var tvImageCounter: TextView
    private lateinit var rvDots: RecyclerView
    private lateinit var tvCaption: TextView
    private lateinit var btnLike: ImageView
    private lateinit var tvLikeCount: TextView
    private lateinit var btnComment: ImageView
    private lateinit var tvCommentCount: TextView
    private lateinit var btnShare: ImageView
    private lateinit var btnBookmark: ImageView
    private lateinit var btnMore: ImageView
    private lateinit var layoutCommentInput: View
    private lateinit var etComment: EditText
    private lateinit var btnSendComment: FrameLayout
    private lateinit var rvComments: RecyclerView

    private lateinit var postId: String
    private var currentPost: PostModel? = null

    private val postList = mutableListOf<PostModel>()
    private val pendingCounterSet = mutableSetOf<String>()
    private lateinit var postHandler: PostInteractionHandler

    private val commentList = mutableListOf<CommentModel>()
    private val userMap = mutableMapOf<String, UserModel>()
    private lateinit var commentAdapter: CommentAdapter

    private var commentsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        postId = intent.getStringExtra("POST_ID") ?: run { finish(); return }

        bindViews()
        setupCommentRecyclerView()
        startShimmer()
        loadPost()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        btnSendComment.setOnClickListener { submitComment() }
        etComment.setOnEditorActionListener { _, _, _ -> submitComment(); true }
    }

    private fun bindViews() {
        shimmer            = findViewById(R.id.shimmerPost)
        scrollPost         = findViewById(R.id.scrollPost)
        imgAvatar          = findViewById(R.id.imgPostAvatar)
        tvName             = findViewById(R.id.tvPostName)
        tvTime             = findViewById(R.id.tvPostTime)
        layoutImages       = findViewById(R.id.layoutPostImages)
        vpImages           = findViewById(R.id.vpPostImages)
        tvImageCounter     = findViewById(R.id.tvImageCounter)
        rvDots             = findViewById(R.id.rvDots)
        tvCaption          = findViewById(R.id.tvPostCaption)
        btnLike            = findViewById(R.id.btnLike)
        tvLikeCount        = findViewById(R.id.tvLikeCount)
        btnComment         = findViewById(R.id.btnComment)
        tvCommentCount     = findViewById(R.id.tvCommentCount)
        btnShare           = findViewById(R.id.btnShare)
        btnBookmark        = findViewById(R.id.btnBookmark)
        btnMore            = findViewById(R.id.btnPostMore)
        layoutCommentInput = findViewById(R.id.layoutCommentInput)
        etComment          = findViewById(R.id.etComment)
        btnSendComment     = findViewById(R.id.btnSendComment)
        rvComments         = findViewById(R.id.rvComments)
    }

    private fun setupCommentRecyclerView() {
        commentAdapter = CommentAdapter(
            comments = commentList,
            userMap  = userMap,
            onDeleteClick = { comment ->
                FireStoreUtil.deleteComment(postId, comment.commentId) { success ->
                    if (!success) runOnUiThread {
                        Toast.makeText(this, "Failed to delete comment", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        rvComments.layoutManager = LinearLayoutManager(this)
        rvComments.adapter = commentAdapter
        rvComments.isNestedScrollingEnabled = false
    }

    private fun startShimmer() {
        shimmer.startShimmer()
        shimmer.visibility            = View.VISIBLE
        scrollPost.visibility         = View.GONE
        layoutCommentInput.visibility = View.GONE
    }

    private fun hideShimmer() {
        shimmer.stopShimmer()
        shimmer.visibility            = View.GONE
        scrollPost.visibility         = View.VISIBLE
        layoutCommentInput.visibility = View.VISIBLE
    }

    private fun loadPost() {
        FireStoreUtil.getPostById(postId) { post ->
            if (post == null) {
                runOnUiThread {
                    Toast.makeText(this, "Post not found or deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
                return@getPostById
            }

            FireStoreUtil.isPostLiked(post.postId) { isLiked ->
                FireStoreUtil.isPostBookmarked(post.postId) { isBookmarked ->
                    val readyPost = post.copy(isLiked = isLiked, isBookmarked = isBookmarked)
                    currentPost = readyPost
                    postList.clear()
                    postList.add(readyPost)

                    postHandler = PostInteractionHandler(
                        this, postList, buildDummyAdapter(), pendingCounterSet
                    )

                    runOnUiThread {
                        renderPost(readyPost)
                        hideShimmer()
                        listenForComments()
                    }
                }
            }
        }
    }

    private fun buildDummyAdapter() = PostAdapter(
        posts           = postList,
        onLikeClick     = { _, _ -> },
        onCommentClick  = { _ -> },
        onBookmarkClick = { _, _ -> },
        onShareClick    = { _ -> },
        onAvatarClick   = { _ -> },
        onDeleteClick   = { _ -> }
    )

    private fun renderPost(post: PostModel) {
        if (post.userProfileBase64.isNotEmpty()) {
            ImageUtils.loadBase64(post.userProfileBase64, imgAvatar, getDrawable(R.drawable.ic_avatar))
        } else {
            imgAvatar.setImageResource(R.drawable.ic_avatar)
        }

        tvName.text = post.fullName
        tvTime.text = getTimeAgo(post.createdAt)

        if (post.caption.isNotEmpty()) {
            tvCaption.text       = post.caption
            tvCaption.visibility = View.VISIBLE
        }

        val images = post.allImages()
        if (images.isNotEmpty()) {
            layoutImages.visibility = View.VISIBLE
            vpImages.adapter = com.example.socialconnect.adapters.PostImageAdapter(images)

            if (images.size > 1) {
                tvImageCounter.visibility = View.VISIBLE
                tvImageCounter.text = "1 / ${images.size}"
                vpImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        tvImageCounter.text = "${position + 1} / ${images.size}"
                    }
                })
            }
        }

        tvLikeCount.text    = post.likeCount.toString()
        tvCommentCount.text = post.commentCount.toString()
        updateLikeButton(post.isLiked)
        updateBookmarkButton(post.isBookmarked)

        setupClickListeners(post)
    }

    private fun setupClickListeners(post: PostModel) {
        imgAvatar.setOnClickListener {
            // already in detail, avatar tap can open profile if needed
        }

        btnLike.setOnClickListener {
            postHandler.handleLike(postList[0], 0)
            btnLike.post {
                updateLikeButton(postList[0].isLiked)
                tvLikeCount.text = postList[0].likeCount.toString()
            }
        }

        btnBookmark.setOnClickListener {
            postHandler.handleBookmark(postList[0], 0)
            btnBookmark.post { updateBookmarkButton(postList[0].isBookmarked) }
        }

        // comment icon just scrolls down to the input
        btnComment.setOnClickListener {
            scrollPost.post {
                scrollPost.fullScroll(View.FOCUS_DOWN)
                etComment.requestFocus()
            }
        }

        btnShare.setOnClickListener { postHandler.handleShare(postList[0]) }

        btnMore.setOnClickListener { showMoreOptions(it, post) }
    }

    private fun showMoreOptions(view: View, post: PostModel) {
        val currentUid = AuthUtil.currentUid

        val items = mutableListOf<String>()
        if (post.userId == currentUid) {
            items.add("Delete Post")
            items.add("Edit Post")
        }
        items.add("Copy Link")

        val listPopup = androidx.appcompat.widget.ListPopupWindow(view.context)
        listPopup.anchorView = view
        listPopup.setAdapter(
            android.widget.ArrayAdapter(view.context, android.R.layout.simple_list_item_1, items)
        )
        listPopup.width = 400
        listPopup.isModal = true
        listPopup.setBackgroundDrawable(
            android.graphics.Color.WHITE.toDrawable()
        )
        listPopup.setOnItemClickListener { _, _, position, _ ->
            when (items[position]) {
                "Delete Post" -> {
                    postHandler.showDeleteDialog(postList[0]) { _ -> finish() }
                }
                "Edit Post" -> {
                    val intent = android.content.Intent(this, activity_AddPost::class.java).apply {
                        putExtra("EDIT_MODE", true)
                        putExtra("POST_ID", post.postId)
                        putExtra("EDIT_CAPTION", post.caption)
                        putStringArrayListExtra("EDIT_IMAGES", ArrayList(post.allImages()))
                    }
                    startActivity(intent)
                }
                "Copy Link" -> { }
            }
            listPopup.dismiss()
        }
        listPopup.show()
    }

    private fun submitComment() {
        val text = etComment.text.toString().trim()
        if (text.isEmpty()) return
        if (!CurrentUserCache.isLoaded()) return

        etComment.setText("")
        etComment.clearFocus()

        val comment = CommentModel(
            postId            = postId,
            userId            = AuthUtil.currentUid,
            username          = CurrentUserCache.username,
            fullName          = CurrentUserCache.fullName,
            userProfileBase64 = CurrentUserCache.profileImageBase64,
            text              = text,
            createdAt         = System.currentTimeMillis()
        )

        FireStoreUtil.addComment(comment) { success ->
            if (success) {
                val postOwnerId = currentPost?.userId ?: return@addComment
                if (postOwnerId != AuthUtil.currentUid) {
                    val notification = NotificationModel(
                        toUserId              = postOwnerId,
                        fromUserId            = AuthUtil.currentUid,
                        fromFullName          = CurrentUserCache.fullName,
                        fromUsername          = CurrentUserCache.username,
                        fromUserProfileBase64 = CurrentUserCache.profileImageBase64,
                        type                  = "comment",
                        postId                = postId,
                        postImageBase64       = currentPost?.imageBase64 ?: "",
                        message               = "${CurrentUserCache.fullName} commented on your post",
                        createdAt             = System.currentTimeMillis()
                    )
                    FireStoreUtil.sendNotification(this@PostDetail, notification) {}
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Failed to post comment", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun listenForComments() {
        commentsListener = FireStoreUtil.getComments(postId) { comments ->
            val uidsToFetch = comments.map { it.userId }.toSet()
                .filter { it !in userMap }

            if (uidsToFetch.isEmpty()) {
                updateCommentList(comments)
                return@getComments
            }

            var fetched = 0
            uidsToFetch.forEach { uid ->
                FireStoreUtil.getUser(uid) { user ->
                    if (user != null) userMap[uid] = user
                    fetched++
                    if (fetched == uidsToFetch.size) updateCommentList(comments)
                }
            }
        }
    }

    private fun updateCommentList(comments: List<CommentModel>) {
        runOnUiThread {
            commentList.clear()
            commentList.addAll(comments)
            commentAdapter.notifyDataSetChanged()
            tvCommentCount.text = comments.size.toString()
            if (postList.isNotEmpty()) {
                postList[0] = postList[0].copy(commentCount = comments.size)
            }
        }
    }

    private fun updateLikeButton(isLiked: Boolean) {
        btnLike.setImageResource(
            if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        btnLike.setColorFilter(
            if (isLiked) "#EF4444".toColorInt() else "#1E293B".toColorInt()
        )
        tvLikeCount.setTextColor(
            if (isLiked) "#EF4444".toColorInt() else "#1E293B".toColorInt()
        )
    }

    private fun updateBookmarkButton(isBookmarked: Boolean) {
        btnBookmark.setImageResource(
            if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
        )
        btnBookmark.setColorFilter(
            if (isBookmarked) "#00BFA5".toColorInt() else "#1E293B".toColorInt()
        )
    }

    private fun getTimeAgo(time: Long): String {
        val diff = System.currentTimeMillis() - time
        return when {
            diff < 60_000L         -> "Just now"
            diff < 3_600_000L      -> "${diff / 60_000}m ago"
            diff < 86_400_000L     -> "${diff / 3_600_000}h ago"
            diff < 604_800_000L    -> "${diff / 86_400_000}d ago"
            diff < 2_592_000_000L  -> "${diff / 604_800_000}w ago"
            diff < 31_104_000_000L -> "${diff / 2_592_000_000}mo ago"
            else                   -> "${diff / 31_104_000_000}y ago"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        commentsListener?.remove()
    }
}