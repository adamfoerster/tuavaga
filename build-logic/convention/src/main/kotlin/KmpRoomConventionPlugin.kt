import androidx.room3.gradle.RoomExtension
import com.adamfoerster.tuavaga.buildlogic.lib
import com.adamfoerster.tuavaga.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Room 3 + KSP on every target. The compiler has to run once per target, so each
 * KSP configuration is named explicitly.
 */
class KmpRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("tuavaga.kmp.library")
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("androidx.room3")

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.commonMain.dependencies {
                api(libs.lib("room-runtime"))
            }
            sourceSets.androidMain.dependencies {
                implementation(libs.lib("sqlite-bundled"))
            }
            sourceSets.iosMain.dependencies {
                implementation(libs.lib("sqlite-bundled"))
            }
            sourceSets.wasmJsMain.dependencies {
                implementation(libs.lib("sqlite-web"))
            }
        }

        dependencies {
            listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64", "kspWasmJs").forEach {
                add(it, libs.lib("room-compiler"))
            }
        }
    }
}
