package com.example.socialconnect.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.ChatModel
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.R
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val onClick: (ChatModel) -> Unit
) : ListAdapter<ChatModel, ChatListAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatar: CircleImageView = view.findViewById(R.id.imgAvatar)
        val tvName: TextView           = view.findViewById(R.id.tvName)
        val tvLastMessage: TextView    = view.findViewById(R.id.tvLastMessage)
        val tvTime: TextView           = view.findViewById(R.id.tvTime)
        val tvUnread: TextView         = view.findViewById(R.id.tvUnreadCount)
        val onlineDot: View            = view.findViewById(R.id.viewOnlineDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val ctx  = holder.itemView.context

        // Static or complex properties that bind fully on initial load
        holder.tvName.text = item.fullName.ifEmpty { item.username }

        ImageUtils.loadBase64(
            base64      = item.profileImageBase64,
            imageView   = holder.imgAvatar,
            placeholder = ContextCompat.getDrawable(ctx, R.drawable.ic_avatar)
        )

        // Delegate dynamic/frequent updates to a separate function
        bindDynamicContent(holder, item)

        holder.itemView.setOnClickListener { onClick(item) }
    }

    // Handles partial updates (payloads) to prevent row flickering on updates
    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val item = getItem(position)
            bindDynamicContent(holder, item)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun bindDynamicContent(holder: VH, item: ChatModel) {
        val ctx = holder.itemView.context

        // Last message preview
        holder.tvLastMessage.text = item.lastMessage.ifEmpty { "Say hello 👋" }

        // Time formatting
        holder.tvTime.text = if (item.lastMessageTime > 0L) formatTime(item.lastMessageTime) else ""

        // Unread badge & text coloring
        if (item.unreadCount > 0) {
            holder.tvUnread.visibility = View.VISIBLE
            holder.tvUnread.text       = item.unreadCount.toString()
            holder.tvLastMessage.setTextColor(ContextCompat.getColor(ctx, R.color.teal_green))
        } else {
            holder.tvUnread.visibility = View.GONE
            holder.tvLastMessage.setTextColor(ContextCompat.getColor(ctx, R.color.gray_muted))
        }

        // Online dot
        holder.onlineDot.visibility = if (item.isOnline) View.VISIBLE else View.GONE
    }

    private fun formatTime(timestamp: Long): String {
        val now  = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000       -> "Just now"
            diff < 3_600_000    -> "${diff / 60_000}m"
            diff < 86_400_000   -> TIME_FORMAT.format(Date(timestamp))
            else                -> DATE_FORMAT.format(Date(timestamp))
        }
    }

    companion object {
        // Formatter instances cached here to prevent memory churn/scrolling lag
        private val TIME_FORMAT by lazy { SimpleDateFormat("h:mm a", Locale.getDefault()) }
        private val DATE_FORMAT by lazy { SimpleDateFormat("MMM d", Locale.getDefault()) }

        val DIFF = object : DiffUtil.ItemCallback<ChatModel>() {
            override fun areItemsTheSame(a: ChatModel, b: ChatModel) = a.uid == b.uid
            override fun areContentsTheSame(a: ChatModel, b: ChatModel) = a == b

            // Tells the adapter to update only the text/status, not reload the whole view
            override fun getChangePayload(oldItem: ChatModel, newItem: ChatModel): Any? {
                return if (oldItem.lastMessage != newItem.lastMessage ||
                    oldItem.isOnline != newItem.isOnline ||
                    oldItem.unreadCount != newItem.unreadCount) {
                    true
                } else {
                    super.getChangePayload(oldItem, newItem)
                }
            }
        }
    }
}