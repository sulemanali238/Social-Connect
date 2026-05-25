package com.example.socialconnect.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.R
import com.example.socialconnect.StoryModel
import de.hdodenhof.circleimageview.CircleImageView

class StoryAdapter(
    private val stories: MutableList<StoryModel>,
    private val onClick: (StoryModel, Int) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    inner class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgStoryAvatar: CircleImageView = itemView.findViewById(R.id.imgStoryAvatar)
        val tvFullName: TextView            = itemView.findViewById(R.id.tvStoryFullName)
        val tvTime: TextView                = itemView.findViewById(R.id.tvStoryTime)
        val ringUnseen: View                = itemView.findViewById(R.id.ringUnseen)
        val ringSeen: View                  = itemView.findViewById(R.id.ringSeen)
        val ivMoreOptions: ImageView = itemView.findViewById(R.id.ivMoreOptions)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder =
        StoryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_story, parent, false))

    override fun getItemCount(): Int = stories.size

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]
        holder.tvFullName.text = story.fullName

        if (story.seen) {
            holder.ringUnseen.visibility = View.GONE
            holder.ringSeen.visibility   = View.VISIBLE
            holder.tvTime.visibility     = View.GONE
        } else {
            holder.ringUnseen.visibility = View.VISIBLE
            holder.ringSeen.visibility   = View.GONE
            holder.tvTime.visibility     = View.VISIBLE
            holder.tvTime.text           = getTimeAgo(story.createdAt)
        }

        val imageToShow = if (story.imageBase64.isNotEmpty()) story.imageBase64
        else story.userProfileBase64

        if (imageToShow.isNotEmpty()) {
            ImageUtils.loadBase64(
                imageToShow,
                holder.imgStoryAvatar,
                holder.itemView.context.getDrawable(R.drawable.ic_avatar)
            )
        } else {
            holder.imgStoryAvatar.setImageDrawable(
                holder.itemView.context.getDrawable(R.drawable.ic_avatar)
            )
        }

        holder.itemView.setOnClickListener { onClick(story, position) }

        holder.ivMoreOptions.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(view.context, view)
            popup.menu.add("Mute")
            popup.menu.add("Report")
            popup.menu.add("Hide")
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Mute"   -> { true }
                    "Report" -> { true }
                    "Hide"   -> { true }
                    else     -> false
                }
            }
            popup.show()
        }
    }

    /** Replace entire list and refresh — called from StoryFragment after reload. */
    fun replaceAll(newStories: List<StoryModel>) {
        stories.clear()
        stories.addAll(newStories)
        notifyDataSetChanged()
    }

    /** Mark one row as seen in-place without full reload. */
    fun markAsSeen(position: Int) {
        if (position in 0 until stories.size) {
            stories[position] = stories[position].copy(seen = true)
            notifyItemChanged(position)
        }
    }
    fun enrichStory(position: Int, fullName: String, profileBase64: String) {
        if (position in 0 until stories.size) {
            stories[position] = stories[position].copy(
                fullName = fullName,
                userProfileBase64 = profileBase64
            )
            notifyItemChanged(position)
        }
    }

    fun addStory(story: StoryModel) {
        stories.add(0, story)
        notifyItemInserted(0)
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