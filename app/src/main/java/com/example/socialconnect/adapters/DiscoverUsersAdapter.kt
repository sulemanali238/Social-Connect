package com.example.socialconnect.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.R
import com.example.socialconnect.UserModel
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView

class DiscoverUsersAdapter(
    private val users: MutableList<UserModel>,
    private val onFollowClick: (UserModel, Int) -> Unit,
    private val onUserClick: (UserModel) -> Unit
) : RecyclerView.Adapter<DiscoverUsersAdapter.DiscoverVH>() {

    inner class DiscoverVH(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: CircleImageView  = view.findViewById(R.id.imgDiscoverAvatar)
        val fullName: TextView       = view.findViewById(R.id.tvDiscoverFullName)
        val username: TextView       = view.findViewById(R.id.tvDiscoverUsername)
        val btnFollow: MaterialButton = view.findViewById(R.id.btnDiscoverFollow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoverVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_discover_user, parent, false)
        return DiscoverVH(view)
    }

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: DiscoverVH, position: Int) {
        val user = users[position]

        holder.fullName.text = user.fullName
        holder.username.text = "@${user.username}"

        if (user.profileImageBase64.isNotEmpty()) {
            ImageUtils.loadBase64(
                user.profileImageBase64,
                holder.avatar,
                holder.itemView.context.getDrawable(R.drawable.ic_avatar)
            )
        } else {
            holder.avatar.setImageResource(R.drawable.ic_avatar)
        }

        updateFollowButton(holder.btnFollow, user.isFollowing)

        holder.btnFollow.setOnClickListener {
            onFollowClick(user, holder.adapterPosition)
        }

        holder.avatar.setOnClickListener {
            onUserClick(user)
        }

        holder.fullName.setOnClickListener {
            onUserClick(user)
        }
    }

    fun updateFollowState(position: Int, isFollowing: Boolean) {
        if (position < 0 || position >= users.size) return
        users[position] = users[position].copy(isFollowing = isFollowing)
        notifyItemChanged(position)
    }

    private fun updateFollowButton(btn: MaterialButton, isFollowing: Boolean) {
        if (isFollowing) {
            btn.text = "Following"
            btn.setTextColor(0xFF000000.toInt())
            btn.backgroundTintList = ColorStateList.valueOf(0xFFF2F2F2.toInt())
        } else {
            btn.text = "Follow"
            btn.setTextColor(0xFFFFFFFF.toInt())
            btn.backgroundTintList = ColorStateList.valueOf(0xFF1D9E75.toInt())
        }
    }
}