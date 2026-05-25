package com.example.socialconnect

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import android.widget.Toast
import com.example.socialconnect.adapters.PostAdapter

class PostInteractionHandler {

    private val fragment: Fragment?
    private val activity: AppCompatActivity?
    private val postList: MutableList<PostModel>
    private val postAdapter: PostAdapter
    private val pendingCounterSet: MutableSet<String>

    constructor(
        fragment: Fragment,
        postList: MutableList<PostModel>,
        postAdapter: PostAdapter,
        pendingCounterSet: MutableSet<String>
    ) {
        this.fragment         = fragment
        this.activity         = null
        this.postList         = postList
        this.postAdapter      = postAdapter
        this.pendingCounterSet = pendingCounterSet
    }

    constructor(
        activity: AppCompatActivity,
        postList: MutableList<PostModel>,
        postAdapter: PostAdapter,
        pendingCounterSet: MutableSet<String>
    ) {
        this.fragment         = null
        this.activity         = activity
        this.postList         = postList
        this.postAdapter      = postAdapter
        this.pendingCounterSet = pendingCounterSet
    }

    private val ctx get() = fragment?.requireContext() ?: activity!!
    private val isAdded get() = fragment?.isAdded ?: true
    private val childFm get() = fragment?.childFragmentManager
        ?: (activity as AppCompatActivity).supportFragmentManager

    fun handleLike(post: PostModel, position: Int, sendNotif: Boolean = true) {
        val current = postList.getOrNull(position) ?: return
        val newIsLiked = !current.isLiked
        pendingCounterSet.add(post.postId)
        val newCount = if (newIsLiked) current.likeCount + 1 else (current.likeCount - 1).coerceAtLeast(0)
        postList[position] = current.copy(isLiked = newIsLiked, likeCount = newCount)
        postAdapter.notifyItemChanged(position, PostAdapter.PAYLOAD_INTERACTION)

        FireStoreUtil.likePost(post.postId, current.isLiked) {
            fragment?.view?.postDelayed({ pendingCounterSet.remove(post.postId) }, 1500)
                ?: activity?.window?.decorView?.postDelayed({ pendingCounterSet.remove(post.postId) }, 1500)

            if (newIsLiked && sendNotif && post.userId != AuthUtil.currentUid) {
                FireStoreUtil.sendNotification(
                    ctx,
                    NotificationModel(
                        toUserId              = post.userId,
                        fromUsername          = CurrentUserCache.username,
                        fromFullName          = CurrentUserCache.fullName,
                        fromUserProfileBase64 = CurrentUserCache.profileImageBase64,
                        type                  = "like",
                        postId                = post.postId,
                        postImageBase64       = post.imageBase64
                    )
                ) {}
            }
        }
    }

    // PostInteractionHandler — only handleBookmark changed
    fun handleBookmark(post: PostModel, position: Int) {
        val current = postList.getOrNull(position) ?: return
        postList[position] = current.copy(isBookmarked = !current.isBookmarked)
        postAdapter.notifyPostChanged(position)

        FireStoreUtil.bookmarkPost(post.postId, current.isBookmarked) { success ->
            if (!success || !isAdded) {
                postList[position] = current
                postAdapter.notifyPostChanged(position)
            }
        }
    }

    fun handleComment(post: PostModel) {
        val sheet = BottomSheetComment.newInstance(post.postId, post.commentCount)
        sheet.setOnCommentCountChanged { newCount ->
            val position = postList.indexOfFirst { it.postId == post.postId }
            if (position != -1) {
                postList[position] = postList[position].copy(commentCount = newCount)
                postAdapter.notifyItemChanged(position, PostAdapter.PAYLOAD_COUNTS)
            }
        }
        sheet.show(childFm, BottomSheetComment.TAG)
    }

    fun handleShare(post: PostModel) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "${post.username} shared a post")
            type = "text/plain"
        }
        ctx.startActivity(Intent.createChooser(intent, "Share via"))
    }

    fun handleFollow(targetUid: String, isCurrentlyFollowing: Boolean, onResult: (Boolean) -> Unit) {
        if (isCurrentlyFollowing) {
            FireStoreUtil.unfollowUser(targetUid) { success ->
                if (success) onResult(false)
            }
        } else {
            FireStoreUtil.followUser(targetUid) { success ->
                if (success) {
                    // Send notification
                    if (targetUid != AuthUtil.currentUid) {
                        FireStoreUtil.sendNotification(
                            ctx,
                            NotificationModel(
                                toUserId              = targetUid,
                                fromUsername          = CurrentUserCache.username,
                                fromFullName          = CurrentUserCache.fullName,
                                fromUserProfileBase64 = CurrentUserCache.profileImageBase64,
                                type                  = "follow",
                                postId                = "",
                                postImageBase64       = ""
                            )
                        ) {}
                    }
                    onResult(true)
                }
            }
        }
    }

    fun showDeleteDialog(post: PostModel, onDeleted: (PostModel) -> Unit) {
        AlertDialog.Builder(ctx)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                FireStoreUtil.deletePost(post.postId) { success ->
                    if (!isAdded) return@deletePost
                    if (success) {
                        onDeleted(post)
                        Toast.makeText(ctx, "Post deleted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(ctx, "Failed to delete", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun fetchStatusesInParallel(
        newPosts: List<PostModel>,
        onReady: (List<PostModel>) -> Unit
    ) {
        val total = newPosts.size
        var completed = 0
        val readyPosts = mutableListOf<PostModel>()

        newPosts.forEach { post ->
            if (post.postId.isEmpty()) {
                synchronized(readyPosts) {
                    completed++
                    if (completed == total) onReady(readyPosts.toList())
                }
                return@forEach
            }
            FireStoreUtil.isPostLiked(post.postId) { isLiked ->
                FireStoreUtil.isPostBookmarked(post.postId) { isBookmarked ->
                    synchronized(readyPosts) {
                        readyPosts.add(post.copy(isLiked = isLiked, isBookmarked = isBookmarked))
                        completed++
                        if (completed == total) onReady(readyPosts.toList())
                    }
                }
            }
        }
    }
}