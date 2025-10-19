dependencies {
    // 引入 API
    compileOnly(project(":project:common"))
    compileOnly(project(":project:module-script:script-fluxon"))
    compileOnly(project(":project:module-script:script-kether"))
}

// 编译配置
java {
    sourceCompatibility = JavaVersion.VERSION_HIGHER
    targetCompatibility = JavaVersion.VERSION_HIGHER
}

// 子模块
taboolib { subproject = true }