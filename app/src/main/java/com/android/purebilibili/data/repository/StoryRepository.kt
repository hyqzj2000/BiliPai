// 文件路径: data/repository/StoryRepository.kt
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.StoryItem
import com.android.purebilibili.core.util.Logger

/**
 * 故事模式 (竖屏短视频) 数据仓库
 */
object StoryRepository {
    
    private const val TAG = "StoryRepository"
    
    /**
     * 获取故事流视频列表
     * @param aid 可选，从指定视频开始加载
     * @param pageSize 每页数量
     */
    suspend fun getStoryFeed(
        aid: Long = 0,
        bvid: String = "",
        pageSize: Int = 20
    ): Result<List<StoryItem>> {
        return try {
            val response = NetworkModule.storyApi.getStoryFeed(
                ps = pageSize,
                aid = aid,
                bvid = bvid
            )
            
            if (response.code == 0 && response.data != null) {
                val items = response.data.items ?: emptyList()
                Logger.d(TAG, " 获取故事流成功: ${items.size} 条视频")
                Result.success(items)
            } else {
                Logger.e(TAG, " 获取故事流失败: code=${response.code}, msg=${response.message}")
                Result.failure(Exception("获取失败: ${response.message}"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, " 获取故事流异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取视频播放 URL (通过 bvid)
     * @param bvid 视频 BV 号
     * @param cid 视频 CID
     */
    suspend fun getVideoPlayUrl(bvid: String, cid: Long): String? {
        if (bvid.isEmpty()) {
            Logger.e(TAG, " bvid 为空，无法获取播放 URL")
            return null
        }
        return try {
            // 复用 VideoRepository 的播放 URL 获取逻辑
            val playData = VideoRepository.getPlayUrlData(bvid, cid, 80)
            extractPlayUrl(playData)
        } catch (e: Exception) {
            Logger.e(TAG, " 获取播放 URL 异常", e)
            null
        }
    }
    
    /**
     * 获取视频播放 URL (通过 aid) - 用于 Story 模式
     * @param aid 视频 AV 号
     * @param cid 视频 CID
     */
    suspend fun getVideoPlayUrlByAid(aid: Long, cid: Long): String? {
        if (aid <= 0 || cid <= 0) {
            Logger.e(TAG, " aid=$aid 或 cid=$cid 无效")
            return null
        }
        return try {
            Logger.d(TAG, " 获取播放 URL: aid=$aid, cid=$cid")
            
            // 使用 Legacy API 通过 aid 获取播放地址
            val response = NetworkModule.api.getPlayUrlByAid(aid = aid, cid = cid)
            
            if (response.code == 0 && response.data != null) {
                val url = extractPlayUrl(response.data)
                if (url != null) {
                    Logger.d(TAG, " 获取播放 URL 成功: ${url.take(50)}...")
                    return url
                }
            }
            
            Logger.e(TAG, " 获取播放 URL 失败: code=${response.code}")
            null
        } catch (e: Exception) {
            Logger.e(TAG, " 获取播放 URL 异常", e)
            null
        }
    }
    
    /**
     * 从 PlayUrlData 中提取播放地址
     */
    private fun extractPlayUrl(playData: com.android.purebilibili.data.model.response.PlayUrlData?): String? {
        if (playData == null) return null
        
        // 优先取 durl (MP4) - 对短视频更友好
        val durlUrl = playData.durl?.firstOrNull()?.url
        if (!durlUrl.isNullOrEmpty()) {
            Logger.d(TAG, " durl URL: ${durlUrl.take(50)}...")
            return durlUrl
        }
        
        // 降级到 DASH 视频流
        val dashUrl = playData.dash?.video?.firstOrNull()?.baseUrl
        if (!dashUrl.isNullOrEmpty()) {
            Logger.d(TAG, " DASH URL: ${dashUrl.take(50)}...")
            return dashUrl
        }
        
        Logger.e(TAG, " 无法获取播放 URL")
        return null
    }
}
