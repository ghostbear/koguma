package me.ghostbear.koguma.presentation.mediaQuery

import com.typesafe.config.Config
import me.ghostbear.koguma.domain.mediaQuery.model.MediaLink
import me.ghostbear.koguma.ext.safely

class MediaQueryPreference(
    config: Config
) {

    val availableMediaLinkIds: List<MediaLink.Id> = config.safely { getStringList("koguma.media-query.media-link-ids") }
        ?.map { MediaLink.Id.fromString(it) }
        ?: emptyList()

}