plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.buildkonfig) apply false
}

/**
 * `app.version` (gradle.properties) is the single source of the version. Fails when the newest
 * CHANGELOG.md entry or the iOS MARKETING_VERSION disagree with it, so a bump can't be half done.
 */
val verifyVersion by tasks.registering {
    group = "verification"
    description = "Checks that app.version, CHANGELOG.md and the iOS project agree."

    val version = providers.gradleProperty("app.version")
    val changelog = layout.projectDirectory.file("CHANGELOG.md")
    val xcconfig = layout.projectDirectory.file("iosApp/Configuration/Config.xcconfig")
    inputs.property("version", version)
    inputs.files(changelog, xcconfig)

    doLast {
        val expected = version.get()
        require(Regex("""\d+\.\d+\.\d+""").matches(expected)) {
            "app.version '$expected' is not MAJOR.MINOR.PATCH"
        }

        val latestChangelog = changelog.asFile.readLines()
            .firstNotNullOfOrNull { Regex("""^## \[(\d+\.\d+\.\d+)]""").find(it)?.groupValues?.get(1) }
        require(latestChangelog == expected) {
            "CHANGELOG.md newest entry is '$latestChangelog' but app.version is '$expected'"
        }

        val iosVersion = xcconfig.asFile.readLines()
            .firstNotNullOfOrNull { Regex("""^MARKETING_VERSION=(.+)$""").find(it.trim())?.groupValues?.get(1) }
        require(iosVersion == expected) {
            "iosApp MARKETING_VERSION is '$iosVersion' but app.version is '$expected'"
        }
    }
}

tasks.named("check") {
    dependsOn(verifyVersion)
}
