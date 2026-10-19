package dev.androidstart.ui.theme

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

/**
 * 品牌色。只定义"区别性"的几个槽位，其余交给 Material 3 的默认色阶推导——
 * 手写 30 个颜色只会让后面接手的人不敢改。
 */
private val Brand = Color(0xFF2F5D3A)
private val BrandContainer = Color(0xFFB1D8B8)
private val Accent = Color(0xFF4A6350)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = BrandContainer,
    onPrimaryContainer = Color(0xFF0B2013),
    secondary = Accent,
    tertiary = Color(0xFF7A5A2E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF95D5A2),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF1B4B2A),
    onPrimaryContainer = BrandContainer,
    secondary = Color(0xFFB4CCB8),
    tertiary = Color(0xFFE4C08B),
)

/**
 * 全应用唯一的主题入口。
 *
 * @param dynamicColor 是否跟随系统取色（Android 12+ 的 Material You）。
 *   做品牌应用时把它设成 false，否则你的品牌色在用户机器上会被系统色替换掉。
 */
@Composable
fun AndroidStartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
