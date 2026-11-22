package com.easykebiao.app.util

/**
 * 本软件的身份信息与对外链接。
 *
 * **所有"我们自己的"地址只在这里出现一次**：换仓库时只改 [REPO_URL] / [REPO_SLUG]，
 * 「关于」页与启动时的更新检查会一起生效（不用再翻别处）。
 *
 * 更新检查为什么必须先判 [hasRepo]：不同发布渠道的签名不同，若检查指向别处的 Releases，
 * 一旦那边发了更高版本号，App 就会提示"新版可用"并下载一个装不上来的包
 * （签名不一致，系统会要求先卸载 → 课表数据全丢）。仓库未配置前一律不联网检查。
 */
object AppIdentity {
    /** 作者署名与主页 */
    const val AUTHOR_NAME = "chapwpzip"
    const val AUTHOR_URL = "https://github.com/chapwpzip"

    /** 本项目的开源仓库（2026-09-28 已建仓）。更新检查与"问题反馈"入口随之启用。 */
    const val REPO_URL = "https://github.com/chapwpzip/EasyKeBiao"
    const val REPO_SLUG = "chapwpzip/EasyKeBiao"

    /**
     * 开发者 QQ（2026-09-28 用户令「在关于页面留一个 qq 联系方式方便大家联系我」）。
     * 「关于」页那一行只读这一个常量；点一下 = 复制到剪贴板 ——
     * 不拉起 QQ：装的人手机上不一定有 QQ，而复制是"一定成功"的动作，
     * 号码本身也直接印在行里（看得见、抄得走）。
     */
    const val AUTHOR_QQ = "547991704"

    /** 第三方开源项目（界面设计体系与解析器来源，按各自许可证在「关于」页署名） */
    const val SLEEPY_URL = "https://github.com/lingion/sleepy"
    const val NEVOIT_URL = "https://github.com/Nevodev"
    const val KYANT_URL = "https://github.com/kyant0"

    /** 仓库是否已配置：为空时关于页显示"搭建中"，更新检查整条停用。 */
    val hasRepo: Boolean get() = REPO_URL.isNotBlank() && REPO_SLUG.isNotBlank()

    /** Only the production identity can install production updates. */
    val hasReleaseUpdates: Boolean get() = hasRepo &&
        com.easykebiao.app.BuildConfig.APPLICATION_ID == "com.easykebiao.app" &&
        !com.easykebiao.app.BuildConfig.DEBUG

    /** 仓库 Issues 新建页（[hasRepo] 为 true 时才可用） */
    fun newIssueUrl(): String = "https://github.com/$REPO_SLUG/issues/new"
}
