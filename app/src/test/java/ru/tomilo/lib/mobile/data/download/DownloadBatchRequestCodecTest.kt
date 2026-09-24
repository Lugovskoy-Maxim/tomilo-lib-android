package ru.tomilo.lib.mobile.data.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadBatchRequestCodecTest {
    @Test
    fun roundTripsWholeBatchAndChapterOrder() {
        val request = DownloadBatchRequest(
            titleId = "title-42",
            titleName = "История с длинным названием",
            titleSlug = "story-slug",
            titleCover = null,
            chapters = listOf(
                DownloadChapterRef("chapter-3", "Глава 3"),
                DownloadChapterRef("chapter-4", "Глава 4.5"),
            ),
        )

        assertEquals(request, DownloadBatchRequestCodec.decode(DownloadBatchRequestCodec.encode(request)))
    }

    @Test
    fun ignoresFieldsAddedByANewerAppVersion() {
        val olderCheckpoint = """
            {
              "titleId":"title-1",
              "titleName":"Манхва",
              "titleSlug":"slug",
              "titleCover":null,
              "chapters":[{"chapterId":"chapter-1","chapterLabel":"Глава 1"}],
              "checkpointVersion":2
            }
        """.trimIndent()

        assertEquals(
            DownloadBatchRequest(
                titleId = "title-1",
                titleName = "Манхва",
                titleSlug = "slug",
                titleCover = null,
                chapters = listOf(DownloadChapterRef("chapter-1", "Глава 1")),
            ),
            DownloadBatchRequestCodec.decode(olderCheckpoint),
        )
    }

    @Test
    fun rejectsMalformedOrIncompleteCheckpoint() {
        assertNull(DownloadBatchRequestCodec.decode("not-json"))
        assertNull(DownloadBatchRequestCodec.decode("{\"titleId\":\"title-1\"}"))
    }
}
