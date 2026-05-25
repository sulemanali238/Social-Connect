package com.example.socialconnect.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.view.ContextThemeWrapper
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.AuthUtil
import com.example.socialconnect.ChatUtil
import com.example.socialconnect.ImageUtils
import com.example.socialconnect.MessageModel
import com.example.socialconnect.R
import com.example.socialconnect.adapters.MessageAdapter
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.Query
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

class ChatMessageFragment : Fragment() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: FrameLayout
    private lateinit var btnBack: ImageView
    private lateinit var tvOtherName: TextView
    private lateinit var tvOnlineStatus: TextView
    private lateinit var imgAvatar: CircleImageView
    private lateinit var tvConnectionBanner: TextView
    private lateinit var cardConnectionBanner: com.google.android.material.card.MaterialCardView


    private val currentUid get() = AuthUtil.currentUid ?: ""
    private lateinit var otherUid: String
    private lateinit var otherName: String
    private lateinit var otherImageBase64: String

    private val messages = mutableListOf<MessageModel>()
    private lateinit var adapter: MessageAdapter

    private var messagesListenerPair: Pair<Query, ChildEventListener>? = null
    private var presenceListenerPair: Pair<DatabaseReference, ValueEventListener>? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_chat_message, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        otherUid         = arguments?.getString("OTHER_UID") ?: ""
        otherName        = arguments?.getString("OTHER_NAME") ?: ""
        otherImageBase64 = arguments?.getString("OTHER_IMAGE_URL") ?: ""


        rvMessages      = view.findViewById(R.id.rvMessages)
        etMessage       = view.findViewById(R.id.etMessage)
        btnSend         = view.findViewById(R.id.btnSend)
        btnBack         = view.findViewById(R.id.btnBack)
        tvOtherName     = view.findViewById(R.id.tvOtherName)
        tvOnlineStatus  = view.findViewById(R.id.tvOnlineStatus)
        imgAvatar       = view.findViewById(R.id.imgOtherAvatar)
        cardConnectionBanner = view.findViewById(R.id.cardConnectionBanner)
        tvConnectionBanner   = view.findViewById(R.id.tvConnectionBanner)

        tvOtherName.text = otherName
        imgAvatar.setImageResource(R.drawable.ic_avatar) // placeholder first

        ChatUtil.getUserById(otherUid) { userModel ->
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                ImageUtils.loadBase64(userModel?.profileImageBase64, imgAvatar,
                    requireContext().getDrawable(R.drawable.ic_avatar))
            }
        }

        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        rvMessages.layoutManager = layoutManager
        adapter = MessageAdapter(messages, currentUid)
        rvMessages.adapter = adapter

        btnSend.setOnClickListener { sendMessage() }

        view.findViewById<ImageView>(R.id.btnMoreOptions).setOnClickListener {
            showMoreOptionsMenu(it)
        }

        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage(); true
            } else false
        }

        listenForMessages()
        listenForPresence()
        ChatUtil.markMessagesAsRead(otherUid)
        checkAndShowConnectionBanner()
    }
    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return
        etMessage.setText("")

        ChatUtil.sendMessage( otherUid, text) { }
    }
    private fun listenForMessages() {
        messagesListenerPair = ChatUtil.listenForMessages(
            otherUid        = otherUid,
            onNewMessage    = { message ->
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    if (messages.none { it.messageId == message.messageId }) {
                        messages.add(message)
                        adapter.notifyItemInserted(messages.size - 1)
                        rvMessages.scrollToPosition(messages.size - 1)
                        if (message.senderId == otherUid) {
                            ChatUtil.markMessagesAsRead(otherUid)
                        }
                    }
                }
            },
            onMessageChanged = { updatedMessage ->
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    val idx = messages.indexOfFirst { it.messageId == updatedMessage.messageId }
                    if (idx != -1) {
                        messages[idx] = updatedMessage
                        adapter.notifyItemChanged(idx)
                    }
                }
            }
        )
    }

    private fun listenForPresence() {
        presenceListenerPair = ChatUtil.listenForPresence(otherUid) { isOnline, lastSeen ->
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                if (isOnline) {
                    tvOnlineStatus.text = "Online"
                    tvOnlineStatus.setTextColor(
                        requireContext().getColor(R.color.teal_green)
                    )
                } else {
                    tvOnlineStatus.text = formatLastSeen(lastSeen)
                    tvOnlineStatus.setTextColor(
                        requireContext().getColor(R.color.gray_muted)
                    )
                }
            }
        }
    }
    private fun showMoreOptionsMenu(anchor: View) {
        val items = listOf("View Profile", "Clear Chat", "Report", "Block")

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
            when (position) {
                0 -> openOtherProfile()
                1 -> confirmClearChat()
                2 -> reportUser()
                3 -> blockUser()
            }
            listPopup.dismiss()
        }
        listPopup.show()
    }    private fun openOtherProfile() {
        val fragment = ProfileFragment().apply {
            arguments = Bundle().apply {
                putString("USER_ID", otherUid)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun confirmClearChat() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Clear Chat")
            .setMessage("Are you sure you want to clear all messages? This cannot be undone.")
            .setPositiveButton("Clear") { _, _ -> clearChat() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearChat() {
        val chatId = ChatUtil.buildChatId(currentUid, otherUid)
        com.google.firebase.database.FirebaseDatabase.getInstance()
            .reference.child("chats").child(chatId).child("messages").removeValue()
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                messages.clear()
                adapter.notifyDataSetChanged()
                android.widget.Toast.makeText(requireContext(), "Chat cleared", android.widget.Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                android.widget.Toast.makeText(requireContext(), "Failed to clear chat", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    private fun reportUser() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Report User")
            .setMessage("Report ${otherName} to admin for inappropriate behaviour?")
            .setPositiveButton("Report") { _, _ ->
                android.widget.Toast.makeText(requireContext(), "Report submitted to admin", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun blockUser() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Block User")
            .setMessage("Block ${otherName}? They won't be able to message you.")
            .setPositiveButton("Block") { _, _ ->
                android.widget.Toast.makeText(requireContext(), "Coming Soon", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatLastSeen(timestamp: Long): String {
        if (timestamp == 0L) return "Offline"
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000     -> "Just now"
            diff < 3_600_000  -> "Active ${diff / 60_000}m ago"
            diff < 86_400_000 -> "Active ${diff / 3_600_000}h ago"
            else              -> "Offline"
        }
    }
    private fun checkAndShowConnectionBanner() {
        ChatUtil.getFollowingList(currentUid) { myFollowing ->
            ChatUtil.getFollowingList(otherUid) { theirFollowing ->
                if (!isAdded) return@getFollowingList

                val iFollow    = myFollowing.contains(otherUid)
                val theyFollow = theirFollowing.contains(currentUid)
                val isMutual   = iFollow && theyFollow

                if (!isMutual) {
                    val message = when {
                        !iFollow && !theyFollow ->
                            "⚠ You're not mutually connected on Social Connect."

                        iFollow && !theyFollow ->
                            "⚠ $otherName hasn't followed you back yet."

                        !iFollow && theyFollow ->
                            "⚠ You haven't followed $otherName back yet."

                        else -> null
                    }

                    message?.let {
                        activity?.runOnUiThread {
                            if (!isAdded) return@runOnUiThread
                            tvConnectionBanner.text = it
                            cardConnectionBanner.visibility = View.VISIBLE
                            cardConnectionBanner.postDelayed({
                                if (isAdded) cardConnectionBanner.visibility = View.GONE
                            }, 2500)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messagesListenerPair?.let  { (query, listener) -> query.removeEventListener(listener) }
        presenceListenerPair?.let  { (ref,   listener) -> ref.removeEventListener(listener)   }
    }
}