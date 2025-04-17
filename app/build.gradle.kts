import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.graphql)
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
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.graphql.kotlin.ktor.client)
}

application {
    mainClass = "me.ghostbear.koguma.ApplicationKt"
}


graphql {
    client {
        endpoint = "https://graphql.anilist.co/"
        packageName = "com.example.generated"
        serializer = GraphQLSerializer.KOTLINX
    }
}
