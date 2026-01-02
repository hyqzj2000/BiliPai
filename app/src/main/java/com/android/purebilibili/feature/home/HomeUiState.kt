// 文件路径: feature/home/HomeUiState.kt
package com.android.purebilibili.feature.home

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.model.response.LiveRoom

/**
 * 用户状态
 *  性能优化：@Immutable 告诉 Compose 此类不可变，减少不必要的重组
 */
@Immutable
data class UserState(
    val isLogin: Boolean = false,
    val face: String = "",
    val name: String = "",
    val mid: Long = 0,
    val level: Int = 0,
    val coin: Double = 0.0,
    val bcoin: Double = 0.0,
    val following: Int = 0,
    val follower: Int = 0,
    val dynamic: Int = 0,
    val isVip: Boolean = false,
    val vipLabel: String = ""
)

/**
 * 首页分类枚举（含 Bilibili 分区 ID）
 */
enum class HomeCategory(val label: String, val tid: Int = 0) {
    RECOMMEND("推荐", 0),
    FOLLOW("关注", 0),    //  关注动态
    POPULAR("热门", 0),
    LIVE("直播", 0),
    ANIME("追番", 13),     // 番剧分区
    MOVIE("影视", 181),    // 影视分区
    GAME("游戏", 4),       // 游戏分区
    KNOWLEDGE("知识", 36), // 知识分区
    TECH("科技", 188)      // 科技分区
}

/**
 * 直播子分类
 */
enum class LiveSubCategory(val label: String) {
    FOLLOWED("关注"),
    POPULAR("热门")
}

/**
 * 首页 UI 状态
 *  性能优化：@Stable 告诉 Compose 此类字段变化可被追踪，优化重组
 */
@Stable
data class HomeUiState(
    val videos: List<VideoItem> = emptyList(),
    val liveRooms: List<LiveRoom> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: UserState = UserState(),
    val currentCategory: HomeCategory = HomeCategory.RECOMMEND,
    val liveSubCategory: LiveSubCategory = LiveSubCategory.FOLLOWED,
    val refreshKey: Long = 0L,
    val followingMids: Set<Long> = emptySet(),
    //  [新增] 标签页显示索引（独立于内容分类，用于特殊分类导航后保持标签位置）
    val displayedTabIndex: Int = 0,
    //  [彩蛋] 刷新成功后的趣味消息
    val refreshMessage: String? = null,
    //  [新增] 正在消散动画中的视频 BVIDs（动画完成后移除）
    val dissolvingVideos: Set<String> = emptySet()
)
