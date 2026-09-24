package ru.tomilo.lib.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.ChatTime
import ru.tomilo.lib.mobile.core.userFacingError
import ru.tomilo.lib.mobile.data.api.CommentDto
import ru.tomilo.lib.mobile.data.api.UserDto
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloDanger
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSuccess
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloText

private val fallbackCommentReactions = listOf("👍", "👎", "❤️", "🔥", "😂", "😮", "😢", "🎉", "👏")
private val HeartActive = Color(0xFFEF4444)
private val CommentDivider = Color.White.copy(alpha = 0.065f)
private val ReplyConnector = TomiloPrimary.copy(alpha = 0.34f)
private val ReplySurface = TomiloPrimary.copy(alpha = 0.055f)
private val commentBody = TextStyle(
    fontSize = 15.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Normal,
    color = TomiloText,
)
private val sortTabs = listOf(
    "newest" to "Новые",
    "oldest" to "Старые",
    "popular" to "Популярные",
)

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
    currentUser: UserDto? = null,
    titleId: String? = null,
) {
    var loading by remember { mutableStateOf(true) }
    var comments by remember { mutableStateOf<List<CommentDto>>(emptyList()) }
    var commentsTotal by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var spoiler by remember { mutableStateOf(false) }
    var posting by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var sortOrder by remember { mutableStateOf("newest") }
    var replyingTo by remember { mutableStateOf<String?>(null) }
    var editingComment by remember { mutableStateOf<CommentDto?>(null) }
    var deleteTarget by remember { mutableStateOf<CommentDto?>(null) }
    var reportTarget by remember { mutableStateOf<CommentDto?>(null) }
    var reportDraft by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var reactionEmojis by remember { mutableStateOf(fallbackCommentReactions) }
    var reactionPickerComment by remember { mutableStateOf<CommentDto?>(null) }
    var pendingReactionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(entityId, entityType, reload, sortOrder) {
        loading = comments.isEmpty()
        error = null
        socialRepository.comments(entityType, entityId, sortOrder = sortOrder)
            .onSuccess {
                comments = it.comments.sortedComments(sortOrder)
                commentsTotal = it.total.coerceAtLeast(it.comments.totalCommentCount())
            }
            .onFailure { error = it.commentError("Не удалось загрузить комментарии.") }
        loading = false
    }

    LaunchedEffect(Unit) {
        socialRepository.commentReactionEmojis().onSuccess { available ->
            if (available.isNotEmpty()) reactionEmojis = available
        }
    }

    fun requireLogin(): Boolean {
        if (isLoggedIn) return true
        onLoginRequired()
        return false
    }

    fun react(comment: CommentDto, emoji: String) {
        if (!requireLogin()) return
        val id = comment.stableId()
        if (id.isBlank() || id in pendingReactionIds) return
        pendingReactionIds = pendingReactionIds + id
        reactionPickerComment = null
        scope.launch {
            socialRepository.toggleCommentReaction(id, emoji)
                .onSuccess { reload += 1 }
                .onFailure { error = it.commentError("Не удалось отправить реакцию.") }
            pendingReactionIds = pendingReactionIds - id
        }
    }

    fun post() {
        if (!requireLogin() || draft.isBlank() || posting) return
        val edit = editingComment
        scope.launch {
            posting = true
            val result = if (edit != null) {
                socialRepository.updateComment(edit.stableId(), draft, spoiler)
            } else {
                socialRepository.postComment(
                    entityType = entityType,
                    entityId = entityId,
                    content = draft,
                    parentId = replyingTo,
                    isSpoiler = spoiler,
                )
            }
            result
                .onSuccess {
                    draft = ""
                    spoiler = false
                    replyingTo = null
                    editingComment = null
                    focusManager.clearFocus(force = true)
                    keyboard?.hide()
                    reload += 1
                }
                .onFailure { error = it.commentError("Не удалось сохранить комментарий.") }
            posting = false
        }
    }

    fun startEdit(comment: CommentDto) {
        if (!requireLogin()) return
        editingComment = comment
        replyingTo = null
        draft = comment.content.orEmpty()
        spoiler = comment.isSpoiler == true
    }

    fun cancelComposer() {
        if (editingComment != null) {
            draft = ""
            spoiler = false
        }
        editingComment = null
        replyingTo = null
    }

    val canSend = !posting && draft.isNotBlank()
    val placeholder = when {
        editingComment != null -> "Редактировать комментарий…"
        replyingTo != null -> "Написать ответ…"
        else -> "Напишите комментарий…"
    }
    val sendLabel = when {
        posting && editingComment != null -> "Сохранение…"
        posting -> "Отправка…"
        editingComment != null -> "Сохранить"
        else -> "Отправить"
    }
    val composerHint = when {
        editingComment != null -> "Редактирование"
        replyingTo != null -> "Ответ на комментарий"
        else -> null
    }

    Column(modifier = modifier.fillMaxWidth().background(TomiloBg)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Комментарии",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )
            if (!loading && comments.isNotEmpty()) {
                Text(
                    " $commentsTotal",
                    color = TomiloMuted,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }

        HorizontalDivider(color = CommentDivider)

        if (isLoggedIn) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                if (composerHint != null) {
                    Row(
                        Modifier.padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(composerHint, color = TomiloMuted, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Отмена",
                            color = TomiloMuted,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { cancelComposer() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    DecoratedAvatar(
                        avatarUrl = currentUser?.avatar,
                        username = currentUser?.username,
                        decorations = currentUser?.decorations(),
                        size = 36.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 36.dp),
                            textStyle = commentBody,
                            cursorBrush = SolidColor(TomiloPrimary),
                            decorationBox = { inner ->
                                Box {
                                    if (draft.isEmpty()) {
                                        Text(placeholder, color = TomiloMuted, fontSize = 15.sp)
                                    }
                                    inner()
                                }
                            },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 10.dp),
                            color = TomiloBorder.copy(alpha = 0.7f),
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { spoiler = !spoiler }
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (spoiler) TomiloPrimary else TomiloMuted,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Спойлер",
                                    fontSize = 13.sp,
                                    color = if (spoiler) TomiloText else TomiloMuted,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                sendLabel,
                                color = TomiloBg,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(TomiloText.copy(alpha = if (canSend) 1f else 0.35f))
                                    .clickable(enabled = canSend, onClick = { post() })
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                "Войдите, чтобы оставить комментарий",
                color = TomiloMuted,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLoginRequired)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            sortTabs.forEach { (value, label) ->
                val selected = sortOrder == value
                Column(
                    Modifier
                        .wrapContentWidth()
                        .clickable { sortOrder = value }
                        .padding(top = 2.dp),
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) TomiloText else TomiloMuted,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Box(
                        Modifier
                            .height(2.dp)
                            .fillMaxWidth()
                            .background(if (selected) TomiloText else Color.Transparent),
                    )
                }
            }
        }

        if (notice != null) {
            Text(
                notice.orEmpty(),
                color = TomiloSuccess,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (error != null && comments.isNotEmpty()) {
            Text(
                error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        when {
            loading -> {
                HorizontalDivider(color = TomiloBorder.copy(alpha = 0.7f))
                CommentsSkeleton()
            }
            error != null && comments.isEmpty() -> Text(
                error.orEmpty(),
                color = TomiloMuted,
                modifier = Modifier.padding(16.dp),
            )
            comments.isEmpty() -> {
                HorizontalDivider(color = TomiloBorder.copy(alpha = 0.7f))
                Text(
                    "Пока нет комментариев. Будьте первым!",
                    color = TomiloMuted,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 40.dp),
                )
            }
            else -> comments.forEach { c ->
                CommentThread(
                    comment = c,
                    currentUserId = currentUser?.stableId(),
                    isStaff = currentUser?.isStaff() == true,
                    isLoggedIn = isLoggedIn,
                    onOpenUser = onOpenUser,
                    pendingReactionIds = pendingReactionIds,
                    onReaction = { comment, emoji -> react(comment, emoji) },
                    onAddReaction = { comment ->
                        if (requireLogin()) reactionPickerComment = comment
                    },
                    onReply = { comment ->
                        if (requireLogin()) {
                            editingComment = null
                            replyingTo = comment.stableId()
                        }
                    },
                    onEdit = { startEdit(it) },
                    onDelete = { comment ->
                        if (requireLogin()) deleteTarget = comment
                    },
                    onReport = { comment ->
                        if (requireLogin()) {
                            reportDraft = ""
                            reportTarget = comment
                        }
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    reactionEmojis.forEach { emoji ->
                        Text(
                            emoji,
                            fontSize = 22.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { react(comment, emoji) }
                                .padding(10.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { reactionPickerComment = null }) { Text("Закрыть") }
            },
        )
    }

    deleteTarget?.let { target ->
        val staffDeletingOther = currentUser?.isStaff() == true &&
            target.authorId() != currentUser?.stableId()
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Удалить комментарий") },
            text = {
                Text(
                    when {
                        staffDeletingOther && target.authorName().isNotBlank() ->
                            "Вы действуете как администратор. Удалить комментарий пользователя ${target.authorName()}?"
                        staffDeletingOther ->
                            "Вы действуете как администратор. Удалить комментарий другого пользователя?"
                        else -> "Вы уверены, что хотите удалить этот комментарий?"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = target.stableId()
                        deleteTarget = null
                        scope.launch {
                            posting = true
                            socialRepository.deleteComment(id)
                                .onSuccess { reload += 1 }
                                .onFailure { error = it.commentError("Не удалось удалить комментарий.") }
                            posting = false
                        }
                    },
                ) { Text("Удалить", color = TomiloDanger) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Отмена") }
            },
        )
    }

    reportTarget?.let { target ->
        val canSubmitReport = reportDraft.trim().length >= 10 && !posting
        AlertDialog(
            onDismissRequest = {
                reportTarget = null
                reportDraft = ""
            },
            title = { Text("Жалоба на комментарий") },
            text = {
                Column {
                    Text(
                        "Опишите нарушение: оскорбления, спойлеры без пометки, спам и т.д.",
                        color = TomiloMuted,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reportDraft,
                        onValueChange = { reportDraft = it.take(5000) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Не менее 10 символов…") },
                        minLines = 4,
                    )
                    Text(
                        "Минимум 10 символов · до 5000",
                        color = TomiloMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canSubmitReport,
                    onClick = {
                        val id = target.stableId()
                        val body = reportDraft
                        scope.launch {
                            posting = true
                            socialRepository.reportComment(id, body, titleId)
                                .onSuccess {
                                    reportTarget = null
                                    reportDraft = ""
                                    notice = "Жалоба отправлена. Модераторы рассмотрят её в ближайшее время."
                                    error = null
                                }
                                .onFailure { error = it.commentError("Не удалось отправить жалобу.") }
                            posting = false
                        }
                    },
                ) { Text(if (posting) "Отправка…" else "Отправить") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        reportTarget = null
                        reportDraft = ""
                    },
                ) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun CommentsSkeleton() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        repeat(3) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TomiloSurface2),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .width(96.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TomiloSurface2),
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TomiloSurface2),
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.6f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TomiloSurface2),
                    )
                }
            }
            HorizontalDivider(color = TomiloBorder.copy(alpha = 0.55f))
        }
    }
}

@Composable
private fun CommentThread(
    comment: CommentDto,
    currentUserId: String?,
    isStaff: Boolean,
    isLoggedIn: Boolean,
    onOpenUser: (String) -> Unit,
    pendingReactionIds: Set<String>,
    onReaction: (CommentDto, String) -> Unit,
    onAddReaction: (CommentDto) -> Unit,
    onReply: (CommentDto) -> Unit,
    onEdit: (CommentDto) -> Unit,
    onDelete: (CommentDto) -> Unit,
    onReport: (CommentDto) -> Unit,
    level: Int = 0,
) {
    CommentItem(
        comment = comment,
        currentUserId = currentUserId,
        isStaff = isStaff,
        isLoggedIn = isLoggedIn,
        onOpenUser = onOpenUser,
        level = level,
        reactionPending = comment.stableId() in pendingReactionIds,
        onReaction = { emoji -> onReaction(comment, emoji) },
        onAddReaction = { onAddReaction(comment) },
        onReply = { onReply(comment) },
        onEdit = { onEdit(comment) },
        onDelete = { onDelete(comment) },
        onReport = { onReport(comment) },
    )
    if (comment.replies.orEmpty().isNotEmpty()) {
        Box(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val x = 31.dp.toPx()
                    val inset = 12.dp.toPx()
                    drawLine(
                        ReplyConnector,
                        Offset(x, inset),
                        Offset(x, (size.height - inset).coerceAtLeast(inset)),
                        1.dp.toPx(),
                    )
                },
        ) {
            Column(Modifier.fillMaxWidth()) {
                comment.replies.orEmpty().forEach { reply ->
                    CommentThread(
                        comment = reply,
                        currentUserId = currentUserId,
                        isStaff = isStaff,
                        isLoggedIn = isLoggedIn,
                        onOpenUser = onOpenUser,
                        pendingReactionIds = pendingReactionIds,
                        onReaction = onReaction,
                        onAddReaction = onAddReaction,
                        onReply = onReply,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onReport = onReport,
                        level = (level + 1).coerceAtMost(2),
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: CommentDto,
    currentUserId: String?,
    isStaff: Boolean,
    isLoggedIn: Boolean,
    onOpenUser: (String) -> Unit,
    level: Int,
    reactionPending: Boolean,
    onReaction: (String) -> Unit,
    onAddReaction: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
) {
    val isReply = level > 0
    var spoilerRevealed by remember(comment.stableId()) { mutableStateOf(false) }
    var textExpanded by remember(comment.stableId()) { mutableStateOf(false) }
    var textOverflows by remember(comment.stableId()) { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val hasSpoiler = comment.isSpoiler == true
    val avatarSize = if (isReply) 32.dp else 36.dp
    val replyCount = comment.replyTotal()
    val heartCount = comment.heartCount()
    val liked = comment.likedHeart()
    val canOpenAuthor = comment.authorId().isNotBlank()
    val isOwner = !currentUserId.isNullOrBlank() && currentUserId == comment.authorId()
    val canEdit = isOwner
    val canDelete = isOwner || isStaff
    val canReport = isLoggedIn && !isOwner && comment.authorId().isNotBlank()
    val canOpenMenu = canEdit || canDelete || canReport

    Column(
        Modifier
            .fillMaxWidth()
            .then(if (isReply) Modifier.padding(start = 24.dp) else Modifier),
    ) {
        HorizontalDivider(
            modifier = if (isReply) Modifier.padding(start = 24.dp) else Modifier,
            color = CommentDivider,
        )
        if (comment.hiddenBySystem == true) {
            Text(
                "Комментарий скрыт системой модерации",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
            return@Column
        }
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (isReply) Modifier
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ReplySurface)
                    else Modifier,
                )
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            DecoratedAvatar(
                avatarUrl = comment.authorAvatar(),
                username = comment.authorName(),
                decorations = comment.authorDecorations(),
                size = avatarSize,
                modifier = Modifier.clickable(enabled = canOpenAuthor) {
                    onOpenUser(comment.authorId())
                },
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        comment.authorName(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TomiloText,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable(enabled = canOpenAuthor) {
                                onOpenUser(comment.authorId())
                            },
                    )
                    Spacer(Modifier.weight(1f))
                    ChatTime.threadAgo(comment.createdAt)?.let { ago ->
                        Text(
                            ago,
                            fontSize = 13.sp,
                            color = TomiloMuted,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    if (canOpenMenu) {
                        Box {
                            Icon(
                                Icons.Default.MoreHoriz,
                                contentDescription = "Меню",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { menuOpen = true }
                                    .padding(7.dp),
                                tint = TomiloMuted,
                            )
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                            ) {
                                if (canEdit) {
                                    DropdownMenuItem(
                                        text = { Text("Редактировать") },
                                        onClick = {
                                            menuOpen = false
                                            onEdit()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                        },
                                    )
                                }
                                if (canDelete) {
                                    DropdownMenuItem(
                                        text = { Text("Удалить", color = TomiloDanger) },
                                        onClick = {
                                            menuOpen = false
                                            onDelete()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = TomiloDanger,
                                            )
                                        },
                                    )
                                }
                                if (canReport) {
                                    DropdownMenuItem(
                                        text = { Text("Пожаловаться") },
                                        onClick = {
                                            menuOpen = false
                                            onReport()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Flag, contentDescription = null)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                if (comment.isEdited == true || hasSpoiler) {
                    Row(
                        Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (comment.isEdited == true) {
                            Text("изм.", fontSize = 12.sp, color = TomiloMuted)
                        }
                        if (comment.isEdited == true && hasSpoiler) {
                            Spacer(Modifier.width(8.dp))
                        }
                        if (hasSpoiler) {
                            Icon(
                                Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = TomiloMuted,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("спойлер", fontSize = 12.sp, color = TomiloMuted)
                        }
                    }
                }
                if (hasSpoiler && !spoilerRevealed) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 7.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(TomiloSurface2)
                            .border(1.dp, TomiloPrimary.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                            .clickable { spoilerRevealed = true }
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Комментарий может содержать спойлер.",
                            fontSize = 13.sp,
                            color = TomiloMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Нажмите, чтобы открыть",
                            fontSize = 13.sp,
                            color = TomiloText,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    Column(Modifier.padding(top = 2.dp)) {
                        Text(
                            comment.content.orEmpty(),
                            style = commentBody,
                            maxLines = if (textExpanded) Int.MAX_VALUE else 6,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { result ->
                                if (!textExpanded) textOverflows = result.hasVisualOverflow
                            },
                        )
                        if (textOverflows || textExpanded) {
                            Text(
                                if (textExpanded) "Свернуть" else "Показать полностью",
                                color = TomiloPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { textExpanded = !textExpanded }
                                    .padding(top = 4.dp, end = 8.dp, bottom = 2.dp),
                            )
                        }
                    }
                }
                Row(
                    Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (reactionPending) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        CommentAction(onClick = { onReaction(CommentDto.HEART_REACTION) }) {
                            Icon(
                                if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Нравится",
                                modifier = Modifier.size(18.dp),
                                tint = if (liked) HeartActive else TomiloMuted,
                            )
                            if (heartCount > 0) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    heartCount.toString(),
                                    fontSize = 13.sp,
                                    color = if (liked) HeartActive else TomiloMuted,
                                )
                            }
                        }
                    }
                    if (level < 2) {
                        CommentAction(onClick = onReply) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = "Ответить",
                                modifier = Modifier.size(18.dp),
                                tint = TomiloMuted,
                            )
                            if (replyCount > 0) {
                                Spacer(Modifier.width(4.dp))
                                Text(replyCount.toString(), fontSize = 13.sp, color = TomiloMuted)
                            }
                        }
                    }
                    comment.extraReactions().forEach { reaction ->
                        CommentAction(onClick = { onReaction(reaction.emoji) }) {
                            Text(reaction.emoji, fontSize = 15.sp)
                            if (reaction.resolvedCount() > 0) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    reaction.resolvedCount().toString(),
                                    fontSize = 13.sp,
                                    color = TomiloMuted,
                                )
                            }
                        }
                    }
                    CommentAction(onClick = onAddReaction) {
                        Icon(
                            Icons.Default.AddReaction,
                            contentDescription = "Добавить реакцию",
                            modifier = Modifier.size(18.dp),
                            tint = TomiloMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentAction(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

private fun List<CommentDto>.sortedComments(order: String): List<CommentDto> {
    val stamp = { c: CommentDto -> ChatTime.parseInstant(c.createdAt)?.toEpochMilli() ?: 0L }
    val score = { c: CommentDto -> c.reactionCounts().sumOf { it.resolvedCount() } }
    return when (order) {
        "oldest" -> sortedBy(stamp)
        "popular" -> sortedByDescending(score)
        else -> sortedByDescending(stamp)
    }
}

private fun List<CommentDto>.totalCommentCount(): Int = sumOf { comment ->
    1 + comment.replies.orEmpty().totalCommentCount()
}

private fun Throwable.commentError(fallback: String): String =
    userFacingError(message?.takeIf(String::isNotBlank) ?: fallback)
