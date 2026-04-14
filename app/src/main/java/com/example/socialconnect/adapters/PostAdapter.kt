package com.example.socialconnect.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.PostModel
import com.example.socialconnect.R
import de.hdodenhof.circleimageview.CircleImageView
import androidx.core.graphics.toColorInt

class PostAdapter(

    private val posts: MutableList<PostModel>,
    private val onLikeClick: (PostModel, Int) -> Unit,
    private val onCommentClick: (PostModel) -> Unit,
    private val onBookmarkClick: (PostModel, Int) -> Unit,
    private val onShareClick: (PostModel) -> Unit,
    private val onAvatarClick: (PostModel) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: CircleImageView = itemView.findViewById(R.id.imgPostAvatar)
        val tvUsername: TextView = itemView.findViewById(R.id.tvPostUsername)
        val tvTime: TextView = itemView.findViewById(R.id.tvPostTime)
        val imgContent: ImageView = itemView.findViewById(R.id.imgPostContent)
        val tvCaption: TextView = itemView.findViewById(R.id.tvPostCaption)
        val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        val btnComment: ImageView = itemView.findViewById(R.id.btnComment)
        val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)
        val btnBookmark: ImageView = itemView.findViewById(R.id.btnBookmark)
        val btnShare: ImageView = itemView.findViewById(R.id.btnShare)
        val btnMore: ImageView = itemView.findViewById(R.id.btnPostMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        holder.tvUsername.text = post.username
        holder.tvTime.text = getTimeAgo(post.createdAt)
        holder.tvLikeCount.text = post.likeCount.toString()
        holder.tvCommentCount.text = post.commentCount.toString()

        // Avatar using base64
        ImageUtils.loadBase64(
            post.userProfileBase64,
            holder.imgAvatar,
            holder.itemView.context.getDrawable(R.drawable.ic_avatar)
        )

        // Post image using base64
        if (post.imageBase64.isNotEmpty()) {
            holder.imgContent.visibility = View.VISIBLE
            ImageUtils.loadBase64(
                post.imageBase64,
                holder.imgContent,
                null
            )
        } else {
            holder.imgContent.visibility = View.GONE
        }

        // Caption
        if (post.caption.isNotEmpty()) {
            holder.tvCaption.visibility = View.VISIBLE
            holder.tvCaption.text = post.caption
        } else {
            holder.tvCaption.visibility = View.GONE
        }

        // Like state
        if (post.isLiked) {
            holder.btnLike.setImageResource(R.drawable.ic_heart_filled)
            holder.btnLike.setColorFilter(Color.RED)
            holder.tvLikeCount.setTextColor(Color.RED)
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline)
            holder.btnLike.setColorFilter("#9CA3AF".toColorInt())
            holder.tvLikeCount.setTextColor("#9CA3AF".toColorInt())
        }

        // Bookmark state
        if (post.isBookmarked) {
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_filled)
            holder.btnBookmark.setColorFilter("#00BFA5".toColorInt())
        } else {
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_outline)
            holder.btnBookmark.setColorFilter("#9CA3AF".toColorInt())
        }

        // Clicks
        holder.btnLike.setOnClickListener { onLikeClick(post, position) }
        holder.btnComment.setOnClickListener { onCommentClick(post) }
        holder.btnBookmark.setOnClickListener { onBookmarkClick(post, position) }
        holder.btnShare.setOnClickListener { onShareClick(post) }
        holder.imgAvatar.setOnClickListener { onAvatarClick(post) }
        holder.tvUsername.setOnClickListener { onAvatarClick(post) }
    }

    private fun getTimeAgo(time: Long): String {
        val diff = System.currentTimeMillis() - time
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> "${diff / 86_400_000}d ago"
        }
    }

    fun updatePost(position: Int, updatedPost: PostModel) {
        posts[position] = updatedPost
        notifyItemChanged(position)
    }
}