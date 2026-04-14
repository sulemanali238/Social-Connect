package com.example.socialconnect.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.PostModel
import com.example.socialconnect.R

class ProfilePostAdapter(
    private val posts: MutableList<PostModel>,
    private val onClick: (PostModel) -> Unit
) : RecyclerView.Adapter<ProfilePostAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgPost: ImageView = itemView.findViewById(R.id.imgGridPost)
        val icMultiple: ImageView = itemView.findViewById(R.id.icMultiple)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_post, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]

        if (post.imageBase64.isNotEmpty()) {
            ImageUtils.loadBase64(
                post.imageBase64,
                holder.imgPost,
                holder.itemView.context.getDrawable(R.drawable.ic_avatar)
            )
        } else {
            holder.imgPost.setBackgroundColor(
                "#F3F4F6".toColorInt()
            )
        }

        holder.icMultiple.visibility = View.GONE

        holder.itemView.setOnClickListener { onClick(post) }
    }

    fun addPost(post: PostModel) {
        posts.add(0, post)
        notifyItemInserted(0)
    }

    fun removePost(postId: String) {
        val index = posts.indexOfFirst { it.postId == postId }
        if (index != -1) {
            posts.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}