// File: feature/video/ui/section/VideoActionSection.kt
package com.android.purebilibili.feature.video.ui.section

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//  已改用 MaterialTheme.colorScheme.primary
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.data.model.response.ViewInfo
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Video Action Section Components
 * 
 * Contains components for user interaction:
 * - ActionButtonsRow: Like, coin, favorite, triple, comment buttons
 * - BiliActionButton: Bilibili official style button
 * - ActionButton: Enhanced action button with animations
 * 
 * Requirement Reference: AC3.2 - User action components in dedicated file
 */

/**
 * Action Buttons Row (Bilibili official style: icon + number, no circle background)
 */
@Composable
fun ActionButtonsRow(
    info: ViewInfo,
    isFavorited: Boolean = false,
    isLiked: Boolean = false,
    coinCount: Int = 0,
    downloadProgress: Float = -1f,  //  -1 = 未下载, 0-1 = 进度, 1 = 已完成
    isInWatchLater: Boolean = false,  //  稍后再看状态
    onFavoriteClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onCoinClick: () -> Unit = {},
    onTripleClick: () -> Unit = {},
    onCommentClick: () -> Unit,
    onDownloadClick: () -> Unit = {},  //  下载点击
    onWatchLaterClick: () -> Unit = {},  //  稍后再看点击
    onFavoriteLongClick: () -> Unit = {} // [New] 长按收藏
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Like - 支持长按触发三连
        TripleLikeActionButton(
            isLiked = isLiked,
            likeCount = FormatUtils.formatStat(info.stat.like.toLong()),
            coinCount = FormatUtils.formatStat(info.stat.coin.toLong()),
            isFavorited = isFavorited,
            favoriteCount = FormatUtils.formatStat(info.stat.favorite.toLong()),
            hasCoin = coinCount > 0,
            onLikeClick = onLikeClick,
            onTripleComplete = onTripleClick
        )

        // Coin
        BiliActionButton(
            icon = com.android.purebilibili.core.ui.AppIcons.BiliCoin,
            text = FormatUtils.formatStat(info.stat.coin.toLong()),
            isActive = coinCount > 0,
            activeColor = Color(0xFFFFB300),
            onClick = onCoinClick
        )

        // Favorite
        BiliActionButton(
            icon = if (isFavorited) CupertinoIcons.Filled.Bookmark else CupertinoIcons.Default.Bookmark,
            text = FormatUtils.formatStat(info.stat.favorite.toLong()),
            isActive = isFavorited,
            activeColor = Color(0xFFFFC107),
            onClick = onFavoriteClick,
            onLongClick = onFavoriteLongClick
        )
        
        //  稍后再看
        BiliActionButton(
            icon = if (isInWatchLater) CupertinoIcons.Filled.Clock else CupertinoIcons.Default.Clock,
            text = if (isInWatchLater) "已添加" else "稍后看",
            isActive = isInWatchLater,
            activeColor = Color(0xFF9C27B0),  // 紫色
            onClick = onWatchLaterClick
        )
        
        //  Download
        val downloadText = when {
            downloadProgress >= 1f -> "已缓存"
            downloadProgress >= 0f -> "${(downloadProgress * 100).toInt()}%"
            else -> "缓存"
        }
        val isDownloaded = downloadProgress >= 1f
        val isDownloading = downloadProgress in 0f..0.99f
        BiliActionButton(
            icon = if (isDownloaded) CupertinoIcons.Default.Checkmark else CupertinoIcons.Default.ArrowDown,
            text = downloadText,
            isActive = isDownloaded || isDownloading,
            activeColor = if (isDownloaded) Color(0xFF4CAF50) else Color(0xFF2196F3),
            onClick = onDownloadClick
        )

    }
}

/**
 * 一键三连长按按钮 - 长按显示点赞、投币、收藏三个图标的圆形进度条
 */
@Composable
private fun TripleLikeActionButton(
    isLiked: Boolean,
    likeCount: String,
    coinCount: String,
    isFavorited: Boolean,
    favoriteCount: String,
    hasCoin: Boolean,
    onLikeClick: () -> Unit,
    onTripleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    
    // 长按进度状态
    var isLongPressing by remember { mutableStateOf(false) }
    var longPressProgress by remember { mutableFloatStateOf(0f) }
    val progressDuration = 1500 // 1.5 秒
    
    // 进度动画
    val animatedProgress by animateFloatAsState(
        targetValue = if (isLongPressing) 1f else 0f,
        animationSpec = if (isLongPressing) {
            tween(durationMillis = progressDuration, easing = LinearEasing)
        } else {
            tween(durationMillis = 200, easing = FastOutSlowInEasing)
        },
        label = "tripleLikeProgress",
        finishedListener = { progress ->
            if (progress >= 1f && isLongPressing) {
                haptic(HapticType.MEDIUM)
                onTripleComplete()
                isLongPressing = false
            }
        }
    )
    
    LaunchedEffect(animatedProgress) {
        longPressProgress = animatedProgress
    }
    
    LaunchedEffect(isLongPressing) {
        if (isLongPressing) {
            haptic(HapticType.LIGHT)
        }
    }
    
    // 显示三个图标的进度
    Row(
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isLongPressing = true
                        val released = tryAwaitRelease()
                        isLongPressing = false
                        if (released && longPressProgress < 0.1f) {
                            onLikeClick()
                        }
                    }
                )
            }
    ) {
        // 点赞图标
        TripleProgressIcon(
            icon = if (isLiked) CupertinoIcons.Filled.HandThumbsup else CupertinoIcons.Outlined.HandThumbsup,
            text = likeCount,
            progress = longPressProgress,
            progressColor = MaterialTheme.colorScheme.primary,
            isActive = isLiked
        )
        
        // 投币图标 (只在长按时显示)
        androidx.compose.animation.AnimatedVisibility(
            visible = longPressProgress > 0.05f,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
        ) {
            TripleProgressIcon(
                icon = com.android.purebilibili.core.ui.AppIcons.BiliCoin,
                text = coinCount,
                progress = longPressProgress,
                progressColor = Color(0xFFFFB300),
                isActive = hasCoin
            )
        }
        
        // 收藏图标 (只在长按时显示)
        androidx.compose.animation.AnimatedVisibility(
            visible = longPressProgress > 0.05f,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
        ) {
            TripleProgressIcon(
                icon = if (isFavorited) CupertinoIcons.Filled.Bookmark else CupertinoIcons.Default.Bookmark,
                text = favoriteCount,
                progress = longPressProgress,
                progressColor = Color(0xFFFFC107),
                isActive = isFavorited
            )
        }
    }
}

/**
 * 带圆形进度环的图标
 */
@Composable
private fun TripleProgressIcon(
    icon: ImageVector,
    text: String,
    progress: Float,
    progressColor: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val iconSize = 24.dp
    val ringSize = iconSize + 12.dp
    val strokeWidth = 2.5.dp
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center
        ) {
            // 进度环
            if (progress > 0f) {
                Canvas(modifier = Modifier.size(ringSize)) {
                    val stroke = strokeWidth.toPx()
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                    
                    // 背景环
                    drawArc(
                        color = progressColor.copy(alpha = 0.2f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    
                    // 进度环
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            
            // 图标
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) progressColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(iconSize)
            )
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = if (isActive) progressColor else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/**
 * Bilibili Official Style Action Button - icon + number, no circle background
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BiliActionButton(
    icon: ImageVector,
    text: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null // [New] Long click support
) {
    // Press animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f, // 略微减小缩放感
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )
    
    // Active state pulse animation
    var shouldPulse by remember { mutableStateOf(false) }
    val pulseScale by animateFloatAsState(
        targetValue = if (shouldPulse) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = 0.4f,
            stiffness = 400f
        ),
        label = "pulseScale",
        finishedListener = { shouldPulse = false }
    )
    
    LaunchedEffect(isActive) {
        if (isActive) shouldPulse = true
    }
    
    val contentColor = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale * pulseScale
                scaleY = scale * pulseScale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = contentColor,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/**
 * Enhanced Action Button - with press animation and colored icon
 */
@Composable
fun ActionButton(
    icon: ImageVector,
    text: String,
    isActive: Boolean = false,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconSize: Dp = 24.dp,
    onClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    
    // Press animation state
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pressScale"
    )
    
    // Heartbeat pulse animation - triggered when isActive becomes true
    var shouldPulse by remember { mutableStateOf(false) }
    val pulseScale by animateFloatAsState(
        targetValue = if (shouldPulse) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = 0.35f,
            stiffness = 300f
        ),
        label = "pulseScale",
        finishedListener = { shouldPulse = false }
    )
    
    // Listen for isActive changes
    LaunchedEffect(isActive) {
        if (isActive) {
            shouldPulse = true
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(vertical = 2.dp)
            .width(56.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        // Icon container - uses colored background, higher alpha in dark mode
        Box(
            modifier = Modifier
                .size(38.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .clip(CircleShape)
                .background(iconColor.copy(alpha = if (isDark) 0.15f else 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(iconSize)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
            maxLines = 1
        )
    }
}
