pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            aboutLibraries()
            androidx()
            compose()
            firebase()
            hilt()
            kotlinx()
            ktor()
        }
    }
}

rootProject.name = "Koguma"
include("", "app")

fun VersionCatalogBuilder.aboutLibraries() {
    val version = version("aboutlibraries", "10.4.0")

    library("aboutlibraries-core", "com.mikepenz", "aboutlibraries-core").versionRef(version)
    library("aboutlibraries-compose", "com.mikepenz", "aboutlibraries-compose").versionRef(version)

    plugin("aboutlibraries", "com.mikepenz.aboutlibraries.plugin").versionRef(version)

    bundle("aboutlibraries", listOf("aboutlibraries-core", "aboutlibraries-compose"))
}

fun VersionCatalogBuilder.androidx() {
    library("androidx-viewmodel-compose", "androidx.lifecycle", "lifecycle-viewmodel-compose").version("2.5.1")
    library("androidx-corektx", "androidx.core", "core-ktx").version("1.8.0")
    library("androidx-lifecycle-runtimektx", "androidx.lifecycle", "lifecycle-runtime-ktx").version("2.5.1")
    library("androidx-activity-compose", "androidx.activity", "activity-compose").version("1.5.1")
    library("androidx-junit", "androidx.test.ext", "junit").version("1.1.3")
    library("androidx-espresso", "androidx.test.espresso", "espresso-core").version("3.4.0")
}

fun VersionCatalogBuilder.compose() {
    val compose = version("compose", "1.2.0")

    val navigation = version("navigation", "2.5.1")

    library("compose-navigation", "androidx.navigation", "navigation-compose").versionRef(navigation)

    library("compose-icons", "androidx.compose.material", "material-icons-extended").versionRef(compose)
    library("compose-ui-core", "androidx.compose.ui", "ui").versionRef(compose)
    library("compose-ui-toolingpreview", "androidx.compose.ui", "ui-tooling-preview").versionRef(compose)
    library("compose-material3", "androidx.compose.material3", "material3").version("1.0.0-alpha15")
    library("compose-junit", "androidx.compose.ui", "ui-test-junit4").versionRef(compose)
    library("compose-ui-tooling", "androidx.compose.ui", "ui-tooling").versionRef(compose)
    library("compose-ui-testmanifest", "androidx.compose.ui", "ui-test-manifest").versionRef(compose)
}

fun VersionCatalogBuilder.firebase() {
    library("firebase-bom", "com.google.firebase", "firebase-bom").version("30.3.2")
    library("firebase-analytics", "com.google.firebase", "firebase-analytics-ktx").version("")
    library("firebase-crashlytics", "com.google.firebase", "firebase-crashlytics-ktx").version("")
}

fun VersionCatalogBuilder.hilt() {
    val hilt = version("hilt", "2.43.2")

    library("hilt-android", "com.google.dagger", "hilt-android").versionRef(hilt)
    library("hilt-compiler", "com.google.dagger", "hilt-compiler").versionRef(hilt)
    library("hilt-navigation", "androidx.hilt", "hilt-navigation-compose").version("1.0.0")
}

fun VersionCatalogBuilder.kotlinx() {
    library("kotlinx-serialization", "org.jetbrains.kotlinx", "kotlinx-serialization-json").version("1.4.0-RC")
}

fun VersionCatalogBuilder.ktor() {
    val ktor = version("ktor", "2.0.3")

    library("ktor-core", "io.ktor", "ktor-client-core").versionRef(ktor)
    library("ktor-cio", "io.ktor", "ktor-client-cio").versionRef(ktor)

    library("ktor-content-negotiation", "io.ktor", "ktor-client-content-negotiation").versionRef(ktor)
    library("ktor-serialization", "io.ktor", "ktor-serialization-kotlinx-json").versionRef(ktor)

    library("ktor-logging", "io.ktor", "ktor-client-logging").versionRef(ktor)

    bundle("ktor", listOf("ktor-core", "ktor-cio", "ktor-content-negotiation", "ktor-serialization", "ktor-logging"))
}