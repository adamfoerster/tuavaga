import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.util.Properties

plugins {
    alias(libs.plugins.tuavaga.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.database)

            api(project.dependencies.platform(libs.supabase.bom))
            api(libs.supabase.auth)
            api(libs.supabase.postgrest)
            implementation(libs.kotlinx.serialization.json)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

// Secrets come from local.properties (not versioned) or environment variables (CI).
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

fun config(key: String): String =
    localProperties.getProperty(key) ?: providers.environmentVariable(key).orNull ?: ""

buildkonfig {
    packageName = "com.adamfoerster.tuavaga.core.data"
    objectName = "AppConfig"
    defaultConfigs {
        buildConfigField(STRING, "SUPABASE_URL", config("SUPABASE_URL"))
        buildConfigField(STRING, "SUPABASE_ANON_KEY", config("SUPABASE_ANON_KEY"))
        buildConfigField(STRING, "ASSETS_BASE_URL", config("ASSETS_BASE_URL"))
    }
}
