package com.android.purebilibili.core.util

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationUiPolicyTest {

    @Test
    fun compactScreenNeverUsesSidebarEvenWhenEnabled() {
        val windowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Compact,
            heightSizeClass = WindowHeightSizeClass.Medium,
            widthDp = 480.dp,
            heightDp = 900.dp
        )

        assertFalse(shouldUseSidebarNavigationForLayout(windowSizeClass, tabletUseSidebar = true))
    }

    @Test
    fun mediumTabletUsesSidebarWhenEnabled() {
        val windowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Medium,
            heightSizeClass = WindowHeightSizeClass.Medium,
            widthDp = 700.dp,
            heightDp = 1000.dp
        )

        assertTrue(shouldUseSidebarNavigationForLayout(windowSizeClass, tabletUseSidebar = true))
    }

    @Test
    fun tabletStillUsesBottomBarWhenSidebarDisabled() {
        val windowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Expanded,
            heightSizeClass = WindowHeightSizeClass.Medium,
            widthDp = 1024.dp,
            heightDp = 768.dp
        )

        assertFalse(shouldUseSidebarNavigationForLayout(windowSizeClass, tabletUseSidebar = false))
    }

    @Test
    fun homeDrawerDisabledWhenSidebarNavigationEnabled() {
        assertFalse(shouldEnableHomeDrawer(useSideNavigation = true))
    }

    @Test
    fun homeDrawerEnabledWhenUsingBottomNavigation() {
        assertTrue(shouldEnableHomeDrawer(useSideNavigation = false))
    }

    @Test
    fun videoDetailPredictiveBackEnabledInNormalPortraitState() {
        assertTrue(
            shouldEnableVideoDetailPredictiveBack(
                isFullscreenMode = false,
                isPortraitFullscreen = false,
                isPhoneInLandscapeSplitView = false,
                hasBlockingOverlay = false
            )
        )
    }

    @Test
    fun videoDetailPredictiveBackDisabledWhenFullscreen() {
        assertFalse(
            shouldEnableVideoDetailPredictiveBack(
                isFullscreenMode = true,
                isPortraitFullscreen = false,
                isPhoneInLandscapeSplitView = false,
                hasBlockingOverlay = false
            )
        )
    }

    @Test
    fun videoDetailPredictiveBackDisabledWhenBlockingOverlayVisible() {
        assertFalse(
            shouldEnableVideoDetailPredictiveBack(
                isFullscreenMode = false,
                isPortraitFullscreen = false,
                isPhoneInLandscapeSplitView = false,
                hasBlockingOverlay = true
            )
        )
    }

    @Test
    fun predictiveBackCommitsOnlyWhenGestureNotCancelled() {
        assertTrue(shouldCommitPredictiveBackGesture(cancelled = false))
        assertFalse(shouldCommitPredictiveBackGesture(cancelled = true))
    }
}
