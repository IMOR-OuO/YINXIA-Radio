package com.example.soundbox.data

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import android.webkit.MimeTypeMap
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class SoundBoxRepository(private val context: Context) {

    private val rootDir = File(context.filesDir, "soundbox")
    private val boxesDir = File(rootDir, "boxes")
    private val audioDir = File(rootDir, "audio")
    private val settingsFile = File(rootDir, "settings.json")

    init {
        boxesDir.mkdirs()
        audioDir.mkdirs()
    }

    fun audioDirectory(): File = audioDir

    fun audioFile(asset: AudioAsset): File = File(audioDir, asset.fileName)

    fun loadBoxes(): List<MusicBox> {
        return boxesDir.listFiles { file -> file.isFile && file.extension == "json" }
            ?.mapNotNull { file ->
                runCatching { JsonCodec.boxFromJson(JSONObject(file.readText())) }.getOrNull()
            }
            ?.sortedByDescending { it.updatedAt }
            .orEmpty()
    }

    fun saveBox(box: MusicBox) {
        atomicWrite(File(boxesDir, "${box.id}.json"), JsonCodec.boxToJson(box).toString(2))
    }

    fun deleteBox(boxId: String): List<MusicBox> {
        File(boxesDir, "$boxId.json").delete()
        val remaining = loadBoxes()
        val referencedFiles = remaining.flatMap { box -> box.pads.mapNotNull { it.audio?.fileName } }.toSet()
        audioDir.listFiles()?.forEach { file ->
            if (file.name !in referencedFiles) file.delete()
        }
        return remaining
    }

    fun duplicateBox(source: MusicBox, newName: String): MusicBox {
        val copy = source.copy(
            id = UUID.randomUUID().toString(),
            name = newName,
            pads = source.pads.map { pad -> pad.copy(id = UUID.randomUUID().toString()) },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        saveBox(copy)
        return copy
    }

    fun backgroundFile(fileName: String?): File? {
        if (fileName.isNullOrBlank()) return null
        return File(rootDir, fileName).takeIf { it.exists() }
    }

    fun importBackground(uri: Uri): String {
        val destination = File(rootDir, "background_custom_${System.currentTimeMillis()}.img")
        val temp = File(rootDir, "background_custom.tmp")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().buffered().use { output -> input.copyTo(output) }
            } ?: throw IOException("无法读取所选图片")
            if (destination.exists()) destination.delete()
            if (!temp.renameTo(destination)) {
                temp.copyTo(destination, overwrite = true)
                temp.delete()
            }
            rootDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("background_custom_") && file.name != destination.name) {
                    file.delete()
                }
            }
            return destination.name
        } catch (error: Exception) {
            temp.delete()
            throw error
        }
    }

    fun restoreDefaultBackground() {
        rootDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("background_custom_")) file.delete()
        }
    }
    fun loadSettings(): AppSettings {
        if (!settingsFile.exists()) return AppSettings()
        return runCatching { JsonCodec.settingsFromJson(JSONObject(settingsFile.readText())) }
            .getOrDefault(AppSettings())
    }

    fun saveSettings(settings: AppSettings) {
        atomicWrite(settingsFile, JsonCodec.settingsToJson(settings).toString(2))
    }
    fun importAudio(uri: Uri): AudioImportResult {
        val resolver = context.contentResolver
        val displayName = queryDisplayName(resolver, uri) ?: "audio"
        val mimeType = resolver.getType(uri)
        val extension = resolveExtension(displayName, mimeType)
        val id = UUID.randomUUID().toString()
        val fileName = "$id.$extension"
        val destination = File(audioDir, fileName)
        val temp = File(audioDir, "$fileName.importing")

        try {
            resolver.openInputStream(uri)?.use { input ->
                temp.outputStream().buffered().use { output -> input.copyTo(output) }
            } ?: throw IOException("无法读取所选音频")

            val duration = readDuration(temp)
            if (duration <= 0L) throw IOException("无法识别音频内容")

            if (!temp.renameTo(destination)) {
                temp.copyTo(destination, overwrite = true)
                temp.delete()
            }

            return AudioImportResult(
                asset = AudioAsset(
                    id = id,
                    displayName = displayName.substringBeforeLast('.', displayName),
                    fileName = fileName,
                    durationMs = duration,
                    mimeType = mimeType
                ),
                suggestedPadName = displayName.substringBeforeLast('.', displayName).take(24)
            )
        } catch (error: Exception) {
            temp.delete()
            destination.delete()
            throw error
        }
    }

    fun exportBox(box: MusicBox, outputStream: OutputStream) {
        ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(JsonCodec.boxToJson(box).toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            box.pads.mapNotNull { it.audio }.distinctBy { it.fileName }.forEach { asset ->
                val source = audioFile(asset)
                if (!source.exists()) return@forEach
                zip.putNextEntry(ZipEntry("audio/${asset.fileName}"))
                source.inputStream().buffered().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }
    fun importBox(inputStream: InputStream): MusicBox {
        val staging = File(context.cacheDir, "soundbox-import-${UUID.randomUUID()}")
        staging.mkdirs()
        try {
            ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    if (entry.isDirectory) return@forEach
                    val safePath = entry.name.replace('\\', '/')
                    if (safePath.contains("..") || safePath.startsWith("/")) {
                        throw IOException("压缩包包含非法路径")
                    }
                    val output = File(staging, safePath)
                    output.parentFile?.mkdirs()
                    output.outputStream().buffered().use { zip.copyTo(it) }
                }
            }

            val manifest = File(staging, "manifest.json")
            if (!manifest.exists()) throw IOException("缺少 manifest.json")
            val imported = JsonCodec.boxFromJson(JSONObject(manifest.readText()))
            val now = System.currentTimeMillis()
            val newPads = imported.pads.map { pad ->
                val oldAudio = pad.audio
                if (oldAudio == null) {
                    pad.copy(id = UUID.randomUUID().toString())
                } else {
                    val source = File(staging, "audio/${oldAudio.fileName}")
                    if (!source.exists()) {
                        pad.copy(id = UUID.randomUUID().toString(), audio = null)
                    } else {
                        val newAudioId = UUID.randomUUID().toString()
                        val extension = source.extension.ifBlank { "audio" }
                        val newFileName = "$newAudioId.$extension"
                        source.copyTo(File(audioDir, newFileName), overwrite = true)
                        pad.copy(
                            id = UUID.randomUUID().toString(),
                            audio = oldAudio.copy(id = newAudioId, fileName = newFileName)
                        )
                    }
                }
            }
            val box = imported.copy(
                id = UUID.randomUUID().toString(),
                name = "${imported.name}（导入）",
                pads = newPads,
                createdAt = now,
                updatedAt = now
            )
            saveBox(box)
            return box
        } finally {
            staging.deleteRecursively()
        }
    }
    private fun readDuration(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        }
    }

    private fun resolveExtension(displayName: String, mimeType: String?): String {
        val fromName = displayName.substringAfterLast('.', "").lowercase()
        if (fromName in SUPPORTED_EXTENSIONS) return fromName
        val fromMime = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it)?.lowercase() }
        if (fromMime in SUPPORTED_EXTENSIONS) return fromMime!!
        return "mp3"
    }

    private fun atomicWrite(file: File, content: String) {
        val atomicFile = AtomicFile(file)
        val stream = atomicFile.startWrite()
        try {
            stream.write(content.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (error: Exception) {
            atomicFile.failWrite(stream)
            throw error
        }
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "opus")
    }
}