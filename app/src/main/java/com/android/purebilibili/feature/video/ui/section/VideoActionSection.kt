// File: feature/video/ui/section/VideoActionSection.kt
package com.android.purebilibili.feature.video.ui.section

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.android.purebilibili.data.model.response.ViewInfo

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
    onWatchLaterClick: () -> Unit = {}  //  稍后再看点击
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Like
        BiliActionButton(
            icon = if (isLiked) CupertinoIcons.Filled.Heart else CupertinoIcons.Default.Heart,
            text = FormatUtils.formatStat(info.stat.like.toLong()),
            isActive = isLiked,
            activeColor = MaterialTheme.colorScheme.primary,
            onClick = onLikeClick
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
            onClick = onFavoriteClick
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

        // Triple action
        BiliActionButton(
            icon = CupertinoIcons.Filled.Heart,
            text = "三连",
            isActive = false,
            activeColor = Color(0xFFE91E63),
            onClick = onTripleClick
        )
    }
}

/**
 * Bilibili Official Style Action Button - icon + number, no circle background
 */
@Composable
private fun BiliActionButton(
    icon: ImageVector,
    text: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    // Press animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
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
    
    val iconColor = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale * pulseScale
                scaleY = scale * pulseScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = textColor,
            fontWeight = FontWeight.Normal,
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
