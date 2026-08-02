package ru.tomilo.lib.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.tomilo.lib.mobile.data.download.BatchDownloadState
import ru.tomilo.lib.mobile.data.download.DownloadStage
import ru.tomilo.lib.mobile.ui.theme.TomiloDanger
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadProgressSheet(
    state: BatchDownloadState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    /** Свернуть sheet, оставив загрузку в фоне (уведомление). */
    onContinueInBackground: () -> Unit = onDismiss,
) {
    if (state.items.isEmpty()) return
    ModalBottomSheet(
        onDismissRequest = {
            // Свайп вниз — не отменяет, а уходит в фон
            if (state.finished) onDismiss()
            else onContinueInBackground()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                if (state.finished) "Загрузка завершена" else "Скачивание офлайн",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (state.titleName.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(state.titleName, color = TomiloMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))

            val pct = (state.overallFraction * 100).toInt().coerceIn(0, 100)
            Text(
                if (state.finished) {
                    buildString {
                        append("${state.completedCount} из ${state.items.size} глав")
                        if (state.failedCount > 0) append(" · ошибок: ${state.failedCount}")
                    }
                } else {
                    val n = (state.activeIndex + 1).coerceAtLeast(1)
                    "Глава $n из ${state.items.size} · $pct%"
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            state.activeItem?.takeIf { !state.finished }?.let { active ->
                Spacer(Modifier.height(4.dp))
                Text(
                    active.chapterLabel + " — " + active.stageLabel,
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (!state.finished) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Можно свернуть приложение — загрузка продолжится в фоне (уведомление).",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { state.overallFraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.height(260.dp)) {
                items(state.items, key = { it.chapterId }) { item ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(item.chapterLabel, modifier = Modifier.weight(1f))
                            Text(
                                item.stageLabel,
                                color = when (item.stage) {
                                    DownloadStage.Failed -> TomiloDanger
                                    DownloadStage.Completed -> MaterialTheme.colorScheme.primary
                                    DownloadStage.DownloadingPages,
                                    DownloadStage.FetchingChapter,
                                    DownloadStage.CheckingAccess,
                                    DownloadStage.Saving,
                                    -> MaterialTheme.colorScheme.primary
                                    else -> TomiloMuted
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (item.stage == DownloadStage.DownloadingPages ||
                            item.stage == DownloadStage.FetchingChapter ||
                            item.stage == DownloadStage.CheckingAccess ||
                            item.stage == DownloadStage.Saving
                        ) {
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { item.fraction.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (item.stage == DownloadStage.DownloadingPages && item.pagesTotal > 0) {
                                Text(
                                    "${item.pagesDone} / ${item.pagesTotal} страниц",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TomiloMuted,
                                )
                            }
                        }
                        item.message?.takeIf {
                            item.stage == DownloadStage.Failed && it.isNotBlank()
                        }?.let { msg ->
                            Text(msg, color = TomiloDanger, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (!state.finished) {
                Button(
                    onClick = onContinueInBackground,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Свернуть — качать в фоне")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Отменить загрузку")
                }
            } else {
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Закрыть")
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
