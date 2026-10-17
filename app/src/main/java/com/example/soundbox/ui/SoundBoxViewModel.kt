package com.example.soundbox.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.soundbox.SoundBoxApplication
import com.example.soundbox.data.AppSettings
import com.example.soundbox.data.MusicBox
import com.example.soundbox.data.Pad
import com.example.soundbox.data.ShortcutKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SoundBoxViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SoundBoxApplication
    private val repository = app.repository
    private val audioEngine = app.audioEngine
    private val backStack = ArrayDeque<AppScreen>()
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        loadData()
        viewModelScope.launch {
            audioEngine.activeVoices.collect { voices ->
                _uiState.update { it.copy(activeVoices = voices) }
            }
        }
    }

    fun navigate(screen: AppScreen) {
        val current = _uiState.value.screen
        if (current != screen) {
            backStack.addLast(current)
            _uiState.update { it.copy(screen = screen, message = null) }
        }
    }

    fun goBack() {
        val previous = backStack.removeLastOrNull() ?: AppScreen.Home
        _uiState.update { it.copy(screen = previous, message = null) }
    }

    fun selectBox(boxId: String, navigateHome: Boolean = false) {
        _uiState.update { it.copy(currentBoxId = boxId) }
        prepareCurrentBox()
        if (navigateHome) navigate(AppScreen.Home)
    }

    fun createBox(name: String = "我的音乐盒") {
        val box = MusicBox(name = name.trim().ifBlank { "我的音乐盒" })
        repository.saveBox(box)
        _uiState.update { state -> state.copy(boxes = listOf(box) + state.boxes, currentBoxId = box.id) }
        prepareCurrentBox()
        navigate(AppScreen.BoxEdit)
    }

    fun updateBoxLayout(boxId: String, preferredColumns: Int?) {
        updateBoxById(boxId) {
            it.copy(preferredColumns = preferredColumns, updatedAt = System.currentTimeMillis())
        }
    }

    fun replaceBox(updatedBox: MusicBox) {
        updateBoxById(updatedBox.id) {
            updatedBox.copy(
                pads = updatedBox.pads.mapIndexed { index, pad ->
                    pad.copy(key = ShortcutKeys.forIndex(index))
                },
                updatedAt = System.currentTimeMillis()
            )
        }
    }
    fun renameBox(boxId: String, name: String) {
        val cleanName = name.trim().ifBlank { "未命名音乐盒" }
        updateBoxById(boxId) { it.copy(name = cleanName, updatedAt = System.currentTimeMillis()) }
    }

    fun duplicateBox(boxId: String) {
        val source = _uiState.value.boxes.firstOrNull { it.id == boxId } ?: return
        val copy = repository.duplicateBox(source, "${source.name} 副本")
        _uiState.update { it.copy(boxes = listOf(copy) + it.boxes, currentBoxId = copy.id) }
        prepareCurrentBox()
        navigate(AppScreen.Home)
    }

    fun deleteBox(boxId: String) {
        val remaining = repository.deleteBox(boxId)
        val nextId = remaining.firstOrNull()?.id
        _uiState.update { it.copy(boxes = remaining, currentBoxId = nextId ?: it.currentBoxId) }
        if (remaining.isEmpty()) createBox()
        prepareCurrentBox()
        navigate(AppScreen.Home)
    }

    fun updatePadAndReorder(updatedPad: Pad) {
        updateCurrentBox { box ->
            val oldIndex = box.pads.indexOfFirst { it.id == updatedPad.id }
            val optionIndex = ShortcutKeys.options(box.pads.size).indexOf(updatedPad.key)
            val targetIndex = optionIndex.takeIf { it >= 0 } ?: oldIndex
            if (oldIndex < 0) return@updateCurrentBox box
            val reordered = box.pads.filterNot { it.id == updatedPad.id }.toMutableList()
            reordered.add(targetIndex.coerceIn(0, reordered.size), updatedPad)
            box.copy(
                pads = reordered.mapIndexed { index, pad -> pad.copy(key = ShortcutKeys.forIndex(index)) },
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun updatePad(updatedPad: Pad) {
        updateCurrentBox { box ->
            box.copy(
                pads = box.pads.map { pad -> if (pad.id == updatedPad.id) updatedPad else pad },
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun deletePad(padId: String, navigateBack: Boolean = false) {
        updateCurrentBox { box ->
            box.copy(
                pads = box.pads
                    .filterNot { it.id == padId }
                    .mapIndexed { index, pad -> pad.copy(key = ShortcutKeys.forIndex(index)) },
                updatedAt = System.currentTimeMillis()
            )
        }
        if (navigateBack) goBack()
    }

    fun saveSettings(settings: AppSettings) {
        _uiState.update { it.copy(settings = settings) }
        repository.saveSettings(settings)
    }

    fun flushSettings() {
        repository.saveSettings(_uiState.value.settings)
    }
    fun selectBackground(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repository.importBackground(uri) }
            }.onSuccess { fileName ->
                val updated = _uiState.value.settings.copy(backgroundFileName = fileName, backgroundApplied = false)
                repository.saveSettings(updated)
                _uiState.update { it.copy(settings = updated, message = "背景已保存") }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.message ?: "背景设置失败") }
            }
        }
    }

    fun restoreDefaultBackground() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.restoreDefaultBackground() }
            val updated = _uiState.value.settings.copy(backgroundFileName = null, backgroundApplied = false)
            repository.saveSettings(updated)
            _uiState.update { it.copy(settings = updated, message = "已恢复默认背景") }
        }
    }

    fun backgroundFile(): File? = repository.backgroundFile(_uiState.value.settings.backgroundFileName)
    fun importAudio(uris: List<Uri>, replacePadId: String? = null) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyMessage = "正在导入音频…", message = null) }
            runCatching {
                withContext(Dispatchers.IO) { uris.map { uri -> repository.importAudio(uri) } }
            }.onSuccess { results ->
                if (replacePadId != null) {
                    val first = results.firstOrNull() ?: return@onSuccess
                    updateCurrentBox { box ->
                        box.copy(
                            pads = box.pads.map { pad ->
                                if (pad.id == replacePadId) {
                                    pad.copy(name = first.suggestedPadName, audio = first.asset, startMs = 0L, endMs = null)
                                } else {
                                    pad
                                }
                            },
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                } else {
                    val startIndex = _uiState.value.currentBox?.pads?.size ?: 0
                    val newPads = results.mapIndexed { index, result ->
                        Pad(
                            name = result.suggestedPadName,
                            key = ShortcutKeys.forIndex(startIndex + index),
                            audio = result.asset
                        )
                    }
                    updateCurrentBox { box ->
                        box.copy(pads = box.pads + newPads, updatedAt = System.currentTimeMillis())
                    }
                }
                _uiState.update { it.copy(message = "已导入 ${results.size} 个音频") }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.message ?: "音频导入失败") }
            }
            _uiState.update { it.copy(busyMessage = null) }
        }
    }

    fun exportBoxTo(boxId: String, outputUri: Uri) {
        val box = _uiState.value.boxes.firstOrNull { it.id == boxId } ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(busyMessage = "正在导出音乐盒…", message = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val output = app.contentResolver.openOutputStream(outputUri)
                        ?: error("无法创建导出文件")
                    output.use { repository.exportBox(box, it) }
                }
            }.onSuccess {
                _uiState.update { it.copy(message = "音乐盒已导出") }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.message ?: "导出失败") }
            }
            _uiState.update { it.copy(busyMessage = null) }
        }
    }

    fun importBox(inputUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(busyMessage = "正在导入音乐盒…", message = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val input = app.contentResolver.openInputStream(inputUri)
                        ?: error("无法读取导入文件")
                    input.use { repository.importBox(it) }
                }
            }.onSuccess { imported ->
                _uiState.update { state ->
                    state.copy(boxes = listOf(imported) + state.boxes, currentBoxId = imported.id)
                }
                prepareCurrentBox()
                navigate(AppScreen.Home)
                _uiState.update { it.copy(message = "音乐盒导入完成") }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.message ?: "音乐盒导入失败") }
            }
            _uiState.update { it.copy(busyMessage = null) }
        }
    }

    fun playPad(pad: Pad) {
        val state = _uiState.value
        audioEngine.play(
            pad = pad,
            globalVolume = state.settings.globalVolume,
            interruptAudio = state.settings.interruptAudio,
            preventRetrigger = state.settings.preventRetrigger
        )
    }

    fun playPadById(padId: String) {
        _uiState.value.currentBox?.pads?.firstOrNull { it.id == padId }?.let(::playPad)
    }

    fun playByKey(key: String) {
        val normalized = key.uppercase()
        _uiState.value.currentBox?.pads?.firstOrNull { it.key?.uppercase() == normalized }?.let(::playPad)
    }

    fun stopPad(padId: String) {
        audioEngine.stopPad(padId)
    }

    fun stopAll() {
        audioEngine.stopAll()
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
    fun audioFile(fileName: String): File = File(repository.audioDirectory(), fileName)

    private fun loadData() {
        val loaded = repository.loadBoxes().map(::ensureShortcutKeys)
        val boxes = if (loaded.isEmpty()) {
            listOf(MusicBox(name = "我的音乐盒").also(repository::saveBox))
        } else {
            loaded
        }
        _uiState.value = AppUiState(
            isLoading = false,
            boxes = boxes,
            currentBoxId = boxes.first().id,
            settings = repository.loadSettings()
        )
        prepareCurrentBox()
    }

    private fun ensureShortcutKeys(box: MusicBox): MusicBox {
        var changed = false
        val pads = box.pads.mapIndexed { index, pad ->
            val expectedKey = ShortcutKeys.forIndex(index)
            if (pad.key != expectedKey) {
                changed = true
                pad.copy(key = expectedKey)
            } else {
                pad
            }
        }
        if (!changed) return box
        return box.copy(pads = pads, updatedAt = System.currentTimeMillis()).also(repository::saveBox)
    }
    private fun prepareCurrentBox() {
        val box = _uiState.value.currentBox ?: return
        val assets = box.pads.mapNotNull { it.audio }.distinctBy { it.id }
        audioEngine.prepare(repository.audioDirectory(), assets)
    }

    private fun updateCurrentBox(transform: (MusicBox) -> MusicBox) {
        val current = _uiState.value.currentBox ?: return
        val updated = transform(current)
        repository.saveBox(updated)
        _uiState.update { state ->
            state.copy(boxes = state.boxes.map { box -> if (box.id == updated.id) updated else box })
        }
        prepareCurrentBox()
    }

    private fun updateBoxById(boxId: String, transform: (MusicBox) -> MusicBox) {
        val current = _uiState.value.boxes.firstOrNull { it.id == boxId } ?: return
        val updated = transform(current)
        repository.saveBox(updated)
        _uiState.update { state ->
            state.copy(boxes = state.boxes.map { box -> if (box.id == updated.id) updated else box })
        }
        if (boxId == _uiState.value.currentBoxId) prepareCurrentBox()
    }
}