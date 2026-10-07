import com.adamfoerster.tuavaga.buildlogic.lib
import com.adamfoerster.tuavaga.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Presentation layer of a feature: Compose + ViewModel + type-safe navigation + Koin,
 * plus the shared core UI modules.
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("tuavaga.kmp.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.commonMain.dependencies {
                implementation(project(":core:domain"))
                implementation(project(":core:presentation"))
                implementation(project(":core:design-system"))

                implementation(libs.lib("androidx-lifecycle-viewmodel-compose"))
                implementation(libs.lib("androidx-lifecycle-runtime-compose"))
                implementation(libs.lib("androidx-navigation-compose"))
                implementation(libs.lib("kotlinx-serialization-json"))

                implementation(project.dependencies.platform(libs.lib("koin-bom")))
                implementation(libs.lib("koin-core"))
                implementation(libs.lib("koin-compose"))
                implementation(libs.lib("koin-compose-viewmodel"))
            }
        }
    }
}
