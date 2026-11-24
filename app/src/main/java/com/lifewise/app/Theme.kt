package com.lifewise.app

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun LifeWiseTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = Color(0xFFB9A8FF), primaryContainer = Color(0xFF3F2A9E))
        else -> lightColorScheme(primary = Color(0xFF5B3FD4), primaryContainer = Color(0xFFE6DEFF))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

val PayCategory.color: Color
    get() = when (this) {
        PayCategory.SUBSCRIPTION -> Color(0xFFE5484D)
        PayCategory.BILLS -> Color(0xFFF5A524)
        PayCategory.RENT -> Color(0xFF3E63DD)
        PayCategory.INSTALLMENT -> Color(0xFF8E4EC6)
        PayCategory.INSURANCE -> Color(0xFF12A594)
        PayCategory.EDUCATION -> Color(0xFF0090FF)
        PayCategory.HEALTH -> Color(0xFF30A46C)
        PayCategory.OTHER -> Color(0xFF7C7C8A)
    }

val FoodType.color: Color
    get() = when (this) {
        FoodType.GROCERIES -> Color(0xFF30A46C)
        FoodType.RESTAURANT -> Color(0xFFE5484D)
        FoodType.DELIVERY -> Color(0xFFF76B15)
        FoodType.COFFEE -> Color(0xFF8B5E3C)
        FoodType.OTHER -> Color(0xFF7C7C8A)
    }

val HealthColor = Color(0xFF3E63DD)
val MedColor = Color(0xFF30A46C)
