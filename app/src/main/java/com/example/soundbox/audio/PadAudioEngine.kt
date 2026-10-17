package com.example.soundbox.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.example.soundbox.data.AudioAsset
import com.example.soundbox.data.Pad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

class PadAudioEngine(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sampleIdToAssetId = ConcurrentHashMap<Int, String>()
    private val assetIdToSampleId = ConcurrentHashMap<String, Int>()
    private val streamIds = ConcurrentHashMap<Int, PadPlayback>()
    private val streamJobs = ConcurrentHashMap<Int, Job>()
    private val pendingPads = ConcurrentHashMap<String, PendingPlayback>()
    private val trimmedPlayers = ConcurrentHashMap<Int, TrimmedPlayback>()
    private val trimmedJobs = ConcurrentHashMap<Int, Job>()
    private val nextTrimmedId = AtomicInteger(1)

    private val voiceCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val loadedState = MutableStateFlow(false)
    private var soundPool: SoundPool = createSoundPool()
    private var preparedSignature: String = ""
    private var audioDirectory: File? = null

    val activeVoices: StateFlow<Map<String, Int>> = voiceCounts.asStateFlow()
    val isLoaded: StateFlow<Boolean> = loadedState.asStateFlow()

    fun prepare(audioDirectory: File, assets: List<AudioAsset>) {
        val signature = assets.joinToString("|") { "${it.id}:${it.fileName}:${it.durationMs}" }
        if (signature == preparedSignature) return
        preparedSignature = signature
        this.audioDirectory = audioDirectory
        stopAll()
        soundPool.release()
        soundPool = createSoundPool()
        sampleIdToAssetId.clear()
        assetIdToSampleId.clear()
        loadedState.value = false

        assets.distinctBy { it.id }.forEach { asset ->
            val file = File(audioDirectory, asset.fileName)
            if (!file.exists()) return@forEach
            val sampleId = soundPool.load(file.absolutePath, 1)
            sampleIdToAssetId[sampleId] = asset.id
            assetIdToSampleId[asset.id] = sampleId
        }
        loadedState.value = true
    }

    fun play(pad: Pad, globalVolume: Float, interruptAudio: Boolean, preventRetrigger: Boolean) {
        if (interruptAudio) stopAll()
        if (preventRetrigger && (voiceCounts.value[pad.id] ?: 0) > 0) stopPad(pad.id)
        if (pad.startMs > 0L || pad.endMs != null) {
            playTrimmed(pad, globalVolume)
            return
        }
        val assetId = pad.audio?.id ?: return
        if (!assetIdToSampleId.containsKey(assetId)) {
            pendingPads[assetId] = PendingPlayback(pad, globalVolume)
            return
        }
        playSample(pad, assetId, globalVolume)
    }

    fun stopPad(padId: String) {
        streamIds.entries.filter { it.value.padId == padId }.map { it.key }.forEach(::stopStream)
        trimmedPlayers.entries.filter { it.value.padId == padId }.map { it.key }.forEach(::stopTrimmed)
    }

    fun stopAll() {
        streamIds.keys.toList().forEach(::stopStream)
        trimmedPlayers.keys.toList().forEach(::stopTrimmed)
        pendingPads.clear()
        voiceCounts.value = emptyMap()
    }

    fun release() {
        stopAll()
        soundPool.release()
        scope.coroutineContext[Job]?.cancel()
    }

    private fun createSoundPool(): SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()
        .also { pool ->
            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status != 0) return@setOnLoadCompleteListener
                val assetId = sampleIdToAssetId[sampleId] ?: return@setOnLoadCompleteListener
                pendingPads.remove(assetId)?.let { pending ->
                    playSample(pending.pad, assetId, pending.globalVolume)
                }
            }
        }
    private fun playSample(pad: Pad, assetId: String, globalVolume: Float) {
        val sampleId = assetIdToSampleId[assetId] ?: return
        val volume = (pad.volume * globalVolume).coerceIn(0f, 1f)
        val rate = pad.speed.coerceIn(0.5f, 2f)
        val streamId = soundPool.play(sampleId, volume, volume, 1, 0, rate)
        if (streamId == 0) return

        streamIds[streamId] = PadPlayback(pad.id)
        updateVoice(pad.id, 1)

        val sourceDuration = pad.audio?.durationMs?.takeIf { it > 0 } ?: DEFAULT_DURATION_MS
        val clipStart = pad.startMs.coerceIn(0L, max(0L, sourceDuration - 1))
        val clipEnd = (pad.endMs ?: sourceDuration).coerceIn(clipStart + 1, sourceDuration)
        val playbackDuration = ((clipEnd - clipStart) / rate).toLong().coerceAtLeast(80L)
        streamJobs[streamId] = scope.launch {
            delay(playbackDuration)
            stopStream(streamId)
        }
    }

    private fun stopStream(streamId: Int) {
        soundPool.stop(streamId)
        streamJobs.remove(streamId)?.cancel()
        val playback = streamIds.remove(streamId) ?: return
        updateVoice(playback.padId, -1)
    }

    private fun playTrimmed(pad: Pad, globalVolume: Float) {
        val asset = pad.audio ?: return
        val directory = audioDirectory ?: return
        val file = File(directory, asset.fileName)
        if (!file.exists()) return

        val volume = (pad.volume * globalVolume).coerceIn(0f, 1f)
        val rate = pad.speed.coerceIn(0.5f, 2f)
        val sourceDuration = asset.durationMs.takeIf { it > 0 } ?: DEFAULT_DURATION_MS
        val startMs = pad.startMs.coerceIn(0L, max(0L, sourceDuration - 1))
        val endMs = (pad.endMs ?: sourceDuration).coerceIn(startMs + 1, sourceDuration)
        val id = nextTrimmedId.getAndIncrement()
        val player = MediaPlayer()

        trimmedPlayers[id] = TrimmedPlayback(
            padId = pad.id,
            player = player,
            startMs = startMs,
            endMs = endMs,
            rate = rate
        )
        updateVoice(pad.id, 1)

        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            player.setDataSource(file.absolutePath)
            player.setVolume(volume, volume)
            player.setOnPreparedListener { mediaPlayer ->
                if (!trimmedPlayers.containsKey(id)) return@setOnPreparedListener
                runCatching {
                    mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(rate)
                }
                if (startMs > 0L) {
                    mediaPlayer.seekTo(startMs, MediaPlayer.SEEK_CLOSEST)
                } else {
                    mediaPlayer.start()
                    scheduleTrimmedStop(id)
                }
            }
            player.setOnSeekCompleteListener {
                if (trimmedPlayers.containsKey(id)) {
                    runCatching { player.start() }
                    scheduleTrimmedStop(id)
                }
            }
            player.setOnCompletionListener { stopTrimmed(id) }
            player.setOnErrorListener { _, _, _ ->
                stopTrimmed(id)
                true
            }
            player.prepareAsync()
        } catch (_: Exception) {
            stopTrimmed(id)
        }
    }

    private fun scheduleTrimmedStop(id: Int) {
        val playback = trimmedPlayers[id] ?: return
        trimmedJobs.remove(id)?.cancel()
        val duration = ((playback.endMs - playback.startMs) / playback.rate)
            .toLong()
            .coerceAtLeast(80L)
        trimmedJobs[id] = scope.launch {
            delay(duration)
            stopTrimmed(id)
        }
    }

    private fun stopTrimmed(id: Int) {
        val playback = trimmedPlayers.remove(id) ?: return
        trimmedJobs.remove(id)?.cancel()
        runCatching { playback.player.stop() }
        runCatching { playback.player.release() }
        updateVoice(playback.padId, -1)
    }
    private fun updateVoice(padId: String, delta: Int) {
        voiceCounts.value = buildMap {
            putAll(voiceCounts.value)
            val next = (get(padId) ?: 0) + delta
            if (next <= 0) remove(padId) else put(padId, next)
        }
    }

    private data class TrimmedPlayback(
        val padId: String,
        val player: MediaPlayer,
        val startMs: Long,
        val endMs: Long,
        val rate: Float
    )
    private data class PadPlayback(val padId: String)

    private data class PendingPlayback(
        val pad: Pad,
        val globalVolume: Float
    )

    companion object {
        private const val MAX_STREAMS = 32
        private const val DEFAULT_DURATION_MS = 1_000L
    }
}