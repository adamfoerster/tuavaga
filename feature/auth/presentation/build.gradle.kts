plugins {
    alias(libs.plugins.tuavaga.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.auth.domain)
        }
    }
}
