package com.wirdku.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Word(
    val id: String = "",
    val arabic: String = "",
    val audio: String? = null,
    val slowAudio: String? = null,
    val makhraj: String = "",
    val tajwid: String = "",
    val verified: Boolean = false,
)

@Serializable
data class Section(
    val id: String,
    val title: String = "",
    val arabic: String,
    val rumi: String = "",
    val translation: String = "",
    val explanation: String = "",
    val audio: String? = null,
    val words: List<Word> = emptyList(),
)

@Serializable
data class WirdCollection(
    val id: String,
    val title: String,
    val arabicTitle: String = "",
    val description: String = "",
    val active: Boolean = true,
    val audio: String? = null,
    val sourceStatus: String = "",
    val sections: List<Section> = emptyList(),
)

@Serializable data class CollectionIndex(val collections: List<String> = emptyList())

@Serializable
data class LetterInfo(
    val char: String, val name: String, val group: String,
    val makhraj: String, val tip: String, val verified: Boolean = false,
)

@Serializable
data class MarkInfo(val mark: String, val name: String, val note: String, val verified: Boolean = false)

@Serializable
data class MakhrajData(
    val reviewStatus: String = "",
    val letters: List<LetterInfo> = emptyList(),
    val marks: List<MarkInfo> = emptyList(),
)
