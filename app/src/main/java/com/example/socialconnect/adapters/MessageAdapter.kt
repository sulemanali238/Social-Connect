package com.example.socialconnect.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.MessageModel
import com.example.socialconnect.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val messages: List<MessageModel>,
    private val currentUid: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT     = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    // ─── ViewHolders ──────────────────────────────────────────────────

    inner class SentVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvText:       TextView  = view.findViewById(R.id.tvMessageText)
        val tvTime:       TextView  = view.findViewById(R.id.tvTime)
        val ivReadReceipt: ImageView = view.findViewById(R.id.ivReadReceipt)
    }

    inner class ReceivedVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tvMessageText)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    // ─── Adapter overrides ────────────────────────────────────────────

    override fun getItemViewType(position: Int): Int =
        if (messages[position].senderId == currentUid) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            SentVH(inflater.inflate(R.layout.item_message_sent, parent, false))
        } else {
            ReceivedVH(inflater.inflate(R.layout.item_message_received, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))

        when (holder) {
            is SentVH -> {
                holder.tvText.text = message.text
                holder.tvTime.text = timeStr

                // Read receipt — teal tick if read, grey if still unread
                if (message.isRead) {
                    holder.ivReadReceipt.setImageResource(R.drawable.ic_check)
                    holder.ivReadReceipt.setColorFilter(
                        holder.itemView.context.getColor(R.color.teal_green)
                    )
                } else {
                    holder.ivReadReceipt.setImageResource(R.drawable.ic_check)
                    holder.ivReadReceipt.setColorFilter(
                        holder.itemView.context.getColor(R.color.gray_muted)
                    )
                }
            }

            is ReceivedVH -> {
                holder.tvText.text = message.text
                holder.tvTime.text = timeStr
            }
        }
    }

    override fun getItemCount() = messages.size
}