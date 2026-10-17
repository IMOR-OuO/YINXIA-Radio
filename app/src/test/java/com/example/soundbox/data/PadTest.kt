package com.example.soundbox.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PadTest {

    @Test
    fun effectiveEndMsPrefersExplicitEnd() {
        val pad = Pad(
            name = "测试",
            audio = AudioAsset(displayName = "test", fileName = "test.mp3", durationMs = 5_000L),
            endMs = 2_500L
        )

        assertEquals(2_500L, pad.effectiveEndMs)
    }

    @Test
    fun effectiveEndMsFallsBackToAudioDuration() {
        val pad = Pad(
            name = "测试",
            audio = AudioAsset(displayName = "test", fileName = "test.mp3", durationMs = 5_000L)
        )

        assertEquals(5_000L, pad.effectiveEndMs)
    }

    @Test
    fun effectiveEndMsUsesMaxValueWhenDurationIsUnknown() {
        val pad = Pad(name = "测试")

        assertEquals(Long.MAX_VALUE, pad.effectiveEndMs)
    }
}