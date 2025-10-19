repositories {
    maven("https://jitpack.io")
}

dependencies {
    // 如果不需要跨平台，可以在此处引入 Bukkit 核心
    // compileOnly("ink.ptms.core:v11903:11903:mapped")
    // compileOnly("ink.ptms.core:v11903:11903:universal")
    compileOnly("com.github.Redempt:Crunch:1.0.7")
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.6.2-beta-r3-b")
}

// 子模块
taboolib { subproject = true }