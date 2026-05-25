package com.example.socialconnect

data class ChatModel(
    val uid: String = "",
    val username: String = "",
    val fullName: String = "",
    val profileImageBase64: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)