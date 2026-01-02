// 文件路径: core/cache/PlayUrlCache.kt
package com.android.purebilibili.core.cache

import android.util.LruCache
import com.android.purebilibili.data.model.response.PlayUrlData

/**
 * 播放地址缓存管理器
 * 
 * 使用 LruCache 缓存视频播放 URL，减少重复网络请求。
 * 缓存上限 50 条，有效期 10 分钟。
 */
object PlayUrlCache {
    
    private const val TAG = "PlayUrlCache"
    private const val MAX_CACHE_SIZE = 80  //  优化：增加缓存容量
    private const val CACHE_DURATION_MS = 10 * 60 * 1000L // 10 分钟
    
    /**
     * 缓存条目
     */
    data class CachedPlayUrl(
        val bvid: String,
        val cid: Long,
        val data: PlayUrlData,
        val quality: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        val expiresAt: Long get() = timestamp + CACHE_DURATION_MS
        
        fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    }
    
    /**
     * 生成缓存键
     */
    private fun generateKey(bvid: String, cid: Long): String = "$bvid:$cid"
    
    /**
     * 缓存实例
     */
    private val cache: LruCache<String, CachedPlayUrl> = LruCache(MAX_CACHE_SIZE)
    
    /**
     * 获取缓存的播放地址
     * 
     * @param bvid 视频 BV 号
     * @param cid 视频 CID
     * @return 缓存的播放数据，如果缓存不存在或已过期则返回 null
     */
    @Synchronized
    fun get(bvid: String, cid: Long): PlayUrlData? {
        val key = generateKey(bvid, cid)
        val cached = cache.get(key)
        
        return when {
            cached == null -> {
                com.android.purebilibili.core.util.Logger.d(TAG, " Cache miss: bvid=$bvid, cid=$cid")
                null
            }
            cached.isExpired() -> {
                com.android.purebilibili.core.util.Logger.d(TAG, "⏰ Cache expired: bvid=$bvid, cid=$cid")
                cache.remove(key)
                null
            }
            else -> {
                val remainingMs = cached.expiresAt - System.currentTimeMillis()
                com.android.purebilibili.core.util.Logger.d(TAG, " Cache hit: bvid=$bvid, cid=$cid, expires in ${remainingMs / 1000}s")
                cached.data
            }
        }
    }
    
    /**
     * 添加播放地址到缓存
     * 
     * @param bvid 视频 BV 号
     * @param cid 视频 CID
     * @param data 播放数据
     * @param quality 当前画质
     */
    @Synchronized
    fun put(bvid: String, cid: Long, data: PlayUrlData, quality: Int = 0) {
        val key = generateKey(bvid, cid)
        val entry = CachedPlayUrl(
            bvid = bvid,
            cid = cid,
            data = data,
            quality = quality
        )
        cache.put(key, entry)
        com.android.purebilibili.core.util.Logger.d(TAG, " Cached: bvid=$bvid, cid=$cid, quality=$quality")
    }
    
    /**
     * 使指定视频的缓存失效
     */
    @Synchronized
    fun invalidate(bvid: String, cid: Long) {
        val key = generateKey(bvid, cid)
        cache.remove(key)
        com.android.purebilibili.core.util.Logger.d(TAG, " Invalidated: bvid=$bvid, cid=$cid")
    }
    
    /**
     * 清除所有缓存
     */
    @Synchronized
    fun clear() {
        cache.evictAll()
        com.android.purebilibili.core.util.Logger.d(TAG, " Cache cleared")
    }
    
    /**
     * 获取当前缓存大小
     */
    fun size(): Int = cache.size()
    
    /**
     * 获取缓存统计信息（调试用）
     */
    fun getStats(): String {
        return "PlayUrlCache: size=${size()}, maxSize=$MAX_CACHE_SIZE, " +
               "hitCount=${cache.hitCount()}, missCount=${cache.missCount()}"
    }
}
