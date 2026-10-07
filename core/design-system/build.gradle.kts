plugins {
    alias(libs.plugins.tuavaga.kmp.compose)
}

kotlin {
    android {
        // Compose resources (the Kerb fonts) are packaged as Android assets.
        androidResources.enable = true
    }
    sourceSets {
        commonMain.dependencies {
            api(libs.compose.components.resources)
        }
    }
}

compose.resources {
    packageOfResClass = "com.adamfoerster.tuavaga.core.designsystem.resources"
}
