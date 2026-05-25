package com.example.socialconnect

object CurrentUserCache {
    var fullName: String = ""
    var username: String = ""
    var bio: String = ""
    var profileImageBase64: String = ""
    var joinedAt: Long = 0L
    var posts: MutableList<PostModel> = mutableListOf()

    fun isLoaded(): Boolean = username.isNotEmpty()

    fun populate(user: UserModel) {
        fullName = user.fullName
        username = user.username
        bio = user.bio
        profileImageBase64 = user.profileImageBase64
        joinedAt = user.createdAt
    }

    fun clear() {
        fullName = ""
        username = ""
        bio = ""
        profileImageBase64 = ""
        joinedAt = 0L  // ADD THIS
        posts.clear()
    }
}