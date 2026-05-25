package com.example.socialconnect

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object InsightUtl {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var listenerRegistration: ListenerRegistration? = null

    private val currentUid get() = auth.currentUser?.uid ?: ""

    // ── Create default insights document if it doesn't exist ─────────────────
    fun initInsights(onResult: (Boolean, String) -> Unit) {
        if (currentUid.isEmpty()) {
            onResult(false, "User not logged in")
            return
        }

        val docRef = db.collection("insights").document(currentUid)

        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    onResult(true, "")
                    return@addOnSuccessListener
                }

                val defaultData = hashMapOf(
                    "profileName"           to "",
                    "profileHandle"         to "",
                    "joinDate"              to "",
                    "profileInitials"       to "",
                    "profileImageBase64"    to "",

                    "followers"             to 0,
                    "following"             to 0,
                    "posts"                 to 0,

                    "healthScore"           to 0,

                    "likesReceived"         to 0,
                    "comments"              to 0,
                    "shares"                to 0,
                    "saves"                 to 0,

                    "avgReachPerPost"       to 0,
                    "avgImpressions"        to 0,
                    "engagementRatePct"     to 0.0,
                    "topPostLikes"          to 0,
                    "profileVisits"         to 0,

                    "heatmapValues"         to listOf(0,0,0,0,0,0,0, 0,0,0,0,0,0,0, 0,0,0,0,0,0,0, 0,0,0,0,0,0,0),

                    "regions"               to emptyList<Map<String, Any>>(),

                    "deviceMobilePct"       to 0,
                    "deviceDesktopPct"      to 0,
                    "deviceTabletPct"       to 0,

                    "postBreakdown"         to emptyList<Map<String, Any>>()
                )

                docRef.set(defaultData)
                    .addOnSuccessListener { onResult(true, "") }
                    .addOnFailureListener { onResult(false, it.message ?: "Failed to create insights") }
            }
            .addOnFailureListener {
                onResult(false, it.message ?: "Failed to check insights")
            }
    }

    // ── Sync real data into insights document ─────────────────────────────────
    fun syncInsights(onResult: (Boolean, String) -> Unit) {
        if (currentUid.isEmpty()) {
            onResult(false, "User not logged in")
            return
        }

        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { userDoc ->
                if (!userDoc.exists()) { onResult(false, "User not found"); return@addOnSuccessListener }

                val fullName       = userDoc.getString("fullName")           ?: ""
                val username       = userDoc.getString("username")           ?: ""
                val profilePic     = userDoc.getString("profileImageBase64") ?: ""
                val followerCount  = (userDoc.getLong("followerCount")       ?: 0).toInt()
                val followingCount = (userDoc.getLong("followingCount")      ?: 0).toInt()
                val postCount      = (userDoc.getLong("postCount")           ?: 0).toInt()
                val createdAt      = userDoc.getLong("createdAt")            ?: 0L

                val initials = fullName.trim().split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }

                val joinDate = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(createdAt))

                // ── Step 2: Fetch posts ───────────────────────────────────
                db.collection("posts")
                    .whereEqualTo("userId", currentUid)
                    .get()
                    .addOnSuccessListener { postsSnap ->
                        val posts = postsSnap.documents.mapNotNull {
                            it.toObject(PostModel::class.java)?.copy(postId = it.id)
                        }

                        val totalLikes    = posts.sumOf { it.likeCount }
                        val totalComments = posts.sumOf { it.commentCount }
                        val topPostLikes  = posts.maxOfOrNull { it.likeCount } ?: 0
                        val avgReach      = if (posts.isEmpty()) 0 else totalLikes / posts.size
                        val avgImpress    = if (posts.isEmpty()) 0 else (totalLikes + totalComments) / posts.size

                        // ── Fixed engagement rate ─────────────────────────
                        // Formula: avg engagements per post / followers × 100
                        // Capped at 100%, requires both posts and followers
                        val engRate: Float = if (followerCount > 0 && posts.isNotEmpty()) {
                            val avgEngPerPost = (totalLikes + totalComments).toFloat() / posts.size
                            ((avgEngPerPost / followerCount) * 100f).coerceIn(0f, 100f)
                        } else 0f

                        val photoCount    = posts.count { it.hasImages() }
                        val textCount     = posts.count { !it.hasImages() }
                        val total         = posts.size.coerceAtLeast(1)
                        val postBreakdown = listOf(
                            mapOf("label" to "Photos", "percentage" to (photoCount * 100 / total)),
                            mapOf("label" to "Text",   "percentage" to (textCount  * 100 / total))
                        )
                        val heatmap = computeHeatmap(posts)

                        // ── Step 3: Fetch bookmark count ──────────────────
                        db.collection("users")
                            .document(currentUid)
                            .collection("bookmarks")
                            .get()
                            .addOnSuccessListener { bookmarksSnap ->
                                val savesCount = bookmarksSnap.size()

                                // ── Step 4: Write to insights ─────────────
                                val updatedData = hashMapOf(
                                    "profileName"        to fullName,
                                    "profileHandle"      to username,
                                    "joinDate"           to joinDate,
                                    "profileInitials"    to initials,
                                    "profileImageBase64" to profilePic,

                                    "followers"          to followerCount,
                                    "following"          to followingCount,
                                    "posts"              to postCount,

                                    "healthScore"        to computeHealthScore(followerCount, totalLikes, totalComments, posts.size),

                                    "likesReceived"      to totalLikes,
                                    "comments"           to totalComments,
                                    "shares"             to 0,
                                    "saves"              to savesCount,  // ← real count now

                                    "avgReachPerPost"    to avgReach,
                                    "avgImpressions"     to avgImpress,
                                    "engagementRatePct"  to engRate,     // ← fixed formula
                                    "topPostLikes"       to topPostLikes,
                                    "profileVisits"      to 0,

                                    "heatmapValues"      to heatmap,

                                    "regions"            to listOf(
                                        mapOf("flag" to "🇵🇰", "name" to "Pakistan", "percentage" to 100)
                                    ),

                                    "deviceMobilePct"    to 100,
                                    "deviceDesktopPct"   to 0,
                                    "deviceTabletPct"    to 0,

                                    "postBreakdown"      to postBreakdown
                                )

                                db.collection("insights").document(currentUid)
                                    .update(updatedData as Map<String, Any>)
                                    .addOnSuccessListener { onResult(true, "") }
                                    .addOnFailureListener { onResult(false, it.message ?: "Failed to sync") }
                            }
                            .addOnFailureListener {
                                onResult(false, it.message ?: "Failed to fetch bookmarks")
                            }
                    }
                    .addOnFailureListener {
                        onResult(false, it.message ?: "Failed to fetch posts")
                    }
            }
            .addOnFailureListener {
                onResult(false, it.message ?: "Failed to fetch user")
            }
    }

    // ── Compute heatmap from post timestamps ──────────────────────────────────
    private fun computeHeatmap(posts: List<PostModel>): List<Int> {
        val heatmap   = MutableList(28) { 0 }
        val now       = System.currentTimeMillis()
        val fourWeeks = 28L * 24 * 60 * 60 * 1000
        val cal       = java.util.Calendar.getInstance()

        posts.forEach { post ->
            val age = now - post.createdAt
            if (age > fourWeeks) return@forEach

            cal.timeInMillis = post.createdAt
            val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
            val weekIndex = (age / (7L * 24 * 60 * 60 * 1000)).toInt().coerceIn(0, 3)
            val index     = weekIndex * 7 + dayOfWeek

            heatmap[index] = (heatmap[index] + 1).coerceAtMost(4)
        }

        return heatmap
    }

    // ── Compute health score 0–100 ────────────────────────────────────────────
    private fun computeHealthScore(followers: Int, likes: Int, comments: Int, posts: Int): Int {
        if (posts == 0) return 0
        val postScore  = (posts * 8).coerceAtMost(50)
        val engScore   = ((likes + comments) * 2).coerceAtMost(50)
        return (postScore + engScore).coerceAtMost(100)
    }

    // ── Start real-time listener ──────────────────────────────────────────────
    fun listenToInsights(
        onSuccess: (InsightsModel) -> Unit,
        onError:   (String) -> Unit
    ) {
        if (currentUid.isEmpty()) {
            onError("User not logged in")
            return
        }

        listenerRegistration = db.collection("insights")
            .document(currentUid)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    onError("No insights data found")
                    return@addSnapshotListener
                }

                try {
                    val model = InsightsModel(
                        profileName        = snapshot.getString("profileName")        ?: "",
                        profileHandle      = snapshot.getString("profileHandle")      ?: "",
                        joinDate           = snapshot.getString("joinDate")           ?: "",
                        profileInitials    = snapshot.getString("profileInitials")    ?: "",
                        profileImageBase64 = snapshot.getString("profileImageBase64") ?: "",

                        followers            = (snapshot.getLong("followers")            ?: 0).toInt(),
                        following            = (snapshot.getLong("following")            ?: 0).toInt(),
                        posts                = (snapshot.getLong("posts")                ?: 0).toInt(),

                        healthScore          = (snapshot.getLong("healthScore")          ?: 0).toInt(),

                        likesReceived        = (snapshot.getLong("likesReceived")        ?: 0).toInt(),
                        comments             = (snapshot.getLong("comments")             ?: 0).toInt(),
                        shares               = (snapshot.getLong("shares")               ?: 0).toInt(),
                        saves                = (snapshot.getLong("saves")                ?: 0).toInt(),

                        avgReachPerPost      = (snapshot.getLong("avgReachPerPost")      ?: 0).toInt(),
                        avgImpressions       = (snapshot.getLong("avgImpressions")       ?: 0).toInt(),
                        engagementRatePct    = (snapshot.getDouble("engagementRatePct")  ?: 0.0).toFloat(),
                        topPostLikes         = (snapshot.getLong("topPostLikes")         ?: 0).toInt(),
                        profileVisits        = (snapshot.getLong("profileVisits")        ?: 0).toInt(),

                        heatmapValues = (snapshot.get("heatmapValues") as? List<*>)
                            ?.mapNotNull { (it as? Long)?.toInt() }
                            ?: emptyList(),

                        regions = (snapshot.get("regions") as? List<*>)
                            ?.mapNotNull { item ->
                                val map = item as? Map<*, *> ?: return@mapNotNull null
                                RegionData(
                                    flag       = map["flag"]       as? String ?: "",
                                    name       = map["name"]       as? String ?: "",
                                    percentage = (map["percentage"] as? Long)?.toInt() ?: 0
                                )
                            } ?: emptyList(),

                        deviceMobilePct  = (snapshot.getLong("deviceMobilePct")  ?: 0).toInt(),
                        deviceDesktopPct = (snapshot.getLong("deviceDesktopPct") ?: 0).toInt(),
                        deviceTabletPct  = (snapshot.getLong("deviceTabletPct")  ?: 0).toInt(),

                        postBreakdown = (snapshot.get("postBreakdown") as? List<*>)
                            ?.mapNotNull { item ->
                                val map = item as? Map<*, *> ?: return@mapNotNull null
                                PostTypeData(
                                    label      = map["label"]      as? String ?: "",
                                    percentage = (map["percentage"] as? Long)?.toInt() ?: 0
                                )
                            } ?: emptyList()
                    )

                    onSuccess(model)

                } catch (e: Exception) {
                    onError("Failed to parse insights: ${e.message}")
                }
            }
    }

    // ── Stop listener (call from onDestroy) ───────────────────────────────────
    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}