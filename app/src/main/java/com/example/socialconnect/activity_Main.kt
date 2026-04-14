package com.example.socialconnect

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.socialconnect.fragments.HomeFragment
import com.example.socialconnect.fragments.ProfileFragment
import com.example.socialconnect.fragments.SearchFragment
import com.example.socialconnect.fragments.StoryFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class activity_Main : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var indicators: List<View>

    private val homeFragment = HomeFragment()
    private val searchFragment = SearchFragment()
    private val storyFragment = StoryFragment()
    private val profileFragment = ProfileFragment()

    private var activeFragment: Fragment = homeFragment
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)
        indicators = listOf(
            findViewById(R.id.indicator0),
            findViewById(R.id.indicator1),
            findViewById(R.id.indicator2),
            findViewById(R.id.indicator3),
            findViewById(R.id.indicator4)
        )

        setupFragments()
        setIndicator(0)
        applyIconTints(0)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showFragment(homeFragment)
                    setIndicator(0)
                    applyIconTints(0)
                    true
                }
                R.id.nav_search -> {
                    showFragment(searchFragment)
                    setIndicator(1)
                    applyIconTints(1)
                    true
                }
                R.id.nav_add_post -> {
                    startActivity(Intent(this, activity_AddPost::class.java))
                    false
                }
                R.id.nav_story -> {
                    showFragment(storyFragment)
                    setIndicator(3)
                    applyIconTints(3)
                    true
                }
                R.id.nav_profile -> {
                    showFragment(profileFragment)
                    setIndicator(4)
                    applyIconTints(4)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFragments() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment)
            .add(R.id.fragmentContainer, storyFragment, "story").hide(storyFragment)
            .add(R.id.fragmentContainer, searchFragment, "search").hide(searchFragment)
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
        // skip indicator 2 (add post) — never shows indicator
        indicators.forEachIndexed { index, view ->
            view.visibility =
                if (index == selectedIndex && index != 2) View.VISIBLE
                else View.INVISIBLE
        }
        currentIndex = selectedIndex
    }

    private fun applyIconTints(selectedIndex: Int) {
        val teal = ContextCompat.getColor(this, R.color.teal_green)
        val grey = ContextCompat.getColor(this, R.color.grey_nav)

        val menuView = bottomNav.getChildAt(0) as? android.view.ViewGroup ?: return

        for (i in 0 until menuView.childCount) {
            val item = menuView.getChildAt(i)
            val icon = item.findViewById<android.widget.ImageView>(
                com.google.android.material.R.id.navigation_bar_item_icon_view
            )
            icon?.setColorFilter(
                when {
                    i == 2 -> teal      // add post always teal
                    i == selectedIndex -> teal   // selected tab teal
                    else -> grey        // others grey
                }
            )
        }
    }
}