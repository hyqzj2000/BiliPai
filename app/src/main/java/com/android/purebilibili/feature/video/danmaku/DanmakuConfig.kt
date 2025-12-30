// 文件路径: feature/video/danmaku/DanmakuConfig.kt
package com.android.purebilibili.feature.video.danmaku

import android.content.Context
import com.bytedance.danmaku.render.engine.control.DanmakuConfig as EngineConfig

/**
 * 弹幕配置管理
 * 
 * 管理弹幕的样式、速度、透明度等设置
 * 适配 ByteDance DanmakuRenderEngine
 */
class DanmakuConfig {
    
    // 弹幕开关
    var isEnabled = true
    
    // 透明度 (0.0 - 1.0)
    var opacity = 0.85f
    
    // 字体缩放 (0.5 - 2.0)
    var fontScale = 1.0f
    
    // 滚动速度因子 (数值越大弹幕越慢)
    var speedFactor = 1.0f
    
    // 显示区域比例 (0.25, 0.5, 0.75, 1.0)
    var displayAreaRatio = 0.5f
    
    // 顶部边距（像素）
    var topMarginPx = 0
    
    /**
     * 应用配置到 DanmakuRenderEngine 的 DanmakuConfig
     * 
     * DanmakuRenderEngine 的配置结构:
     * - config.text: TextConfig (size, color, strokeWidth, strokeColor)
     * - config.scroll: ScrollLayerConfig (moveTime, lineHeight, lineMargin, margin)
     * - config.common: CommonConfig (alpha, bufferSize, bufferDiscardRule)
     */
    fun applyTo(engineConfig: EngineConfig) {
        engineConfig.apply {
            // 通用配置 - 透明度 (0-255 Int)
            common.alpha = (opacity * 255).toInt()
            
            // 文字配置 - 字体大小 (增大基准值以提高可见性)
            text.size = 42f * fontScale
            
            // 滚动层配置
            // moveTime: 弹幕滚过屏幕的时间（毫秒），越大越慢
            // speedFactor > 1 表示更慢（更长的 moveTime）
            // 基准值 5000ms，speedFactor=1 时 5000ms，speedFactor=2 时 10000ms
            val baseTime = 5000L
            scroll.moveTime = (baseTime * speedFactor).toLong().coerceIn(2000L, 10000L)
            
            // 🔥🔥 [修复] 显示区域控制
            // 通过 lineCount 限制最大行数来实现显示区域控制
            val maxLines = getMaxLines()
            scroll.lineCount = maxLines
            
            android.util.Log.w("DanmakuConfig", "📋 Applied: opacity=$opacity, fontSize=${text.size}, moveTime=${scroll.moveTime}ms, displayArea=$displayAreaRatio, maxLines=$maxLines")
        }
    }
    
    /**
     * 根据显示区域比例计算最大行数
     * 🔥🔥 [修复] 不能返回 Int.MAX_VALUE，否则弹幕引擎会尝试为海量行分配内存导致 OOM
     */
    private fun getMaxLines(): Int = when {
        displayAreaRatio <= 0.25f -> 3
        displayAreaRatio <= 0.5f -> 5
        displayAreaRatio <= 0.75f -> 8
        else -> 12  // 🔥🔥 [修复] 最大 12 行，不能用 Int.MAX_VALUE
    }
    
    companion object {
        /**
         * 获取状态栏高度（像素）
         */
        fun getStatusBarHeight(context: Context): Int {
            val resourceId = context.resources.getIdentifier(
                "status_bar_height", "dimen", "android"
            )
            return if (resourceId > 0) {
                context.resources.getDimensionPixelSize(resourceId)
            } else {
                (24 * context.resources.displayMetrics.density).toInt()
            }
        }
    }
}
