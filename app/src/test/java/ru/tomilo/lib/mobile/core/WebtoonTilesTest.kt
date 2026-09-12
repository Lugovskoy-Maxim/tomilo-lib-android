package ru.tomilo.lib.mobile.core

import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.tomilo.lib.mobile.data.api.ChapterDto
import ru.tomilo.lib.mobile.data.api.NetworkModule

class WebtoonTilesTest {
    @Test
    fun tallPageIsSplitWithoutGapsOrCropping() {
        val dimensions = PageDimensions(width = 1200, height = 10100)
        val tiles = WebtoonTiles.split(dimensions)

        assertEquals(listOf(4096, 4096, 1908), tiles.map { it.height })
        assertEquals(listOf(0, 4096, 8192), tiles.map { it.top })
        assertEquals(dimensions.height, tiles.sumOf { it.height })
        assertTrue(tiles.zipWithNext().all { (left, right) -> left.top + left.height == right.top })
    }

    @Test
    fun resizedImageMapsTilesWithoutOverlapOrGap() {
        val claimed = PageDimensions(width = 1800, height = 10_000)
        val tiles = WebtoonTiles.split(claimed)
        val sourceWidth = 1600
        val sourceHeight = 8888
        val rects = tiles.map { WebtoonTiles.mapTileToSource(it, claimed, sourceWidth, sourceHeight) }

        assertEquals(0, rects.first().top)
        assertEquals(sourceHeight, rects.last().bottom)
        assertTrue(rects.zipWithNext().all { (left, right) -> left.bottom == right.top })
        assertEquals(sourceHeight, rects.sumOf { it.height })
        assertTrue(rects.all { it.left == 0 && it.right == sourceWidth })
    }

    @Test
    fun matchingSourceKeepsOriginalTileBounds() {
        val claimed = PageDimensions(width = 1200, height = 10_100)
        val tiles = WebtoonTiles.split(claimed)
        val last = WebtoonTiles.mapTileToSource(tiles.last(), claimed, 1200, 10_100)
        assertEquals(tiles.last().top, last.top)
        assertEquals(10_100, last.bottom)
        assertEquals(1200, last.right)
    }

    @Test
    fun tallerFileMapsTilesWithoutOverlapOrGap() {
        val claimed = PageDimensions(width = 1200, height = 8_000)
        val tiles = WebtoonTiles.split(claimed)
        val rects = tiles.map { WebtoonTiles.mapTileToSource(it, claimed, 1200, 12_000) }

        assertEquals(0, rects.first().top)
        assertEquals(12_000, rects.last().bottom)
        assertTrue(rects.zipWithNext().all { (left, right) -> left.bottom == right.top })
        assertEquals(12_000, rects.sumOf { it.height })
    }

    @Test
    fun measuredFileSplitStartsOnTileBoundary() {
        val measured = PageDimensions(width = 1600, height = 8_888)
        val tiles = WebtoonTiles.split(measured)
        val rects = tiles.map {
            WebtoonTiles.mapTileToSource(it, measured, measured.width, measured.height)
        }

        assertEquals(listOf(0, 4096, 8192), tiles.map { it.top })
        assertTrue(tiles.zipWithNext().all { (left, right) -> left.top + left.height == right.top })
        assertEquals(tiles.map { it.top }, rects.map { it.top })
        assertEquals(measured.height, rects.last().bottom)
    }

    @Test
    fun chapterReadsPageDimensionsFromCurrentApi() {
        val chapter = NetworkModule.json.decodeFromString<ChapterDto>(
            """
            {
              "_id": "chapter",
              "pages": ["https://cdn.tomilo-lib.ru/page.webp"],
              "pageDimensions": [{"width":1200,"height":10100,"_id":"dimension"}]
            }
            """.trimIndent(),
        )

        assertEquals(PageDimensions(1200, 10100), chapter.pageDimensions?.single())
    }
}
