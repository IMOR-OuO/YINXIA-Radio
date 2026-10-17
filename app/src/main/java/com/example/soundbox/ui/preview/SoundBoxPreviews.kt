package com.example.soundbox.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.soundbox.data.AppSettings
import com.example.soundbox.data.AudioAsset
import com.example.soundbox.data.MusicBox
import com.example.soundbox.data.Pad
import com.example.soundbox.data.ShortcutKeys
import com.example.soundbox.ui.AppUiState
import com.example.soundbox.ui.screens.AboutScreen
import com.example.soundbox.ui.screens.BoxManagerScreen
import com.example.soundbox.ui.screens.HomeScreen
import com.example.soundbox.ui.screens.MusicBoxEditScreen
import com.example.soundbox.ui.screens.PadEditScreen
import com.example.soundbox.ui.theme.SoundBoxTheme

private val previewPads = listOf(
    Pad(
        id = "pad-ji",
        name = "鸡",
        key = "A",
        audio = AudioAsset(id = "audio-ji", displayName = "鸡", fileName = "ji.wav", durationMs = 1_200L)
    ),
    Pad(
        id = "pad-ni",
        name = "你",
        key = "S",
        audio = AudioAsset(id = "audio-ni", displayName = "你", fileName = "ni.wav", durationMs = 1_500L)
    ),
    Pad(
        id = "pad-tai",
        name = "太",
        key = "D",
        audio = AudioAsset(id = "audio-tai", displayName = "太", fileName = "tai.wav", durationMs = 1_000L)
    ),
    Pad(
        id = "pad-mei",
        name = "美",
        key = "F",
        audio = AudioAsset(id = "audio-mei", displayName = "美", fileName = "mei.wav", durationMs = 1_300L)
    ),
    Pad(
        id = "pad-drum",
        name = "鼓",
        key = "G",
        audio = AudioAsset(id = "audio-drum", displayName = "鼓", fileName = "drum.wav", durationMs = 800L)
    ),
    Pad(
        id = "pad-bass",
        name = "贝斯",
        key = "H",
        audio = AudioAsset(id = "audio-bass", displayName = "贝斯", fileName = "bass.wav", durationMs = 2_400L)
    )
)

private val previewBox = MusicBox(
    id = "preview-box",
    name = "鸡你太美",
    pads = previewPads
)

private val previewState = AppUiState(
    isLoading = false,
    boxes = listOf(previewBox),
    currentBoxId = previewBox.id,
    settings = AppSettings(
        interruptAudio = false,
        globalVolume = 0.82f,
        keepScreenOn = false
    ),
    activeVoices = mapOf(
        "pad-ji" to 1,
        "pad-drum" to 2
    )
)

@Preview(
    name = "首页 - 手机竖屏",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
    backgroundColor = 0xFF181818
)
@Composable
private fun HomePhonePreview() {
    SoundBoxTheme {
        HomeScreen(
            state = previewState,
            onOpenManager = {},
            onEditBox = {},
            onAddAudio = {},
            onPlayPad = {},
            onEditPad = {},
            onStopAll = {},
            onKeyPad = {}
        )
    }
}


@Preview(
    name = "音频列表 / 音乐盒管理",
    group = "SoundBox Pages",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
    backgroundColor = 0xFF181818
)
@Composable
private fun BoxManagerPreview() {
    SoundBoxTheme {
        BoxManagerScreen(
            state = previewState,
            onBack = {},
            onSelect = {},
            onCreate = {},
            onRename = { _, _ -> },
            onDuplicate = {},
            onDelete = {},
            onImportArchive = {},
            onExportArchive = { _, _ -> },
            onOpenAbout = {},
            onEditBox = {}
        )
    }
}

@Preview(
    name = "音乐盒编辑",
    group = "SoundBox Pages",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
    backgroundColor = 0xFF181818
)
@Composable
private fun MusicBoxEditPreview() {
    SoundBoxTheme {
        MusicBoxEditScreen(
            box = previewBox,
            onBack = {},
            onSave = {},
            onImportAudio = {},
            onPlayPad = {},
            onEditPad = {},
            onExportArchive = {}
        )
    }
}

@Preview(
    name = "单个按键编辑",
    group = "SoundBox Pages",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
    backgroundColor = 0xFF181818
)
@Composable
private fun PadEditPreview() {
    SoundBoxTheme {
                             PadEditScreen(
            pad = previewPads.first(),
            audioFile = null,
            onBack = {},
            onSave = {},
            onDelete = {},
            shortcutOptions = ShortcutKeys.options(previewPads.size),
            onReplaceAudio = {}
        )
    }
}

@Preview(
    name = "关于软件",
    group = "SoundBox Pages",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
    backgroundColor = 0xFF181818
)
@Composable
private fun AboutPreview() {
    SoundBoxTheme {
        AboutScreen(
            settings = previewState.settings,
            backgroundFile = null,
            onSettingsChange = {},
            onSelectBackground = {},
            onRestoreDefaultBackground = {},
            onBack = {}
        )
    }
}
