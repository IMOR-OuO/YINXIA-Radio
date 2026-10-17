package com.example.soundbox

import android.app.Application
import com.example.soundbox.audio.PadAudioEngine
import com.example.soundbox.data.SoundBoxRepository

class SoundBoxApplication : Application() {

    val repository: SoundBoxRepository by lazy { SoundBoxRepository(this) }
    val audioEngine: PadAudioEngine by lazy { PadAudioEngine(this) }

    override fun onTerminate() {
        audioEngine.release()
        super.onTerminate()
    }
}