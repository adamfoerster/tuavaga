plugins {
    alias(libs.plugins.tuavaga.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
    }
}
