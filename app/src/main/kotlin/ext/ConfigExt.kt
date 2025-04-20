package me.ghostbear.koguma.ext

import com.typesafe.config.Config
import com.typesafe.config.ConfigException

fun <T> Config.safely(block: Config.() -> T): T? {
    return try {
        block(this)
    } catch (e: ConfigException) {
        null
    }
}
