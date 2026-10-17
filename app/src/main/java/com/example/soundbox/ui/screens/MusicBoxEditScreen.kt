package com.example.soundbox.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soundbox.data.MusicBox
import com.example.soundbox.data.Pad
import com.example.soundbox.data.ShortcutKeys
import com.example.soundbox.ui.theme.SoundBoxColors

@Composable
fun MusicBoxEditScreen(
    box: MusicBox?,
    onBack: () -> Unit,
    onSave: (MusicBox) -> Unit,
    onImportAudio: (List<Uri>) -> Unit,
    onPlayPad: (Pad) -> Unit,
    onEditPad: (Pad) -> Unit,
    onExportArchive: (Uri) -> Unit
) {
    if (box == null) {
        Box(modifier = Modifier.fillMaxSize().background(SoundBoxColors.Background))
        return
    }
    var name by rememberSaveable(box.id) { mutableStateOf(box.name) }
    var preferredColumns by rememberSaveable(box.id) { mutableStateOf(box.preferredColumns) }
    var deletedPadIds by remember(box.id) { mutableStateOf(emptySet<String>()) }
    val visiblePads = box.pads.filterNot { it.id in deletedPadIds }
    val inPreview = LocalInspectionMode.current
    val importLauncher = if (inPreview) null else rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) onImportAudio(uris)
    }
    val exportLauncher = if (inPreview) null else rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(onExportArchive)
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
                text = "编辑音乐盒",
                modifier = Modifier.weight(1f),
                color = SoundBoxColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            TopBarActionButton(
                text = "导出",
                onClick = { exportLauncher?.launch("${box.name}.soundbox") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            TopBarActionButton(
                text = "保存",
                onClick = {
                    onSave(
                        box.copy(
                            name = name.trim().ifBlank { "未命名音乐盒" },
                            preferredColumns = preferredColumns,
                            pads = visiblePads.mapIndexed { index, pad ->
                                pad.copy(key = ShortcutKeys.forIndex(index))
                            },
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("音乐盒名称") }
            )
            Text(
                text = "布局",
                modifier = Modifier.padding(top = 14.dp),
                color = SoundBoxColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LayoutOption(
                    label = "2×X",
                    selected = preferredColumns == 2,
                    onClick = { preferredColumns = 2 },
                    modifier = Modifier.weight(1f)
                )
                LayoutOption(
                    label = "3×X",
                    selected = preferredColumns == 3,
                    onClick = { preferredColumns = 3 },
                    modifier = Modifier.weight(1f)
                )
                LayoutOption(
                    label = "5×X",
                    selected = preferredColumns == 5,
                    onClick = { preferredColumns = 5 },
                    modifier = Modifier.weight(1f)
                )
                LayoutOption(
                    label = "自适应",
                    selected = preferredColumns == null,
                    onClick = { preferredColumns = null },
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "单击播放，双击编辑按键",
                modifier = Modifier.padding(top = 10.dp),
                color = SoundBoxColors.TextSecondary,
                fontSize = 12.sp
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(visiblePads, key = { it.id }) { pad ->
                PadEditRow(
                    pad = pad,
                    onPreview = { onPlayPad(pad) },
                    onEdit = { onEditPad(pad) },
                    onDelete = { deletedPadIds = deletedPadIds + pad.id }
                )
            }
            item(key = "add-audio") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SoundBoxColors.Background)
                        .clickable { importLauncher?.launch(arrayOf("audio/*")) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = SoundBoxColors.Accent)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("导入音频", color = SoundBoxColors.Accent, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun PadEditRow(
    pad: Pad,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SoundBoxColors.Card)
            .pointerInput(pad.id) {
                detectTapGestures(
                    onTap = { onPreview() },
                    onDoubleTap = { onEdit() }
                )
            }
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pad.name,
                color = SoundBoxColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = buildString {
                    append(pad.key?.let { "快捷键 $it" } ?: "未绑定快捷键")
                    pad.audio?.let { append(" · ${it.displayName}") }
                },
                modifier = Modifier.padding(top = 3.dp),
                color = SoundBoxColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        IconButton(onClick = onPreview) {
            Icon(Icons.Default.PlayArrow, contentDescription = "试听", tint = SoundBoxColors.Accent)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "删除", tint = SoundBoxColors.Danger)
        }
    }
}
@Composable
private fun TopBarActionButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF333333))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
@Composable
private fun LayoutOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) SoundBoxColors.CardPressed else SoundBoxColors.Card)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) SoundBoxColors.Accent else SoundBoxColors.Border,
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) SoundBoxColors.Accent else SoundBoxColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}