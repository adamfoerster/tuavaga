rootProject.name = "tuavaga"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":app")

include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:presentation")
include(":core:design-system")

include(":feature:auth:domain")
include(":feature:auth:data")
include(":feature:auth:presentation")

include(":feature:bookings:presentation")

include(":feature:explore:domain")
include(":feature:explore:data")
include(":feature:explore:presentation")

include(":feature:hosting:domain")
include(":feature:hosting:data")
include(":feature:hosting:presentation")

include(":feature:messages:domain")
include(":feature:messages:data")
include(":feature:messages:presentation")

include(":feature:notifications:domain")
include(":feature:notifications:data")
include(":feature:notifications:presentation")

include(":feature:onboarding:presentation")

include(":feature:profile:domain")
include(":feature:profile:data")
include(":feature:profile:presentation")
