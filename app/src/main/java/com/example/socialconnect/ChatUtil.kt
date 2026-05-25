package com.example.socialconnect

import android.content.Context
import com.example.socialconnect.FireStoreUtil.db
import com.example.socialconnect.FireStoreUtil.getUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.util.UUID

object ChatUtil {

    private val rtdb = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val currentUid get() = auth.currentUser?.uid ?: ""

    // ─── FIRESTORE HELPERS ───────────────────────────────────────────

    fun getFollowingList(uid: String, onResult: (List<String>) -> Unit) {
        db.collection("users")
            .document(uid)
            .collection("following")
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.documents.mapNotNull { it.getString("uid") })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun getUserById(uid: String, onResult: (UserModel?) -> Unit) {
        getUser(uid, onResult)
    }

    // ─── CHAT ID ─────────────────────────────────────────────────────

    fun buildChatId(uid1: String, uid2: String): String =
        if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"


    fun sendMessage(
        otherUid: String,
        text: String,
        onResult: (Boolean) -> Unit
    ) {
        val chatId    = buildChatId(currentUid, otherUid)
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val message = MessageModel(
            messageId  = messageId,
            senderId   = currentUid,
            receiverId = otherUid,
            text       = text,
            timestamp  = timestamp,
            isRead     = false
        )

        val chatRef = rtdb.child("chats").child(chatId)

        chatRef.child("messages").child(messageId).setValue(message)
            .addOnSuccessListener {

                // 2 — update last message preview
                val lastMsg = mapOf(
                    "text"      to text,
                    "timestamp" to timestamp,
                    "senderId"  to currentUid,
                    "isRead"    to false
                )
                chatRef.child("lastMessage").setValue(lastMsg)

                onResult(true)
            }
            .addOnFailureListener { onResult(false) }
    }

    // ─── LISTEN FOR MESSAGES (real-time) ─────────────────────────────

    fun listenForMessages(
        otherUid: String,
        onNewMessage: (MessageModel) -> Unit,
        onMessageChanged: (MessageModel) -> Unit
    ): Pair<Query, ChildEventListener> {
        val chatId = buildChatId(currentUid, otherUid)
        val query: Query = rtdb.child("chats").child(chatId).child("messages")
            .orderByChild("timestamp")

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(MessageModel::class.java)?.let { onNewMessage(it) }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(MessageModel::class.java)?.let { onMessageChanged(it) }
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        query.addChildEventListener(listener)
        return Pair(query, listener)
    }

    // ─── MARK MESSAGES AS READ ───────────────────────────────────────

    fun markMessagesAsRead(otherUid: String) {
        val chatId    = buildChatId(currentUid, otherUid)
        val lastMsgRef = rtdb.child("chats").child(chatId).child("lastMessage")

        lastMsgRef.get().addOnSuccessListener { snapshot ->
            val senderId = snapshot.child("senderId").getValue(String::class.java) ?: ""
            val isRead   = snapshot.child("isRead").getValue(Boolean::class.java) ?: true
            if (senderId == otherUid && !isRead) {
                lastMsgRef.child("isRead").setValue(true)
            }
        }
    }

    // ─── LISTEN FOR LAST MESSAGE (chat list preview) ─────────────────

    fun listenForLastMessage(
        otherUid: String,
        onUpdate: (text: String, timestamp: Long, senderId: String, isRead: Boolean) -> Unit
    ): Pair<DatabaseReference, ValueEventListener> {
        val chatId = buildChatId(currentUid, otherUid)
        val ref    = rtdb.child("chats").child(chatId).child("lastMessage")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val text      = snapshot.child("text").getValue(String::class.java) ?: ""
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                val senderId  = snapshot.child("senderId").getValue(String::class.java) ?: ""
                val isRead    = snapshot.child("isRead").getValue(Boolean::class.java) ?: true
                onUpdate(text, timestamp, senderId, isRead)
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        ref.addValueEventListener(listener)
        return Pair(ref, listener)
    }

    // ─── PRESENCE ────────────────────────────────────────────────────

    fun setPresenceOnline() {
        if (currentUid.isEmpty()) return
        val ref = rtdb.child("presence").child(currentUid)
        ref.child("online").setValue(true)
        ref.child("online").onDisconnect().setValue(false)
        ref.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP)
    }

    fun setPresenceOffline() {
        if (currentUid.isEmpty()) return
        val ref = rtdb.child("presence").child(currentUid)
        ref.child("online").setValue(false)
        ref.child("lastSeen").setValue(ServerValue.TIMESTAMP)
    }

    fun listenForPresence(
        otherUid: String,
        onUpdate: (isOnline: Boolean, lastSeen: Long) -> Unit
    ): Pair<DatabaseReference, ValueEventListener> {
        val ref = rtdb.child("presence").child(otherUid)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.child("online").getValue(Boolean::class.java) ?: false
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                onUpdate(isOnline, lastSeen)
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        ref.addValueEventListener(listener)
        return Pair(ref, listener)
    }
}