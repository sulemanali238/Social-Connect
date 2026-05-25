package com.example.socialconnect

data class UserModel(
    val uid: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val bio: String = "",
    val website: String = "",
    val profileImageBase64: String = "",
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    var isFollowing: Boolean = false,
    val postCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val fcmToken: String = ""   // ← ADD THIS
)