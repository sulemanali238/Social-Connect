package com.example.socialconnect

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val title = data["title"] ?: remoteMessage.notification?.title ?: "New Notification"
        val body  = data["body"]  ?: remoteMessage.notification?.body  ?: ""
        val type  = data["type"]  ?: ""
        val fromUserId = data["fromUserId"] ?: ""

        showNotification(title, body, type, fromUserId)
    }

    // Called when FCM assigns a new token to this device
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save the new token to Firestore
        FireStoreUtil.saveFcmToken(token)
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
        fromUserId: String
    ) {
        val channelId = "social_connect_notifications"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create channel (required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Social Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Likes, comments, and follows"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        // Tapping the notification opens NotificationActivity
        val intent = Intent(this, NotificationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("type", type)
            putExtra("fromUserId", fromUserId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = when (type) {
            "like"    -> R.drawable.ic_heart_filled
            "comment" -> R.drawable.ic_comment
            "follow"  -> R.drawable.ic_plus
            else      -> R.drawable.ic_bell
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}