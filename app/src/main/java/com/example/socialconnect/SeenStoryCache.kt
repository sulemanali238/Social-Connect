package com.example.socialconnect

import android.content.Context
import android.content.SharedPreferences

object SeenStoryCache {

    private const val PREF_NAME = "seen_stories"
    private const val KEY_SEEN = "seen_ids"

    private fun getPrefs(context: Context): SharedPreferences {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        return context.getSharedPreferences("seen_stories_$uid", Context.MODE_PRIVATE)
    }

    fun markSeen(context: Context, storyId: String) {
        val prefs = getPrefs(context)
        val existing = prefs.getStringSet(KEY_SEEN, mutableSetOf()) ?: mutableSetOf()
        val updated = existing.toMutableSet()
        updated.add(storyId)
        prefs.edit().putStringSet(KEY_SEEN, updated).apply()
    }

    fun isSeen(context: Context, storyId: String): Boolean {
        val prefs = getPrefs(context)
        val existing = prefs.getStringSet(KEY_SEEN, emptySet()) ?: emptySet()
        return existing.contains(storyId)
    }

    fun clearOldStories(context: Context, activeStoryIds: List<String>) {
        // remove IDs that no longer exist to save space
        val prefs = getPrefs(context)
        val existing = prefs.getStringSet(KEY_SEEN, emptySet()) ?: emptySet()
        val cleaned = existing.filter { activeStoryIds.contains(it) }.toSet()
        prefs.edit().putStringSet(KEY_SEEN, cleaned).apply()
    }
}