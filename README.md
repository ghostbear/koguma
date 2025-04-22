# koguma

A Discord bot for anime/manga/novel interactions.

Uses [AniList](https://anilist.co) to find anime/manga/light novels.

## Message interactions

Start a discussion or give a recommendation directly from chat messages.

| Type        | Syntax      | 
|-------------|-------------|
| Anime       | `{{query}}` |
| Manga       | `<<query>>` |
| Light Novel | `[[query]]` |

Updating a chat message will trigger searches.

## Slash command interactions

Use `/search type: query:` to search for an anime, manga, or light novel.

## Pagination

Both message and slash interactions support pagination.

For embeds, use the buttons to page forward or backward.

For list results, append `[page]` at the end of the message interaction syntax.

```text
Check out {{anime}}[2] and <<manga>>[3]
```

Remember that updating chat messages will trigger searches, which makes it easier to page through results for list results.