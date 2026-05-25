package com.example.socialconnect

data class StoryModel(
    val storyId: String = "",
    val userId: String = "",
    val username: String = "",
    val fullName: String = "",        // add this
    val userProfileBase64: String = "",
    val imageBase64: String = "",
    val storyText: String = "",
    val seen: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val viewers: List<String> = emptyList()
)