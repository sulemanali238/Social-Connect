package com.example.socialconnect.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.socialconnect.PostModel
import com.example.socialconnect.R
import com.example.socialconnect.UserModel
import de.hdodenhof.circleimageview.CircleImageView
import androidx.core.graphics.toColorInt
import androidx.core.graphics.drawable.toDrawable

class PostAdapter(
    private val posts: MutableList<PostModel>,
    private val onLikeClick: (PostModel, Int) -> Unit,
    private val onCommentClick: (PostModel) -> Unit,
    private val onBookmarkClick: (PostModel, Int) -> Unit,
    private val onShareClick: (PostModel) -> Unit,
    private val onAvatarClick: (PostModel) -> Unit,
    private val onDeleteClick: (PostModel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val PAYLOAD_COUNTS      = "counts_only"
        const val PAYLOAD_INTERACTION = "interaction"
        const val VIEW_TYPE_SUGGEST   = 0
        const val VIEW_TYPE_POST      = 1
    }

    // ── Suggested strip state ────────────────────────────────────────
    private var suggestedUsers: MutableList<UserModel> = mutableListOf()
    private var onFollowClick: ((UserModel, Int) -> Unit)? = null
    private var onSeeAllClick: (() -> Unit)? = null
    private var onSuggestedUserClick: ((UserModel) -> Unit)? = null
    private var suggestedAdapter: SuggestedUsersAdapter? = null

    fun setSuggestedUsers(
        users: List<UserModel>,
        onFollow: (UserModel, Int) -> Unit,
        onSeeAll: () -> Unit,
        onUserClick: (UserModel) -> Unit
    ) {
        suggestedUsers       = users.toMutableList()
        onFollowClick        = onFollow
        onSeeAllClick        = onSeeAll
        onSuggestedUserClick = onUserClick
        notifyItemChanged(0)
    }

    fun updateSuggestedFollowState(position: Int, isFollowing: Boolean) {
        if (position < 0 || position >= suggestedUsers.size) return
        suggestedUsers[position] = suggestedUsers[position].copy(isFollowing = isFollowing)
        suggestedAdapter?.updateFollowState(position, isFollowing)
    }

    // ── View holders ────────────────────────────────────────────────

    inner class SuggestStripViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rvSuggested: RecyclerView = view.findViewById(R.id.rvSuggestedUsers)
        val tvSeeAll: TextView        = view.findViewById(R.id.tvSeeAll)
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: CircleImageView  = itemView.findViewById(R.id.imgPostAvatar)
        val tvFullName: TextView        = itemView.findViewById(R.id.tvPostName)
        val tvTime: TextView            = itemView.findViewById(R.id.tvPostTime)
        val layoutPostImages: View      = itemView.findViewById(R.id.layoutPostImages)
        val vpPostImages: ViewPager2    = itemView.findViewById(R.id.vpPostImages)
        val tvImageCounter: TextView    = itemView.findViewById(R.id.tvImageCounter)
        val rvDots: RecyclerView        = itemView.findViewById(R.id.rvDots)
        val tvCaption: TextView         = itemView.findViewById(R.id.tvPostCaption)
        val btnLike: ImageView          = itemView.findViewById(R.id.btnLike)
        val tvLikeCount: TextView       = itemView.findViewById(R.id.tvLikeCount)
        val btnComment: ImageView       = itemView.findViewById(R.id.btnComment)
        val tvCommentCount: TextView    = itemView.findViewById(R.id.tvCommentCount)
        val btnBookmark: ImageView      = itemView.findViewById(R.id.btnBookmark)
        val btnShare: ImageView         = itemView.findViewById(R.id.btnShare)
        val btnMore: ImageView          = itemView.findViewById(R.id.btnPostMore)
        val layoutNameArea: View        = itemView.findViewById(R.id.layoutPostNameArea)
    }

    // ── Core adapter overrides ───────────────────────────────────────

    override fun getItemViewType(position: Int): Int {
        return if (position == 0 && suggestedUsers.isNotEmpty()) VIEW_TYPE_SUGGEST
        else VIEW_TYPE_POST
    }

    override fun getItemCount(): Int {
        return if (suggestedUsers.isNotEmpty()) posts.size + 1 else posts.size
    }
    fun notifyPostChanged(postPosition: Int) {
        val adapterPos = if (suggestedUsers.isNotEmpty()) postPosition + 1 else postPosition
        notifyItemChanged(adapterPos, PAYLOAD_INTERACTION)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SUGGEST) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_suggested_strip, parent, false)
            SuggestStripViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_post, parent, false)
            PostViewHolder(view)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (holder is SuggestStripViewHolder) { bindSuggestStrip(holder); return }
        if (payloads.isEmpty()) { onBindViewHolder(holder, position); return }

        val post = posts[getPostPosition(position)]
        val h    = holder as PostViewHolder
        payloads.distinct().forEach { payload ->
            if (payload == PAYLOAD_COUNTS || payload == PAYLOAD_INTERACTION) {
                bindLikeState(h, post)
                bindBookmarkState(h, post)
                h.tvCommentCount.text = post.commentCount.toString()
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SuggestStripViewHolder) { bindSuggestStrip(holder); return }

        val post = posts[getPostPosition(position)]
        val h    = holder as PostViewHolder

        h.tvFullName.text     = post.fullName
        h.tvTime.text         = getTimeAgo(post.createdAt)
        h.tvLikeCount.text    = post.likeCount.toString()
        h.tvCommentCount.text = post.commentCount.toString()

        if (post.userProfileBase64.isNotEmpty()) {
            com.example.socialconnect.ImageUtils.loadBase64(
                post.userProfileBase64,
                h.imgAvatar,
                h.itemView.context.getDrawable(R.drawable.ic_avatar)
            )
        } else {
            h.imgAvatar.setImageDrawable(
                h.itemView.context.getDrawable(R.drawable.ic_avatar)
            )
        }

        // ── Images ──────────────────────────────────────────────────
        val images = post.allImages()
        if (images.isNotEmpty()) {
            h.layoutPostImages.visibility = View.VISIBLE

            val imageAdapter = PostImageAdapter(images)
            h.vpPostImages.adapter = imageAdapter

            if (images.size > 1) {
                h.tvImageCounter.visibility = View.VISIBLE
                h.tvImageCounter.text       = "1 / ${images.size}"

                h.rvDots.visibility = View.VISIBLE
                val dotAdapter = DotIndicatorAdapter(images.size, 0)
                h.rvDots.layoutManager = LinearLayoutManager(
                    h.itemView.context, LinearLayoutManager.HORIZONTAL, false
                )
                h.rvDots.adapter = dotAdapter

                h.vpPostImages.tag?.let {
                    if (it is ViewPager2.OnPageChangeCallback)
                        h.vpPostImages.unregisterOnPageChangeCallback(it)
                }

                val callback = object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(page: Int) {
                        h.tvImageCounter.text = "${page + 1} / ${images.size}"
                        dotAdapter.setActive(page)
                    }
                }
                h.vpPostImages.registerOnPageChangeCallback(callback)
                h.vpPostImages.tag = callback

            } else {
                h.tvImageCounter.visibility = View.GONE
                h.rvDots.visibility         = View.GONE
            }
        } else {
            h.layoutPostImages.visibility = View.GONE
            h.vpPostImages.adapter        = null
        }

        // ── Caption ─────────────────────────────────────────────────
        if (post.caption.isNotEmpty()) {
            h.tvCaption.visibility = View.VISIBLE
            h.tvCaption.text       = post.caption
        } else {
            h.tvCaption.visibility = View.GONE
        }

        bindLikeState(h, post)
        bindBookmarkState(h, post)
        val adapterPos = position

        h.btnLike.setOnClickListener {
            val postPos = getPostPosition(adapterPos)
            if (postPos >= 0) {
                onLikeClick(post, postPos)
                notifyItemChanged(adapterPos, PAYLOAD_INTERACTION)
            }
        }

        h.btnBookmark.setOnClickListener {
            val postPos = getPostPosition(adapterPos)
            if (postPos >= 0) onBookmarkClick(post, postPos)
        }

        h.btnComment.setOnClickListener { onCommentClick(post) }
        h.btnShare.setOnClickListener    { onShareClick(post) }
        h.imgAvatar.setOnClickListener   { onAvatarClick(post) }
        h.btnMore.setOnClickListener     { showMoreOptions(it, post) }

        h.layoutNameArea.setOnClickListener {
            val intent = android.content.Intent(h.itemView.context, com.example.socialconnect.PostDetail::class.java).apply {
                putExtra("POST_ID", post.postId)
            }
            h.itemView.context.startActivity(intent)
        }
    }

    private fun bindSuggestStrip(holder: SuggestStripViewHolder) {
        suggestedAdapter = SuggestedUsersAdapter(
            users         = suggestedUsers,
            onFollowClick = { user, pos -> onFollowClick?.invoke(user, pos) },
            onUserClick   = { user -> onSuggestedUserClick?.invoke(user) }
        )
        holder.rvSuggested.layoutManager = LinearLayoutManager(
            holder.itemView.context, LinearLayoutManager.HORIZONTAL, false
        )
        holder.rvSuggested.adapter = suggestedAdapter
        holder.tvSeeAll.setOnClickListener { onSeeAllClick?.invoke() }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun getPostPosition(position: Int): Int {
        return if (suggestedUsers.isNotEmpty()) position - 1 else position
    }

    private fun bindLikeState(holder: PostViewHolder, post: PostModel) {
        val red   = "#EF4444".toColorInt()
        val black = "#262626".toColorInt()

        if (post.isLiked) {
            holder.btnLike.setImageResource(R.drawable.ic_heart_filled)
            holder.btnLike.setColorFilter(red)
            holder.tvLikeCount.setTextColor(red)
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline)
            holder.btnLike.setColorFilter(black)
            holder.tvLikeCount.setTextColor(black)
        }
        holder.tvLikeCount.text = post.likeCount.toString()
    }

    private fun bindBookmarkState(holder: PostViewHolder, post: PostModel) {
        if (post.isBookmarked) {
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_filled)
            holder.btnBookmark.setColorFilter("#00BFA5".toColorInt())
        } else {
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_outline)
            holder.btnBookmark.clearColorFilter()
        }
    }

    fun removePostAt(position: Int) {
        val postPos = getPostPosition(position)
        if (postPos >= 0 && postPos < posts.size) {
            posts.removeAt(postPos)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, posts.size)
        }
    }

    private fun showMoreOptions(view: View, post: PostModel) {
        val currentUid = com.example.socialconnect.AuthUtil.currentUid

        val items = mutableListOf<String>()
        if (post.userId == currentUid) {
            items.add("Delete Post")
            items.add("Edit Post")
        }
        items.add("Copy Link")

        val listPopup = androidx.appcompat.widget.ListPopupWindow(view.context)
        listPopup.anchorView = view
        listPopup.setAdapter(
            android.widget.ArrayAdapter(view.context, android.R.layout.simple_list_item_1, items)
        )
        listPopup.width = 400
        listPopup.isModal = true
        listPopup.setBackgroundDrawable(
            android.graphics.Color.WHITE.toDrawable()
        )
        listPopup.setOnItemClickListener { _, _, position, _ ->
            val selected = items[position]
            when (selected) {
                "Delete Post" -> onDeleteClick(post)
                "Edit Post" -> {
                    val intent = android.content.Intent(view.context, com.example.socialconnect.activity_AddPost::class.java)
                    intent.putExtra("EDIT_MODE", true)
                    intent.putExtra("POST_ID", post.postId)
                    intent.putExtra("EDIT_CAPTION", post.caption)
                    intent.putStringArrayListExtra("EDIT_IMAGES", ArrayList(post.allImages()))
                    view.context.startActivity(intent)
                }
                "Copy Link" -> { }
            }
            listPopup.dismiss()
        }
        listPopup.show()
    }
    fun updatePost(position: Int, updatedPost: PostModel) {
        if (position != -1) {
            posts[position] = updatedPost
            notifyItemChanged(position, PAYLOAD_INTERACTION)
        }
    }

    fun updateCommentCount(position: Int, newCount: Int) {
        val postPos = getPostPosition(position)
        if (postPos < 0 || postPos >= posts.size) return
        posts[postPos] = posts[postPos].copy(commentCount = newCount)
        notifyItemChanged(position, PAYLOAD_COUNTS)
    }

    private fun getTimeAgo(time: Long): String {
        val diff = System.currentTimeMillis() - time
        return when {
            diff < 60_000L         -> "Just now"
            diff < 3_600_000L      -> "${diff / 60_000}m"
            diff < 86_400_000L     -> "${diff / 3_600_000}h"
            diff < 604_800_000L    -> "${diff / 86_400_000}d"
            diff < 2_592_000_000L  -> "${diff / 604_800_000}w"
            diff < 31_104_000_000L -> "${diff / 2_592_000_000}mo"
            else                   -> "${diff / 31_104_000_000}y"
        }
    }
}