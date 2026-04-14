package com.example.socialconnect.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.FireStoreUtil
import com.example.socialconnect.PostModel
import com.example.socialconnect.R
import com.example.socialconnect.UserModel
import com.example.socialconnect.adapters.PostAdapter
import com.example.socialconnect.adapters.SearchAdapter

class SearchFragment : Fragment() {

    private lateinit var rvSearchResults: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var layoutEmptyState: View
    private lateinit var layoutTopBarDefault: LinearLayout
    private lateinit var layoutTopBarSearch: LinearLayout
    private lateinit var layoutSearchTabs: LinearLayout
    private lateinit var tabPeople: LinearLayout
    private lateinit var tabPosts: LinearLayout
    private lateinit var indicatorPeople: View
    private lateinit var indicatorPosts: View
    private lateinit var tvPeopleTab: TextView
    private lateinit var tvPostsTab: TextView
    private lateinit var btnBackSearch: ImageView
    private lateinit var btnClearSearch: ImageView
    private lateinit var mainContent: View

    private lateinit var searchAdapter: SearchAdapter
    private lateinit var postSearchAdapter: PostAdapter
    private val searchResults = mutableListOf<UserModel>()
    private val postResults = mutableListOf<PostModel>()

    private var isSearchActive = false
    private var isPeopleTabActive = true
    private var searchRunnable: Runnable? = null
    private val searchHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupAdapters()
        setupClickListeners(view)
        setupBackPress()
        setupSearchInput()
    }

    private fun initViews(view: View) {
        rvSearchResults = view.findViewById(R.id.rvSearchResults)
        etSearch = view.findViewById(R.id.etSearch)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        layoutTopBarDefault = view.findViewById(R.id.layoutTopBarDefault)
        layoutTopBarSearch = view.findViewById(R.id.layoutTopBarSearch)
        layoutSearchTabs = view.findViewById(R.id.layoutSearchTabs)
        tabPeople = view.findViewById(R.id.tabPeople)
        tabPosts = view.findViewById(R.id.tabPosts)
        indicatorPeople = view.findViewById(R.id.indicatorPeople)
        indicatorPosts = view.findViewById(R.id.indicatorPosts)
        tvPeopleTab = tabPeople.getChildAt(0) as TextView
        tvPostsTab = tabPosts.getChildAt(0) as TextView
        btnBackSearch = view.findViewById(R.id.btnBackSearch)
        btnClearSearch = view.findViewById(R.id.btnClearSearch)
        mainContent = view.findViewById(R.id.mainContent)
    }

    private fun setupAdapters() {
        searchAdapter = SearchAdapter(
            users = searchResults,
            onUserClick = { },
            onFollowClick = { user, position -> handleFollow(user, position) }
        )

        postSearchAdapter = PostAdapter(
            posts = postResults,
            onLikeClick = { post, position -> },
            onCommentClick = { },
            onBookmarkClick = { post, position -> },
            onShareClick = { },
            onAvatarClick = { }
        )
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.searchBox).setOnClickListener {
            showSearchState()
        }

        btnBackSearch.setOnClickListener {
            hideSearchState()
        }

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
        }

        tabPeople.setOnClickListener {
            switchTab(true)
        }

        tabPosts.setOnClickListener {
            switchTab(false)
        }
    }

    private fun switchTab(isPeople: Boolean) {
        isPeopleTabActive = isPeople

        // update tab indicators
        indicatorPeople.setBackgroundResource(
            if (isPeople) R.color.teal_green
            else android.R.color.transparent
        )
        indicatorPosts.setBackgroundResource(
            if (!isPeople) R.color.teal_green
            else android.R.color.transparent
        )

        // update tab text colors
        tvPeopleTab.setTextColor(
            if (isPeople)
                requireContext().getColor(R.color.teal_green)
            else
                requireContext().getColor(android.R.color.darker_gray)
        )
        tvPostsTab.setTextColor(
            if (!isPeople)
                requireContext().getColor(R.color.teal_green)
            else
                requireContext().getColor(android.R.color.darker_gray)
        )

        // switch adapter
        if (isPeople) {
            rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
            rvSearchResults.adapter = searchAdapter
        } else {
            rvSearchResults.layoutManager = GridLayoutManager(requireContext(), 2)
            rvSearchResults.adapter = postSearchAdapter
        }

        // re-run search with current query if not empty
        val query = etSearch.text.toString().trim()
        if (query.isNotEmpty()) {
            triggerSearch(query)
        }
    }

    private fun setupBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isSearchActive) hideSearchState()
                    else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun setupSearchInput() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                btnClearSearch.visibility =
                    if (query.isEmpty()) View.GONE else View.VISIBLE

                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                if (query.isEmpty()) {
                    rvSearchResults.visibility = View.GONE
                    layoutEmptyState.visibility = View.VISIBLE
                    searchResults.clear()
                    postResults.clear()
                    searchAdapter.notifyDataSetChanged()
                    postSearchAdapter.notifyDataSetChanged()
                } else {
                    searchRunnable = Runnable { triggerSearch(query) }
                    searchHandler.postDelayed(searchRunnable!!, 500)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun triggerSearch(query: String) {
        if (isPeopleTabActive) searchUsers(query)
        else searchPosts(query)
    }

    private fun searchUsers(query: String) {
        FireStoreUtil.searchUsers(query) { users ->
            if (!isAdded) return@searchUsers
            searchResults.clear()
            searchResults.addAll(users)
            searchAdapter.notifyDataSetChanged()
            toggleEmptyState(users.isEmpty())
        }
    }

    private fun searchPosts(query: String) {
        FireStoreUtil.searchPosts(query) { posts ->
            if (!isAdded) return@searchPosts
            postResults.clear()
            postResults.addAll(posts)
            postSearchAdapter.notifyDataSetChanged()
            toggleEmptyState(posts.isEmpty())
        }
    }

    private fun toggleEmptyState(isEmpty: Boolean) {
        rvSearchResults.visibility = if (isEmpty) View.GONE else View.VISIBLE
        layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun handleFollow(user: UserModel, position: Int) {
        FireStoreUtil.isFollowing(user.uid) { isFollowing ->
            if (!isAdded) return@isFollowing
            if (isFollowing) {
                FireStoreUtil.unfollowUser(user.uid) { success ->
                    if (success && isAdded) searchAdapter.notifyItemChanged(position)
                }
            } else {
                FireStoreUtil.followUser(user.uid) { success ->
                    if (success && isAdded) searchAdapter.notifyItemChanged(position)
                }
            }
        }
    }

    private fun showSearchState() {
        isSearchActive = true
        layoutTopBarDefault.visibility = View.GONE
        layoutTopBarSearch.visibility = View.VISIBLE
        layoutSearchTabs.visibility = View.VISIBLE
        mainContent.visibility = View.GONE
        requireActivity().findViewById<View>(R.id.bottomNavContainer).visibility = View.GONE
        etSearch.requestFocus()
        val imm = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideSearchState() {
        isSearchActive = false
        layoutTopBarDefault.visibility = View.VISIBLE
        layoutTopBarSearch.visibility = View.GONE
        layoutSearchTabs.visibility = View.GONE
        mainContent.visibility = View.VISIBLE
        requireActivity().findViewById<View>(R.id.bottomNavContainer).visibility = View.VISIBLE
        etSearch.setText("")
        searchResults.clear()
        postResults.clear()
        searchAdapter.notifyDataSetChanged()
        postSearchAdapter.notifyDataSetChanged()
        rvSearchResults.visibility = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
        val imm = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }
}