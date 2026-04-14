package com.example.socialconnect

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest

object AuthUtil {

    private val auth = FirebaseAuth.getInstance()

    // ─── CURRENT USER ───────────────────────────────────────────────

    val currentUser: FirebaseUser? get() = auth.currentUser

    val currentUid: String get() = auth.currentUser?.uid ?: ""

    val isLoggedIn: Boolean get() = auth.currentUser != null

    val isEmailVerified: Boolean get() = auth.currentUser?.isEmailVerified == true

    // ─── SIGN UP ────────────────────────────────────────────────────

    fun signUp(
        email: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onResult(true, "")
            }
            .addOnFailureListener {
                onResult(false, it.message ?: "Sign up failed")
            }
    }

    // ─── LOGIN ──────────────────────────────────────────────────────

    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onResult(true, "")
            }
            .addOnFailureListener {
                onResult(false, it.message ?: "Login failed")
            }
    }

    // ─── LOGOUT ─────────────────────────────────────────────────────

    fun logout() {
        auth.signOut()
    }

    // ─── EMAIL VERIFICATION ─────────────────────────────────────────

    fun sendEmailVerification(onResult: (Boolean, String) -> Unit) {
        auth.currentUser?.sendEmailVerification()
            ?.addOnSuccessListener {
                onResult(true, "")
            }
            ?.addOnFailureListener {
                onResult(false, it.message ?: "Failed to send verification email")
            } ?: onResult(false, "No user found")
    }

    // ─── FORGOT PASSWORD ────────────────────────────────────────────

    fun sendPasswordResetEmail(
        email: String,
        onResult: (Boolean, String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                onResult(true, "")
            }
            .addOnFailureListener {
                onResult(false, it.message ?: "Failed to send reset email")
            }
    }

    // ─── UPDATE DISPLAY NAME ────────────────────────────────────────

    fun updateDisplayName(name: String, onResult: (Boolean) -> Unit) {
        val profileUpdates = userProfileChangeRequest {
            displayName = name
        }
        auth.currentUser?.updateProfile(profileUpdates)
            ?.addOnSuccessListener { onResult(true) }
            ?.addOnFailureListener { onResult(false) }
    }

    // ─── DELETE ACCOUNT ─────────────────────────────────────────────

    fun deleteAccount(onResult: (Boolean) -> Unit) {
        auth.currentUser?.delete()
            ?.addOnSuccessListener { onResult(true) }
            ?.addOnFailureListener { onResult(false) }
    }
}