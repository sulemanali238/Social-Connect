package com.example.socialconnect.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.R

class StoryFragment : Fragment() {

    private lateinit var rvStories: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_story, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvStories = view.findViewById(R.id.rvStories)
        rvStories.layoutManager = LinearLayoutManager(requireContext())

        view.findViewById<View>(R.id.btnAddStory).setOnClickListener {
            // image picker will be wired here later
        }

        // Adapter + Firestore will be wired here later
    }
}