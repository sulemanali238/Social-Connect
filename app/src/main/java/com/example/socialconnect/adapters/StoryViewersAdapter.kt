package com.example.socialconnect.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.R
import com.example.socialconnect.UserModel
import de.hdodenhof.circleimageview.CircleImageView

class StoryViewersAdapter(private val users: List<UserModel>) :
    RecyclerView.Adapter<StoryViewersAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: CircleImageView = view.findViewById(R.id.imgViewerAvatar)
        val name: TextView          = view.findViewById(R.id.tvViewerName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story_viewer, parent, false)
    )

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = users[position]
        holder.name.text = user.fullName.ifEmpty { user.username }
        if (user.profileImageBase64.isNotEmpty()) {
            ImageUtils.loadBase64(user.profileImageBase64, holder.avatar,
                holder.itemView.context.getDrawable(R.drawable.ic_avatar))
        }
    }
}