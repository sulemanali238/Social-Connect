package com.example.socialconnect

data class PostModel(
    val postId: String = "",
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val userProfileBase64: String = "",
    val caption: String = "",
    val imageBase64: String = "",
    val images: List<String> = emptyList(), // new: up to 5 images
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    var isLiked: Boolean = false,
    var isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Unified accessor — new posts use `images`, old posts fall back to `imageBase64`
    fun allImages(): List<String> {
        return if (images.isNotEmpty()) images
        else if (imageBase64.isNotEmpty()) listOf(imageBase64)
        else emptyList()
    }

    fun hasImages(): Boolean = allImages().isNotEmpty()
}