dependencies {
    compileOnly(project(":project:module-registration:registration-v12103-paper"))
    compileOnly("ink.ptms.core:v12103:12103:mapped")
    // Mojang API
    compileOnly("com.mojang:brigadier:1.0.18")
}

// 编译配置
java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_HIGHER
    targetCompatibility = JavaVersion.VERSION_HIGHER
}

// 子模块
taboolib { subproject = true }