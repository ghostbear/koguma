package me.ghostbear.koguma.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.cache.normalized.api.CacheKey
import com.apollographql.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import com.apollographql.cache.normalized.normalizedCache
import com.apollographql.ktor.http.KtorHttpEngine
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import me.ghostbear.koguma.core.mediaQueryMatch.MediaQueryMatcher
import me.ghostbear.koguma.core.mediaQueryMatch.mediaQueryMatch.InterpreterMediaQueryMatcher
import me.ghostbear.koguma.data.mediaQuery.aniList.cache.Cache
import me.ghostbear.koguma.data.mediaQuery.dataSource.GuildAwareMediaDataSource
import me.ghostbear.koguma.data.mediaQuery.dataSource.GuildPreference
import me.ghostbear.koguma.data.mediaQueryAnilist.dataSource.AniListMediaDataSource
import me.ghostbear.koguma.data.mediaQueryMangabaka.dataSource.MangabakaMediaDataSource
import me.ghostbear.koguma.domain.mediaQuery.dataSource.MediaDataSource
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Duration.Companion.minutes

val dataModule = module {
    single<HttpClient> {
        HttpClient {
            expectSuccess = false
            install(ContentNegotiation) {
                json()
            }
        }
    }

    single {
        ApolloClient.Builder()
            .serverUrl("https://graphql.anilist.co/")
            .httpEngine(
                KtorHttpEngine(
                    HttpClient {
                        expectSuccess = false
                        install(HttpTimeout) {
                            connectTimeoutMillis = 1.minutes.inWholeMilliseconds
                            requestTimeoutMillis = 1.minutes.inWholeMilliseconds
                        }
                    }
                )
            )
            .normalizedCache(
                normalizedCacheFactory = MemoryCacheFactory(50 * 1024 * 1024, 5.minutes.inWholeMilliseconds),
                cacheKeyGenerator = TypePolicyCacheKeyGenerator(
                    typePolicies = Cache.typePolicies,
                    keyScope = CacheKey.Scope.SERVICE
                ),
                cacheResolver = FieldPolicyCacheResolver(
                    keyScope = CacheKey.Scope.SERVICE,
                    fieldPolicies = Cache.fieldPolicies
                )
            )
            .build()
    }

    singleOf(::GuildPreference)

    singleOf(::MangabakaMediaDataSource)
    singleOf(::AniListMediaDataSource)

    single<MediaDataSource> {
        GuildAwareMediaDataSource(
            get(),
            listOf(
                get<MangabakaMediaDataSource>(),
                get<AniListMediaDataSource>()
            )
        )
    }

    singleOf(::InterpreterMediaQueryMatcher) bind MediaQueryMatcher::class
}
