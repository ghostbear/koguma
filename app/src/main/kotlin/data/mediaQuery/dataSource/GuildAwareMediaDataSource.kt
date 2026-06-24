package me.ghostbear.koguma.data.mediaQuery.dataSource

import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.currentCoroutineContext
import me.ghostbear.koguma.domain.mediaQuery.dataSource.MediaDataSource
import me.ghostbear.koguma.domain.mediaQuery.model.MediaQuery
import me.ghostbear.koguma.domain.mediaQuery.model.MediaResult
import me.ghostbear.koguma.domain.mediaQuery.model.MediaType
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

data class GuildIdContext(
    val guildId: Snowflake?
) : AbstractCoroutineContextElement(GuildIdContext) {
    companion object : CoroutineContext.Key<GuildIdContext>
}

suspend fun currentGuildIdOrNull(): Snowflake? {
    val coroutineContext = currentCoroutineContext()
    return coroutineContext[GuildIdContext]?.guildId
}

class GuildAwareMediaDataSource(
    preference: GuildPreference,
    dataSources: List<MediaDataSource>,
) : MediaDataSource {


    val defaultDataSource: MediaDataSource
    val dataSources: Map<Snowflake, MediaDataSource>

    init {
        val map = dataSources
            .associateBy { it::class }
        this.defaultDataSource = map[preference.defaultDataSource] ?: throw IllegalStateException("Default data source called ${preference.defaultDataSource} is not found in data sources: ${dataSources.map { it::class }}")
        this.dataSources = preference.dataSource
            .mapNotNull { (key, value) ->
                val dataSource = map[value] ?: throw IllegalStateException("Data source called $value is not found in data sources: ${dataSources.map { it::class }}")
                key to dataSource
            }
            .toMap()
    }

    override fun isSupported(type: MediaType): Boolean {
        return false
    }

    override suspend fun query(mediaQuery: MediaQuery): MediaResult {
        val guildId = currentGuildIdOrNull()

        return dataSources.getOrDefault(guildId, defaultDataSource).query(mediaQuery)
    }

    override suspend fun query(vararg mediaQuery: MediaQuery): List<MediaResult> {
        val guildId = currentGuildIdOrNull()

        return dataSources.getOrDefault(guildId, defaultDataSource).query(*mediaQuery)
    }


}