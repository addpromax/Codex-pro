dependencies {
    // 引入 API
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
}

// 子模块
taboolib { subproject = true }