package ru.tomilo.lib.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.CommentDto
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder

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
    val scope = rememberCoroutineScope()

    LaunchedEffect(entityId, entityType, reload) {
        loading = true
        error = null
        socialRepository.comments(entityType, entityId)
            .onSuccess { comments = it }
            .onFailure { error = it.message }
        loading = false
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
                CommentItem(
                    comment = c,
                    onOpenUser = onOpenUser,
                    onLike = {
                        if (!isLoggedIn) {
                            onLoginRequired()
                        } else {
                            scope.launch {
                                socialRepository.likeComment(c.stableId())
                                    .onSuccess { reload += 1 }
                                    .onFailure { error = it.message }
                            }
                        }
                    },
                )
                c.replies.orEmpty().forEach { reply ->
                    CommentItem(
                        comment = reply,
                        onOpenUser = onOpenUser,
                        indent = true,
                        onLike = {
                            if (!isLoggedIn) {
                                onLoginRequired()
                            } else {
                                scope.launch {
                                    socialRepository.likeComment(reply.stableId())
                                        .onSuccess { reload += 1 }
                                        .onFailure { error = it.message }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: CommentDto,
    onOpenUser: (String) -> Unit,
    onLike: () -> Unit,
    indent: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 28.dp else 0.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TomiloSurface2.copy(alpha = 0.62f))
            .border(1.dp, TomiloBorder.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AsyncImage(
            model = MediaUrl.resolve(comment.authorAvatar()),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(TomiloSurface2)
                .clickable(enabled = comment.authorId().isNotBlank()) {
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
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(onClick = onLike) {
                    Icon(
                        Icons.Default.ThumbUp,
                        contentDescription = "Нравится",
                        tint = TomiloMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    (comment.likesCount ?: 0).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TomiloMuted,
                )
            }
        }
    }
}
