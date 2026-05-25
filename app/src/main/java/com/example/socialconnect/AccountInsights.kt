package com.example.socialconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AccountInsights : AppCompatActivity() {

    // ── Repository ────────────────────────────────────────────────────────────
    private val insightUtl = InsightUtl

    // ── View references (no ViewBinding) ─────────────────────────────────────
    private lateinit var btnBack: ImageView
    private lateinit var tvPeriodChip: TextView

    // Hero
    private lateinit var tvHeroName: TextView
    private lateinit var tvHeroHandle: TextView
    private lateinit var tvHeroInitials: TextView
    private lateinit var imgHeroProfilePic: de.hdodenhof.circleimageview.CircleImageView
    private lateinit var tvHealthScore: TextView

    // Stat boxes (include views)
    private lateinit var statFollowers: View
    private lateinit var statFollowing: View
    private lateinit var statPosts: View

    // Engagement cards (include views)
    private lateinit var engLikes: View
    private lateinit var engComments: View
    private lateinit var engShares: View
    private lateinit var engSaves: View

    // Performance rows (include views)
    private lateinit var perfReach: View
    private lateinit var perfImpressions: View
    private lateinit var perfEngRate: View
    private lateinit var perfTopPost: View
    private lateinit var perfProfileVisits: View

    // Heatmap
    private lateinit var heatmapGrid: HeatmapGridView

    // Audience
    private lateinit var containerRegions: LinearLayout

    // Device chips (include views)
    private lateinit var chipMobile: View
    private lateinit var chipDesktop: View
    private lateinit var chipTablet: View

    // Post breakdown
    private lateinit var containerPostBreakdown: LinearLayout

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_insights)

        initViews()
        setupTopBar()

        insightUtl.initInsights { success, _ ->
            if (success) {
                insightUtl.syncInsights { synced, error ->
                    if (synced) {
                        insightUtl.listenToInsights(
                            onSuccess = { data -> populateData(data) },
                            onError   = { message ->
                                android.util.Log.e("AccountInsights", "Error: $message")
                            }
                        )
                    } else {
                        android.util.Log.e("AccountInsights", "Sync failed: $error")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        insightUtl.stopListening()
    }

    // ── Init all views via findViewById ───────────────────────────────────────
    private fun initViews() {
        btnBack       = findViewById(R.id.btnBack)
        tvPeriodChip  = findViewById(R.id.tvPeriodChip)

        tvHeroName        = findViewById(R.id.tvHeroName)
        tvHeroHandle      = findViewById(R.id.tvHeroHandle)
        tvHeroInitials    = findViewById(R.id.tvHeroInitials)
        imgHeroProfilePic = findViewById(R.id.imgHeroProfilePic)
        tvHealthScore     = findViewById(R.id.tvHealthScore)

        statFollowers = findViewById(R.id.statFollowers)
        statFollowing = findViewById(R.id.statFollowing)
        statPosts     = findViewById(R.id.statPosts)

        engLikes    = findViewById(R.id.engLikes)
        engComments = findViewById(R.id.engComments)
        engShares   = findViewById(R.id.engShares)
        engSaves    = findViewById(R.id.engSaves)

        perfReach         = findViewById(R.id.perfReach)
        perfImpressions   = findViewById(R.id.perfImpressions)
        perfEngRate       = findViewById(R.id.perfEngRate)
        perfTopPost       = findViewById(R.id.perfTopPost)
        perfProfileVisits = findViewById(R.id.perfProfileVisits)

        heatmapGrid = findViewById(R.id.heatmapGrid)

        containerRegions = findViewById(R.id.containerRegions)

        chipMobile  = findViewById(R.id.chipMobile)
        chipDesktop = findViewById(R.id.chipDesktop)
        chipTablet  = findViewById(R.id.chipTablet)

        containerPostBreakdown = findViewById(R.id.containerPostBreakdown)
    }

    // ── Top bar ───────────────────────────────────────────────────────────────
    private fun setupTopBar() {
        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        tvPeriodChip.setOnClickListener {
            // TODO: open a BottomSheetDialog with period options (7 days / 30 days / 90 days)
        }
    }

    // ── Master populate ───────────────────────────────────────────────────────
    private fun populateData(data: InsightsModel) {
        populateHero(data)
        populateEngagement(data)
        populatePerformance(data)
        populateHeatmap(data.heatmapValues)
        populateRegions(data)
        populateDevices(data)
        populatePostBreakdown(data.postBreakdown)
    }

    // ── Hero section ──────────────────────────────────────────────────────────
    private fun populateHero(data: InsightsModel) {
        tvHeroName.text     = data.profileName
        tvHeroHandle.text   = "@${data.profileHandle} · Joined ${data.joinDate}"
        tvHeroInitials.text = data.profileInitials
        tvHealthScore.text  = data.healthScore.toString()

        if (data.profileImageBase64.isNotEmpty()) {
            ImageUtils.loadBase64(
                base64      = data.profileImageBase64,
                imageView   = imgHeroProfilePic,
                placeholder = getDrawable(R.drawable.ic_avatar)
            )
            imgHeroProfilePic.visibility = View.VISIBLE
            tvHeroInitials.visibility    = View.GONE
        } else {
            imgHeroProfilePic.visibility = View.GONE
            tvHeroInitials.visibility    = View.VISIBLE
        }

        populateStatBox(statFollowers, formatCount(data.followers), "Followers")
        populateStatBox(statFollowing, formatCount(data.following), "Following")
        populateStatBox(statPosts,     formatCount(data.posts),     "Posts")
    }

    private fun populateStatBox(view: View, number: String, label: String) {
        view.findViewById<TextView>(R.id.tvStatValue).text = number
        view.findViewById<TextView>(R.id.tvStatLabel).text  = label
    }

    // ── Engagement section ────────────────────────────────────────────────────
    private fun populateEngagement(data: InsightsModel) {
        populateEngCard(engLikes,    formatCount(data.likesReceived), "Likes",    R.drawable.ic_heart_filled)
        populateEngCard(engComments, formatCount(data.comments),      "Comments", R.drawable.ic_comment)
        populateEngCard(engShares,   formatCount(data.shares),        "Shares",   R.drawable.ic_share)
        populateEngCard(engSaves,    formatCount(data.saves),         "Saves",    R.drawable.ic_bookmark_filled)
    }

    private fun populateEngCard(view: View, value: String, label: String, iconRes: Int) {
        view.findViewById<TextView>(R.id.tvEngValue).text = value
        view.findViewById<TextView>(R.id.tvEngLabel).text = label
        view.findViewById<ImageView>(R.id.ivEngIcon).setImageResource(iconRes)
    }

    // ── Content performance ────────────────────────────────────────────────────
    private fun populatePerformance(data: InsightsModel) {
        populatePerfRow(perfReach,         "Avg. reach per post", formatCount(data.avgReachPerPost))
        populatePerfRow(perfImpressions,   "Avg. impressions",    formatCount(data.avgImpressions))
        populatePerfRow(perfEngRate,       "Engagement rate",     "%.1f%%".format(data.engagementRatePct))
        populatePerfRow(perfTopPost,       "Top post likes",      formatCount(data.topPostLikes))
        populatePerfRow(perfProfileVisits, "Profile visits",      formatCount(data.profileVisits))
    }

    private fun populatePerfRow(view: View, name: String, value: String) {
        view.findViewById<TextView>(R.id.tvPerfName).text  = name
        view.findViewById<TextView>(R.id.tvPerfValue).text = value
    }

    // ── Heatmap ───────────────────────────────────────────────────────────────
    private fun populateHeatmap(values: List<Int>) {
        heatmapGrid.setData(values)
    }

    // ── Audience regions ──────────────────────────────────────────────────────
    private fun populateRegions(data: InsightsModel) {
        val inflater = LayoutInflater.from(this)
        containerRegions.removeAllViews()

        data.regions.forEachIndexed { index, region ->
            val row = inflater.inflate(R.layout.item_region_row, containerRegions, false)

            row.findViewById<TextView>(R.id.tvRegionFlag).text = region.flag
            row.findViewById<TextView>(R.id.tvRegionName).text = region.name
            row.findViewById<TextView>(R.id.tvRegionPct).text  = "${region.percentage}%"

            val bar = row.findViewById<View>(R.id.viewRegionBar)
            bar.post {
                val track   = bar.parent as FrameLayout
                val targetW = (track.width * region.percentage / 100f).toInt()
                bar.layoutParams = bar.layoutParams.also { it.width = targetW }
                bar.requestLayout()
            }

            if (index == data.regions.lastIndex) row.setPadding(0, 0, 0, 0)

            containerRegions.addView(row)
        }
    }

    // ── Device chips ──────────────────────────────────────────────────────────
    private fun populateDevices(data: InsightsModel) {
        populateDeviceChip(chipMobile,  "${data.deviceMobilePct}%",  "Mobile")
        populateDeviceChip(chipDesktop, "${data.deviceDesktopPct}%", "Desktop")
        populateDeviceChip(chipTablet,  "${data.deviceTabletPct}%",  "Tablet")
    }

    private fun populateDeviceChip(view: View, pct: String, label: String) {
        view.findViewById<TextView>(R.id.tvDevicePct).text   = pct
        view.findViewById<TextView>(R.id.tvDeviceLabel).text = label
    }

    // ── Post breakdown bars ────────────────────────────────────────────────────
    private fun populatePostBreakdown(breakdown: List<PostTypeData>) {
        val inflater = LayoutInflater.from(this)
        containerPostBreakdown.removeAllViews()

        breakdown.forEachIndexed { index, item ->
            val row = inflater.inflate(R.layout.item_breakdown_bar, containerPostBreakdown, false)

            row.findViewById<TextView>(R.id.tvBarLabel).text = item.label
            row.findViewById<TextView>(R.id.tvBarValue).text = "${item.percentage}%"

            val bar = row.findViewById<View>(R.id.viewBar)
            bar.post {
                val track   = bar.parent as FrameLayout
                val targetW = (track.width * item.percentage / 100f).toInt()
                bar.layoutParams = bar.layoutParams.also { it.width = targetW }
                bar.requestLayout()
            }

            if (index == breakdown.lastIndex) row.setPadding(0, 0, 0, 0)

            containerPostBreakdown.addView(row)
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private fun formatCount(n: Int): String = when {
        n >= 1_000_000 -> "%.1fM".format(n / 1_000_000f)
        n >= 1_000     -> "%.1fK".format(n / 1_000f)
        else           -> n.toString()
    }

    private fun formatDelta(delta: Int): String {
        val arrow = if (delta >= 0) "▲" else "▼"
        val sign  = if (delta >= 0) "+" else ""
        return "$arrow $sign$delta this week"
    }
}