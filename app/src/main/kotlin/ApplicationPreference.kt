package me.ghostbear.koguma

import com.typesafe.config.Config
import me.ghostbear.koguma.ext.safely

class ApplicationPreference(
    config: Config
) {

    val token: String = config.safely { getString("koguma.token") } ?: error("Missing required configuration '$.koguma.token'")

}