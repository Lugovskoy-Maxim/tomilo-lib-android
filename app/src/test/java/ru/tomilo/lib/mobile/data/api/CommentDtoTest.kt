package ru.tomilo.lib.mobile.data.api

import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Test

class CommentDtoTest {
    @Test
    fun currentApiPayloadResolvesPopulatedAuthorAndEmojiReactions() {
        val payload = """
            {
              "_id": "6a35102a9d8d05bd08f7274d",
              "userId": {
                "_id": "69ee670414e6d2264d2bedb9",
                "username": "Lord_of_the_ban",
                "avatar": "/avatars/user.webp",
                "equippedDecorations": { "frame": "/frames/gold.webp" }
              },
              "content": "Комментарий",
              "likes": 0,
              "dislikes": 0,
              "myReactions": ["❤️"],
              "isEdited": true,
              "reactions": [
                { "emoji": "❤️", "count": 4 },
                { "emoji": "👍", "count": 1 }
              ]
            }
        """.trimIndent()

        val comment = NetworkModule.json.decodeFromString<CommentDto>(payload)

        assertEquals("Lord_of_the_ban", comment.authorName())
        assertEquals("69ee670414e6d2264d2bedb9", comment.authorId())
        assertEquals("/avatars/user.webp", comment.authorAvatar())
        assertEquals(listOf("❤️", "👍"), comment.reactionCounts().map { it.emoji })
        assertEquals(4, comment.heartCount())
        assertEquals(true, comment.likedHeart())
        assertEquals(listOf("👍"), comment.extraReactions().map { it.emoji })
        assertEquals(true, comment.isEdited)
    }

    @Test
    fun isEditedDefaultsAbsent() {
        val payload = """{ "_id": "c1", "content": "ok" }"""
        val comment = NetworkModule.json.decodeFromString<CommentDto>(payload)
        assertEquals(null, comment.isEdited)
    }

    @Test
    fun stringUserIdAndLegacyLikesRemainCompatible() {
        val payload = """
            {
              "_id": "comment-id",
              "userId": "user-id",
              "likes": 3,
              "dislikesCount": 2
            }
        """.trimIndent()

        val comment = NetworkModule.json.decodeFromString<CommentDto>(payload)

        assertEquals("user-id", comment.authorId())
        assertEquals(listOf("👍", "👎"), comment.reactionCounts().map { it.emoji })
        assertEquals(listOf(3, 2), comment.reactionCounts().map { it.resolvedCount() })
    }
}
