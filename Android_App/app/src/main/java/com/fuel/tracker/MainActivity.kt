// MainActivity - نقطة الدخول الرئيسية للتطبيق
// تشغيل Compose والتهيئة

package com.fuel.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuel.tracker.ui.screens.MainScreen
import com.fuel.tracker.ui.theme.FuelTrackerTheme
import com.fuel.tracker.viewmodel.FuelViewModel
import androidx.lifecycle.ViewModelProvider
import com.fuel.tracker.api.ApiService
import com.fuel.tracker.repository.FuelRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val API_BASE_URL = "http://10.0.2.2:8000/"  // للمحاكي (emulator)
        // أو استخدم الـ IP الفعلي للخادم: "http://192.168.1.X:8000/"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "🚀 بدء تطبيق تتبع محطات الوقود")
        
        // إنشاء Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        // إنشاء ApiService
        val apiService = retrofit.create(ApiService::class.java)
        
        // إنشاء Repository
        val repository = FuelRepository(apiService)
        
        // إنشاء ViewModelFactory
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return FuelViewModel(repository) as T
            }
        }
        
        setContent {
            FuelTrackerTheme {
                Surface(
                    modifier = androidx.compose.foundation.layout.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    // الحصول على ViewModel باستخدام Factory
                    val fuelViewModel: FuelViewModel = viewModel(factory = viewModelFactory)
                    
                    // عرض الشاشة الرئيسية
                    MainScreen(viewModel = fuelViewModel)
                }
            }
        }
    }
}
