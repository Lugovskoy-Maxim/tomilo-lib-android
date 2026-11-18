package ru.tomilo.lib.mobile.ui.components

import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import ru.tomilo.lib.mobile.R
import ru.tomilo.lib.mobile.core.MediaUrl

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun DecorationMedia(
    model: String?,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    onError: () -> Unit = {},
) {
    val url = model ?: return
    if (!url.substringBefore('#').substringBefore('?').endsWith(".webm", ignoreCase = true)) {
        AsyncImage(url, contentDescription, modifier, contentScale = contentScale, onError = { onError() })
        return
    }

    val context = LocalContext.current
    val reportError by rememberUpdatedState(onError)
    var player by remember(url) { mutableStateOf<ExoPlayer?>(null) }
    var failed by remember(url) { mutableStateOf(false) }
    LifecycleStartEffect(url) {
        val sources = MediaUrl.candidates(url).ifEmpty { listOf(url) }
        val http = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(5_000)
            .setReadTimeoutMs(8_000)
        val mediaFactory = DefaultMediaSourceFactory(DefaultDataSource.Factory(context, http))
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(0))
        val video = ExoPlayer.Builder(context).setMediaSourceFactory(mediaFactory).build()
        var sourceIndex = 0
        video.trackSelectionParameters = video.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
        video.repeatMode = Player.REPEAT_MODE_ONE
        video.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (sourceIndex < sources.lastIndex) {
                    sourceIndex++
                    video.setMediaItem(MediaItem.fromUri(sources[sourceIndex]))
                    video.prepare()
                } else {
                    failed = true
                    reportError()
                }
            }
        })
        video.setMediaItem(MediaItem.fromUri(sources.first()))
        video.playWhenReady = true
        video.prepare()
        player = video
        onStopOrDispose {
            player = null
            video.release()
        }
    }
    if (!failed) {
        AndroidView(
            factory = { LayoutInflater.from(it).inflate(R.layout.decoration_video, null) as PlayerView },
            modifier = modifier,
            update = {
                it.resizeMode = if (contentScale == ContentScale.Crop) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
                it.player = player
            },
            onRelease = { it.player = null },
        )
    }
}
