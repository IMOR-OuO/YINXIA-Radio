package com.example.soundbox.data

import java.util.UUID

data class AudioAsset(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val fileName: String,
    val durationMs: Long = 0L,
    val mimeType: String? = null
)

data class Pad(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val key: String? = null,
    val audio: AudioAsset? = null,
    val volume: Float = 1f,
    val speed: Float = 1f,
    val startMs: Long = 0L,
    val endMs: Long? = null,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L
) {
    val effectiveEndMs: Long
        get() = endMs ?: audio?.durationMs?.takeIf { it > 0 } ?: Long.MAX_VALUE
}

data class MusicBox(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val preferredColumns: Int? = null,
    val pads: List<Pad> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class AppSettings(
    val interruptAudio: Boolean = false,
    val globalVolume: Float = 1f,
    val keepScreenOn: Boolean = false,
    val preventRetrigger: Boolean = false,
    val backgroundFileName: String? = null,
    val backgroundOpacity: Float = 0.65f,
    val backgroundApplied: Boolean = false
)

data class AudioImportResult(
    val asset: AudioAsset,
    val suggestedPadName: String
)