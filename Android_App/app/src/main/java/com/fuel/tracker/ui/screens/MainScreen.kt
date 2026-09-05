// ملف الشاشة الرئيسية باستخدام Jetpack Compose
// يعرض قائمة الرسائل وخاصية السحب للتحديث

package com.fuel.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fuel.tracker.api.Message
import com.fuel.tracker.viewmodel.FuelViewModel
import com.fuel.tracker.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.*

/**
 * الشاشة الرئيسية لعرض الرسائل
 */
@Composable
fun MainScreen(viewModel: FuelViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.loadMessages()
            // محاكاة تأخير التحديث
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1500)
                isRefreshing = false
            }
        }
    )
    
    // تحميل البيانات عند أول دخول للشاشة
    LaunchedEffect(Unit) {
        viewModel.loadMessages()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚗 تتبع محطات الوقود") },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
                elevation = 8.dp
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // شريط البحث
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.searchMessages(it) },
                onClearClick = { viewModel.clearSearch() },
                isSearching = isSearching
            )
            
            // محتوى الشاشة (مع خاصية السحب للتحديث)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                when (uiState) {
                    is UiState.Idle -> {
                        // الحالة الابتدائية
                        Text("جاري التحضير...")
                    }
                    is UiState.Loading -> {
                        // حالة التحميل
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Success -> {
                        // حالة النجاح - عرض الرسائل
                        if (messages.isEmpty()) {
                            EmptyState()
                        } else {
                            MessagesList(messages = messages)
                        }
                    }
                    is UiState.Error -> {
                        // حالة الخطأ
                        ErrorState(message = (uiState as UiState.Error).message)
                    }
                }
                
                // مؤشر السحب للتحديث
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

/**
 * شريط البحث
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    isSearching: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            placeholder = { Text("🔍 ابحث عن (بنزين، مازوت، مزدحم...)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearClick) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { /* تم البحث */ }),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.surface
            )
        )
    }
}

/**
 * قائمة الرسائل
 */
@Composable
fun MessagesList(messages: List<Message>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { message ->
            MessageCard(message = message)
        }
    }
}

/**
 * بطاقة الرسالة الواحدة
 */
@Composable
fun MessageCard(message: Message) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // رقم الرسالة والتاريخ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📨 #${message.message_id}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary
                )
                Text(
                    text = formatDate(message.date),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // نص الرسالة
            Text(
                text = message.text,
                style = MaterialTheme.typography.body2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            // زر الوسائط (إن وجدت)
            if (message.media_path != null) {
                MediaButton(mediaPath = message.media_path)
            }
        }
    }
}

/**
 * زر عرض الوسائط
 */
@Composable
fun MediaButton(mediaPath: String) {
    Button(
        onClick = { /* سيتم فتح الصورة من تيليجرام */ },
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary
        )
    ) {
        Icon(
            Icons.Default.Image,
            contentDescription = "وسائط",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("عرض الصورة/الفيديو")
    }
}

/**
 * حالة الفراغ (لا توجد رسائل)
 */
@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📭",
                style = MaterialTheme.typography.h2,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "لا توجد رسائل حالياً",
                style = MaterialTheme.typography.h6
            )
            Text(
                text = "حاول السحب للتحديث",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * حالة الخطأ
 */
@Composable
fun ErrorState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "❌",
                style = MaterialTheme.typography.h2,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "حدث خطأ",
                style = MaterialTheme.typography.h6
            )
            Text(
                text = message,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.error
            )
        }
    }
}

/**
 * تنسيق التاريخ بصيغة مفهومة
 */
private fun formatDate(dateString: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(dateString)
        
        val outputSdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
        outputSdf.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}
