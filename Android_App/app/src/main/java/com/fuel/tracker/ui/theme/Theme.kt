// ملف Theme للتطبيق
// تحديد الألوان والأنماط

package com.fuel.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkMode
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// الألوان الأساسية
private val PrimaryColor = Color(0xFF2196F3)
private val SecondaryColor = Color(0xFFFFC107)
private val TertiaryColor = Color(0xFF4CAF50)
private val ErrorColor = Color(0xFFFF5252)

// ألوان الوضع الفاتح
private val LightColors = lightColors(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    surface = Color(0xFFFAFAFA),
    background = Color(0xFFFFFFFF),
    error = ErrorColor
)

// ألوان الوضع الداكن
private val DarkColors = darkColors(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    surface = Color(0xFF212121),
    background = Color(0xFF121212),
    error = ErrorColor
)

@Composable
fun FuelTrackerTheme(
    darkTheme: Boolean = isSystemInDarkMode(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }
    
    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
