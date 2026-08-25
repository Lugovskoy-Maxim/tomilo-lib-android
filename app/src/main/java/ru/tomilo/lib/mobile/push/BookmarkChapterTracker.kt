package ru.tomilo.lib.mobile.push

internal data class BookmarkChapterSnapshot(
    val titleId: String,
    val titleName: String,
    val chapterCount: Int,
)

internal fun findBookmarkChapterUpdates(
    previous: Map<String, Int>,
    current: List<BookmarkChapterSnapshot>,
    alreadyDeliveredTitleIds: Set<String> = emptySet(),
): List<BookmarkChapterSnapshot> = current.filter { item ->
    val oldCount = previous[item.titleId]
    oldCount != null &&
        item.chapterCount > oldCount &&
        item.titleId !in alreadyDeliveredTitleIds
}
