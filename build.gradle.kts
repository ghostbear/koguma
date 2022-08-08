buildscript {
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.43.2")
        classpath("com.google.gms:google-services:4.3.13")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.1")
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "7.3.0-beta05" apply false
    id("com.android.library") version "7.3.0-beta05" apply false
    kotlin("android") version "1.7.10" apply false
}
