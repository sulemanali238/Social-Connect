package com.example.socialconnect

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class activity_SplashScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var tvLoading: TextView
    private lateinit var loadingBarFill: View
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        auth = FirebaseAuth.getInstance()
        tvLoading = findViewById(R.id.tvLoading)
        loadingBarFill = findViewById(R.id.loadingBarFill)

        animateLoadingText()
        animateLoadingBar()

        // Navigate after 3 seconds
        handler.postDelayed({
            navigateNext()
        }, 3000)
    }

    private fun animateLoadingText() {
        val dots = listOf("LOADING", "LOADING.", "LOADING..", "LOADING...")
        var index = 0

        val runnable = object : Runnable {
            override fun run() {
                tvLoading.text = dots[index % dots.size]
                index++
                handler.postDelayed(this, 300)
            }
        }
        handler.post(runnable)
    }

    private fun animateLoadingBar() {
        loadingBarFill.post {
            val parentWidth = (loadingBarFill.parent as View).width

            val animator = ValueAnimator.ofInt(0, parentWidth)
            animator.duration = 3000
            animator.interpolator = DecelerateInterpolator()

            animator.addUpdateListener { anim ->
                val params = loadingBarFill.layoutParams
                params.width = anim.animatedValue as Int
                loadingBarFill.layoutParams = params
            }

            animator.start()
        }
    }

    private fun navigateNext() {
        val user = auth.currentUser

        val intent = if (user != null && user.isEmailVerified) {
            // Already logged in → go to Home
            Intent(this, activity_Main::class.java)
        } else {
            // Not logged in → go to Launcher
            Intent(this, activity_Launchers::class.java)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up handler to prevent memory leaks
        handler.removeCallbacksAndMessages(null)
    }
}