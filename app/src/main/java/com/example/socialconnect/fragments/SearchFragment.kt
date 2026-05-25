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
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialconnect.*
import com.example.socialconnect.adapters.PostAdapter
import com.example.socialconnect.adapters.RecentSearchAdapter
import com.example.socialconnect.adapters.SearchAdapter

class SearchFragment : Fragment() {

    private lateinit var rvSearchResults: RecyclerView
    private lateinit var rvRecentSearches: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var layoutSearchTabs: LinearLayout
    private lateinit var tabPeople: LinearLayout
    private lateinit var tabPosts: LinearLayout
    private lateinit var indicatorPeople: View
    private lateinit var indicatorPosts: View
    private lateinit var tvPeopleTab: TextView
    private lateinit var tvPostsTab: TextView
    private lateinit var btnBackSearchfrg: ImageView
    private lateinit var btnClearSearch: ImageView
    private lateinit var mainContent: View
    private lateinit var layoutEmptyState: View
    private lateinit var ivEmptyIcon: ImageView
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var layoutRecentSearches: View
    private lateinit var tvClearAll: TextView

    private lateinit var searchAdapter: SearchAdapter
    private lateinit var postSearchAdapter: PostAdapter
    private lateinit var recentSearchAdapter: RecentSearchAdapter
    private lateinit var postHandler: PostInteractionHandler

    private val searchResults = mutableListOf<UserModel>()
    private val postResults = mutableListOf<PostModel>()

    private val peopleCache = mutableListOf<UserModel>()
    private var lastPeopleQuery = ""
    private val postsCache = mutableListOf<PostModel>()
    private var lastPostsQuery = ""

    private val recentSearches = mutableListOf<String>()
    private val MAX_RECENT = 8

    private var isPeopleTabActive = true
    private lateinit var searchProgressBar: android.widget.ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupAdapters()
        postHandler = PostInteractionHandler(this, mutableListOf(), postSearchAdapter, mutableSetOf())
        setupClickListeners()
        setupSearchInput()
        setupBackPress()
        loadRecentSearches()
        openKeyboardImmediately()
    }

    private fun initViews(view: View) {
        rvSearchResults   = view.findViewById(R.id.rvSearchResults)
        rvRecentSearches  = view.findViewById(R.id.rvRecentSearches)
        etSearch          = view.findViewById(R.id.etSearch)
        layoutSearchTabs  = view.findViewById(R.id.layoutSearchTabs)
        tabPeople         = view.findViewById(R.id.tabPeople)
        tabPosts          = view.findViewById(R.id.tabPosts)
        indicatorPeople   = view.findViewById(R.id.indicatorPeople)
        indicatorPosts    = view.findViewById(R.id.indicatorPosts)
        tvPeopleTab       = tabPeople.getChildAt(0) as TextView
        tvPostsTab        = tabPosts.getChildAt(0) as TextView
        btnBackSearchfrg  = view.findViewById(R.id.btnBackSearchfrg)
        btnClearSearch    = view.findViewById(R.id.btnClearSearch)
        mainContent       = view.findViewById(R.id.mainContent)
        layoutEmptyState  = view.findViewById(R.id.layoutEmptyState)
        ivEmptyIcon       = view.findViewById(R.id.ivEmptyIcon)
        tvEmptyTitle      = view.findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle   = view.findViewById(R.id.tvEmptySubtitle)
        layoutRecentSearches = view.findViewById(R.id.layoutRecentSearches)
        tvClearAll        = view.findViewById(R.id.tvClearAll)
        searchProgressBar = view.findViewById(R.id.searchProgressBar)
    }

    // Keyboard opens + EditText focused as soon as fragment appears
    private fun openKeyboardImmediately() {
        etSearch.requestFocus()
        etSearch.postDelayed({
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    private fun setupAdapters() {
        searchAdapter = SearchAdapter(
            users = searchResults,
            onUserClick = { user ->
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) saveRecentSearch(query)
                navigateToUserProfile(user)
            },
            onFollowClick = { user, position -> handleFollow(user, position) }
        )

        postSearchAdapter = PostAdapter(
            posts = postResults,
            onLikeClick = { _, _ -> },
            onCommentClick = { },
            onBookmarkClick = { _, _ -> },
            onShareClick = { },
            onAvatarClick = { post -> navigateToUserProfile(post) },
            onDeleteClick = { }
        )

        recentSearchAdapter = RecentSearchAdapter(
            searches = recentSearches,
            onItemClick = { query ->
                etSearch.setText(query)
                etSearch.setSelection(query.length)
            },
            onRemoveClick = { query ->
                recentSearches.remove(query)
                recentSearchAdapter.notifyDataSetChanged()
                saveRecentSearchesToPrefs()
                updateRecentVisibility()
            }
        )

        rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        rvSearchResults.adapter = searchAdapter

        rvRecentSearches.layoutManager = LinearLayoutManager(requireContext())
        rvRecentSearches.adapter = recentSearchAdapter
    }

    private fun setupClickListeners() {
        btnBackSearchfrg.setOnClickListener {
            hideKeyboard()
            parentFragmentManager.popBackStack()
        }
        btnClearSearch.setOnClickListener {
            etSearch.setText("")
        }
        tabPeople.setOnClickListener { switchTab(true) }
        tabPosts.setOnClickListener { switchTab(false) }
        tvClearAll.setOnClickListener {
            val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Clear Recent Searches")
                .setMessage("Are you sure you want to clear all recent searches?")
                .setPositiveButton("Confirm") { _, _ ->
                    recentSearches.clear()
                    recentSearchAdapter.notifyDataSetChanged()
                    saveRecentSearchesToPrefs()
                    updateRecentVisibility()
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.setOnShowListener {
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(requireContext().getColor(R.color.teal_green))
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(requireContext().getColor(R.color.grey_nav))
            }

            dialog.show()
        }
    }

    private fun setupSearchInput() {
        // Pressing search/done on keyboard triggers search
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    saveRecentSearch(query)
                    hideKeyboard()
                    showTabsAndSearch(query)
                }
                true
            } else false
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                btnClearSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE

                if (query.isEmpty()) {
                    // Back to recent list, hide tabs and results
                    layoutSearchTabs.visibility = View.GONE
                    rvSearchResults.visibility = View.GONE
                    layoutEmptyState.visibility = View.GONE
                    mainContent.visibility = View.GONE
                    searchResults.clear()
                    postResults.clear()
                    searchAdapter.notifyDataSetChanged()
                    postSearchAdapter.notifyDataSetChanged()
                    updateRecentVisibility()
                }
                // Don't auto-search while typing — wait for keyboard search button
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Called only when user presses Search on keyboard
    private fun showTabsAndSearch(query: String) {
        layoutSearchTabs.visibility = View.VISIBLE
        layoutRecentSearches.visibility = View.GONE
        mainContent.visibility = View.GONE
        triggerSearch(query)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }

    private fun setupBackPress() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val query = etSearch.text.toString()
                if (query.isNotEmpty()) {
                    etSearch.setText("")
                } else {
                    hideKeyboard()
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun updateRecentVisibility() {
        layoutRecentSearches.visibility =
            if (recentSearches.isNotEmpty() && etSearch.text.isEmpty()) View.VISIBLE
            else View.GONE

        // Show idle state only if no recents
        mainContent.visibility =
            if (recentSearches.isEmpty() && etSearch.text.isEmpty()) View.VISIBLE
            else View.GONE
    }

    private fun triggerSearch(query: String) {
        if (isPeopleTabActive) {
            // Cache hit — data already loaded, skip progress bar
            if (query == lastPeopleQuery && peopleCache.isNotEmpty()) {
                searchResults.clear()
                searchResults.addAll(peopleCache)
                searchAdapter.notifyDataSetChanged()
                toggleResultsState(searchResults.isEmpty(), query)
                return
            }
            // Fresh fetch — show loading
            showLoading()
            FireStoreUtil.searchUsers(query) { users ->
                if (!isAdded) return@searchUsers
                val filtered = users.filter { it.uid != AuthUtil.currentUid }
                fetchFollowStatuses(filtered) { usersWithStatus ->
                    peopleCache.clear(); peopleCache.addAll(usersWithStatus)
                    lastPeopleQuery = query
                    searchResults.clear(); searchResults.addAll(usersWithStatus)
                    hideLoading()
                    searchAdapter.notifyDataSetChanged()
                    toggleResultsState(usersWithStatus.isEmpty(), query)
                }
            }
        } else {
            // Cache hit — skip progress bar
            if (query == lastPostsQuery && postsCache.isNotEmpty()) {
                postResults.clear()
                postResults.addAll(postsCache)
                postSearchAdapter.notifyDataSetChanged()
                toggleResultsState(postResults.isEmpty(), query)
                return
            }
            // Fresh fetch — show loading
            showLoading()
            FireStoreUtil.searchPosts(query) { posts ->
                if (!isAdded) return@searchPosts
                postsCache.clear(); postsCache.addAll(posts)
                lastPostsQuery = query
                postResults.clear(); postResults.addAll(posts)
                hideLoading()
                postSearchAdapter.notifyDataSetChanged()
                toggleResultsState(posts.isEmpty(), query)
            }
        }
    }

    private fun fetchFollowStatuses(users: List<UserModel>, onDone: (List<UserModel>) -> Unit) {
        if (users.isEmpty()) { onDone(emptyList()); return }
        val result = Array<UserModel?>(users.size) { null }
        var completed = 0
        users.forEachIndexed { index, user ->
            FireStoreUtil.isFollowing(user.uid) { isFollowing ->
                result[index] = user.copy(isFollowing = isFollowing)
                if (++completed == users.size) {
                    activity?.runOnUiThread { if (isAdded) onDone(result.filterNotNull()) }
                }
            }
        }
    }

    private fun handleFollow(user: UserModel, position: Int) {
        val current = searchResults.getOrNull(position) ?: return

        // Optimistic UI update
        searchResults[position] = current.copy(isFollowing = !current.isFollowing)
        val cacheIndex = peopleCache.indexOfFirst { it.uid == current.uid }
        if (cacheIndex != -1) peopleCache[cacheIndex] = peopleCache[cacheIndex].copy(isFollowing = !current.isFollowing)
        searchAdapter.notifyItemChanged(position)

        postHandler.handleFollow(user.uid, current.isFollowing) { isNowFollowing ->
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                searchResults[position] = current.copy(isFollowing = isNowFollowing)
                if (cacheIndex != -1) peopleCache[cacheIndex] = peopleCache[cacheIndex].copy(isFollowing = isNowFollowing)
                searchAdapter.notifyItemChanged(position)
            }
        }
    }

    private fun toggleResultsState(isEmpty: Boolean, query: String) {
        if (isEmpty) {
            rvSearchResults.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
            ivEmptyIcon.setImageResource(R.drawable.ic_close)
            tvEmptyTitle.text = "No results for \"$query\""
            tvEmptySubtitle.text = "Try searching for a different name or keyword."
        } else {
            rvSearchResults.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
        }
    }

    private fun switchTab(isPeople: Boolean) {
        if (isPeopleTabActive == isPeople) return
        isPeopleTabActive = isPeople
        indicatorPeople.setBackgroundResource(if (isPeople) R.color.teal_green else android.R.color.transparent)
        indicatorPosts.setBackgroundResource(if (!isPeople) R.color.teal_green else android.R.color.transparent)
        tvPeopleTab.setTextColor(requireContext().getColor(if (isPeople) R.color.teal_green else android.R.color.darker_gray))
        tvPostsTab.setTextColor(requireContext().getColor(if (!isPeople) R.color.teal_green else android.R.color.darker_gray))
        rvSearchResults.adapter = if (isPeople) searchAdapter else postSearchAdapter
        val query = etSearch.text.toString().trim()
        if (query.isNotEmpty()) triggerSearch(query)
    }

    private fun loadRecentSearches() {
        val prefs = requireContext().getSharedPreferences("search_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("recent_searches", emptySet()) ?: emptySet()
        val ordered = saved.mapNotNull { entry ->
            val parts = entry.split("|||")
            if (parts.size == 2) parts[0].toIntOrNull()?.let { it to parts[1] } else null
        }.sortedBy { it.first }.map { it.second }
        recentSearches.clear()
        recentSearches.addAll(ordered)
        recentSearchAdapter.notifyDataSetChanged()
        updateRecentVisibility()
    }

    private fun saveRecentSearch(query: String) {
        if (query.isBlank()) return
        recentSearches.remove(query)
        recentSearches.add(0, query)
        if (recentSearches.size > MAX_RECENT) recentSearches.removeAt(recentSearches.size - 1)
        recentSearchAdapter.notifyDataSetChanged()
        saveRecentSearchesToPrefs()
        updateRecentVisibility()
    }

    private fun saveRecentSearchesToPrefs() {
        val prefs = requireContext().getSharedPreferences("search_prefs", Context.MODE_PRIVATE)
        val toSave = recentSearches.mapIndexed { i, s -> "$i|||$s" }.toSet()
        prefs.edit().putStringSet("recent_searches", toSave).apply()
    }

    private fun navigateToUserProfile(post: PostModel) {
        val activity = requireActivity() as activity_Main
        if (post.userId == AuthUtil.currentUid) activity.openMyProfile()
        else activity.openOtherProfile(post.userId)
    }

    private fun navigateToUserProfile(user: UserModel) {
        val activity = requireActivity() as activity_Main
        if (user.uid == AuthUtil.currentUid) activity.openMyProfile()
        else activity.openOtherProfile(user.uid)
    }

    private fun showLoading() {
        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            searchProgressBar.visibility = View.VISIBLE
            rvSearchResults.visibility = View.GONE
            layoutEmptyState.visibility = View.GONE
        }
    }

    private fun hideLoading() {
        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            searchProgressBar.visibility = View.GONE
        }
    }
}