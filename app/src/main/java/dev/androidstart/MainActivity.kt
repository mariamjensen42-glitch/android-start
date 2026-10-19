package dev.androidstart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.androidstart.navigation.AppNavDisplay
import dev.androidstart.ui.theme.AndroidStartTheme

/**
 * 唯一的 Activity。**不要**往这里加业务逻辑，也不要新建第二个 Activity——
 * 页面切换一律走 `navigation/` 里的 Nav3 路由。
 *
 * `@AndroidEntryPoint` 让 Hilt 把依赖注入到本 Activity 的作用域；
 * 本项目不需要在 Activity 里取任何依赖，但 Nav3 的各个 NavEntry 需要它来拿
 * Hilt 生成的 ViewModel 工厂，所以这个注解不能省。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 必须在 super.onCreate 之前调用，否则首帧会先画一次系统栏再跳变。
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AndroidStartTheme {
                AppNavDisplay()
            }
        }
    }
}
