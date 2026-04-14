package com.example.socialconnect.adapters

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

        holder.itemView.setOnClickListener { onUserClick(user) }
        holder.btnFollow.setOnClickListener { onFollowClick(user, position) }
    }

    fun updateList(newList: List<UserModel>) {
        users.clear()
        users.addAll(newList)
        notifyDataSetChanged()
    }
}