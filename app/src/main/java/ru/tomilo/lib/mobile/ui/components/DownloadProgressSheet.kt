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
) {
    if (state.items.isEmpty()) return
    ModalBottomSheet(
        onDismissRequest = {
            if (state.finished) onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Загрузка офлайн", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "${state.completedCount}/${state.items.size} готово" +
                    if (state.failedCount > 0) " · ошибок: ${state.failedCount}" else "",
                color = TomiloMuted,
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { state.overallFraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.height(280.dp)) {
                items(state.items, key = { it.chapterId }) { item ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(item.chapterLabel, modifier = Modifier.weight(1f))
                            Text(
                                item.stageLabel,
                                color = when (item.stage) {
                                    DownloadStage.Failed -> TomiloDanger
                                    DownloadStage.Completed -> MaterialTheme.colorScheme.primary
                                    else -> TomiloMuted
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (item.stage == DownloadStage.DownloadingPages ||
                            item.stage == DownloadStage.FetchingChapter ||
                            item.stage == DownloadStage.CheckingAccess
                        ) {
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { item.fraction.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (!state.finished) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Отменить")
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
