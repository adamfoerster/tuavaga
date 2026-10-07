package com.adamfoerster.tuavaga.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

const val BASE_PACKAGE = "com.adamfoerster.tuavaga"

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.lib(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).get()

fun VersionCatalog.int(alias: String): Int =
    findVersion(alias).get().requiredVersion.toInt()

/** `:core:design-system` -> `com.adamfoerster.tuavaga.core.designsystem` */
val Project.derivedNamespace: String
    get() = BASE_PACKAGE + path.replace("-", "").replace(":", ".")
