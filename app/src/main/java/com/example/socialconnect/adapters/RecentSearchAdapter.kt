package com.example.socialconnect.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.R

class RecentSearchAdapter(
    private val searches: MutableList<String>,
    private val onItemClick: (String) -> Unit,
    private val onRemoveClick: (String) -> Unit
) : RecyclerView.Adapter<RecentSearchAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuery: TextView = itemView.findViewById(R.id.tvRecentQuery)
        val btnRemove: ImageView = itemView.findViewById(R.id.btnRemoveRecent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_search, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = searches.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val query = searches[position]
        holder.tvQuery.text = query
        holder.itemView.setOnClickListener { onItemClick(query) }
        holder.btnRemove.setOnClickListener { onRemoveClick(query) }
    }
}