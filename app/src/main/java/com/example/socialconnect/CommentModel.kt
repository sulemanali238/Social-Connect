package com.example.socialconnect

data class CommentModel(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val fullName: String = "",        // ← add this
    val userProfileBase64: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)