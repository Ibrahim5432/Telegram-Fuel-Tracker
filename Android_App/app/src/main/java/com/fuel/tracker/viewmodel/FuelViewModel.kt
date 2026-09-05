// ملف ViewModel لإدارة حالة التطبيق
// يتعامل مع منطق التطبيق بشكل منفصل عن الواجهات

package com.fuel.tracker.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuel.tracker.api.Message
import com.fuel.tracker.repository.FuelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * حالات التطبيق المختلفة
 */
sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val messages: List<Message>) : UiState()
    data class Error(val message: String) : UiState()
}

/**
 * ViewModel لإدارة البيانات والحالة
 */
class FuelViewModel(private val repository: FuelRepository) : ViewModel() {
    
    companion object {
        private const val TAG = "FuelViewModel"
    }
    
    // حالة الرسائل
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // قائمة الرسائل الحالية
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    // نص البحث
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // هل يجري بحث حالي
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    /**
     * جلب آخر الرسائل
     */
    fun loadMessages(limit: Int = 50) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            Log.d(TAG, "🔄 جاري تحميل الرسائل...")
            
            try {
                repository.getLatestMessages(limit).collect { result ->
                    result.onSuccess { messages ->
                        _messages.value = messages
                        _uiState.value = UiState.Success(messages)
                        Log.d(TAG, "✅ تم تحميل ${messages.size} رسالة")
                    }.onFailure { error ->
                        _uiState.value = UiState.Error(error.message ?: "خطأ غير معروف")
                        Log.e(TAG, "❌ خطأ: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "خطأ في تحميل البيانات")
                Log.e(TAG, "❌ استثناء: ${e.message}")
            }
        }
    }
    
    /**
     * البحث عن رسائل
     */
    fun searchMessages(keyword: String) {
        _searchQuery.value = keyword
        
        if (keyword.isBlank()) {
            // إذا كانت الكلمة فارغة، أعد تحميل جميع الرسائل
            loadMessages()
            _isSearching.value = false
            return
        }
        
        viewModelScope.launch {
            _isSearching.value = true
            _uiState.value = UiState.Loading
            Log.d(TAG, "🔍 جاري البحث عن: $keyword")
            
            try {
                repository.searchMessages(keyword).collect { result ->
                    result.onSuccess { messages ->
                        _messages.value = messages
                        _uiState.value = UiState.Success(messages)
                        Log.d(TAG, "✅ تم العثور على ${messages.size} نتيجة")
                    }.onFailure { error ->
                        _uiState.value = UiState.Error(error.message ?: "خطأ في البحث")
                        Log.e(TAG, "❌ خطأ في البحث: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "خطأ في البحث")
                Log.e(TAG, "❌ استثناء: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    /**
     * مسح البحث
     */
    fun clearSearch() {
        _searchQuery.value = ""
        loadMessages()
    }
}
