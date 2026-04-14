package com.example.socialconnect

data class StoryModel(
    val storyId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileBase64: String = "",
    val imageBase64: String = "",
    val seen: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)