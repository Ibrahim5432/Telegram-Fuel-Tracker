// ملف خدمة API للاتصال بالخادم
// استخدام Retrofit لإرسال الطلبات HTTP

package com.fuel.tracker.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import com.fuel.tracker.models.MessageResponse

/**
 * واجهة Retrofit لخدمة API
 * تحتوي على تعريفات جميع نقاط النهاية (Endpoints)
 */
interface ApiService {
    
    /**
     * الحصول على آخر الرسائل
     * @param limit عدد الرسائل المطلوب إرجاعها (افتراضي: 50)
     */
    @GET("get-latest-messages")
    suspend fun getLatestMessages(
        @Query("limit") limit: Int = 50
    ): Response<MessageResponse>
    
    /**
     * البحث عن رسائل معينة
     * @param keyword الكلمة المفتاحية للبحث
     * @param limit عدد النتائج (افتراضي: 50)
     */
    @GET("search")
    suspend fun searchMessages(
        @Query("keyword") keyword: String,
        @Query("limit") limit: Int = 50
    ): Response<MessageResponse>
    
    /**
     * فحص صحة الخادم
     */
    @GET("health")
    suspend fun healthCheck(): Response<HealthCheckResponse>
    
    /**
     * الحصول على الإحصائيات
     */
    @GET("stats")
    suspend fun getStats(): Response<StatsResponse>
}

/**
 * نموذج الاستجابة من الخادم
 */
data class MessageResponse(
    val status: String,
    val count: Int,
    val channel: String,
    val data: List<Message>
)

/**
 * نموذج الرسالة الواحدة
 */
data class Message(
    val id: Int,
    val message_id: Int,
    val text: String,
    val date: String,
    val media_path: String?
)

/**
 * نموذج استجابة فحص الصحة
 */
data class HealthCheckResponse(
    val status: String,
    val server: String,
    val database: String,
    val messages_count: Int
)

/**
 * نموذج الإحصائيات
 */
data class StatsResponse(
    val status: String,
    val channel: String,
    val total_messages: Int,
    val messages_with_media: Int,
    val last_sync: String
)
