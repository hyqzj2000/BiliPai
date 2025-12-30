// 文件路径: feature/story/StoryViewModel.kt
package com.android.purebilibili.feature.story

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.model.response.StoryItem
import com.android.purebilibili.data.repository.StoryRepository
import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StoryUiState(
    val items: List<StoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentIndex: Int = 0
)

class StoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()
    
    private var lastAid: Long = 0
    
    init {
        loadInitialStories()
    }
    
    private fun loadInitialStories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = StoryRepository.getStoryFeed()
            result.onSuccess { items ->
                _uiState.value = _uiState.value.copy(
                    items = items,
                    isLoading = false
                )
                if (items.isNotEmpty()) {
                    lastAid = items.last().playerArgs?.aid ?: 0
                    
                    // 🔥 调试日志：输出第一个视频的详细信息
                    val firstItem = items.first()
                    Logger.d("StoryVM", "🎬 第一个视频: title=${firstItem.title.take(20)}")
                    Logger.d("StoryVM", "🎬 cover=${firstItem.cover.take(50)}...")
                    Logger.d("StoryVM", "🎬 playerArgs: bvid=${firstItem.playerArgs?.bvid}, cid=${firstItem.playerArgs?.cid}, aid=${firstItem.playerArgs?.aid}")
                    Logger.d("StoryVM", "🎬 owner: name=${firstItem.owner?.name}, face=${firstItem.owner?.face?.take(30)}")
                }
                Logger.d("StoryVM", "🎬 加载了 ${items.size} 个故事视频")
            }.onFailure { e ->
                Logger.e("StoryVM", "🔥 加载故事失败: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }
    
    fun loadMoreStories() {
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = StoryRepository.getStoryFeed(aid = lastAid)
            result.onSuccess { newItems ->
                val currentItems = _uiState.value.items
                _uiState.value = _uiState.value.copy(
                    items = currentItems + newItems,
                    isLoading = false
                )
                if (newItems.isNotEmpty()) {
                    lastAid = newItems.last().playerArgs?.aid ?: 0
                }
                Logger.d("StoryVM", "🎬 加载更多: ${newItems.size} 个视频")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun updateCurrentIndex(index: Int) {
        _uiState.value = _uiState.value.copy(currentIndex = index)
        
        // 当接近列表末尾时自动加载更多
        val items = _uiState.value.items
        if (index >= items.size - 3 && items.isNotEmpty()) {
            loadMoreStories()
        }
    }
    
    fun refresh() {
        lastAid = 0
        loadInitialStories()
    }
}
