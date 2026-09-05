// ملف المستودع (Repository) لإدارة جلب البيانات
// يقوم بالفصل بين الخدمات والواجهات

package com.fuel.tracker.repository

import android.util.Log
import com.fuel.tracker.api.ApiService
import com.fuel.tracker.api.Message
import com.fuel.tracker.api.MessageResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response

/**
 * فئة المستودع للتعامل مع جلب البيانات
 */
class FuelRepository(private val apiService: ApiService) {
    
    companion object {
        private const val TAG = "FuelRepository"
    }
    
    /**
     * جلب آخر الرسائل على شكل Flow (متدفق)
     * يسمح بتحديث البيانات بشكل تفاعلي
     */
    fun getLatestMessages(limit: Int = 50): Flow<Result<List<Message>>> = flow {
        try {
            Log.d(TAG, "🔄 جاري جلب الرسائل من الخادم...")
            
            val response = apiService.getLatestMessages(limit)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d(TAG, "✅ تم جلب ${body.data.size} رسالة بنجاح")
                    emit(Result.success(body.data))
                } else {
                    Log.e(TAG, "❌ الاستجابة فارغة")
                    emit(Result.failure(Exception("الاستجابة فارغة")))
                }
            } else {
                Log.e(TAG, "❌ خطأ من الخادم: ${response.code()}")
                emit(Result.failure(Exception("خطأ الخادم: ${response.code()}")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ خطأ في الاتصال: ${e.message}")
            emit(Result.failure(e))
        }
    }
    
    /**
     * البحث عن رسائل معينة
     */
    fun searchMessages(keyword: String, limit: Int = 50): Flow<Result<List<Message>>> = flow {
        try {
            if (keyword.isBlank()) {
                Log.w(TAG, "⚠️ الكلمة المفتاحية فارغة")
                emit(Result.failure(Exception("الكلمة المفتاحية لا يمكن أن تكون فارغة")))
                return@flow
            }
            
            Log.d(TAG, "🔍 جاري البحث عن: $keyword")
            
            val response = apiService.searchMessages(keyword, limit)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d(TAG, "✅ تم العثور على ${body.data.size} نتيجة")
                    emit(Result.success(body.data))
                } else {
                    emit(Result.failure(Exception("الاستجابة فارغة")))
                }
            } else {
                Log.e(TAG, "❌ خطأ في البحث: ${response.code()}")
                emit(Result.failure(Exception("خطأ البحث: ${response.code()}")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ خطأ في البحث: ${e.message}")
            emit(Result.failure(e))
        }
    }
}
