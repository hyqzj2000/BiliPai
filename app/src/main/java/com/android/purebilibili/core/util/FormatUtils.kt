package com.android.purebilibili.core.util

object FormatUtils {
    /**
     * 将数字格式化为 B站风格 (例如: 1.2万)
     */
    fun formatStat(count: Long): String {
        return when {
            count >= 100000000 -> String.format("%.1f亿", count / 100000000.0)
            count >= 10000 -> String.format("%.1f万", count / 10000.0)
            else -> count.toString()
        }
    }

    /**
     * 将秒数格式化为 mm:ss
     */
    fun formatDuration(seconds: Int): String {
        val min = seconds / 60
        val sec = seconds % 60
        return String.format("%02d:%02d", min, sec)
    }

    /**
     * 修复图片 URL (核心修复)
     * 1. 补全 https 前缀
     * 2. 自动添加缩放后缀节省流量
     */
    fun fixImageUrl(url: String?): String {
        if (url.isNullOrEmpty()) return "" // 防止空指针

        var newUrl = url

        // 修复无协议头的链接 (//i0.hdslb.com...)
        if (newUrl.startsWith("//")) {
            newUrl = "https:$newUrl"
        }
        // 修复 http 链接
        if (newUrl.startsWith("http://")) {
            newUrl = newUrl.replace("http://", "https://")
        }

        // 如果没有后缀，加上缩放参数 (宽640, 高400)
        if (!newUrl.contains("@")) {
            newUrl = "$newUrl@640w_400h.webp"
        }
        return newUrl
    }

    /**
     * 格式化观看进度
     */
    fun formatProgress(progress: Int, duration: Int): String {
        if (duration <= 0) return "已看"
        if (progress == -1) return "已看" // finish
        if (progress == 0) return "未观看"
        val percent = (progress.toFloat() / duration.toFloat() * 100).toInt()
        return if (percent >= 99) "已看完" else "已看$percent%"
    }
    
    /**
     *  格式化发布时间 (相对时间 + 日期)
     * 例如: "3小时前" / "昨天" / "2024-01-15"
     */
    fun formatPublishTime(timestampSeconds: Long): String {
        if (timestampSeconds <= 0) return ""
        
        val now = System.currentTimeMillis()
        val pubTime = timestampSeconds * 1000L
        val diff = now - pubTime
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            seconds < 60 -> "刚刚"
            minutes < 60 -> "${minutes}分钟前"
            hours < 24 -> "${hours}小时前"
            days == 1L -> "昨天"
            days < 7 -> "${days}天前"
            else -> {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                sdf.format(java.util.Date(pubTime))
            }
        }
    }
}