package com.example.socialconnect.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.R
import com.example.socialconnect.StoryModel
import de.hdodenhof.circleimageview.CircleImageView

class StoryAdapter(
    private val stories: MutableList<StoryModel>,
    private val onClick: (StoryModel) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    inner class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: CircleImageView = itemView.findViewById(R.id.imgStoryAvatar)
        val tvUsername: TextView = itemView.findViewById(R.id.tvStoryUsername)
        val tvTime: TextView = itemView.findViewById(R.id.tvStoryTime)
        val viewSeenDot: View = itemView.findViewById(R.id.viewSeenDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun getItemCount(): Int = stories.size

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]

        holder.tvUsername.text = story.username
        holder.tvTime.text = getTimeAgo(story.createdAt)

        // seen dot — visible if story is already seen
        holder.viewSeenDot.visibility = if (story.seen) View.VISIBLE else View.GONE

        // story ring color change if seen
        val ringView = (holder.itemView as ViewGroup).getChildAt(0)
        ringView.alpha = if (story.seen) 0.4f else 1f

        // load avatar
        ImageUtils.loadBase64(
            story.userProfileBase64,
            holder.imgAvatar,
            holder.itemView.context.getDrawable(R.drawable.ic_avatar)
        )

        holder.itemView.setOnClickListener { onClick(story) }
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

    fun addStory(story: StoryModel) {
        stories.add(0, story)
        notifyItemInserted(0)
    }

    fun markAsSeen(position: Int) {
        stories[position] = stories[position].copy(seen = true)
        notifyItemChanged(position)
    }
}