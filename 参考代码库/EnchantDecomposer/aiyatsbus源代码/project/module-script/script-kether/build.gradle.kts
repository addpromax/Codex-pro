dependencies {
    // 引入 API
    compileOnly(project(":project:common"))
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

// 编译配置
java {
    sourceCompatibility = JavaVersion.VERSION_HIGHER
    targetCompatibility = JavaVersion.VERSION_HIGHER
}

// 子模块
taboolib { subproject = true }