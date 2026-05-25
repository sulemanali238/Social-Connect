package com.example.socialconnect.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.FireStoreUtil
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.NotificationModel
import com.example.socialconnect.R
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Calendar
import androidx.core.graphics.toColorInt
import com.example.socialconnect.UserModel

class NotificationAdapter(
    private val notifications: MutableList<NotificationModel>,
    private val onClick: (NotificationModel) -> Unit,
    private val onMenuClick: (NotificationModel, View) -> Unit,
    private val userMap: MutableMap<String, UserModel?>,


) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM   = 1
    }

    // Sealed list: either a header string or a notif
    private val displayList = mutableListOf<Any>()

    init { buildDisplayList() }

    private fun buildDisplayList() {
        displayList.clear()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val today   = notifications.filter { it.createdAt >= todayStart }
        val earlier = notifications.filter { it.createdAt <  todayStart }

        if (today.isNotEmpty()) {
            displayList.add("Today")
            displayList.addAll(today)
        }
        if (earlier.isNotEmpty()) {
            displayList.add("Earlier")
            displayList.addAll(earlier)
        }
    }

    // ── ViewHolders ──────────────────────────────────────────────

    inner class HeaderViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvHeader: TextView = v.findViewById(R.id.tvNotifHeader)
    }

    inner class NotifViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imgAvatar:     CircleImageView = v.findViewById(R.id.imgNotifAvatar)
        val tvMessage:     TextView        = v.findViewById(R.id.tvNotifMessage)
        val tvTime:        TextView        = v.findViewById(R.id.tvNotifTime)
        val viewUnseenDot: View            = v.findViewById(R.id.viewUnseenDot)
        val btnMenu:       ImageView       = v.findViewById(R.id.btnNotifMenu)
        val imgTypeBadge:   ImageView       = v.findViewById(R.id.imgNotifTypeBadge)
    }
    // ── Adapter overrides ────────────────────────────────────────

    override fun getItemViewType(position: Int) =
        if (displayList[position] is String) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM

    override fun getItemCount() = displayList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_notif_header, parent, false))
        } else {
            NotifViewHolder(inflater.inflate(R.layout.item_notification, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.tvHeader.text = displayList[position] as String
            return
        }

        val notif = displayList[position] as NotificationModel
        holder as NotifViewHolder

        val user  = userMap[notif.fromUserId]
        val name  = user?.fullName?.ifEmpty { notif.fromFullName } ?: notif.fromFullName
        val avatar = user?.profileImageBase64 ?: notif.fromUserProfileBase64

        holder.tvTime.text = getTimeAgo(notif.createdAt)
        holder.viewUnseenDot.visibility = if (!notif.seen) View.VISIBLE else View.GONE
        holder.itemView.setBackgroundResource(
            if (!notif.seen) R.color.notif_unseen_bg else android.R.color.transparent
        )

        holder.tvMessage.text = when (notif.type) {
            "like"    -> "$name liked your post"
            "comment" -> "$name commented on your post"
            "follow"  -> "$name started following you"
            else      -> notif.message
        }

        when (notif.type) {
            "like" -> {
                holder.imgTypeBadge.visibility = View.VISIBLE
                holder.imgTypeBadge.setImageResource(R.drawable.ic_heart_filled)
                holder.imgTypeBadge.setColorFilter("#EF4444".toColorInt())
                holder.imgTypeBadge.setBackgroundResource(R.drawable.bg_notif_badge_red)
            }
            "comment" -> {
                holder.imgTypeBadge.visibility = View.VISIBLE
                holder.imgTypeBadge.setImageResource(R.drawable.ic_comment)
                holder.imgTypeBadge.setColorFilter("#0284C7".toColorInt())
                holder.imgTypeBadge.setBackgroundResource(R.drawable.bg_notif_badge_blue)
            }
            "follow" -> {
                holder.imgTypeBadge.visibility = View.VISIBLE
                holder.imgTypeBadge.setImageResource(R.drawable.ic_connect)
                holder.imgTypeBadge.setColorFilter("#16A34A".toColorInt())
                holder.imgTypeBadge.setBackgroundResource(R.drawable.bg_notif_badge_green)
            }
            else -> holder.imgTypeBadge.visibility = View.GONE
        }

        if (!avatar.isNullOrEmpty()) {
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

        val notifRow = holder.itemView.findViewById<View>(R.id.notifRow)
        notifRow.setOnClickListener { onClick(notif) }
        holder.btnMenu.setOnClickListener { onMenuClick(notif, it) }
        holder.btnMenu.visibility =
            if (notif.notificationId.isEmpty()) View.INVISIBLE else View.VISIBLE
    }

    // ── Public helpers ───────────────────────────────────────────

    fun markAllSeen() {
        notifications.forEachIndexed { i, n ->
            if (!n.seen) notifications[i] = n.copy(seen = true)
        }
        buildDisplayList()
        notifyDataSetChanged()
    }

    fun addNotification(notif: NotificationModel) {
        notifications.add(0, notif)
        buildDisplayList()
        notifyDataSetChanged()
    }

    fun replaceAll(newList: List<NotificationModel>) {
        notifications.clear()
        notifications.addAll(newList)
        buildDisplayList()
        notifyDataSetChanged()
    }
    fun removeNotification(notif: NotificationModel) {
        notifications.remove(notif)
        buildDisplayList()
        notifyDataSetChanged()
    }

    fun markSingleSeen(notif: NotificationModel) {
        val index = notifications.indexOfFirst { it.notificationId == notif.notificationId }
        if (index != -1) {
            notifications[index] = notifications[index].copy(seen = true)
            buildDisplayList()
            notifyDataSetChanged()
        }
    }


    private fun getTimeAgo(time: Long): String {
        val diff = System.currentTimeMillis() - time
        return when {
            diff < 60_000L        -> "Just now"
            diff < 3_600_000L     -> "${diff / 60_000}m ago"
            diff < 86_400_000L    -> "${diff / 3_600_000}h ago"
            diff < 604_800_000L   -> "${diff / 86_400_000}d ago"
            else                  -> "${diff / 604_800_000}w ago"
        }
    }
}