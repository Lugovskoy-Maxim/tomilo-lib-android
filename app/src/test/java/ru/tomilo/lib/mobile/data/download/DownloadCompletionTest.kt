package ru.tomilo.lib.mobile.data.download

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadCompletionTest {
    @Test
    fun failedAndCancelledChaptersRemainPendingForWorkerRetry() {
        val failed = progress(DownloadStage.Failed)
        val cancelled = progress(DownloadStage.Cancelled)

        assertTrue(BatchDownloadState(items = listOf(failed)).hasIncompleteChapters())
        assertTrue(BatchDownloadState(items = listOf(cancelled)).hasIncompleteChapters())
    }

    @Test
    fun onlyCompletedChaptersAllowWorkerSuccess() {
        val completed = progress(DownloadStage.Completed)

        assertFalse(BatchDownloadState(items = listOf(completed)).hasIncompleteChapters())
        assertFalse(BatchDownloadState().hasIncompleteChapters())
    }

    private fun progress(stage: DownloadStage) = ChapterDownloadProgress(
        chapterId = "chapter-1",
        chapterLabel = "Глава 1",
        stage = stage,
    )
}
