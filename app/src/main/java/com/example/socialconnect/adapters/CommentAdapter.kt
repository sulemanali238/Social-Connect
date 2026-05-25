package com.example.socialconnect.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.CommentModel
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.R
import com.example.socialconnect.UserModel
import de.hdodenhof.circleimageview.CircleImageView

class CommentAdapter(
    private val comments: MutableList<CommentModel>,
    private val userMap: Map<String, UserModel>,
    private val onDeleteClick: (CommentModel) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: CircleImageView = itemView.findViewById(R.id.imgCommentAvatar)
        val tvUsername: TextView       = itemView.findViewById(R.id.tvCommentUsername)
        val tvText: TextView           = itemView.findViewById(R.id.tvCommentText)
        val tvTime: TextView           = itemView.findViewById(R.id.tvCommentTime)
        val btnDelete: ImageView       = itemView.findViewById(R.id.btnCommentDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CommentViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
    )

    override fun getItemCount() = comments.size

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        val user = userMap[comment.userId]

        holder.tvText.text     = comment.text
        holder.tvTime.text     = getTimeAgo(comment.createdAt)
        holder.tvUsername.text = user?.fullName?.ifEmpty { comment.fullName } ?: comment.fullName

        val avatar = user?.profileImageBase64 ?: comment.userProfileBase64
        if (avatar.isNotEmpty()) {
            ImageUtils.loadBase64(
                avatar,
                holder.imgAvatar,
                holder.itemView.context.getDrawable(R.drawable.ic_avatar)
            )
        } else {
            holder.imgAvatar.setImageDrawable(
                holder.itemView.context.getDrawable(R.drawable.ic_avatar)
            )
        }

        holder.btnDelete.visibility =
            if (comment.userId == com.example.socialconnect.AuthUtil.currentUid)
                View.VISIBLE else View.GONE
        holder.btnDelete.setOnClickListener { onDeleteClick(comment) }
    }

    private fun getTimeAgo(time: Long): String {
        val diff = System.currentTimeMillis() - time
        return when {
            diff < 60_000     -> "Just now"
            diff < 3_600_000  -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else              -> "${diff / 86_400_000}d ago"
        }
    }
}