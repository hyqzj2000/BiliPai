package com.android.purebilibili.navigation

sealed class ScreenRoutes(val route: String) {
    object Home : ScreenRoutes("home")
    object Search : ScreenRoutes("search")
    object Settings : ScreenRoutes("settings")
    object Login : ScreenRoutes("login")
    object Profile : ScreenRoutes("profile")

    // 🔥 新增路由：历史记录和收藏
    object History : ScreenRoutes("history")
    object Favorite : ScreenRoutes("favorite")
    object WatchLater : ScreenRoutes("watch_later")  // 🔥🔥 [新增] 稍后再看
    object LiveList : ScreenRoutes("live_list")  // 🔥🔥 [新增] 直播列表
    
    // 🔥 关注列表页面
    object Following : ScreenRoutes("following/{mid}") {
        fun createRoute(mid: Long): String {
            return "following/$mid"
        }
    }
    
    // 🔥 离线缓存列表
    object DownloadList : ScreenRoutes("download_list")
    
    // 🔥 动态页面
    object Dynamic : ScreenRoutes("dynamic")
    
    // 🔥🔥 [新增] 竖屏短视频 (故事模式)
    object Story : ScreenRoutes("story")

    // 🔥 开源许可证页面
    object OpenSourceLicenses : ScreenRoutes("open_source_licenses")
    
    // 🔥 二级设置页面
    object AppearanceSettings : ScreenRoutes("appearance_settings")
    object PlaybackSettings : ScreenRoutes("playback_settings")
    object PermissionSettings : ScreenRoutes("permission_settings")  // 🔐 权限管理
    object PluginsSettings : ScreenRoutes("plugins_settings")  // 🔌 插件中心
    object BottomBarSettings : ScreenRoutes("bottom_bar_settings")  // 🔥 底栏管理
    // 🔥🔥 [新增] 更多外观设置子页面
    object ThemeSettings : ScreenRoutes("theme_settings")  // 主题设置
    object IconSettings : ScreenRoutes("icon_settings")  // 图标设置
    object AnimationSettings : ScreenRoutes("animation_settings")  // 动画设置

    object VideoPlayer : ScreenRoutes("video_player/{bvid}?cid={cid}") {
        fun createRoute(bvid: String, cid: Long = 0): String {
            return "video_player/$bvid?cid=$cid"
        }
    }
    
    // 🔥🔥 [新增] UP主空间页面
    object Space : ScreenRoutes("space/{mid}") {
        fun createRoute(mid: Long): String {
            return "space/$mid"
        }
    }
    
    // 🔥🔥 [新增] 直播播放页面
    object Live : ScreenRoutes("live/{roomId}?title={title}&uname={uname}") {
        fun createRoute(roomId: Long, title: String, uname: String): String {
            val encodedTitle = android.net.Uri.encode(title)
            val encodedUname = android.net.Uri.encode(uname)
            return "live/$roomId?title=$encodedTitle&uname=$encodedUname"
        }
    }
    
    // 🔥🔥 [新增] 音频模式页面
    object AudioMode : ScreenRoutes("audio_mode")
    
    // 🔥🔥 [新增] 番剧/影视页面 - 支持初始类型参数
    object Bangumi : ScreenRoutes("bangumi?type={type}") {
        fun createRoute(initialType: Int = 1): String {
            return "bangumi?type=$initialType"
        }
    }
    
    object BangumiDetail : ScreenRoutes("bangumi/{seasonId}") {
        fun createRoute(seasonId: Long): String {
            return "bangumi/$seasonId"
        }
    }
    
    // 🔥🔥 [新增] 番剧播放页面
    object BangumiPlayer : ScreenRoutes("bangumi/play/{seasonId}/{epId}") {
        fun createRoute(seasonId: Long, epId: Long): String {
            return "bangumi/play/$seasonId/$epId"
        }
    }
    
    // 🔥 分区页面
    object Partition : ScreenRoutes("partition")
    
    // 🔥 分类详情页面
    object Category : ScreenRoutes("category/{tid}?name={name}") {
        fun createRoute(tid: Int, name: String): String {
            return "category/$tid?name=${android.net.Uri.encode(name)}"
        }
    }
}