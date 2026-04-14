package com.example.socialconnect

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object FireStoreUtil {

    val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
            .get()
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

    // ─── POSTS ──────────────────────────────────────────────────────

    fun getAllPosts(onResult: (List<PostModel>) -> Unit) {
        db.collection("posts")
            .addSnapshotListener { result, error ->
                if (error != null || result == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val posts = result.documents.mapNotNull {
                    it.toObject(PostModel::class.java)
                }
                onResult(posts)
            }
    }

    fun getUserPosts(uid: String, onResult: (List<PostModel>) -> Unit) {
        db.collection("posts")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { result, error ->
                if (error != null || result == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val posts = result.documents.mapNotNull {
                    it.toObject(PostModel::class.java)
                }
                onResult(posts)
            }
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
                // increment user post count
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
        db.collection("posts")
            .document(postId)
            .delete()
            .addOnSuccessListener {
                // decrement user post count
                db.collection("users")
                    .document(currentUid)
                    .update("postCount",
                        com.google.firebase.firestore.FieldValue.increment(-1))
                onResult(true)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun likePost(postId: String, isLiked: Boolean, onResult: (Boolean) -> Unit) {
        val likeRef = db.collection("posts")
            .document(postId)
            .collection("likes")
            .document(currentUid)

        val postRef = db.collection("posts").document(postId)

        if (isLiked) {
            // unlike
            likeRef.delete()
                .addOnSuccessListener {
                    postRef.update("likeCount",
                        com.google.firebase.firestore.FieldValue.increment(-1))
                    onResult(true)
                }
                .addOnFailureListener { onResult(false) }
        } else {
            // like
            likeRef.set(mapOf("userId" to currentUid))
                .addOnSuccessListener {
                    postRef.update("likeCount",
                        com.google.firebase.firestore.FieldValue.increment(1))
                    onResult(true)
                }
                .addOnFailureListener { onResult(false) }
        }
    }

    // in FirestoreUtil.kt — replace bookmarkPost function
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
        db.collection("posts")
            .document(postId)
            .collection("likes")
            .document(currentUid)
            .get()
            .addOnSuccessListener { onResult(it.exists()) }
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

    // ─── SEARCH ─────────────────────────────────────────────────────

    fun searchUsers(query: String, onResult: (List<UserModel>) -> Unit) {
        db.collection("users")
            .whereGreaterThanOrEqualTo("username", query)
            .whereLessThanOrEqualTo("username", query + "\uf8ff")
            .get()
            .addOnSuccessListener { result ->
                val users = result.documents.mapNotNull {
                    it.toObject(UserModel::class.java)
                }.filter { it.uid != currentUid } // exclude self
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

    // ─── FOLLOW ─────────────────────────────────────────────────────

    fun followUser(targetUid: String, onResult: (Boolean) -> Unit) {
        val batch = db.batch()

        // add to current user following
        val followingRef = db.collection("users")
            .document(currentUid)
            .collection("following")
            .document(targetUid)

        // add to target user followers
        val followerRef = db.collection("users")
            .document(targetUid)
            .collection("followers")
            .document(currentUid)

        batch.set(followingRef, mapOf("uid" to targetUid))
        batch.set(followerRef, mapOf("uid" to currentUid))

        batch.commit()
            .addOnSuccessListener {
                // update counts
                db.collection("users").document(currentUid)
                    .update("followingCount",
                        com.google.firebase.firestore.FieldValue.increment(1))
                db.collection("users").document(targetUid)
                    .update("followerCount",
                        com.google.firebase.firestore.FieldValue.increment(1))
                onResult(true)
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
                db.collection("users").document(currentUid)
                    .update("followingCount",
                        com.google.firebase.firestore.FieldValue.increment(-1))
                db.collection("users").document(targetUid)
                    .update("followerCount",
                        com.google.firebase.firestore.FieldValue.increment(-1))
                onResult(true)
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

    fun getComments(postId: String, onResult: (List<CommentModel>) -> Unit) {
        db.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { result, error ->
                if (error != null || result == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val comments = result.documents.mapNotNull {
                    it.toObject(CommentModel::class.java)
                }
                onResult(comments)
            }
    }

    fun addComment(comment: CommentModel, onResult: (Boolean) -> Unit) {
        val commentId = db.collection("posts")
            .document(comment.postId)
            .collection("comments")
            .document().id

        val newComment = comment.copy(
            commentId = commentId,
            userId = currentUid,
            createdAt = System.currentTimeMillis()
        )

        db.collection("posts")
            .document(comment.postId)
            .collection("comments")
            .document(commentId)
            .set(newComment)
            .addOnSuccessListener {
                // increment comment count
                db.collection("posts")
                    .document(comment.postId)
                    .update("commentCount",
                        com.google.firebase.firestore.FieldValue.increment(1))
                onResult(true)
            }
            .addOnFailureListener { onResult(false) }
    }

    fun deleteComment(postId: String, commentId: String, onResult: (Boolean) -> Unit) {
        db.collection("posts")
            .document(postId)
            .collection("comments")
            .document(commentId)
            .delete()
            .addOnSuccessListener {
                db.collection("posts")
                    .document(postId)
                    .update("commentCount",
                        com.google.firebase.firestore.FieldValue.increment(-1))
                onResult(true)
            }
            .addOnFailureListener { onResult(false) }
    }

    // ─── NOTIFICATIONS ──────────────────────────────────────────────

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

    fun sendNotification(notification: NotificationModel, onResult: (Boolean) -> Unit) {
        val notifId = db.collection("notifications").document().id
        val newNotif = notification.copy(
            notificationId = notifId,
            fromUserId = currentUid,
            createdAt = System.currentTimeMillis()
        )
        db.collection("notifications")
            .document(notifId)
            .set(newNotif)
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
}