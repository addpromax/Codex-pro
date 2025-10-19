rootProject.name = "Aiyatsbus"

include("plugin")
include("project:common")
include("project:common-impl")
include("project:module-language")
include("project:module-bukkit")
// 旧版本自定义附魔注册器
include("project:module-registration:registration-legacy")
// 1.20.4 (1.20.3) 自定义附魔注册器
include("project:module-registration:registration-v12004-paper")
include("project:module-registration:registration-v12004-vanilla")
// 跳过 1.20.5, 1.20.6
// 1.21 自定义附魔注册器
include("project:module-registration:registration-v12100-paper")
include("project:module-registration:registration-v12100-vanilla")
// 1.21.3 (1.20.2) 自定义附魔注册器
include("project:module-registration:registration-v12103-paper")
include("project:module-registration:registration-v12103-vanilla")
// 1.21.4 自定义附魔注册器
include("project:module-registration:registration-v12104-paper")
include("project:module-registration:registration-v12104-vanilla")

include("project:module-ingame")
include("project:module-script")
include("project:module-script:script-fluxon")
include("project:module-script:script-kether")

include("project:module-nms")
include("project:module-nms:nms-v12005")

include("project:module-compat")