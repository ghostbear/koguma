import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.sentry.jvm)
    alias(libs.plugins.apollo)
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
    implementation(libs.uri)
    implementation(libs.kord.core)
    implementation(libs.logback.classic)
    implementation(libs.caffeine)
    implementation(libs.config)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.apollo.runtime)
    implementation(libs.apollo.normalized.cache)
    implementation(libs.apollo.ktor.engine)
    testImplementation(libs.kotlin.test.junit5)
}

sentry {
    includeSourceContext = true
    includeDependenciesReport = true
    autoInstallation {
        enabled = true
    }
}

tasks.named("generateSentryBundleIdJava") {
    mustRunAfter(tasks.named("generateAniListApolloSources"))
}

application {
    mainClass = "me.ghostbear.koguma.ApplicationKt"
}


apollo {
    service("aniList") {
        packageName.set("me.ghostbear.koguma.data.mediaQuery.aniList")
        codegenModels.set("responseBased")

        introspection {
            endpointUrl.set("https://graphql.anilist.co/")
            schemaFile.set(file("src/main/graphql/schema.graphqls"))
        }
    }
}