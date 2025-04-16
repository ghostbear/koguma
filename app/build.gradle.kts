import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
    }
}


dependencies {
    implementation(libs.bundles.kotlinx)
    implementation("com.eygraber:uri-kmp:0.0.19")
    implementation(libs.kord.core)
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")
}

application {
    mainClass = "me.ghostbear.koguma.ApplicationKt"
}
