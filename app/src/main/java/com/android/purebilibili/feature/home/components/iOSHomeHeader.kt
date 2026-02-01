// 文件路径: feature/home/components/iOSHomeHeader.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance  //  状态栏亮度计算
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.kyant.backdrop.drawBackdrop 
import com.kyant.backdrop.effects.lens 
import com.android.purebilibili.core.ui.effect.simpMusicLiquidGlass 
import com.kyant.backdrop.backdrops.LayerBackdrop
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.iOSTapEffect
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.feature.home.UserState
import com.android.purebilibili.core.theme.iOSSystemGray
import dev.chrisbanes.haze.HazeState
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.ui.blur.BlurStyles
import com.android.purebilibili.core.ui.blur.BlurIntensity

/**
 *  简洁版首页头部 (带滚动隐藏/显示动画)
 * 
 * 注意：Header 不使用 hazeChild 模糊效果（会导致渲染问题）
 * 磨砂效果仅保留给 BottomBar（在屏幕底部可以正常工作）
 * hazeState 参数保留以保持 API 兼容性
 */
@Composable
fun iOSHomeHeader(
    scrollOffset: Float,
    user: UserState,
    onAvatarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    categoryIndex: Int,
    onCategorySelected: (Int) -> Unit,
    onPartitionClick: () -> Unit = {},  //  新增：分区按钮回调
    isScrollingUp: Boolean = true,
    collapseThreshold: androidx.compose.ui.unit.Dp = 60.dp,
    hazeState: HazeState? = null,  // 保留参数兼容性，但不用于模糊
    onStatusBarDoubleTap: () -> Unit = {},
    //  [新增] 下拉刷新状态
    //  [新增] 下拉刷新状态
    isRefreshing: Boolean = false,
    pullProgress: Float = 0f,  // 0.0 ~ 1.0+ 下拉进度
    pagerState: androidx.compose.foundation.pager.PagerState? = null, // [New] PagerState for sync
    // [New] LayerBackdrop for liquid glass effect
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop? = null,
    homeSettings: com.android.purebilibili.core.store.HomeSettings? = null
) {
    val haptic = rememberHapticFeedback()
    val density = LocalDensity.current

    // 计算滚动进度
    val maxOffsetPx = with(density) { 50.dp.toPx() }
    val scrollProgress = (scrollOffset / maxOffsetPx).coerceIn(0f, 1f)
    
    //  [优化] 下拉刷新时强制展开标签页
    //  防止下拉回弹时的微小滚动偏移以及刷新状态下标签页消失
    val progress = if (pullProgress > 0f || isRefreshing) 0f else scrollProgress
    
    // [Feature] 自动折叠逻辑 (Auto-Collapse)
    // 当向上滚动 (isScrollingUp) 或处于顶部时显示；向下滚动时隐藏
    // 使用 animateDpAsState 实现平滑过渡
    val isHeaderVisible = isScrollingUp || scrollOffset < 100f
    
    // 计算位移: 隐藏时向上移动整个 Header 高度
    // Header 总高度 ≈ 状态栏 + 搜索栏(52dp) + Tab栏(约44dp)
    // 但这里我们只隐藏 Search Bar 部分，保留 Tab 栏？
    // 用户需求 "TopBar上滑自动折叠隐藏"，通常指整个 Header 或者只留 Status Bar
    // 参考 B站/Piliplus，通常是隐藏 Search Bar，Tab 吸顶? 
    // 或者整个都隐藏？
    // 假设隐藏整个 Header 内容区域 (Search + Tabs)，或者只保留 Tabs?
    // 让我们尝试 "整体隐藏" 但保留 Status Bar 占位? 不需要，Content 会滚上来。
    // 为了平滑，我们移动 offsetY。
    
    // 既然 iOS 风格 App Store 是大标题滚上去变成小标题。
    // 这里并没有大标题。
    // 我们实现：向下滚动时，整个 Header 向上滑出屏幕。
    // 向上滚动时，Header 滑入。
    
    // [Feature] Sticky Search Bar Logic
    // Only the Tab Row collapses (slides up & fades/clips)
    
    val tabHeight = 44.dp
    val animatedTabHeight by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isHeaderVisible) tabHeight else 0.dp,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
        label = "tabHeight"
    )
    
    val animatedTabTranslationY by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isHeaderVisible) 0.dp else (-44).dp,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
        label = "tabTranslation"
    )

    // 状态栏高度
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val searchBarHeight = 52.dp
    val totalHeaderTopPadding = statusBarHeight + searchBarHeight
    
    // [Feature] Liquid Glass Logic
    val isGlassEnabled = homeSettings?.isLiquidGlassEnabled == true

    //  读取当前模糊强度以确定背景透明度
    val blurIntensity by SettingsManager.getBlurIntensity(LocalContext.current)
        .collectAsState(initial = BlurIntensity.THIN)
    val backgroundAlpha = BlurStyles.getBackgroundAlpha(blurIntensity) * 0.8f // Slightly more transparent for glass
    
    val isSimpMusic = homeSettings?.liquidGlassStyle == com.android.purebilibili.core.store.LiquidGlassStyle.SIMP_MUSIC
    
    val targetHeaderColor = if (isGlassEnabled) {
         MaterialTheme.colorScheme.surface.copy(alpha = 0.01f) // Almost transparent
    } else {
         MaterialTheme.colorScheme.surface.copy(alpha = if (hazeState != null) backgroundAlpha else 1f)
    }
    
    // [UX优化] 平滑过渡顶部栏背景色 (Smooth Header Color Transition)
    val animatedHeaderColor by animateColorAsState(
        targetValue = targetHeaderColor,
        animationSpec = androidx.compose.animation.core.tween<androidx.compose.ui.graphics.Color>(300),
        label = "headerColor"
    )
    
    // Unified Header Container (Status Bar + Search Bar + Tabs)
    val isSupported = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(10f) // Ensure high z-index for the whole header
            //.offset(y = animatedTranslationY) // [Removed] Whole header translation
             // [Revert] Removed Liquid Glass Effect due to performance issues
             .run {
                  this.then(if (hazeState != null) Modifier.unifiedBlur(hazeState) else Modifier)
                      .background(animatedHeaderColor)
             }
            .padding(bottom = 0.dp) // Reset padding, controlled by spacer
    ) {
        // 1. Status Bar Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarHeight)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            haptic(HapticType.LIGHT)
                            onStatusBarDoubleTap()
                        }
                    )
                }
        )

        // 2. Search Bar + Avatar + Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .iOSTapEffect { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    if (user.isLogin && user.face.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(FormatUtils.fixImageUrl(user.face))
                                .crossfade(true).build(),
                            contentDescription = "用户头像",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("未", fontSize = 11.sp, fontWeight = FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Search Box
            // [优化] 外层容器用于居中，内层容器限制最大宽度 (640dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .clickable { 
                            haptic(HapticType.LIGHT)
                            onSearchClick() 
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            CupertinoIcons.Default.MagnifyingGlass,
                            contentDescription = "搜索",
                            tint = iOSSystemGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // [优化] 响应式字体大小
                        val isTablet = com.android.purebilibili.core.util.LocalWindowSizeClass.current.isTablet
                        Text(
                            text = "搜索视频、UP主...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = if (isTablet) 16.sp else 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = iOSSystemGray,
                            maxLines = 1
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Settings Button
            IconButton(
                onClick = { 
                    haptic(HapticType.LIGHT)
                    onSettingsClick() 
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    CupertinoIcons.Default.Gear,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        // 3. Category Tabs (Merged directly below Search Bar)
        // Adjust padding to make them close
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(-1f) // Slide behind search bar
                .height(animatedTabHeight) // Animate height to pull content up if needed (though we handle content separate)
                // Actually, height animation is critical for the header background to shrink
                .graphicsLayer {
                    translationY = animatedTabTranslationY.toPx()
                }
                .clip(RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)) // Ensure internal clip if needed, mostly container clip
        ) {
            CategoryTabRow(
                selectedIndex = categoryIndex,
                onCategorySelected = onCategorySelected,
                onPartitionClick = onPartitionClick,
                pagerState = pagerState
            )
        }
    }

}
