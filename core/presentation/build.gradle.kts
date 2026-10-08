plugins {
    alias(libs.plugins.tuavaga.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.designSystem)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
    }
}
