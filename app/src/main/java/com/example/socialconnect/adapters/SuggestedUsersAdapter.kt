package com.example.socialconnect.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.R
import com.example.socialconnect.UserModel
import de.hdodenhof.circleimageview.CircleImageView

class SuggestedUsersAdapter(
    private val users: MutableList<UserModel>,
    private val onFollowClick: (UserModel, Int) -> Unit,
    private val onUserClick: (UserModel) -> Unit
) : RecyclerView.Adapter<SuggestedUsersAdapter.SuggestedVH>() {

    inner class SuggestedVH(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: CircleImageView = view.findViewById(R.id.imgSuggestedAvatar)
        val name: TextView = view.findViewById(R.id.tvSuggestedName)
        val username: TextView = view.findViewById(R.id.tvSuggestedUsername)
        val btnFollow: Button = view.findViewById(R.id.btnSuggestedFollow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestedVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggested_user, parent, false)
        return SuggestedVH(view)
    }

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: SuggestedVH, position: Int) {
        val user = users[position]

        holder.name.text = user.fullName
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

        // Button state
        updateFollowButton(holder.btnFollow, user.isFollowing)

        holder.btnFollow.setOnClickListener {
            onFollowClick(user, holder.adapterPosition)
        }

        holder.avatar.setOnClickListener {
            onUserClick(user)
        }
        holder.name.setOnClickListener {
            onUserClick(user)
        }
    }

    fun updateFollowState(position: Int, isFollowing: Boolean) {
        if (position < 0 || position >= users.size) return
        users[position] = users[position].copy(isFollowing = isFollowing)
        notifyItemChanged(position)
    }

    private fun updateFollowButton(btn: Button, isFollowing: Boolean) {
        if (isFollowing) {
            btn.text = "Following"
            btn.setTextColor(0xFF6B7280.toInt())
            btn.backgroundTintList = ColorStateList.valueOf(0x00000000)
            (btn as? com.google.android.material.button.MaterialButton)?.strokeColor =
                ColorStateList.valueOf(0xFFE5E7EB.toInt())
        } else {
            btn.text = "Follow"
            btn.setTextColor(0xFF00BFA5.toInt())
            btn.backgroundTintList = ColorStateList.valueOf(0x00000000)
            (btn as? com.google.android.material.button.MaterialButton)?.strokeColor =
                ColorStateList.valueOf(0xFF00BFA5.toInt())
        }
    }
}