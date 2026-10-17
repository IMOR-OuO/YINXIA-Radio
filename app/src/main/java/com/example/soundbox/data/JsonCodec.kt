package com.example.soundbox.data

import org.json.JSONArray
import org.json.JSONObject

internal object JsonCodec {

    private const val SCHEMA_VERSION = 1

    fun boxToJson(box: MusicBox): JSONObject = JSONObject()
        .put("schemaVersion", SCHEMA_VERSION)
        .put("id", box.id)
        .put("name", box.name)
        .put("preferredColumns", box.preferredColumns ?: JSONObject.NULL)
        .put("createdAt", box.createdAt)
        .put("updatedAt", box.updatedAt)
        .put("pads", JSONArray().apply {
            box.pads.forEach { put(padToJson(it)) }
        })

    fun boxFromJson(json: JSONObject): MusicBox {
        val padsJson = json.optJSONArray("pads") ?: JSONArray()
        val pads = buildList {
            for (index in 0 until padsJson.length()) {
                padsJson.optJSONObject(index)?.let { add(padFromJson(it)) }
            }
        }
        return MusicBox(
            id = json.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
            name = json.optString("name").ifBlank { "未命名音乐盒" },
            preferredColumns = if (json.isNull("preferredColumns")) null else json.optInt("preferredColumns").takeIf { it > 0 },
            pads = pads,
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }

    fun settingsToJson(settings: AppSettings): JSONObject = JSONObject()
        .put("interruptAudio", settings.interruptAudio)
        .put("globalVolume", settings.globalVolume.toDouble())
        .put("keepScreenOn", settings.keepScreenOn)
        .put("preventRetrigger", settings.preventRetrigger)
        .put("backgroundFileName", settings.backgroundFileName ?: JSONObject.NULL)
        .put("backgroundOpacity", settings.backgroundOpacity.toDouble())
        .put("backgroundApplied", settings.backgroundApplied)

    fun settingsFromJson(json: JSONObject): AppSettings = AppSettings(
        interruptAudio = json.optBoolean("interruptAudio", false),
        globalVolume = json.optDouble("globalVolume", 1.0).toFloat().coerceIn(0f, 1f),
        keepScreenOn = json.optBoolean("keepScreenOn", false),
        preventRetrigger = json.optBoolean("preventRetrigger", false),
        backgroundFileName = if (json.isNull("backgroundFileName")) {
            null
        } else {
            json.optString("backgroundFileName").takeIf { it.isNotBlank() }
        },
        backgroundOpacity = json.optDouble("backgroundOpacity", 0.65).toFloat().coerceIn(0f, 1f),
        backgroundApplied = json.optBoolean("backgroundApplied", false)
    )

    private fun padToJson(pad: Pad): JSONObject = JSONObject()
        .put("id", pad.id)
        .put("name", pad.name)
        .put("key", pad.key ?: JSONObject.NULL)
        .put("audio", pad.audio?.let(::audioToJson) ?: JSONObject.NULL)
        .put("volume", pad.volume.toDouble())
        .put("speed", pad.speed.toDouble())
        .put("startMs", pad.startMs)
        .put("endMs", pad.endMs ?: JSONObject.NULL)
        .put("fadeInMs", pad.fadeInMs)
        .put("fadeOutMs", pad.fadeOutMs)

    private fun padFromJson(json: JSONObject): Pad = Pad(
        id = json.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
        name = json.optString("name").ifBlank { "未命名按键" },
        key = if (json.isNull("key")) null else json.optString("key").takeIf { it.isNotBlank() },
        audio = json.optJSONObject("audio")?.let(::audioFromJson),
        volume = json.optDouble("volume", 1.0).toFloat().coerceIn(0f, 1f),
        speed = json.optDouble("speed", 1.0).toFloat().coerceIn(0.5f, 2f),
        startMs = json.optLong("startMs", 0L).coerceAtLeast(0L),
        endMs = if (json.isNull("endMs")) null else json.optLong("endMs").coerceAtLeast(0L),
        fadeInMs = json.optLong("fadeInMs", 0L).coerceAtLeast(0L),
        fadeOutMs = json.optLong("fadeOutMs", 0L).coerceAtLeast(0L)
    )

    private fun audioToJson(audio: AudioAsset): JSONObject = JSONObject()
        .put("id", audio.id)
        .put("displayName", audio.displayName)
        .put("fileName", audio.fileName)
        .put("durationMs", audio.durationMs)
        .put("mimeType", audio.mimeType ?: JSONObject.NULL)

    private fun audioFromJson(json: JSONObject): AudioAsset? {
        val fileName = json.optString("fileName")
        if (fileName.isBlank()) return null
        return AudioAsset(
            id = json.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
            displayName = json.optString("displayName").ifBlank { fileName },
            fileName = fileName,
            durationMs = json.optLong("durationMs", 0L).coerceAtLeast(0L),
            mimeType = if (json.isNull("mimeType")) null else json.optString("mimeType").takeIf { it.isNotBlank() }
        )
    }
}