package ru.tomilo.lib.mobile.core

enum class ReaderLayout {
    WEBTOON,
    PAGER,
}

enum class ReaderDirection {
    LTR,
    RTL,
}

object ReaderMode {
    fun inferLayout(type: String?): ReaderLayout {
        val t = type?.lowercase().orEmpty()
        return when {
            t.contains("manhwa") || t.contains("manhua") || t.contains("webtoon") ->
                ReaderLayout.WEBTOON
            t.contains("manga") || t.contains("comic") || t.contains("комикс") ->
                ReaderLayout.PAGER
            else -> ReaderLayout.WEBTOON
        }
    }

    fun inferDirection(type: String?): ReaderDirection {
        val t = type?.lowercase().orEmpty()
        return if (t.contains("manga")) ReaderDirection.RTL else ReaderDirection.LTR
    }

    fun typeLabel(type: String?): String = GenreLabels.type(type)

    fun layoutLabel(layout: ReaderLayout): String = when (layout) {
        ReaderLayout.WEBTOON -> "Лента"
        ReaderLayout.PAGER -> "Страницы"
    }

    fun directionLabel(direction: ReaderDirection): String = when (direction) {
        ReaderDirection.LTR -> "Слева направо"
        ReaderDirection.RTL -> "Справа налево"
    }
}
