package me.ghostbear.koguma.data.mediaQuery.dataSource

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueType
import dev.kord.common.entity.Snowflake
import me.ghostbear.koguma.data.mediaQueryAnilist.dataSource.AniListMediaDataSource
import me.ghostbear.koguma.domain.mediaQuery.dataSource.MediaDataSource
import me.ghostbear.koguma.ext.safely
import kotlin.reflect.KClass

class GuildPreference(config: Config) {

    val defaultDataSource: KClass<out MediaDataSource> = config.safely {
        val defaultClass = getString("koguma.data-source.guild.default")
        val clazz = Class.forName(defaultClass).kotlin
        clazz as KClass<out MediaDataSource>
    } ?: AniListMediaDataSource::class

    val dataSource: Map<Snowflake, KClass<out MediaDataSource>> = config.safely {
        val config = getStringList("koguma.data-source.guild.config")
        config
            .map { it.split("=") }
            .associate { (key, value) ->
                val className = value
                val clazz = Class.forName(className)
                Snowflake(key) to clazz.kotlin as KClass<out MediaDataSource>
            }
    } ?: emptyMap()

}