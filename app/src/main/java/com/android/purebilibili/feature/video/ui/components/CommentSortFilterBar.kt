package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.feature.video.viewmodel.CommentSortMode
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*

/**
 *  评论排序筛选栏
 * 包含：评论数量 | 热度/时间 排序切换 | 只看UP主 切换
 *  排序和"只看UP主"互斥：激活一个时取消另一个的选中状态
 */
@Composable
fun CommentSortFilterBar(
    count: Int,
    sortMode: CommentSortMode,
    upOnlyFilter: Boolean,
    onSortModeChange: (CommentSortMode) -> Unit,
    onUpOnlyToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //  评论标题 + 数量
        Text(
            text = "评论",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = FormatUtils.formatStat(count.toLong()),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        //  排序选项（热度/时间）- 当 upOnlyFilter 激活时不显示选中状态
        SortChipGroup(
            currentMode = sortMode,
            isActive = !upOnlyFilter,  //  互斥：UP筛选激活时，排序按钮不高亮
            onModeChange = onSortModeChange
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        //  只看UP主筛选
        UpOnlyChip(
            isActive = upOnlyFilter,
            onClick = onUpOnlyToggle
        )
    }
}

/**
 * 排序 Chip 组（热度/时间）-  使用边框样式，与"只看UP主"一致
 * @param isActive 当为 false 时，所有按钮显示为未选中状态（互斥效果）
 */
@Composable
private fun SortChipGroup(
    currentMode: CommentSortMode,
    isActive: Boolean = true,
    onModeChange: (CommentSortMode) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CommentSortMode.entries.forEach { mode ->
            //  只有 isActive 为 true 且当前模式匹配时才显示选中状态
            val isSelected = isActive && mode == currentMode
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
                else 
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                animationSpec = tween(200),
                label = "chip_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "chip_text"
            )
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .clickable { onModeChange(mode) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

/**
 * 只看UP主 Chip
 */
@Composable
private fun UpOnlyChip(
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "up_chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "up_chip_text"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActive) 
            MaterialTheme.colorScheme.primary 
        else 
            Color.Transparent,
        animationSpec = tween(200),
        label = "up_chip_border"
    )
    
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isActive) CupertinoIcons.Default.CheckmarkCircle else CupertinoIcons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = textColor
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "只看UP主",
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
            color = textColor
        )
    }
}
