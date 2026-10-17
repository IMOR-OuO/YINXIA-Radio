package com.example.soundbox

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.soundbox.data.ShortcutKeys
import com.example.soundbox.R
import com.example.soundbox.ui.AppScreen
import com.example.soundbox.ui.SoundBoxViewModel
import com.example.soundbox.ui.screens.AboutScreen
import com.example.soundbox.ui.screens.BoxManagerScreen
import com.example.soundbox.ui.screens.HomeScreen
import com.example.soundbox.ui.screens.MusicBoxEditScreen
import com.example.soundbox.ui.screens.PadEditScreen
import com.example.soundbox.ui.theme.SoundBoxColors
import com.example.soundbox.ui.theme.SoundBoxTheme

class MainActivity : ComponentActivity() {

    private val soundBoxViewModel: SoundBoxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoundBoxTheme {
                SoundBoxApp(soundBoxViewModel)
            }
        }
    }

    override fun onPause() {
        soundBoxViewModel.flushSettings()
        super.onPause()
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (
            event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0 &&
            !event.isCtrlPressed &&
            !event.isAltPressed &&
            !event.isMetaPressed &&
            soundBoxViewModel.uiState.value.screen == AppScreen.Home
        ) {
            val unicode = event.unicodeChar
            if (unicode != 0) {
                val text = String(Character.toChars(unicode))
                val letter = text.firstOrNull()?.takeIf { it.isLetter() }
                if (letter != null) {
                    soundBoxViewModel.playByKey(letter.uppercase())
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun SoundBoxApp(viewModel: SoundBoxViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.importAudio(uris)
    }

    LocalView.current.keepScreenOn = state.settings.keepScreenOn

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    BackHandler(enabled = state.screen != AppScreen.Home) {
        viewModel.goBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoundBoxColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AppBackground(viewModel.backgroundFile(), state.settings.backgroundOpacity, state.settings.backgroundApplied)
        when (val screen = state.screen) {
            AppScreen.Home -> HomeScreen(
                state = state,
                onOpenManager = { viewModel.navigate(AppScreen.BoxManager) },
                onEditBox = { viewModel.navigate(AppScreen.BoxEdit) },
                onAddAudio = { audioPicker.launch(arrayOf("audio/*")) },
                onPlayPad = viewModel::playPad,
                onEditPad = { viewModel.navigate(AppScreen.PadEdit(it.id)) },
                onStopAll = viewModel::stopAll,
                onKeyPad = viewModel::playByKey
            )
            AppScreen.BoxManager -> BoxManagerScreen(
                state = state,
                onBack = viewModel::goBack,
                onSelect = { viewModel.selectBox(it) },
                onCreate = viewModel::createBox,
                onRename = viewModel::renameBox,
                onDuplicate = viewModel::duplicateBox,
                onDelete = viewModel::deleteBox,
                onImportArchive = viewModel::importBox,
                onExportArchive = viewModel::exportBoxTo,
                onOpenAbout = { viewModel.navigate(AppScreen.About) },
                onEditBox = { id ->
                    viewModel.selectBox(id, navigateHome = false)
                    viewModel.navigate(AppScreen.BoxEdit)
                }
            )

            AppScreen.BoxEdit -> {
                val box = state.currentBox
                MusicBoxEditScreen(
                    box = box,
                    onBack = viewModel::goBack,
                    onSave = { updatedBox ->
                        viewModel.replaceBox(updatedBox)
                        viewModel.goBack()
                    },
                    onImportAudio = { viewModel.importAudio(it) },
                    onPlayPad = viewModel::playPad,
                    onEditPad = { viewModel.navigate(AppScreen.PadEdit(it.id)) },
                    onExportArchive = { uri -> box?.let { viewModel.exportBoxTo(it.id, uri) } }
                )
            }

            is AppScreen.PadEdit -> {
                val pad = state.currentBox?.pads?.firstOrNull { it.id == screen.padId }
                PadEditScreen(
                    pad = pad,
                    audioFile = pad?.audio?.let { viewModel.audioFile(it.fileName) },
                    onBack = viewModel::goBack,
                    onSave = {
                        viewModel.updatePadAndReorder(it)
                        viewModel.goBack()
                    },
                    onDelete = { viewModel.deletePad(it, navigateBack = true) },
                    shortcutOptions = ShortcutKeys.options(state.currentBox?.pads?.size ?: 0),
                    onReplaceAudio = { pad?.let { current -> viewModel.importAudio(listOf(it), replacePadId = current.id) } }
                )
            }

            AppScreen.About -> AboutScreen(
                settings = state.settings,
                backgroundFile = viewModel.backgroundFile(),
                onBack = viewModel::goBack,
                onSettingsChange = viewModel::saveSettings,
                onSelectBackground = viewModel::selectBackground,
                onRestoreDefaultBackground = viewModel::restoreDefaultBackground
            )
        }

        if (state.busyMessage != null) {
            LinearProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
@Composable
private fun AppBackground(file: File?, opacity: Float, applied: Boolean) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = if (applied) file?.absolutePath else null, key2 = if (applied) file?.lastModified() else 0L) {
        value = if (applied && file?.exists() == true) {
            withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull()
            }
        } else {
            null
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(opacity.coerceIn(0f, 1f)),
            contentScale = ContentScale.Crop
        )
    }
}