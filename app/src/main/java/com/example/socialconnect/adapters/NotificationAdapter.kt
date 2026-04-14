package com.example.socialconnect.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.NotificationModel
import com.example.socialconnect.R
import de.hdodenhof.circleimageview.CircleImageView

class NotificationAdapter(
    private val notifications: MutableList<NotificationModel>,
    private val onClick: (NotificationModel) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotifViewHolder>() {

    inner class NotifViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: CircleImageView = itemView.findViewById(R.id.imgNotifAvatar)
        val tvMessage: TextView = itemView.findViewById(R.id.tvNotifMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvNotifTime)
        val imgPost: ImageView = itemView.findViewById(R.id.imgNotifPost)
        val viewUnseenDot: View = itemView.findViewById(R.id.viewUnseenDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotifViewHolder(view)
    }

    override fun getItemCount(): Int = notifications.size

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        val notif = notifications[position]

        // message based on type
        holder.tvMessage.text = when (notif.type) {
            "like" -> "${notif.fromUsername} liked your post"
            "comment" -> "${notif.fromUsername} commented on your post"
            "follow" -> "${notif.fromUsername} started following you"
            else -> notif.message
        }

        holder.tvTime.text = getTimeAgo(notif.createdAt)

        // unseen dot
        holder.viewUnseenDot.visibility = if (!notif.seen) View.VISIBLE else View.GONE

        // avatar
        ImageUtils.loadBase64(
            notif.fromUserProfileBase64,
            holder.imgAvatar,
            holder.itemView.context.getDrawable(R.drawable.ic_avatar)
        )

        // post thumbnail only for like and comment
        if (notif.type == "like" || notif.type == "comment") {
            holder.imgPost.visibility = View.VISIBLE
            ImageUtils.loadBase64(
                notif.postImageBase64,
                holder.imgPost,
                null
            )
        } else {
            holder.imgPost.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(notif) }
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

    fun markAllSeen() {
        notifications.forEachIndexed { index, notif ->
            if (!notif.seen) {
                notifications[index] = notif.copy(seen = true)
                notifyItemChanged(index)
            }
        }
    }

    fun addNotification(notif: NotificationModel) {
        notifications.add(0, notif)
        notifyItemInserted(0)
    }
}