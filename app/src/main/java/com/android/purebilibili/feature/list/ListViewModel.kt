// 文件路径: feature/list/ListViewModel.kt
package com.android.purebilibili.feature.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 通用的 UI 状态
data class ListUiState(
    val title: String = "",
    val items: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// 基类 ViewModel
abstract class BaseListViewModel(application: Application, private val pageTitle: String) : AndroidViewModel(application) {
    protected val _uiState = MutableStateFlow(ListUiState(title = pageTitle, isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val items = fetchItems()
                _uiState.value = _uiState.value.copy(isLoading = false, items = items)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }

    // 子类必须实现此方法来提供数据
    abstract suspend fun fetchItems(): List<VideoItem>
}

// --- 历史记录 ViewModel (支持游标分页加载) ---
class HistoryViewModel(application: Application) : BaseListViewModel(application, "历史记录") {
    
    // 游标分页状态
    private var cursorMax: Long = 0
    private var cursorViewAt: Long = 0
    private var hasMore = true
    private var isLoadingMore = false
    
    //  暴露加载更多状态
    private val _isLoadingMoreState = MutableStateFlow(false)
    val isLoadingMoreState = _isLoadingMoreState.asStateFlow()
    
    private val _hasMoreState = MutableStateFlow(true)
    val hasMoreState = _hasMoreState.asStateFlow()
    
    // [新增] 保存完整的历史记录项（包含导航信息）
    private val _historyItemsMap = mutableMapOf<String, com.android.purebilibili.data.model.response.HistoryItem>()
    
    /**
     * 根据 bvid 获取历史记录项的导航信息
     */
    fun getHistoryItem(bvid: String): com.android.purebilibili.data.model.response.HistoryItem? {
        return _historyItemsMap[bvid]
    }
    
    override suspend fun fetchItems(): List<VideoItem> {
        // 重置游标
        cursorMax = 0
        cursorViewAt = 0
        _historyItemsMap.clear()
        
        val result = com.android.purebilibili.data.repository.HistoryRepository.getHistoryList(
            ps = 30,
            max = 0,
            viewAt = 0
        )
        
        val historyResult = result.getOrNull()
        if (historyResult == null) {
            hasMore = false
            _hasMoreState.value = false
            return emptyList()
        }
        
        // 更新游标
        historyResult.cursor?.let { cursor ->
            cursorMax = cursor.max
            cursorViewAt = cursor.view_at
        }
        
        // 判断是否还有更多
        hasMore = historyResult.list.isNotEmpty() && historyResult.cursor != null && historyResult.cursor.max > 0
        _hasMoreState.value = hasMore
        
        // 保存历史记录项并转换为 VideoItem
        val historyItems = historyResult.list.map { it.toHistoryItem() }
        historyItems.forEach { item ->
            _historyItemsMap[item.videoItem.bvid] = item
        }
        
        com.android.purebilibili.core.util.Logger.d("HistoryVM", " First page: ${historyResult.list.size} items, hasMore=$hasMore, nextMax=$cursorMax")
        
        return historyItems.map { it.videoItem }
    }
    
    //  加载更多
    fun loadMore() {
        if (isLoadingMore || !hasMore) return
        
        viewModelScope.launch {
            isLoadingMore = true
            _isLoadingMoreState.value = true
            
            try {
                com.android.purebilibili.core.util.Logger.d("HistoryVM", " loadMore: max=$cursorMax, viewAt=$cursorViewAt")
                
                val result = com.android.purebilibili.data.repository.HistoryRepository.getHistoryList(
                    ps = 30,
                    max = cursorMax,
                    viewAt = cursorViewAt
                )
                
                val historyResult = result.getOrNull()
                if (historyResult == null || historyResult.list.isEmpty()) {
                    hasMore = false
                    _hasMoreState.value = false
                    return@launch
                }
                
                // 更新游标
                historyResult.cursor?.let { cursor ->
                    cursorMax = cursor.max
                    cursorViewAt = cursor.view_at
                }
                
                // 判断是否还有更多
                hasMore = historyResult.cursor != null && historyResult.cursor.max > 0
                _hasMoreState.value = hasMore
                
                // 保存历史记录项并转换为 VideoItem
                val historyItems = historyResult.list.map { it.toHistoryItem() }
                historyItems.forEach { item ->
                    _historyItemsMap[item.videoItem.bvid] = item
                }
                
                val newItems = historyItems.map { it.videoItem }
                com.android.purebilibili.core.util.Logger.d("HistoryVM", " Loaded ${newItems.size} more items, hasMore=$hasMore")
                
                if (newItems.isNotEmpty()) {
                    // 追加到现有列表（过滤重复）
                    val currentItems = _uiState.value.items
                    val existingBvids = currentItems.map { it.bvid }.toSet()
                    val uniqueNewItems = newItems.filter { it.bvid !in existingBvids }
                    _uiState.value = _uiState.value.copy(items = currentItems + uniqueNewItems)
                    com.android.purebilibili.core.util.Logger.d("HistoryVM", " Total items: ${_uiState.value.items.size}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                com.android.purebilibili.core.util.Logger.e("HistoryVM", " loadMore failed", e)
            } finally {
                isLoadingMore = false
                _isLoadingMoreState.value = false
            }
        }
    }
}

// --- 收藏 ViewModel (支持分页加载所有收藏夹) ---
class FavoriteViewModel(application: Application) : BaseListViewModel(application, "我的收藏") {
    
    // 分页状态
    private var currentPage = 1
    private var hasMore = true
    private var allFolderIds: List<Long> = emptyList()  //  所有收藏夹 ID
    private var currentFolderIndex = 0  //  当前正在加载的收藏夹索引
    private var isLoadingMore = false
    
    //  暴露加载更多状态
    private val _isLoadingMoreState = MutableStateFlow(false)
    val isLoadingMoreState = _isLoadingMoreState.asStateFlow()
    
    private val _hasMoreState = MutableStateFlow(true)
    val hasMoreState = _hasMoreState.asStateFlow()
    
    override suspend fun fetchItems(): List<VideoItem> {
        val api = NetworkModule.api

        // 1. 先获取用户信息，拿到 mid (用户ID)
        val navResp = api.getNavInfo()
        val mid = navResp.data?.mid
        if (mid == null || mid == 0L) {
            throw Exception("无法获取用户信息，请先登录")
        }

        // 2. 获取该用户的收藏夹列表
        val foldersResult = com.android.purebilibili.data.repository.FavoriteRepository.getFavFolders(mid)
        val folders = foldersResult.getOrNull()
        if (folders.isNullOrEmpty()) {
            hasMore = false
            _hasMoreState.value = false
            return emptyList()
        }

        // 3.  保存所有收藏夹 ID
        allFolderIds = folders.map { it.id }
        currentFolderIndex = 0
        currentPage = 1
        
        com.android.purebilibili.core.util.Logger.d("FavoriteVM", " Found ${allFolderIds.size} folders: ${folders.map { "${it.title}(${it.media_count})" }}")

        // 4. 获取第一个收藏夹内的视频（第一页）
        val listResult = com.android.purebilibili.data.repository.FavoriteRepository.getFavoriteList(
            mediaId = allFolderIds[0], 
            pn = 1
        )
        val items = listResult.getOrNull()?.map { it.toVideoItem() } ?: emptyList()
        
        com.android.purebilibili.core.util.Logger.d("FavoriteVM", " First folder loaded ${items.size} items")
        
        // 判断是否还有更多（本收藏夹还有更多，或还有其他收藏夹）
        hasMore = items.size >= 20 || allFolderIds.size > 1
        _hasMoreState.value = hasMore
        
        return items
    }
    
    //  加载更多
    fun loadMore() {
        if (isLoadingMore || !hasMore || allFolderIds.isEmpty()) return
        
        viewModelScope.launch {
            isLoadingMore = true
            _isLoadingMoreState.value = true
            
            try {
                currentPage++
                com.android.purebilibili.core.util.Logger.d("FavoriteVM", " loadMore: folder=$currentFolderIndex, page=$currentPage")
                
                val listResult = com.android.purebilibili.data.repository.FavoriteRepository.getFavoriteList(
                    mediaId = allFolderIds[currentFolderIndex], 
                    pn = currentPage
                )
                var newItems = listResult.getOrNull()?.map { it.toVideoItem() } ?: emptyList()
                
                com.android.purebilibili.core.util.Logger.d("FavoriteVM", " Loaded ${newItems.size} items from folder $currentFolderIndex page $currentPage")
                
                //  如果当前收藏夹没有更多内容，尝试加载下一个收藏夹
                if (newItems.isEmpty() || newItems.size < 20) {
                    currentFolderIndex++
                    if (currentFolderIndex < allFolderIds.size) {
                        // 重置页码，加载下一个收藏夹
                        currentPage = 1
                        com.android.purebilibili.core.util.Logger.d("FavoriteVM", " Moving to next folder: $currentFolderIndex")
                        
                        val nextFolderResult = com.android.purebilibili.data.repository.FavoriteRepository.getFavoriteList(
                            mediaId = allFolderIds[currentFolderIndex], 
                            pn = 1
                        )
                        val nextItems = nextFolderResult.getOrNull()?.map { it.toVideoItem() } ?: emptyList()
                        newItems = newItems + nextItems
                        hasMore = nextItems.size >= 20 || currentFolderIndex < allFolderIds.size - 1
                    } else {
                        // 所有收藏夹都加载完了
                        hasMore = false
                    }
                } else {
                    hasMore = true
                }
                
                _hasMoreState.value = hasMore
                
                if (newItems.isNotEmpty()) {
                    // 追加到现有列表（过滤重复）
                    val currentItems = _uiState.value.items
                    val existingBvids = currentItems.map { it.bvid }.toSet()
                    val uniqueNewItems = newItems.filter { it.bvid !in existingBvids }
                    _uiState.value = _uiState.value.copy(items = currentItems + uniqueNewItems)
                    com.android.purebilibili.core.util.Logger.d("FavoriteVM", " Total items: ${_uiState.value.items.size}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                com.android.purebilibili.core.util.Logger.e("FavoriteVM", " loadMore failed", e)
                // 加载更多失败时回退页码
                currentPage--
            } finally {
                isLoadingMore = false
                _isLoadingMoreState.value = false
            }
        }
    }
}