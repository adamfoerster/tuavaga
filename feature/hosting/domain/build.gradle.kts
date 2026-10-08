plugins {
    alias(libs.plugins.tuavaga.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            api(libs.kotlinx.datetime)
        }
    }
}
