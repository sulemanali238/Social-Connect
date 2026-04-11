package com.example.socialconnect

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.socialconnect.fragments.HomeFragment
import com.example.socialconnect.fragments.ProfileFragment
import com.example.socialconnect.fragments.SearchFragment
import com.example.socialconnect.fragments.StoryFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class activity_Main : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var indicators: List<View>

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

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            bottomNav.selectedItemId = R.id.nav_home
            setIndicator(0)
        }

        bottomNav.setOnItemSelectedListener { item ->
            val index = when (item.itemId) {
                R.id.nav_home -> 0
                R.id.nav_search -> 1
                R.id.nav_add_post -> 2
                R.id.nav_story -> 3
                R.id.nav_profile -> 4
                else -> 0
            }

            setIndicator(index)

            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_search -> loadFragment(SearchFragment())
                R.id.nav_add_post -> { /* add post later */ }
                R.id.nav_story -> loadFragment(StoryFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }
    }

    private fun setIndicator(selectedIndex: Int) {
        indicators.forEachIndexed { index, view ->
            view.visibility = if (index == selectedIndex) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}