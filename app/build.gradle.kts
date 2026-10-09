import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.tuavaga.kmp.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    val xcf = XCFramework("ComposeApp")
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            xcf.add(this)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("tuavaga")
        browser {
            commonWebpackConfig {
                outputFileName = "tuavaga.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    open = false
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.data)
            implementation(projects.core.database)
            implementation(projects.core.presentation)
            implementation(projects.core.designSystem)
            implementation(projects.feature.auth.domain)
            implementation(projects.feature.auth.data)
            implementation(projects.feature.auth.presentation)
            implementation(projects.feature.bookings.presentation)
            implementation(projects.feature.profile.domain)
            implementation(projects.feature.profile.data)
            implementation(projects.feature.profile.presentation)
            implementation(projects.feature.explore.domain)
            implementation(projects.feature.explore.data)
            implementation(projects.feature.explore.presentation)
            implementation(projects.feature.hosting.domain)
            implementation(projects.feature.hosting.data)
            implementation(projects.feature.hosting.presentation)
            implementation(projects.feature.messages.domain)
            implementation(projects.feature.messages.data)
            implementation(projects.feature.messages.presentation)
            implementation(projects.feature.notifications.domain)
            implementation(projects.feature.notifications.data)
            implementation(projects.feature.notifications.presentation)
            implementation(projects.feature.onboarding.presentation)

            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.kotlinx.serialization.json)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}
