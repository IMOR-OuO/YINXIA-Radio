package com.example.soundbox.data

object ShortcutKeys {

    fun forIndex(index: Int): String {
        require(index >= 0)
        if (index < 26) {
            return ('A'.code + index).toChar().toString()
        }
        val extra = index - 26
        val letter = ('A'.code + ((extra / 100) % 26)).toChar()
        val number = (extra % 100) + 1
        return "$letter$number"
    }

    fun options(count: Int): List<String> = List(count.coerceAtLeast(0), ::forIndex)
}