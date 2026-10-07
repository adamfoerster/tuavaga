import com.adamfoerster.tuavaga.buildlogic.derivedNamespace
import com.adamfoerster.tuavaga.buildlogic.int
import com.adamfoerster.tuavaga.buildlogic.lib
import com.adamfoerster.tuavaga.buildlogic.libs
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Base for every shared module: Kotlin Multiplatform with the three app targets
 * (Android, iOS, wasmJs) and the Android KMP library plugin.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")

        extensions.configure<KotlinMultiplatformExtension> {
            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                namespace = derivedNamespace
                compileSdk = libs.int("android-compileSdk")
                minSdk = libs.int("android-minSdk")
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
                // Runs commonTest on the JVM (fast, no browser/simulator needed).
                withHostTest {}
            }

            iosArm64()
            iosSimulatorArm64()

            @Suppress("OPT_IN_USAGE")
            wasmJs {
                browser()
            }

            applyDefaultHierarchyTemplate()

            sourceSets.commonMain.dependencies {
                implementation(libs.lib("kotlinx-coroutines-core"))
            }
            sourceSets.commonTest.dependencies {
                implementation(libs.lib("kotlin-test"))
                implementation(libs.lib("kotlinx-coroutines-test"))
            }

            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }
}
