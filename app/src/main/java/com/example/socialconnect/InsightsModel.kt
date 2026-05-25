package com.example.socialconnect
// ─────────────────────────────────────────────
// Data models for Account Insights
// ─────────────────────────────────────────────

data class InsightsModel(
    val profileName: String = "",
    val profileHandle: String = "",
    val joinDate: String = "",
    val profileInitials: String = "",
    val profileImageBase64: String = "",

    val followers: Int = 0,
    val following: Int = 0,
    val posts: Int = 0,

    val healthScore: Int = 0,

    val likesReceived: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0,
    val saves: Int = 0,

    val avgReachPerPost: Int = 0,
    val avgImpressions: Int = 0,
    val engagementRatePct: Float = 0f,
    val topPostLikes: Int = 0,
    val profileVisits: Int = 0,

    val heatmapValues: List<Int> = emptyList(),
    val regions: List<RegionData> = emptyList(),

    val deviceMobilePct: Int = 0,
    val deviceDesktopPct: Int = 0,
    val deviceTabletPct: Int = 0,

    val postBreakdown: List<PostTypeData> = emptyList()
)

data class RegionData(
    @field:JvmField val flag: String = "",
    @field:JvmField val name: String = "",
    @field:JvmField val percentage: Int = 0
)

data class PostTypeData(
    @field:JvmField val label: String = "",
    @field:JvmField val percentage: Int = 0
)