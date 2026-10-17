package com.example.soundbox.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soundbox.data.Pad
import com.example.soundbox.ui.components.AudioPreviewPlayer
import com.example.soundbox.ui.theme.SoundBoxColors
import java.io.File
import kotlin.math.absoluteValue
import kotlin.math.max

@Composable
fun PadEditScreen(
    pad: Pad?,
    audioFile: File?,
    onBack: () -> Unit,
    onSave: (Pad) -> Unit,
    onDelete: (String) -> Unit,
    shortcutOptions: List<String>,
    onReplaceAudio: (Uri) -> Unit
) {
    if (pad == null) {
        Box(modifier = Modifier.fillMaxSize().background(SoundBoxColors.Background))
        return
    }

    val duration = pad.audio?.durationMs?.takeIf { it > 0 } ?: 1L
    var name by rememberSaveable(pad.id) { mutableStateOf(pad.name) }
    var key by rememberSaveable(pad.id) { mutableStateOf(pad.key.orEmpty()) }
    var shortcutMenuOpen by remember { mutableStateOf(false) }
    var volume by remember(pad.id) { mutableFloatStateOf(pad.volume) }
    var speed by remember(pad.id) { mutableFloatStateOf(pad.speed) }
    var startMs by remember(pad.id) { mutableFloatStateOf(pad.startMs.toFloat()) }
    var endMs by remember(pad.id) { mutableFloatStateOf((pad.endMs ?: duration).toFloat()) }
    val inPreview = LocalInspectionMode.current
    val replaceLauncher = if (inPreview) null else rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onReplaceAudio)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoundBoxColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(SoundBoxColors.TopBar)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = SoundBoxColors.TextPrimary)
            }
            Text(
                text = "按键编辑",
                modifier = Modifier.weight(1f),
                color = SoundBoxColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = {
                onSave(
                    pad.copy(
                        name = name.trim().ifBlank { "未命名按键" },
                        key = key.trim().uppercase().take(1).takeIf { it.isNotBlank() },
                        volume = volume,
                        speed = speed,
                        startMs = startMs.toLong().coerceAtLeast(0L),
                        endMs = endMs.toLong().coerceAtLeast(startMs.toLong() + 1L)
                    )
                )
            }) {
                Text("保存", color = SoundBoxColors.Accent)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("键名") }
            )

            Box {
                OutlinedTextField(
                    value = key,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    singleLine = true,
                    label = { Text("按键排序") },
                    placeholder = { Text("点击选择排序") },
                    trailingIcon = {
                        TextButton(onClick = { shortcutMenuOpen = true }) {
                            Text("选择")
                        }
                    }
                )
                DropdownMenu(
                    expanded = shortcutMenuOpen,
                    onDismissRequest = { shortcutMenuOpen = false }
                ) {

                    shortcutOptions.distinct().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                key = option
                                shortcutMenuOpen = false
                            }
                        )
                    }
                }
            }

            Text(
                text = "音频",
                color = SoundBoxColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                text = pad.audio?.displayName ?: "未绑定音频",
                color = SoundBoxColors.TextSecondary
            )

            if (pad.audio != null) {
                WaveformEditor(
                    seed = pad.audio.id,
                    startFraction = (startMs / duration.toFloat()).coerceIn(0f, 1f),
                    endFraction = (endMs / duration.toFloat()).coerceIn(0f, 1f)
                )

                Text(
                    text = "${formatMs(startMs.toLong())} - ${formatMs(endMs.toLong())} / ${formatMs(duration)}",
                    color = SoundBoxColors.TextSecondary,
                    fontSize = 13.sp
                )

                Column {
                    Text("开始位置", color = SoundBoxColors.TextSecondary, fontSize = 13.sp)
                    Slider(
                        value = startMs.coerceIn(0f, max(0f, endMs - 1f)),
                        onValueChange = { startMs = it.coerceIn(0f, endMs - 1f) },
                        valueRange = 0f..duration.toFloat()
                    )
                }

                Column {
                    Text("结束位置", color = SoundBoxColors.TextSecondary, fontSize = 13.sp)
                    Slider(
                        value = endMs.coerceIn(startMs + 1f, duration.toFloat()),
                        onValueChange = { endMs = it.coerceIn(startMs + 1f, duration.toFloat()) },
                        valueRange = 0f..duration.toFloat()
                    )
                }
            }

            Column {
                Text(
                    text = "音量 ${(volume * 100).toInt()}%",
                    color = SoundBoxColors.TextSecondary,
                    fontSize = 13.sp
                )
                Slider(value = volume, onValueChange = { volume = it }, valueRange = 0f..1f)
            }

            Column {
                Text(
                    text = "播放速度 ${"%.2f".format(speed)}x",
                    color = SoundBoxColors.TextSecondary,
                    fontSize = 13.sp
                )
                Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..2f)
            }

            if (pad.audio != null && !inPreview) {
                AudioPreviewPlayer(
                    file = audioFile,
                    startMs = startMs.toLong(),
                    endMs = endMs.toLong(),
                    volume = volume,
                    speed = speed
                )
            }

            OutlinedButton(
                onClick = { replaceLauncher?.launch(arrayOf("audio/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (pad.audio == null) "导入音频" else "替换音频")
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = { onDelete(pad.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("删除按键", color = SoundBoxColors.Danger)
            }
        }
    }
}
@Composable
private fun WaveformEditor(
    seed: String,
    startFraction: Float,
    endFraction: Float
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(SoundBoxColors.Card)
    ) {
        val barCount = 72
        val gap = size.width / barCount
        val barWidth = max(1f, gap * 0.55f)
        var hash = seed.hashCode().absoluteValue
        repeat(barCount) { index ->
            hash = hash * 1664525 + 1013904223
            val normalized = ((hash ushr 8) % 1000) / 1000f
            val barHeight = size.height * (0.12f + normalized * 0.72f)
            val fraction = index / barCount.toFloat()
            val selected = fraction in startFraction..endFraction
            drawRect(
                color = if (selected) SoundBoxColors.Accent else SoundBoxColors.Border,
                topLeft = Offset(index * gap, (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

private fun formatMs(value: Long): String {
    val totalSeconds = (value / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}
