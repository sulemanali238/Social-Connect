package com.example.socialconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.adapters.CommentAdapter
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.hdodenhof.circleimageview.CircleImageView
import com.google.firebase.firestore.ListenerRegistration

class BottomSheetComment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "CommentBottomSheet"
        private const val ARG_POST_ID = "post_id"
        private const val ARG_COMMENT_COUNT = "comment_count"

        fun newInstance(postId: String, commentCount: Int): BottomSheetComment {
            val sheet = BottomSheetComment()
            sheet.arguments = Bundle().apply {
                putString(ARG_POST_ID, postId)
                putInt(ARG_COMMENT_COUNT, commentCount)
            }
            return sheet
        }
    }

    private lateinit var rvComments: RecyclerView
    private lateinit var etComment: EditText
    private lateinit var btnSend: ImageView
    private lateinit var tvCommentCount: TextView
    private lateinit var imgCurrentUserAvatar: CircleImageView
    private lateinit var shimmerComments: ShimmerFrameLayout

    private val commentList = mutableListOf<CommentModel>()
    private val userMap = mutableMapOf<String, UserModel>()
    private lateinit var commentAdapter: CommentAdapter

    private var postId: String = ""
    private var currentCommentCount = 0
    private var onCommentCountChanged: ((Int) -> Unit)? = null
    private var isSending = false
    private var commentsListener: ListenerRegistration? = null
    private var hasLoadedOnce = false

    fun setOnCommentCountChanged(callback: (Int) -> Unit) {
        onCommentCountChanged = callback
    }

    override fun getTheme(): Int = R.style.RoundedBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_comment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Force full expand immediately ────────────────────────────
        dialog?.setOnShowListener {
            val sheet = dialog?.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            sheet?.let {
                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isDraggable = true
            }
        }

        postId             = arguments?.getString(ARG_POST_ID) ?: ""
        currentCommentCount = arguments?.getInt(ARG_COMMENT_COUNT) ?: 0

        initViews(view)
        setupAdapter()
        loadComments()
        setupSendButton()
        loadCurrentUserAvatar()
    }

    private fun initViews(view: View) {
        rvComments          = view.findViewById(R.id.rvComments)
        etComment           = view.findViewById(R.id.etComment)
        btnSend             = view.findViewById(R.id.btnSendComment)
        tvCommentCount      = view.findViewById(R.id.tvCommentCount)
        imgCurrentUserAvatar = view.findViewById(R.id.imgCurrentUserAvatar)
        shimmerComments     = view.findViewById(R.id.shimmerComments)

        tvCommentCount.text = currentCommentCount.toString()

        // show shimmer immediately
        shimmerComments.startShimmer()
        shimmerComments.visibility = View.VISIBLE
        rvComments.visibility      = View.GONE
    }

    private fun setupAdapter() {
        commentAdapter = CommentAdapter(
            comments      = commentList,
            userMap       = userMap,
            onDeleteClick = { comment -> deleteComment(comment) }
        )
        rvComments.layoutManager = LinearLayoutManager(requireContext())
        rvComments.adapter       = commentAdapter
    }

    private fun loadCurrentUserAvatar() {
        if (CurrentUserCache.profileImageBase64.isNotEmpty()) {
            ImageUtils.loadBase64(
                CurrentUserCache.profileImageBase64,
                imgCurrentUserAvatar,
                requireContext().getDrawable(R.drawable.ic_avatar)
            )
        }
    }

    private fun loadComments() {
        commentsListener = FireStoreUtil.getComments(postId) { comments ->
            if (!isAdded || isSending) return@getComments

            if (comments.isEmpty()) {
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    hideShimmer()
                    commentList.clear()
                    commentAdapter.notifyDataSetChanged()
                    updateCount(0)
                }
                return@getComments
            }

            // ── Batch fetch all unique users ─────────────────────────
            val uniqueUids = comments.map { it.userId }.distinct()
            var remaining  = uniqueUids.size

            uniqueUids.forEach { uid ->
                FireStoreUtil.getUser(uid) { user ->
                    if (user != null) userMap[uid] = user
                    remaining--

                    if (remaining == 0 && isAdded) {
                        activity?.runOnUiThread {
                            if (!isAdded) return@runOnUiThread
                            commentList.clear()
                            commentList.addAll(comments)
                            commentAdapter.notifyDataSetChanged()
                            updateCount(comments.size)
                            hideShimmer()
                            if (commentList.isNotEmpty())
                                rvComments.scrollToPosition(commentList.size - 1)
                        }
                    }
                }
            }
        }
    }

    private fun hideShimmer() {
        shimmerComments.stopShimmer()
        shimmerComments.visibility = View.GONE
        rvComments.visibility      = View.VISIBLE
    }

    private fun setupSendButton() {
        btnSend.setOnClickListener {
            val text = etComment.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            sendComment(text)
        }
    }

    private fun sendComment(text: String) {
        etComment.setText("")
        isSending = true

        val comment = CommentModel(
            postId = postId,
            userId = AuthUtil.currentUid,
            username = CurrentUserCache.username,
            fullName = CurrentUserCache.fullName,
            userProfileBase64 = CurrentUserCache.profileImageBase64,
            text = text,
            createdAt = System.currentTimeMillis()
        )

        // add current user to userMap so new comment shows correctly
        userMap[AuthUtil.currentUid] = UserModel(
            uid = AuthUtil.currentUid,
            username = CurrentUserCache.username,
            fullName = CurrentUserCache.fullName,
            profileImageBase64 = CurrentUserCache.profileImageBase64
        )

        commentList.add(comment)
        commentAdapter.notifyItemInserted(commentList.size - 1)
        rvComments.scrollToPosition(commentList.size - 1)
        updateCount(currentCommentCount + 1)

        FireStoreUtil.addComment(comment) { success ->
            if (!isAdded) return@addComment  // ← ADD THIS
            requireActivity().runOnUiThread {
                isSending = false
                if (!success) {
                    commentList.removeLastOrNull()
                    commentAdapter.notifyDataSetChanged()
                    updateCount(currentCommentCount - 1)
                    etComment.setText(text)
                    Toast.makeText(requireContext(), "Failed to send comment", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            FireStoreUtil.getPost(postId) { post ->
                if (post == null) return@getPost
                if (post.userId == AuthUtil.currentUid) return@getPost
                if (!isAdded) return@getPost              // ← ADD THIS
                val ctx = context ?: return@getPost       // ← CHANGE THIS
                FireStoreUtil.sendNotification(
                    ctx,       // ← USE ctx instead of requireContext()
                    NotificationModel(
                        toUserId = post.userId,
                        fromUsername = CurrentUserCache.username,
                        fromFullName = CurrentUserCache.fullName,
                        fromUserProfileBase64 = CurrentUserCache.profileImageBase64,
                        type = "comment",
                        postId = postId,
                        postImageBase64 = post.imageBase64
                    )
                ) {}
            }
        }
    }

    private fun deleteComment(comment: CommentModel) {
        val index = commentList.indexOf(comment)
        if (index != -1) {
            commentList.removeAt(index)
            commentAdapter.notifyItemRemoved(index)
            updateCount(currentCommentCount - 1)
        }

        FireStoreUtil.deleteComment(postId, comment.commentId) { success ->
            if (!isAdded) return@deleteComment
            if (!success) {
                requireActivity().runOnUiThread {
                    if (index != -1) {
                        commentList.add(index, comment)
                        commentAdapter.notifyItemInserted(index)
                        updateCount(currentCommentCount + 1)
                    }
                    Toast.makeText(requireContext(), "Failed to delete comment", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateCount(count: Int) {
        currentCommentCount      = count
        tvCommentCount.text      = count.toString()
        onCommentCountChanged?.invoke(count)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        commentsListener?.remove()
    }
}