package com.example.socialconnect

object CurrentUserCache {
    var fullName: String = ""
    var username: String = ""
    var profileImageBase64: String = ""
    var posts: MutableList<PostModel> = mutableListOf()

    fun isLoaded(): Boolean = username.isNotEmpty()

    fun populate(user: UserModel) {
        fullName = user.fullName
        username = user.username
        profileImageBase64 = user.profileImageBase64
    }

    fun clear() {
        fullName = ""
        username = ""
        profileImageBase64 = ""
        posts.clear()
    }
}