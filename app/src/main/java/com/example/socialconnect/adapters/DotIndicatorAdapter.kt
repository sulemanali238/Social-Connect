package com.example.socialconnect.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.R

class DotIndicatorAdapter(
    private val count: Int,
    private var activeIndex: Int
) : RecyclerView.Adapter<DotIndicatorAdapter.DotViewHolder>() {

    inner class DotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dot_indicator, parent, false)
        return DotViewHolder(view)
    }

    override fun getItemCount(): Int = count

    override fun onBindViewHolder(holder: DotViewHolder, position: Int) {
        holder.itemView.isSelected = position == activeIndex
    }

    fun setActive(index: Int) {
        val old = activeIndex
        activeIndex = index
        notifyItemChanged(old)
        notifyItemChanged(index)
    }
}