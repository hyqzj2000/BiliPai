package com.android.purebilibili.core.util

/**
 * 统一平板导航模式判定，避免不同页面断点不一致导致底栏/侧栏显示冲突。
 */
internal fun shouldUseSidebarNavigationForLayout(
    windowSizeClass: WindowSizeClass,
    tabletUseSidebar: Boolean
): Boolean {
    return tabletUseSidebar && windowSizeClass.shouldUseSideNavigation
}

/**
 * 首页侧边抽屉仅在底栏导航模式下启用，避免与平板侧栏模式叠层冲突。
 */
internal fun shouldEnableHomeDrawer(useSideNavigation: Boolean): Boolean {
    return !useSideNavigation
}

/**
 * 视频详情页仅在“普通竖屏主态”启用预测返回拦截。
 * 其它状态（全屏/竖屏全屏/手机横屏分栏/阻塞层）交给各自 BackHandler 处理。
 */
internal fun shouldEnableVideoDetailPredictiveBack(
    isFullscreenMode: Boolean,
    isPortraitFullscreen: Boolean,
    isPhoneInLandscapeSplitView: Boolean,
    hasBlockingOverlay: Boolean
): Boolean {
    return !isFullscreenMode &&
        !isPortraitFullscreen &&
        !isPhoneInLandscapeSplitView &&
        !hasBlockingOverlay
}

/**
 * 预测返回手势仅在未被取消时提交返回动作。
 */
internal fun shouldCommitPredictiveBackGesture(cancelled: Boolean): Boolean {
    return !cancelled
}
