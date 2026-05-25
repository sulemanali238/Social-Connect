package com.example.socialconnect

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration

object FireStoreUtil {

    val db = FirebaseFirestore.getInstance().apply {
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        firestoreSettings = settings
    }
    private val auth = FirebaseAuth.getInstance()
    var forceServerFetch = false

    // ─── CURRENT USER ───────────────────────────────────────────────

    private val currentUid get() = auth.currentUser?.uid ?: ""


    // ─── USER ───────────────────────────────────────────────────────

    fun createUser(user: UserModel, onResult: (Boolean, String) -> Unit) {
        db.collection("users")
            .document(user.uid)
            .set(user)
            .addOnSuccessListener {
                onResult(true, "")
            }
            .addOnFailureListener {
                onResult(false, it.message ?: "Failed to create user")
            }
    }

    fun getUser(uid: String, onResult: (UserModel?) -> Unit) {
        db.collection("users")
            .document(uid)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { doc ->
                onResult(doc.toObject(UserModel::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun getCurrentUser(onResult: (UserModel?) -> Unit) {
        getUser(currentUid, onResult)
    }

    fun checkEmailExists(email: String, onResult: (Boolean) -> Unit) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { result ->
                onResult(!result.isEmpty)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun updateUser(data: Map<String, Any>, onResult: (Boolean, String) -> Unit) {
        db.collection("users")
            .document(currentUid)
            .update(data)
            .addOnSuccessListener {
                onResult(true, "")
            }
            .addOnFailureListener {
                onResult(false, it.message ?: "Failed to update user")
            }
    }

    fun isUsernameTaken(username: String, onResult: (Boolean) -> Unit) {
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { result ->
                onResult(!result.isEmpty)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }
    fun getSuggestedUsers(onResult: (List<UserModel>) -> Unit) {
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val allUsers = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserModel::class.java)?.copy(uid = doc.id)
                }.filter { it.uid != currentUid } // exclude self

                if (allUsers.isEmpty()) { onResult(emptyList()); return@addOnSuccessListener }

                // Check follow status for each user
                var remaining = allUsers.size
                val result = mutableListOf<UserModel>()

                allUsers.forEach { user ->
                    isFollowing(user.uid) { following ->
                        synchronized(result) {
                            result.add(user.copy(isFollowing = following))
                            if (--remaining == 0) {
                                // Shuffle and take up to 10
                                onResult(result.shuffled().take(10))
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    // ─── POSTS ──────────────────────────────────────────────────────

    fun getAllPosts(onResult: (List<PostModel>) -> Unit) {
        val query = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        if (!forceServerFetch) {
            // Step 1 — serve from cache instantly
            query.get(com.google.firebase.firestore.Source.CACHE)
                .addOnSuccessListener { cacheSnapshot ->
                    if (!cacheSnapshot.isEmpty) {
                        val cachedPosts = cacheSnapshot.documents.mapNotNull { doc ->
                            doc.toObject(PostModel::class.java)?.copy(postId = doc.id)
                        }
                        onResult(cachedPosts)
                    }
                }
                .addOnCompleteListener {
                    query.get(com.google.firebase.firestore.Source.SERVER)
                        .addOnSuccessListener { snapshot ->
                            if (snapshot == null) return@addOnSuccessListener
                            val posts = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(PostModel::class.java)?.copy(postId = doc.id)
                            }
                            if (posts.isEmpty()) return@addOnSuccessListener
                            healAndReturn(posts, onResult)
                        }
                        .addOnFailureListener { onResult(emptyList()) }
                }
        } else {
            // Skip cache — go straight to server
            forceServerFetch = false  // reset after one use
            query.get(com.google.firebase.firestore.Source.SERVER)
                .addOnSuccessListener { snapshot ->
                    if (snapshot == null) return@addOnSuccessListener
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PostModel::class.java)?.copy(postId = doc.id)
                    }
                    if (posts.isEmpty()) return@addOnSuccessListener
                    healAndReturn(posts, onResult)
                }
                .addOnFailureListener { onResult(emptyList()) }
        }
    }
    private fun healAndReturn(posts: List<PostModel>, onResult: (List<PostModel>) -> Unit) {
        val healed = mutableListOf<PostModel>()
        var remaining = posts.size

        posts.forEach { post ->
            db.collection("posts").document(post.postId)
                .collection("comments")
                .get(com.google.firebase.firestore.Source.SERVER)
                .addOnSuccessListener { commentsSnap ->
                    val realCommentCount = commentsSnap.size()
                    db.collection("posts").document(post.postId)
                        .collection("likes")
                        .get(com.google.firebase.firestore.Source.SERVER)
                        .addOnSuccessListener { likesSnap ->
                            val realLikeCount = likesSnap.size()
                            val updates = mutableMapOf<String, Any>()
                            if (realCommentCount != post.commentCount)
                                updates["commentCount"] = realCommentCount
                            if (realLikeCount != post.likeCount)
                                updates["likeCount"] = realLikeCount
                            if (updates.isNotEmpty()) {
                                db.collection("posts").document(post.postId).update(updates)
                            }
                            synchronized(healed) {
                                healed.add(post.copy(commentCount = realCommentCount, likeCount = realLikeCount))
                                if (--remaining == 0) onResult(healed.sortedByDescending { it.createdAt })
                            }
                        }
                        .addOnFailureListener {
                            synchronized(healed) {
                                healed.add(post.copy(commentCount = realCommentCount))
                                if (--remaining == 0) onResult(healed.sortedByDescending { it.createdAt })
                            }
                        }
                }
                .addOnFailureListener {
                    synchronized(healed) {
                        healed.add(post)
                        if (--remaining == 0) onResult(healed.sortedByDescending { it.createdAt })
                    }
                }
        }
    }

    fun getUserPosts(uid: String, onResult: (List<PostModel>) -> Unit) {
        db.collection("posts")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                if (result == null) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }
                val posts = result.documents.mapNotNull { doc ->
                    doc.toObject(PostModel::class.java)?.copy(postId = doc.id) // postId fix too
                }
                onResult(posts)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
    fun getPost(postId: String, onResult: (PostModel?) -> Unit) {
        db.collection("posts")
            .document(postId)
            .get()
            .addOnSuccessListener { doc ->
                onResult(doc.toObject(PostModel::class.java)?.copy(postId = doc.id))
            }
            .addOnFailureListener { onResult(null) }
    }
    fun getPostById(postId: String, callback: (PostModel?) -> Unit) {
        db.collection("posts").document(postId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    callback(doc.toObject(PostModel::class.java)?.copy(postId = doc.id))
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { callback(null) }
    }

    fun createPost(post: PostModel, onResult: (Boolean, String) -> Unit) {
        val postId = db.collection("posts").document().id
        val newPost = post.copy(
            postId = postId,
            userId = currentUid,
            createdAt = System.currentTimeMillis()
        )
        db.collection("posts")
            .document(postId)
            .set(newPost)
            .addOnSuccessListener {
                db.collection("users")
                    .document(currentUid)
                    .update("postCount",
                        com.google.firebase.firestore.FieldValue.increment(1))
                onResult(true, postId)
            }
            .addOnFailureListener {
                onResult(false, it.message ?: "Failed to create post")
            }
    }

    fun deletePost(postId: String, onResult: (Boolean) -> Unit) {
        if (postId.isEmpty()) {
            onResult(false)
            return
        }

        val postRef = db.collection("posts").document(postId)

        // Fetch likes first
        postRef.collection("likes").get()
            .addOnSuccessListener { likesSnapshot ->
                val batch = db.batch()

                // Delete all likes
                for (doc in likesSnapshot.documents) {
                    batch.delete(doc.reference)
                }

                // Fetch comments and delete them too
                postRef.collection("comments").get()
                    .addOnSuccessListener { commentsSnapshot ->
                        for (doc in commentsSnapshot.documents) {
                            batch.delete(doc.reference)
                        }

                        // Delete the main post
                        batch.delete(postRef)

                        batch.commit().addOnCompleteListener { task ->
                            onResult(task.isSuccessful)
                        }
                    }
                    .addOnFailureListener {
                        // Comments fetch failed — still delete likes + post
                        batch.delete(postRef)
                        batch.commit().addOnCompleteListener { task ->
                            onResult(task.isSuccessful)
                        }
                    }
            }
            .addOnFailureListener {
                // Likes fetch failed — try deleting post itself at minimum
                postRef.delete().addOnCompleteListener { onResult(it.isSuccessful) }
            }
    }

    fun likePost(postId: String, wasLiked: Boolean, onResult: (Boolean) -> Unit) {
        if (postId.isEmpty()) return

        val postRef = db.collection("posts").document(postId)
        val increment = if (wasLiked) -1L else 1L

        // 1. Reference to the specific like document
        val likeRef = postRef.collection("likes").document(currentUid)

        // 2. Define the task: Delete if unliking, Set with userId if liking
        val task = if (wasLiked) {
            likeRef.delete()
        } else {
            // CRITICAL: We add "userId" here so the collectionGroup query can find it later
            likeRef.set(mapOf(
                "userId" to currentUid,
                "timestamp" to System.currentTimeMillis()
            ))
        }

        task.addOnSuccessListener {
            // 3. Update the main post document's like count
            val data = mapOf("likeCount" to com.google.firebase.firestore.FieldValue.increment(increment))
            postRef.set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnCompleteListener { onResult(it.isSuccessful) }
        }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun bookmarkPost(postId: String, isBookmarked: Boolean, onResult: (Boolean) -> Unit) {
        val bookmarkRef = db.collection("users")
            .document(currentUid)
            .collection("bookmarks")
            .document(postId)

        if (isBookmarked) {
            // currently bookmarked — remove it
            bookmarkRef.delete()
                .addOnSuccessListener { onResult(true) }
                .addOnFailureListener { onResult(false) }
        } else {
            // not bookmarked — add it
            bookmarkRef.set(
                mapOf(
                    "postId" to postId,
                    "savedAt" to System.currentTimeMillis()
                )
            )
                .addOnSuccessListener { onResult(true) }
                .addOnFailureListener { onResult(false) }
        }
    }

    fun isPostLiked(postId: String, onResult: (Boolean) -> Unit) {
        // SAFETY CHECK: If postId is empty, don't even try to ask Firestore
        if (postId.isEmpty()) {
            onResult(false)
            return
        }

        val currentUid = auth.currentUser?.uid ?: return
        db.collection("posts")
            .document(postId) // This is where it was crashing
            .collection("likes")
            .document(currentUid)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.exists())
            }
            .addOnFailureListener { onResult(false) }
    }

    fun isPostBookmarked(postId: String, onResult: (Boolean) -> Unit) {
        db.collection("users")
            .document(currentUid)
            .collection("bookmarks")
            .document(postId)
            .get()
            .addOnSuccessListener { onResult(it.exists()) }
            .addOnFailureListener { onResult(false) }
    }
    fun getBookmarkedPostIds(onResult: (List<String>) -> Unit) {
        db.collection("users")
            .document(currentUid)
            .collection("bookmarks")
            .get()
            .addOnSuccessListener { result ->
                onResult(result.documents.mapNotNull { it.id })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }
    fun getBookMarkedPosts(postIds: List<String>, onResult: (List<PostModel>) -> Unit) {
        val posts = mutableListOf<PostModel>()
        var remaining = postIds.size

        for (postId in postIds) {
            db.collection("posts")
                .document(postId)
                .get()
                .addOnSuccessListener { doc ->
                    val post = doc.toObject(PostModel::class.java)
                    if (post != null) {
                        post.isBookmarked = true
                        synchronized(posts) { posts.add(post) }
                    } else {
                        // orphan — delete the stale bookmark
                        db.collection("users")
                            .document(currentUid)
                            .collection("bookmarks")
                            .document(postId)
                            .delete()
                    }
                    synchronized(posts) {
                        if (--remaining == 0)
                            onResult(posts.sortedByDescending { it.createdAt })
                    }
                }
                .addOnFailureListener {
                    synchronized(posts) {
                        if (--remaining == 0)
                            onResult(posts.sortedByDescending { it.createdAt })
                    }
                }
        }
    }

    // ─── STORIES ────────────────────────────────────────────────────

    fun getAllStories(onResult: (List<StoryModel>) -> Unit) {
        db.collection("stories")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { result, error ->
                if (error != null || result == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val stories = result.documents.mapNotNull {
                    it.toObject(StoryModel::class.java)
                }
                onResult(stories)
            }
    }

    fun createStory(story: StoryModel, onResult: (Boolean) -> Unit) {
        val storyId = db.collection("stories").document().id
        val newStory = story.copy(
            storyId = storyId,
            userId = currentUid,
            createdAt = System.currentTimeMillis()
        )
        db.collection("stories")
            .document(storyId)
            .set(newStory)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun deleteStory(storyId: String, onResult: (Boolean) -> Unit) {
        db.collection("stories")
            .document(storyId)
            .delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
    fun markStoryViewed(storyId: String) {
        if (storyId.isEmpty()) return
        db.collection("stories")
            .document(storyId)
            .update("viewers", FieldValue.arrayUnion(currentUid))
    }

    fun getStoryViewers(storyId: String, onResult: (List<UserModel>) -> Unit) {
        db.collection("stories")
            .document(storyId)
            .get()
            .addOnSuccessListener { doc ->
                val story = doc.toObject(StoryModel::class.java) ?: return@addOnSuccessListener
                val viewerUids = story.viewers.filter { it != currentUid }
                if (viewerUids.isEmpty()) { onResult(emptyList()); return@addOnSuccessListener }

                val users = mutableListOf<UserModel>()
                var remaining = viewerUids.size
                viewerUids.forEach { uid ->
                    getUser(uid) { user ->
                        if (user != null) synchronized(users) { users.add(user) }
                        if (--remaining == 0) onResult(users)
                    }
                }
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    // ─── SEARCH ─────────────────────────────────────────────────────

    fun searchUsers(query: String, onResult: (List<UserModel>) -> Unit) {
        val lowerQuery = query.lowercase().trim()

        db.collection("users").get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserModel::class.java)?.copy(uid = doc.id)
                }.filter { user ->
                    user.username.lowercase().contains(lowerQuery) ||
                            user.fullName.lowercase().contains(lowerQuery)
                }
                onResult(users)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun searchPosts(query: String, onResult: (List<PostModel>) -> Unit) {
        db.collection("posts")
            .whereGreaterThanOrEqualTo("caption", query)
            .whereLessThanOrEqualTo("caption", query + "\uf8ff")
            .get()
            .addOnSuccessListener { result ->
                val posts = result.documents.mapNotNull {
                    it.toObject(PostModel::class.java)
                }
                onResult(posts)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }


    fun followUser(targetUid: String, onResult: (Boolean) -> Unit) {
        val batch = db.batch()

        val followingRef = db.collection("users")
            .document(currentUid)
            .collection("following")
            .document(targetUid)

        val followerRef = db.collection("users")
            .document(targetUid)
            .collection("followers")
            .document(currentUid)

        batch.set(followingRef, mapOf("uid" to targetUid))
        batch.set(followerRef, mapOf("uid" to currentUid))

        batch.commit()
            .addOnSuccessListener {
                val batch2 = db.batch()
                batch2.update(
                    db.collection("users").document(currentUid),
                    "followingCount", com.google.firebase.firestore.FieldValue.increment(1)
                )
                batch2.update(
                    db.collection("users").document(targetUid),
                    "followerCount", com.google.firebase.firestore.FieldValue.increment(1)
                )
                batch2.commit()
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(false) }
            }
            .addOnFailureListener { onResult(false) }
    }

    fun unfollowUser(targetUid: String, onResult: (Boolean) -> Unit) {
        val batch = db.batch()

        val followingRef = db.collection("users")
            .document(currentUid)
            .collection("following")
            .document(targetUid)

        val followerRef = db.collection("users")
            .document(targetUid)
            .collection("followers")
            .document(currentUid)

        batch.delete(followingRef)
        batch.delete(followerRef)

        batch.commit()
            .addOnSuccessListener {
                val batch2 = db.batch()
                batch2.update(
                    db.collection("users").document(currentUid),
                    "followingCount", com.google.firebase.firestore.FieldValue.increment(-1)
                )
                batch2.update(
                    db.collection("users").document(targetUid),
                    "followerCount", com.google.firebase.firestore.FieldValue.increment(-1)
                )
                batch2.commit()
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(false) }
            }
            .addOnFailureListener { onResult(false) }
    }

    fun isFollowing(targetUid: String, onResult: (Boolean) -> Unit) {
        db.collection("users")
            .document(currentUid)
            .collection("following")
            .document(targetUid)
            .get()
            .addOnSuccessListener { onResult(it.exists()) }
            .addOnFailureListener { onResult(false) }
    }

    // ─── COMMENTS ───────────────────────────────────────────────────

    fun getComments(postId: String, onResult: (List<CommentModel>) -> Unit): ListenerRegistration {
        return db.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { result, error ->
                if (error != null || result == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val comments = result.documents
                    .mapNotNull { it.toObject(CommentModel::class.java) }
                onResult(comments)
            }
    }

    // In FireStoreUtil.addComment, after writing the comment doc:
    fun addComment(comment: CommentModel, onResult: (Boolean) -> Unit) {
        val commentRef = db.collection("posts")
            .document(comment.postId)
            .collection("comments")
            .document()

        val commentWithId = comment.copy(commentId = commentRef.id)

        // Write comment only — no batch
        commentRef.set(commentWithId)
            .addOnSuccessListener {
                // Best-effort count increment — don't fail if this is blocked
                db.collection("posts").document(comment.postId)
                    .update("commentCount", FieldValue.increment(1))
                onResult(true)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    // In FireStoreUtil.deleteComment, same pattern:
    fun deleteComment(postId: String, commentId: String, onResult: (Boolean) -> Unit) {
        db.runBatch { batch ->
            batch.delete(
                db.collection("posts").document(postId)
                    .collection("comments").document(commentId)
            )
            batch.update(
                db.collection("posts").document(postId),
                "commentCount", FieldValue.increment(-1)
            )
        }.addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
    fun syncAllPostCommentCounts(onDone: () -> Unit) {
        db.collection("posts").get()
            .addOnSuccessListener { postsSnap ->
                if (postsSnap.isEmpty) { onDone(); return@addOnSuccessListener }
                var remaining = postsSnap.size()
                postsSnap.documents.forEach { postDoc ->
                    postDoc.reference.collection("comments").get()
                        .addOnSuccessListener { commentsSnap ->
                            val realCount = commentsSnap.size()
                            val storedCount = postDoc.getLong("commentCount")?.toInt() ?: 0
                            if (realCount != storedCount) {
                                postDoc.reference.update("commentCount", realCount)
                            }
                            if (--remaining == 0) onDone()
                        }
                        .addOnFailureListener { if (--remaining == 0) onDone() }
                }
            }
            .addOnFailureListener { onDone() }
    }
    fun getNotifications(onResult: (List<NotificationModel>) -> Unit) {
        db.collection("notifications")
            .whereEqualTo("toUserId", currentUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { result, error ->
                if (error != null || result == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val notifications = result.documents.mapNotNull {
                    it.toObject(NotificationModel::class.java)
                }
                onResult(notifications)
            }
    }

    fun sendNotification(context: Context, notification: NotificationModel, onResult: (Boolean) -> Unit) {
        if (notification.toUserId == currentUid) {
            onResult(false)
            return
        }

        val notifId = db.collection("notifications").document().id

        val data = mapOf(
            "notificationId"         to notifId,
            "toUserId"               to notification.toUserId,
            "fromUserId"             to currentUid,
            "fromUsername"           to notification.fromUsername,
            "fromFullName"           to notification.fromFullName,
            "fromUserProfileBase64"  to notification.fromUserProfileBase64,
            "type"                   to notification.type,
            "postId"                 to notification.postId,
            "postImageBase64"        to notification.postImageBase64,
            "message"                to notification.message,
            "seen"                   to false,
            "createdAt"              to System.currentTimeMillis()
        )
        db.collection("notifications")
            .document(notifId)
            .set(data)
            .addOnSuccessListener {
                onResult(true)
                // ✅ ADD THIS ONE LINE
                sendNotificationToUser(context, notification.toUserId, notification.type, notification.fromFullName)
            }
            .addOnFailureListener { onResult(false) }
    }
    fun deleteNotification(notificationId: String, onResult: (Boolean) -> Unit) {
        if (notificationId.isEmpty()) {
            onResult(false)
            return
        }
        db.collection("notifications")
            .document(notificationId)
            .delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun markSingleNotificationSeen(notificationId: String, onResult: (Boolean) -> Unit) {
        db.collection("notifications")
            .document(notificationId)
            .update("seen", true)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun markNotificationsSeen(onResult: (Boolean) -> Unit) {
        db.collection("notifications")
            .whereEqualTo("toUserId", currentUid)
            .whereEqualTo("seen", false)
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()
                result.documents.forEach { doc ->
                    batch.update(doc.reference, "seen", true)
                }
                batch.commit()
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(false) }
            }
            .addOnFailureListener { onResult(false) }
    }
    fun getUnseenNotificationCount(callback: (Int) -> Unit) {
        db.collection("notifications")
            .whereEqualTo("toUserId", currentUid)
            .whereEqualTo("seen", false)
            .get()
            .addOnSuccessListener { callback(it.size()) }
            .addOnFailureListener { callback(0) }
    }

    fun saveFcmToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
    }
    fun clearFcmToken(onDone: () -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(uid)
            .update("fcmToken", "")
            .addOnCompleteListener { onDone() }
    }

    fun sendNotificationToUser(
        context: Context,
        toUserId: String,
        type: String,
        fromFullName: String
    ) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(toUserId)
            .get()
            .addOnSuccessListener { doc ->
                val token = doc.getString("fcmToken")
                if (!token.isNullOrEmpty()) {
                    FcmHelper.sendPushNotification(context, token, type, fromFullName)
                }
            }
    }

    // ─── Account Deletion ───────────────────────────────────────────────────

    fun deleteAccount(onResult: (Boolean, String) -> Unit) {
        val uid = currentUid
        if (uid.isEmpty()) {
            onResult(false, "No user logged in")
            return
        }

        deleteUserPosts(uid) {
            deleteUserStories(uid) {
                deleteUserBookmarks(uid) {
                    deleteUserFollowData(uid) {
                        db.collection("users").document(uid).delete()
                            .addOnSuccessListener {
                                AuthUtil.deleteAccount { success ->
                                    if (success) {
                                        onResult(true, "")
                                    } else {
                                        onResult(false, "Failed to delete auth account")
                                    }
                                }
                            }
                            .addOnFailureListener {
                                onResult(false, it.message ?: "Failed to delete user document")
                            }
                    }
                }
            }
        }
    }

    private fun deleteUserPosts(uid: String, onDone: () -> Unit) {
        db.collection("posts")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) { onDone(); return@addOnSuccessListener }
                var remaining = snapshot.size()
                for (doc in snapshot.documents) {
                    deletePost(doc.id) {
                        if (--remaining == 0) onDone()
                    }
                }
            }
            .addOnFailureListener { onDone() }
    }

    private fun deleteUserStories(uid: String, onDone: () -> Unit) {
        db.collection("stories")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) { onDone(); return@addOnSuccessListener }
                var remaining = snapshot.size()
                for (doc in snapshot.documents) {
                    deleteStory(doc.id) {
                        if (--remaining == 0) onDone()
                    }
                }
            }
            .addOnFailureListener { onDone() }
    }

    private fun deleteUserBookmarks(uid: String, onDone: () -> Unit) {
        db.collection("users").document(uid)
            .collection("bookmarks").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) { onDone(); return@addOnSuccessListener }
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit().addOnCompleteListener { onDone() }
            }
            .addOnFailureListener { onDone() }
    }

    private fun deleteUserFollowData(uid: String, onDone: () -> Unit) {
        db.collection("users").document(uid)
            .collection("following").get()
            .addOnSuccessListener { following ->
                val batch = db.batch()
                following.documents.forEach { doc ->
                    val targetUid = doc.getString("uid") ?: return@forEach
                    batch.delete(
                        db.collection("users").document(targetUid)
                            .collection("followers").document(uid)
                    )
                    batch.delete(doc.reference)
                }
                db.collection("users").document(uid)
                    .collection("followers").get()
                    .addOnSuccessListener { followers ->
                        followers.documents.forEach { doc ->
                            val followerUid = doc.getString("uid") ?: return@forEach
                            batch.delete(
                                db.collection("users").document(followerUid)
                                    .collection("following").document(uid)
                            )
                            batch.delete(doc.reference)
                        }
                        batch.commit().addOnCompleteListener { onDone() }
                    }
                    .addOnFailureListener { onDone() }
            }
            .addOnFailureListener { onDone() }
    }
    fun updateUserProfileEverywhere(newFullName: String?, newBase64: String?) {

        fun batchUpdate(snapshot: com.google.firebase.firestore.QuerySnapshot,
                        field: String, value: String) {
            if (snapshot.isEmpty) return
            val batch = db.batch()
            snapshot.documents.forEach { batch.update(it.reference, field, value) }
            batch.commit()
        }

        fun runAllUpdates() {
            // Posts
            if (newFullName != null) {
                db.collection("posts").whereEqualTo("userId", currentUid)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .addOnSuccessListener { batchUpdate(it, "fullName", newFullName) }
            }
            if (newBase64 != null) {
                db.collection("posts").whereEqualTo("userId", currentUid)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .addOnSuccessListener { batchUpdate(it, "userProfileBase64", newBase64) }
            }

            // Comments
            if (newFullName != null) {
                db.collectionGroup("comments").whereEqualTo("userId", currentUid)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .addOnSuccessListener { batchUpdate(it, "fullName", newFullName) }
            }
            if (newBase64 != null) {
                db.collectionGroup("comments").whereEqualTo("userId", currentUid)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .addOnSuccessListener { batchUpdate(it, "userProfileBase64", newBase64) }
            }

            // Notifications
            if (newFullName != null) {
                db.collection("notifications").whereEqualTo("fromUserId", currentUid)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .addOnSuccessListener { batchUpdate(it, "fromFullName", newFullName) }
            }
            if (newBase64 != null) {
                db.collection("notifications").whereEqualTo("fromUserId", currentUid)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .addOnSuccessListener { batchUpdate(it, "fromUserProfileBase64", newBase64) }
            }

            // Stories
            if (newFullName != null) {
                db.collection("stories").whereEqualTo("userId", currentUid)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .addOnSuccessListener { batchUpdate(it, "fullName", newFullName) }
            }
            if (newBase64 != null) {
                db.collection("stories").whereEqualTo("userId", currentUid)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .addOnSuccessListener { batchUpdate(it, "userProfileBase64", newBase64) }
            }
        }

        // First update the user document, then propagate everywhere
        db.collection("users").document(currentUid)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { runAllUpdates() }
    }
}