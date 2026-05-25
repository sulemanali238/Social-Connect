package com.example.socialconnect.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.R
import com.example.socialconnect.UserModel
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView

class SearchAdapter(
    private val users: MutableList<UserModel>,
    private val onUserClick: (UserModel) -> Unit,
    private val onFollowClick: (UserModel, Int) -> Unit
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    inner class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: CircleImageView = itemView.findViewById(R.id.imgSearchAvatar)
        val tvFullName: TextView = itemView.findViewById(R.id.tvSearchFullName)
        val tvUsername: TextView = itemView.findViewById(R.id.tvSearchUserName)
        val btnFollow: MaterialButton = itemView.findViewById(R.id.btnFollow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_user, parent, false)
        return SearchViewHolder(view)
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val user = users[position]

        holder.tvFullName.text = user.fullName
        holder.tvUsername.text = "@${user.username}"

        ImageUtils.loadBase64(
            user.profileImageBase64,
            holder.imgAvatar,
            holder.itemView.context.getDrawable(R.drawable.ic_avatar)
        )

        // ✅ Firestore call nahi — model se directly state lo
        applyFollowState(holder, user.isFollowing)

        holder.itemView.setOnClickListener { onUserClick(user) }
        holder.btnFollow.setOnClickListener { onFollowClick(user, position) }
    }

    private fun applyFollowState(holder: SearchViewHolder, isFollowing: Boolean) {
        if (isFollowing) {
            holder.btnFollow.text = "Unfollow"
            holder.btnFollow.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            holder.btnFollow.setTextColor(Color.BLACK)
            holder.btnFollow.strokeWidth = 2
            holder.btnFollow.strokeColor = ColorStateList.valueOf("#DBDBDB".toColorInt())
        } else {
            holder.btnFollow.text = "Follow"
            holder.btnFollow.backgroundTintList = ColorStateList.valueOf("#008080".toColorInt())
            holder.btnFollow.setTextColor(Color.WHITE)
            holder.btnFollow.strokeWidth = 0
        }
    }
}