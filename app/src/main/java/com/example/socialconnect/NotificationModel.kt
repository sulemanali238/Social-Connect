package com.example.socialconnect

data class NotificationModel(
    val notificationId: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val fromUserProfileBase64: String = "",
    val toUserId: String = "",
    val type: String = "",
    val postId: String = "",
    val postImageBase64: String = "",
    val message: String = "",
    val seen: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)