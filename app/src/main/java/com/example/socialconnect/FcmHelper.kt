package com.example.socialconnect

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object FcmHelper {

    private const val FCM_URL = "https://fcm.googleapis.com/v1/projects/social-connect-65f67/messages:send"
    private const val TAG = "FcmHelper"

    // Get OAuth2 access token from service account
    private fun getAccessToken(context: Context): String {
        val inputStream = context.assets.open("service_account.json")
        val credentials = GoogleCredentials
            .fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        credentials.refreshIfExpired()
        return credentials.accessToken.tokenValue
    }

    fun sendPushNotification(
        context: Context,
        toToken: String,
        type: String,
        fromFullName: String
    ) {
        val body = when (type) {
            "like"    -> "$fromFullName liked your post"
            "comment" -> "$fromFullName commented on your post"
            "follow"  -> "$fromFullName started following you"
            else      -> "You have a new notification"
        }

        val json = JSONObject().apply {
            put("message", JSONObject().apply {
                put("token", toToken)
                put("data", JSONObject().apply {
                    put("title", "Social Connect")
                    put("body", body)
                    put("type", type)
                })
                put("android", JSONObject().apply {
                    put("priority", "high")
                })
            })
        }

        Thread {
            try {
                val accessToken = getAccessToken(context)
                val url = URL(FCM_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $accessToken")
                conn.doOutput = true
                conn.outputStream.write(json.toString().toByteArray())

                val responseCode = conn.responseCode
                Log.d(TAG, "Response: $responseCode")

                if (responseCode != 200) {
                    val error = conn.errorStream?.bufferedReader()?.readText()
                    Log.e(TAG, "Error: $error")
                }

                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Exception: ${e.message}")
            }
        }.start()
    }
}