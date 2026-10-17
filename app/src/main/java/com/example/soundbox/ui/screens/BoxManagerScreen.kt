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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soundbox.data.MusicBox
import com.example.soundbox.ui.AppUiState
import com.example.soundbox.ui.components.BottomMode
import com.example.soundbox.ui.components.BottomModeBar
import com.example.soundbox.ui.theme.SoundBoxColors

@Composable
fun BoxManagerScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onImportArchive: (Uri) -> Unit,
    onExportArchive: (String, Uri) -> Unit,
    onOpenAbout: () -> Unit,
    onEditBox: (String) -> Unit
) {
    var renameTarget by remember { mutableStateOf<MusicBox?>(null) }
    var deleteTarget by remember { mutableStateOf<MusicBox?>(null) }
    var createDialog by remember { mutableStateOf(false) }
    var exportTargetId by remember { mutableStateOf<String?>(null) }

    val inPreview = LocalInspectionMode.current
    val importLauncher = if (inPreview) null else rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportArchive)
    }
    val exportLauncher = if (inPreview) null else rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { onExportArchive(exportTargetId.orEmpty(), it) }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()

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
                text = "音乐盒管理",
                modifier = Modifier.weight(1f),
                color = SoundBoxColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { createDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新建音乐盒", tint = SoundBoxColors.TextPrimary)
            }
            TextButton(onClick = { importLauncher?.launch(arrayOf("application/zip", "application/octet-stream")) }) {
                Text("导入", color = SoundBoxColors.Accent)
            }
            IconButton(onClick = onOpenAbout) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(1.dp, SoundBoxColors.TextSecondary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        color = SoundBoxColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.boxes, key = { it.id }) { box ->
                BoxManagerCard(
                    box = box,
                    selected = box.id == state.currentBoxId,
                    onSelect = { onSelect(box.id) },
                    onRename = { renameTarget = box },
                    onDuplicate = { onDuplicate(box.id) },
                    onDelete = { deleteTarget = box },
                    onExport = {
                        exportTargetId = box.id
                        exportLauncher?.launch("${box.name}.soundbox")
                    },
                    onEdit = { onEditBox(box.id) }
                )
            }
        }

        BottomModeBar(
            selected = BottomMode.List,
            onHome = onBack,
            onList = {}
        )
    }

    if (createDialog) {
        NameDialog(
            title = "新建音乐盒",
            initialValue = "我的音乐盒",
            confirmLabel = "创建",
            onDismiss = { createDialog = false },
            onConfirm = { name ->
                createDialog = false
                onCreate(name)
            }
        )
    }

    renameTarget?.let { target ->
        NameDialog(
            title = "重命名音乐盒",
            initialValue = target.name,
            confirmLabel = "保存",
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                renameTarget = null
                onRename(target.id, name)
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除音乐盒？") },
            text = { Text("将删除「${target.name}」及其未被其他音乐盒使用的音频。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    onDelete(target.id)
                }) { Text("删除", color = SoundBoxColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}
@Composable
private fun BoxManagerCard(
    box: MusicBox,
    selected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (selected) 10.dp else 0.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = if (selected) SoundBoxColors.Accent else Color.Transparent,
                spotColor = if (selected) SoundBoxColors.Accent else Color.Transparent
            )
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) SoundBoxColors.CardPressed else SoundBoxColors.Card)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) SoundBoxColors.Accent else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(box.id) {
                detectTapGestures(
                    onTap = { onSelect() },
                    onDoubleTap = {
                        onSelect()
                        onEdit()
                    }
                )
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = box.name,
                color = SoundBoxColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${box.pads.size} 个音效",
                modifier = Modifier.padding(top = 4.dp),
                color = SoundBoxColors.TextSecondary,
                fontSize = 13.sp
            )
        }
        if (selected) {
            Text(
                text = "当前",
                color = SoundBoxColors.Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多操作", tint = SoundBoxColors.TextSecondary)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("重命名") }, onClick = { menuOpen = false; onRename() })
                DropdownMenuItem(text = { Text("复制") }, onClick = { menuOpen = false; onDuplicate() })
                DropdownMenuItem(text = { Text("导出") }, onClick = { menuOpen = false; onExport() })
                DropdownMenuItem(text = { Text("删除", color = SoundBoxColors.Danger) }, onClick = { menuOpen = false; onDelete() })
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("名称") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank()
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}