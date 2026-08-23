package ru.tomilo.lib.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.data.api.CommentDto
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder

private val fallbackCommentReactions = listOf("👍", "👎", "❤️", "🔥", "😂", "😮", "😢", "🎉", "👏")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommentsSection(
    entityType: String,
    entityId: String,
    socialRepository: SocialRepository,
    isLoggedIn: Boolean,
    onLoginRequired: () -> Unit,
    onOpenUser: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var comments by remember { mutableStateOf<List<CommentDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var posting by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var reactionEmojis by remember { mutableStateOf(fallbackCommentReactions) }
    var reactionPickerComment by remember { mutableStateOf<CommentDto?>(null) }
    var pendingReactionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(entityId, entityType, reload) {
        loading = comments.isEmpty()
        error = null
        socialRepository.comments(entityType, entityId)
            .onSuccess { comments = it }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(Unit) {
        socialRepository.commentReactionEmojis().onSuccess { available ->
            if (available.isNotEmpty()) reactionEmojis = available
        }
    }

    fun react(comment: CommentDto, emoji: String) {
        if (!isLoggedIn) {
            onLoginRequired()
            return
        }
        val id = comment.stableId()
        if (id.isBlank() || id in pendingReactionIds) return
        pendingReactionIds = pendingReactionIds + id
        reactionPickerComment = null
        scope.launch {
            socialRepository.toggleCommentReaction(id, emoji)
                .onSuccess { reload += 1 }
                .onFailure { error = it.message }
            pendingReactionIds = pendingReactionIds - id
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Комментарии", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (!loading) StatusPill("${comments.size}")
        }
        Spacer(Modifier.height(8.dp))

        if (isLoggedIn) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Написать комментарий…") },
                minLines = 2,
                shape = RoundedCornerShape(18.dp),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (draft.isBlank() || posting) return@Button
                    scope.launch {
                        posting = true
                        socialRepository.postComment(entityType, entityId, draft)
                            .onSuccess {
                                draft = ""
                                reload += 1
                            }
                            .onFailure { error = it.message }
                        posting = false
                    }
                },
                enabled = !posting && draft.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (posting) "Отправка…" else "Отправить")
            }
        } else {
            Text(
                "Войдите, чтобы комментировать",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onLoginRequired)
                    .padding(vertical = 8.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        if (error != null && comments.isNotEmpty()) {
            Text(
                error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        when {
            loading -> Text("Загрузка…", color = TomiloMuted)
            error != null && comments.isEmpty() -> Text(error ?: "", color = TomiloMuted)
            comments.isEmpty() -> Text("Пока нет комментариев", color = TomiloMuted)
            else -> comments.forEach { c ->
                CommentThread(
                    comment = c,
                    onOpenUser = onOpenUser,
                    pendingReactionIds = pendingReactionIds,
                    onReaction = { comment, emoji -> react(comment, emoji) },
                    onAddReaction = { comment ->
                        if (isLoggedIn) reactionPickerComment = comment else onLoginRequired()
                    },
                )
            }
        }
    }

    reactionPickerComment?.let { comment ->
        AlertDialog(
            onDismissRequest = { reactionPickerComment = null },
            title = { Text("Добавить реакцию") },
            text = {
                FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                    reactionEmojis.forEach { emoji ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .clickable { react(comment, emoji) },
                        ) {
                            Text(emoji, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { reactionPickerComment = null }) { Text("Закрыть") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommentThread(
    comment: CommentDto,
    onOpenUser: (String) -> Unit,
    pendingReactionIds: Set<String>,
    onReaction: (CommentDto, String) -> Unit,
    onAddReaction: (CommentDto) -> Unit,
    level: Int = 0,
) {
    CommentItem(
        comment = comment,
        onOpenUser = onOpenUser,
        level = level,
        reactionPending = comment.stableId() in pendingReactionIds,
        onReaction = { emoji -> onReaction(comment, emoji) },
        onAddReaction = { onAddReaction(comment) },
    )
    comment.replies.orEmpty().forEach { reply ->
        CommentThread(
            comment = reply,
            onOpenUser = onOpenUser,
            pendingReactionIds = pendingReactionIds,
            onReaction = onReaction,
            onAddReaction = onAddReaction,
            level = level + 1,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommentItem(
    comment: CommentDto,
    onOpenUser: (String) -> Unit,
    level: Int,
    reactionPending: Boolean,
    onReaction: (String) -> Unit,
    onAddReaction: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = (level.coerceAtMost(3) * 20).dp, bottom = 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TomiloSurface2.copy(alpha = 0.62f))
            .border(1.dp, TomiloBorder.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        DecoratedAvatar(
            avatarUrl = comment.authorAvatar(),
            username = comment.authorName(),
            decorations = comment.authorDecorations(),
            size = 40.dp,
            modifier = Modifier.clickable(enabled = comment.authorId().isNotBlank()) {
                    onOpenUser(comment.authorId())
                },
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                comment.authorName(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(enabled = comment.authorId().isNotBlank()) {
                    onOpenUser(comment.authorId())
                },
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (comment.isSpoiler == true) "⚠ Спойлер: ${comment.content.orEmpty()}"
                else comment.content.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                comment.createdAt?.take(10)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = TomiloMuted)
                }
                Spacer(Modifier.weight(1f))
                if (reactionPending) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onAddReaction, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.AddReaction, "Добавить реакцию", modifier = Modifier.size(19.dp))
                    }
                }
            }
            val reactions = comment.reactionCounts()
            if (reactions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    reactions.forEach { reaction ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                            modifier = Modifier
                                .padding(bottom = 5.dp)
                                .clickable(enabled = !reactionPending) { onReaction(reaction.emoji) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(reaction.emoji)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    reaction.resolvedCount().toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
