package com.siaa.core.model

object EnglishAlphabet {
    val letterNames: Map<Char, String> = linkedMapOf(
        'A' to "ay", 'B' to "bee", 'C' to "cee", 'D' to "dee", 'E' to "ee",
        'F' to "ef", 'G' to "gee", 'H' to "aitch", 'I' to "eye", 'J' to "jay",
        'K' to "kay", 'L' to "el", 'M' to "em", 'N' to "en", 'O' to "oh",
        'P' to "pee", 'Q' to "cue", 'R' to "ar", 'S' to "ess", 'T' to "tee",
        'U' to "you", 'V' to "vee", 'W' to "double u", 'X' to "ex", 'Y' to "why", 'Z' to "zee"
    )

    fun spellForSpeech(word: String): String = word
        .uppercase()
        .toCharArray()
        .filter { it in 'A'..'Z' }
        .joinToString(", ") { letterNames[it] ?: it.toString() } + "."

    fun spellForDisplay(word: String): String = word
        .uppercase()
        .toCharArray()
        .filter { it in 'A'..'Z' }
        .joinToString(" - ")
}
