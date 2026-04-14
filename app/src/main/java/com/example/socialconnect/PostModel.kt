package com.example.socialconnect

data class PostModel(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileBase64: String = "",
    val caption: String = "",
    val imageBase64: String = "",
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)