// 文件路径: data/repository/DanmakuRepository.kt
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * 弹幕相关数据仓库
 * 从 VideoRepository 拆分出来，专注于弹幕功能
 */
object DanmakuRepository {
    private val api = NetworkModule.api

    // 弹幕数据缓存 - 避免横竖屏切换时重复下载
    private val danmakuCache = LinkedHashMap<Long, ByteArray>(5, 0.75f, true)
    private const val MAX_DANMAKU_CACHE_COUNT = 3  // 最多缓存3个视频的弹幕
    private const val MAX_DANMAKU_CACHE_BYTES = 4L * 1024 * 1024
    private var danmakuCacheBytes = 0L
    
    // Protobuf 弹幕分段缓存
    private val danmakuSegmentCache = LinkedHashMap<Long, List<ByteArray>>(5, 0.75f, true)
    private const val MAX_SEGMENT_CACHE_COUNT = 3
    private const val MAX_SEGMENT_CACHE_BYTES = 12L * 1024 * 1024
    private const val MAX_SEGMENT_PARALLELISM = 3
    private var danmakuSegmentCacheBytes = 0L

    /**
     * 清除弹幕缓存
     */
    fun clearDanmakuCache() {
        synchronized(danmakuCache) {
            danmakuCache.clear()
            danmakuCacheBytes = 0L
        }
        synchronized(danmakuSegmentCache) {
            danmakuSegmentCache.clear()
            danmakuSegmentCacheBytes = 0L
        }
        com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Danmaku cache cleared")
    }

    /**
     * 获取 XML 格式弹幕原始数据
     */
    suspend fun getDanmakuRawData(cid: Long): ByteArray? = withContext(Dispatchers.IO) {
        com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "🎯 getDanmakuRawData: cid=$cid")
        
        // 先检查缓存
        synchronized(danmakuCache) {
            danmakuCache[cid]?.let {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Danmaku cache hit for cid=$cid, size=${it.size}")
                return@withContext it
            }
        }
        
        try {
            val responseBody = api.getDanmakuXml(cid)
            val bytes = responseBody.bytes()
            com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "🎯 Danmaku raw bytes: ${bytes.size}, first byte: ${if (bytes.isNotEmpty()) String.format("0x%02X", bytes[0]) else "empty"}")

            if (bytes.isEmpty()) {
                android.util.Log.w("DanmakuRepo", " Danmaku response is empty!")
                return@withContext null
            }

            val result: ByteArray?
            
            // 检查首字节判断是否压缩
            // XML 以 '<' 开头 (0x3C)
            if (bytes[0] == 0x3C.toByte()) {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Danmaku is plain XML, size=${bytes.size}")
                result = bytes
            } else {
                // 尝试 Deflate 解压
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Danmaku appears compressed, attempting deflate...")
                result = try {
                    val inflater = java.util.zip.Inflater(true) // nowrap=true
                    inflater.setInput(bytes)
                    val outputStream = java.io.ByteArrayOutputStream(bytes.size * 3)
                    val tempBuffer = ByteArray(1024)
                    while (!inflater.finished()) {
                        val count = inflater.inflate(tempBuffer)
                        if (count == 0) {
                             if (inflater.needsInput()) break
                             if (inflater.needsDictionary()) break
                        }
                        outputStream.write(tempBuffer, 0, count)
                    }
                    inflater.end()
                    val decompressed = outputStream.toByteArray()
                    com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Danmaku decompressed: ${bytes.size} → ${decompressed.size} bytes")
                    decompressed
                } catch (e: Exception) {
                    android.util.Log.e("DanmakuRepo", " Deflate failed: ${e.message}")
                    e.printStackTrace()
                    // 解压失败，返回原始数据
                    bytes
                }
            }
            
            // 存入缓存（限制条目数与字节数）
            if (result != null && result.isNotEmpty()) {
                val entrySize = result.size.toLong()
                if (entrySize <= MAX_DANMAKU_CACHE_BYTES) {
                    synchronized(danmakuCache) {
                        danmakuCache.remove(cid)?.let { danmakuCacheBytes -= it.size.toLong() }
                        
                        val iterator = danmakuCache.entries.iterator()
                        while (iterator.hasNext() &&
                            (danmakuCache.size >= MAX_DANMAKU_CACHE_COUNT ||
                                danmakuCacheBytes + entrySize > MAX_DANMAKU_CACHE_BYTES)
                        ) {
                            val eldest = iterator.next()
                            danmakuCacheBytes -= eldest.value.size.toLong()
                            iterator.remove()
                            com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Danmaku cache evicted: cid=${eldest.key}")
                        }
                        danmakuCache[cid] = result
                        danmakuCacheBytes += entrySize
                        com.android.purebilibili.core.util.Logger.d(
                            "DanmakuRepo",
                            " Danmaku cached: cid=$cid, size=${result.size}, cacheSize=${danmakuCache.size}, bytes=$danmakuCacheBytes"
                        )
                    }
                } else {
                    com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Danmaku too large to cache: size=$entrySize")
                }
            }
            
            result
        } catch (e: Exception) {
            android.util.Log.e("DanmakuRepo", " getDanmakuRawData failed: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 获取 Protobuf 格式弹幕 (分段加载)
     * 
     * @param cid 视频 cid
     * @param durationMs 视频时长 (毫秒)，用于计算所需分段数
     * @return 所有分段的 Protobuf 数据列表
     */
    suspend fun getDanmakuSegments(cid: Long, durationMs: Long): List<ByteArray> = withContext(Dispatchers.IO) {
        com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "🎯 getDanmakuSegments: cid=$cid, duration=${durationMs}ms")
        
        // 检查缓存
        synchronized(danmakuSegmentCache) {
            danmakuSegmentCache[cid]?.let {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Protobuf danmaku cache hit: cid=$cid, segments=${it.size}")
                return@withContext it
            }
        }
        
        // 计算所需分段数 (每段 6 分钟 = 360000ms)
        val segmentDurationMs = 360000L
        val segmentCount = ((durationMs + segmentDurationMs - 1) / segmentDurationMs).toInt().coerceAtLeast(1)
        
        com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Fetching $segmentCount segments for ${durationMs}ms video")
        
        data class SegmentResult(val index: Int, val bytes: ByteArray)
        
        // 并发获取分段，限制并发度避免过载
        val segmentResults = coroutineScope {
            val semaphore = Semaphore(MAX_SEGMENT_PARALLELISM)
            (1..segmentCount).map { index ->
                async {
                    semaphore.withPermit {
                        try {
                            val response = api.getDanmakuSeg(oid = cid, segmentIndex = index)
                            val bytes = response.bytes()
                            if (bytes.isNotEmpty()) {
                                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Segment $index: ${bytes.size} bytes")
                                SegmentResult(index, bytes)
                            } else {
                                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Segment $index is empty")
                                null
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("DanmakuRepo", " Segment $index failed: ${e.message}")
                            null
                        }
                    }
                }
            }.awaitAll()
        }
        
        val results = segmentResults
            .filterNotNull()
            .sortedBy { it.index }
            .map { it.bytes }
        
        com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Got ${results.size}/$segmentCount segments for cid=$cid")
        
        // 缓存结果（限制条目数与字节数）
        if (results.isNotEmpty()) {
            val entrySize = results.sumOf { it.size.toLong() }
            if (entrySize <= MAX_SEGMENT_CACHE_BYTES) {
                synchronized(danmakuSegmentCache) {
                    danmakuSegmentCache.remove(cid)?.let { removed ->
                        danmakuSegmentCacheBytes -= removed.sumOf { it.size.toLong() }
                    }
                    
                    val iterator = danmakuSegmentCache.entries.iterator()
                    while (iterator.hasNext() &&
                        (danmakuSegmentCache.size >= MAX_SEGMENT_CACHE_COUNT ||
                            danmakuSegmentCacheBytes + entrySize > MAX_SEGMENT_CACHE_BYTES)
                    ) {
                        val eldest = iterator.next()
                        danmakuSegmentCacheBytes -= eldest.value.sumOf { it.size.toLong() }
                        iterator.remove()
                    }
                    
                    danmakuSegmentCache[cid] = results.toList()
                    danmakuSegmentCacheBytes += entrySize
                }
            } else {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", " Segments too large to cache: size=$entrySize")
            }
        }
        
        results.toList()
    }
}
