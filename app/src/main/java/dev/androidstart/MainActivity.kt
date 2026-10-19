package dev.androidstart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.androidstart.navigation.AppNavDisplay
import dev.androidstart.ui.theme.AndroidStartTheme

/**
 * 唯一的 Activity。**不要**往这里加业务逻辑，也不要新建第二个 Activity——
 * 页面切换一律走 `navigation/` 里的 Nav3 路由。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 必须在 super.onCreate 之前调用，否则首帧会先画一次系统栏再跳变。
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as AndroidStartApp).container

        setContent {
            AndroidStartTheme {
                AppNavDisplay(container = container)
            }
        }
    }
}
