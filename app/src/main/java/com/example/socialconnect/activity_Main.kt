package com.example.socialconnect

import android.content.Intent
import androidx.activity.addCallback
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.socialconnect.fragments.ChatFragment
import com.example.socialconnect.fragments.ChatMessageFragment
import com.example.socialconnect.fragments.HomeFragment
import com.example.socialconnect.fragments.ProfileFragment
import com.example.socialconnect.fragments.SearchFragment
import com.example.socialconnect.fragments.StoryFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.messaging.FirebaseMessaging

class activity_Main : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var bottomNavCard: MaterialCardView
    private lateinit var indicators: List<View>

    private val homeFragment = HomeFragment()
    private val storyFragment = StoryFragment()
    private val chatFragment = ChatFragment()
    private val profileFragment = ProfileFragment()

    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    Log.d("FCM_TOKEN", "Fresh token: $token")
                    FireStoreUtil.saveFcmToken(token)
                }
                .addOnFailureListener { e ->
                    Log.e("FCM_TOKEN", "Failed: ${e.message}")
                }
        }

        FireStoreUtil.syncAllPostCommentCounts {}

        FireStoreUtil.getCurrentUser { user ->
            if (user != null) {
                CurrentUserCache.populate(user)
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            val fm = supportFragmentManager
            if (fm.backStackEntryCount > 0) {
                fm.popBackStack()
                return@addCallback
            }
            if (bottomNav.selectedItemId != R.id.nav_home) {
                bottomNav.selectedItemId = R.id.nav_home
            } else {
                finish()
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                bottomNavCard.visibility = View.VISIBLE
                activeFragment = getCurrentNavFragment()
            }
        }

        initViews()
        setupFragments()
        setupBackNavigation()
        setIndicator(0)
        applyIconTints(0)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { updateUI(homeFragment, 0); true }
                R.id.nav_story -> { updateUI(storyFragment, 1); true }
                R.id.nav_add_post -> {
                    startActivity(Intent(this, activity_AddPost::class.java))
                    false
                }
                R.id.nav_chat -> { updateUI(chatFragment, 3); true }
                R.id.nav_profile -> { updateUI(profileFragment, 4); true }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ChatUtil.setPresenceOnline()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }
    override fun onPause() {
        super.onPause()
        ChatUtil.setPresenceOffline()
    }

    private fun initViews() {
        bottomNav = findViewById(R.id.bottomNav)
        bottomNavCard = findViewById(R.id.bottomNavCard)
        indicators = listOf(
            findViewById(R.id.indicator0),
            findViewById(R.id.indicator1),
            findViewById(R.id.indicator2),
            findViewById(R.id.indicator3),
            findViewById(R.id.indicator4)
        )
    }

    private fun updateUI(fragment: Fragment, index: Int) {
        showFragment(fragment)
        setIndicator(index)
        applyIconTints(index)
    }

    private fun getCurrentNavFragment(): Fragment {
        return when (bottomNav.selectedItemId) {
            R.id.nav_story -> storyFragment
            R.id.nav_chat -> chatFragment
            R.id.nav_profile -> profileFragment
            else -> homeFragment
        }
    }
    private fun handleIncomingIntent(intent: Intent) {
        val profileUid = intent.getStringExtra("OPEN_PROFILE_UID")
        val postId = intent.getStringExtra("OPEN_POST_ID")

        if (!profileUid.isNullOrEmpty()) {
            openOtherProfile(profileUid)
        } else if (!postId.isNullOrEmpty()) {
            openPostById(postId)
        }
    }

    private fun openPostById(postId: String) {
        val intent = Intent(this, PostDetail::class.java).apply {
            putExtra("POST_ID", postId)
        }
        startActivity(intent)
    }

    fun openSearch() {
        val searchFrag = SearchFragment()
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .add(R.id.fragmentContainer, searchFrag, "search_overlay")
            .addToBackStack("search_overlay")
            .commit()
        bottomNavCard.visibility = View.GONE
        activeFragment = searchFrag
    }

    fun openOtherProfile(userId: String) {
        val otherProfile = ProfileFragment().apply {
            arguments = Bundle().apply {
                putString("USER_ID", userId)
            }
        }
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .add(R.id.fragmentContainer, otherProfile, "profile_$userId")
            .addToBackStack("profile_$userId")
            .commit()
        activeFragment = otherProfile
    }

    fun openChatMessage(otherUid: String, otherName: String, otherImageBase64: String) {
        val chatMsgFrag = ChatMessageFragment().apply {
            arguments = Bundle().apply {
                putString("OTHER_UID", otherUid)
                putString("OTHER_NAME", otherName)
                putString("OTHER_IMAGE_URL", otherImageBase64)
            }
        }
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .hide(activeFragment)
            .add(R.id.fragmentContainer, chatMsgFrag, "chat_$otherUid")
            .addToBackStack("chat_$otherUid")
            .commit()
        bottomNavCard.visibility = View.GONE
        activeFragment = chatMsgFrag
    }

    fun openMyProfile() {
        val fm = supportFragmentManager
        if (fm.backStackEntryCount > 0) {
            fm.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        bottomNav.selectedItemId = R.id.nav_profile
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this) {
            val fm = supportFragmentManager
            if (fm.backStackEntryCount > 0) {
                fm.popBackStack()
                return@addCallback
            }
            if (bottomNav.selectedItemId != R.id.nav_home) {
                bottomNav.selectedItemId = R.id.nav_home
            } else {
                finish()
            }
        }
    }

    private fun setupFragments() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment)
            .add(R.id.fragmentContainer, chatFragment, "chat").hide(chatFragment)
            .add(R.id.fragmentContainer, storyFragment, "story").hide(storyFragment)
            .add(R.id.fragmentContainer, homeFragment, "home")
            .commit()
        activeFragment = homeFragment
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(fragment)
            .commit()
        activeFragment = fragment
    }

    private fun setIndicator(selectedIndex: Int) {
        indicators.forEachIndexed { index, view ->
            view.visibility = if (index == selectedIndex && index != 2) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun applyIconTints(selectedIndex: Int) {
        val teal  = ContextCompat.getColor(this, R.color.teal_green)
        val black = ContextCompat.getColor(this, R.color.Black)

        // Map: index -> (outline drawable, filled drawable)
        val iconMap = mapOf(
            0 to Pair(R.drawable.ic_home_outline,    R.drawable.ic_home),
            1 to Pair(R.drawable.ic_story_outline,   R.drawable.ic_story),
            3 to Pair(R.drawable.ic_chat_outline,    R.drawable.ic_chat),
            4 to Pair(R.drawable.ic_profile_outline, R.drawable.ic_profile)
        )

        val menuView = bottomNav.getChildAt(0) as? android.view.ViewGroup ?: return

        for (i in 0 until menuView.childCount) {
            val item = menuView.getChildAt(i)
            val icon = item.findViewById<ImageView>(
                com.google.android.material.R.id.navigation_bar_item_icon_view
            )

            val sizeRes = if (i == 2) R.dimen.plus_icon_size else R.dimen.nav_icon_size
            val size = resources.getDimensionPixelSize(sizeRes)
            icon?.layoutParams?.width  = size
            icon?.layoutParams?.height = size
            icon?.requestLayout()

            if (i == 2) {
                icon?.imageTintList = null
            } else {
                val isSelected = (i == selectedIndex)
                val icons = iconMap[i]
                if (icons != null) {
                    icon?.setImageResource(if (isSelected) icons.second else icons.first)
                    icon?.setColorFilter(if (isSelected) teal else black)
                }
            }
        }
    }
}