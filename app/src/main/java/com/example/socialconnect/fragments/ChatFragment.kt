package com.example.socialconnect.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.socialconnect.AuthUtil
import com.example.socialconnect.ChatModel
import com.example.socialconnect.ChatUtil
import com.example.socialconnect.R
import com.example.socialconnect.activity_Main
import com.example.socialconnect.adapters.ChatListAdapter
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.core.graphics.drawable.toDrawable

class ChatFragment : Fragment() {

    private lateinit var shimmer: ShimmerFrameLayout
    private lateinit var rvChats: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var layoutSearchBar: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var imgSearchArrow: ImageView
    private lateinit var btnFilterChat: ImageView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private val rtdb: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val currentUid get() = AuthUtil.currentUid ?: ""

    private var currentFilterMode = FilterMode.ALL
    private enum class FilterMode { ALL, UNREAD }

    private val adapter = ChatListAdapter { chatModel ->
        (activity as? activity_Main)?.openChatMessage(
            chatModel.uid,
            chatModel.fullName.ifEmpty { chatModel.username },
            chatModel.profileImageBase64
        )
    }

    private val allChatUsers = mutableListOf<ChatModel>()
    private val rtdbListeners = mutableMapOf<String, Pair<DatabaseReference, ValueEventListener>>()
    private var incomingChatsListener: ChildEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_chat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shimmer         = view.findViewById(R.id.shimmerChats)
        rvChats         = view.findViewById(R.id.rvChats)
        layoutEmpty     = view.findViewById(R.id.layoutEmptyChats)
        layoutSearchBar = view.findViewById(R.id.layoutSearchBar)
        etSearch        = view.findViewById(R.id.etSearchChat)
        imgSearchArrow  = view.findViewById(R.id.imgSearchArrow)
        btnFilterChat   = view.findViewById(R.id.btnFilterChat)
        swipeRefresh    = view.findViewById(R.id.swipeRefresh)

        rvChats.layoutManager = LinearLayoutManager(requireContext())
        rvChats.adapter = adapter

        // ── Swipe to refresh ──────────────────────────────────────────
        swipeRefresh.setColorSchemeResources(R.color.teal_green)
        swipeRefresh.setOnRefreshListener {
            synchronized(allChatUsers) { allChatUsers.clear() }
            rtdbListeners.values.forEach { (ref, listener) -> ref.removeEventListener(listener) }
            rtdbListeners.clear()
            incomingChatsListener?.let { rtdb.child("chats").removeEventListener(it) }
            incomingChatsListener = null

            refreshAdapter()

            loadFollowingUsers()
            listenForIncomingChats()

            swipeRefresh.postDelayed({
                if (isAdded) swipeRefresh.isRefreshing = false
            }, 2000)
        }

        // ── Search toggle ─────────────────────────────────────────────
        view.findViewById<LinearLayout>(R.id.layoutSearchClickTarget).setOnClickListener {
            val isOpening = layoutSearchBar.visibility == View.GONE
            if (isOpening) {
                layoutSearchBar.visibility = View.VISIBLE
                etSearch.requestFocus()
                imgSearchArrow.animate().rotation(180f).setDuration(250).start()
            } else {
                layoutSearchBar.visibility = View.GONE
                etSearch.text.clear()
                imgSearchArrow.animate().rotation(0f).setDuration(250).start()
            }
        }

        btnFilterChat.setOnClickListener { showFilterMenu(it) }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { refreshAdapter() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        shimmer.startShimmer()
        loadFollowingUsers()
        listenForIncomingChats()
    }

    private fun loadFollowingUsers() {
        ChatUtil.getFollowingList(currentUid) { followingUids ->
            if (!isAdded) return@getFollowingList
            if (followingUids.isEmpty()) { hideShimmer(); return@getFollowingList }
            loadUserProfiles(followingUids)
        }
    }

    private fun loadUserProfiles(uids: List<String>) {
        var loadedCount = 0
        uids.forEach { uid ->
            ChatUtil.getUserById(uid) { userModel ->
                if (!isAdded) return@getUserById
                if (userModel == null) {
                    loadedCount++
                    if (loadedCount == uids.size) { hideShimmer(); refreshAdapter() }
                    return@getUserById
                }
                val chatModel = ChatModel(
                    uid                = uid,
                    username           = userModel.username,
                    fullName           = userModel.fullName,
                    profileImageBase64 = userModel.profileImageBase64
                )
                addOrUpdateUser(chatModel)
                attachMessageListener(uid)
                loadedCount++
                if (loadedCount == uids.size) {
                    activity?.runOnUiThread {
                        if (!isAdded) return@runOnUiThread
                        hideShimmer()
                        refreshAdapter()
                    }
                }
            }
        }
    }

    private fun listenForIncomingChats() {
        incomingChatsListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handleIncomingChatSnapshot(snapshot)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handleIncomingChatSnapshot(snapshot)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        rtdb.child("chats").addChildEventListener(incomingChatsListener!!)
    }

    private fun handleIncomingChatSnapshot(snapshot: DataSnapshot) {
        if (!isAdded) return

        val chatId = snapshot.key ?: return

        val otherUid = when {
            chatId.startsWith("${currentUid}_") -> chatId.removePrefix("${currentUid}_")
            chatId.endsWith("_${currentUid}")   -> chatId.removeSuffix("_${currentUid}")
            else -> return
        }

        if (otherUid.isEmpty() || otherUid == currentUid) return

        val lastMsgSnap = snapshot.child("lastMessage")
        val text        = lastMsgSnap.child("text").getValue(String::class.java) ?: ""
        val time        = lastMsgSnap.child("timestamp").getValue(Long::class.java) ?: 0L
        val senderId    = lastMsgSnap.child("senderId").getValue(String::class.java) ?: ""
        val isRead      = lastMsgSnap.child("isRead").getValue(Boolean::class.java) ?: true
        val unread      = if (!isRead && senderId != currentUid) 1 else 0

        val alreadyExists = synchronized(allChatUsers) {
            allChatUsers.any { it.uid == otherUid }
        }

        if (alreadyExists) {
            synchronized(allChatUsers) {
                val idx = allChatUsers.indexOfFirst { it.uid == otherUid }
                if (idx != -1) {
                    allChatUsers[idx] = allChatUsers[idx].copy(
                        lastMessage     = text,
                        lastMessageTime = time,
                        unreadCount     = unread
                    )
                }
            }
            activity?.runOnUiThread { if (isAdded) refreshAdapter() }
            return
        }

        ChatUtil.getUserById(otherUid) { userModel ->
            if (!isAdded) return@getUserById
            if (userModel == null) return@getUserById

            val chatModel = ChatModel(
                uid                = otherUid,
                username           = userModel.username,
                fullName           = userModel.fullName,
                profileImageBase64 = userModel.profileImageBase64,
                lastMessage        = text,
                lastMessageTime    = time,
                unreadCount        = unread
            )

            addOrUpdateUser(chatModel)
            attachMessageListener(otherUid)

            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                hideShimmer()
                refreshAdapter()
            }
        }
    }
    private fun attachMessageListener(otherUid: String) {
        if (rtdbListeners.containsKey(otherUid)) return

        val chatId = ChatUtil.buildChatId(currentUid, otherUid)
        val ref    = rtdb.child("chats").child(chatId).child("lastMessage")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                val text     = snapshot.child("text").getValue(String::class.java) ?: ""
                val time     = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                val senderId = snapshot.child("senderId").getValue(String::class.java) ?: ""
                val isRead   = snapshot.child("isRead").getValue(Boolean::class.java) ?: true
                val unread   = if (!isRead && senderId != currentUid) 1 else 0

                synchronized(allChatUsers) {
                    val idx = allChatUsers.indexOfFirst { it.uid == otherUid }
                    if (idx != -1) {
                        allChatUsers[idx] = allChatUsers[idx].copy(
                            lastMessage     = text,
                            lastMessageTime = time,
                            unreadCount     = unread
                        )
                    }
                }
                activity?.runOnUiThread { if (isAdded) refreshAdapter() }
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        ref.addValueEventListener(listener)
        rtdbListeners[otherUid] = Pair(ref, listener)
    }

    private fun addOrUpdateUser(chatModel: ChatModel) {
        synchronized(allChatUsers) {
            val idx = allChatUsers.indexOfFirst { it.uid == chatModel.uid }
            if (idx == -1) allChatUsers.add(chatModel) else allChatUsers[idx] = chatModel
        }
    }

    private fun refreshAdapter() {
        val query = etSearch.text.toString().trim().lowercase()
        var list  = synchronized(allChatUsers) { allChatUsers.toList() }

        if (currentFilterMode == FilterMode.UNREAD) {
            list = list.filter { it.unreadCount > 0 }
        }
        if (query.isNotEmpty()) {
            list = list.filter {
                it.fullName.lowercase().contains(query) ||
                        it.username.lowercase().contains(query)
            }
        }

        val sorted = list.sortedByDescending { it.lastMessageTime }
        adapter.submitList(sorted)
        if (sorted.isEmpty()) showEmpty() else showRecyclerView()
    }

    private fun showFilterMenu(anchor: View) {
        val items = listOf("All Messages", "Unread Only")

        val listPopup = androidx.appcompat.widget.ListPopupWindow(requireContext())
        listPopup.anchorView = anchor
        listPopup.setAdapter(
            android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
        )
        listPopup.width = 400
        listPopup.isModal = true
        listPopup.setBackgroundDrawable(
            android.graphics.Color.WHITE.toDrawable()
        )
        listPopup.setOnItemClickListener { _, _, position, _ ->
            currentFilterMode = if (position == 0) FilterMode.ALL else FilterMode.UNREAD
            refreshAdapter()
            listPopup.dismiss()
        }
        listPopup.show()
    }
    private fun hideShimmer() {
        shimmer.stopShimmer()
        shimmer.visibility = View.GONE
    }

    private fun showEmpty() {
        hideShimmer()
        rvChats.visibility     = View.GONE
        layoutEmpty.visibility = View.VISIBLE
    }

    private fun showRecyclerView() {
        rvChats.visibility     = View.VISIBLE
        layoutEmpty.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rtdbListeners.values.forEach { (ref, listener) -> ref.removeEventListener(listener) }
        rtdbListeners.clear()
        incomingChatsListener?.let { rtdb.child("chats").removeEventListener(it) }
        incomingChatsListener = null
    }
}