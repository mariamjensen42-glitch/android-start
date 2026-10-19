# 项目特定的 R8 规则放这里。
#
# 现状：不需要任何规则。理由写清楚，避免以后有人"保险起见"把 -dontobfuscate 加进来：
# - kotlinx.serialization 从 1.x 起由插件生成序列化器，R8 通过 META-INF 里的
#   *.kotlin_module / 序列化器注册信息自动保留，不需要 keep @Serializable 类。
# - Compose 由 AGP 内置的 consumer rules 处理。
# - 本项目没有反射、没有 JNI、没有运行时动态加载。
#
# 什么时候才该往这里加东西：引入了靠反射的库（如某些 JSON 映射库、依赖类名字符串的 SDK）。
# 加之前先跑 `./gradlew assembleRelease` 并把 APK 装到设备上冒烟，别靠猜。
