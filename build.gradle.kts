import org.jlleitschuh.gradle.ktlint.KtlintPlugin

buildscript {
    dependencies {
        classpath(libs.google.services)
        classpath(libs.firebase.crashlytics.gradle)
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint)
}

allprojects {
    apply<KtlintPlugin>()
}

subprojects {
    plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper> {
        configure<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension> {
            sourceSets.all {
                languageSettings {
                    optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                    optIn("kotlinx.serialization.ExperimentalSerializationApi")
                }
            }
        }
    }
}
