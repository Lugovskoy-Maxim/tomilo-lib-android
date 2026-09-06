package ru.tomilo.lib.mobile.data.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadProgressTest {
    @Test fun activeSummaryDoesNotPrefixChapterWithCompletedCount() {
        val state = BatchDownloadState(
            items = listOf(ChapterDownloadProgress("one", "Глава 1")),
            activeIndex = 0,
        )
        assertTrue(state.statusSummary.startsWith("Глава 1 из 1"))
    }

    @Test fun cancelledBatchShowsUnfinishedWork() {
        val state = BatchDownloadState(
            items = listOf(
                ChapterDownloadProgress("one", "1", DownloadStage.Completed),
                ChapterDownloadProgress("two", "2", DownloadStage.Cancelled),
            ),
            finished = true,
        )
        assertEquals(1, state.cancelledCount)
        assertTrue(state.statusSummary.contains("остановлено 1"))
    }

    @Test fun stalePageProgressCannotExceedCompleteFraction() {
        val item = ChapterDownloadProgress("one", "1", DownloadStage.DownloadingPages, 15, 10)
        assertTrue(item.fraction in 0f..1f)
    }
}
