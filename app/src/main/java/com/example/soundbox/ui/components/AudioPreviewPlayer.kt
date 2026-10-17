package com.example.soundbox.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import com.example.soundbox.ui.theme.SoundBoxColors

@Composable
fun AudioPreviewPlayer(
    file: File?,
    modifier: Modifier = Modifier,
    startMs: Long = 0L,
    endMs: Long? = null,
    volume: Float = 1f,
    speed: Float = 1f
) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    var playing by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(file?.absolutePath, startMs, endMs) {
        player.stop()
        if (file == null || !file.exists()) return@LaunchedEffect
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(file))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs.coerceAtLeast(0L))
                    .setEndPositionMs(endMs ?: C.TIME_END_OF_SOURCE)
                    .build()
            )
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    LaunchedEffect(volume) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    LaunchedEffect(speed) {
        player.setPlaybackSpeed(speed.coerceIn(0.5f, 2f))
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TextButton(
            onClick = {
                if (playing) {
                    player.pause()
                } else {
                    val duration = player.duration
                    val atEnd = player.playbackState == Player.STATE_ENDED ||
                        (duration != C.TIME_UNSET && player.currentPosition >= duration - 50L)
                    if (atEnd) {
                        player.seekTo(startMs.coerceAtLeast(0L))
                    }
                    player.play()
                }
            },
            enabled = file != null
        ) {
            Text(if (playing) "暂停" else "播放", color = SoundBoxColors.Accent)
        }
        TextButton(
            onClick = {
                player.pause()
                player.seekTo(0L)
            },
            enabled = file != null
        ) {
            Text("停止", color = SoundBoxColors.TextSecondary)
        }
    }
}