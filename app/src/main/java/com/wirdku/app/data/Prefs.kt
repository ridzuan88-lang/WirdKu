package com.wirdku.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("wirdku_prefs")

data class Settings(
    val arabicSize: Float = 36f, val malaySize: Float = 16f, val lineSpacing: Float = 1.8f,
    val wordSpacing: Float = 6f, val background: Int = 0, val useNaskh: Boolean = true,
    val showArabic: Boolean = true, val showRumi: Boolean = false,
    val showTranslation: Boolean = true, val showExplanation: Boolean = false,
    val keepScreenOn: Boolean = true,
)

data class ReadingPos(val collectionId: String = "", val sectionIndex: Int = 0)
data class Tasbih(val count: Int = 0, val target: Int = 33, val label: String = "Zikir")

class Prefs(private val ctx: Context) {
    private object K {
        val aSize = floatPreferencesKey("aSize"); val mSize = floatPreferencesKey("mSize")
        val line = floatPreferencesKey("line"); val wsp = floatPreferencesKey("wsp")
        val bg = intPreferencesKey("bg"); val naskh = booleanPreferencesKey("naskh")
        val sA = booleanPreferencesKey("sA"); val sR = booleanPreferencesKey("sR")
        val sT = booleanPreferencesKey("sT"); val sE = booleanPreferencesKey("sE")
        val awake = booleanPreferencesKey("awake")
        val posC = stringPreferencesKey("posC"); val posI = intPreferencesKey("posI")
        val tCount = intPreferencesKey("tCount"); val tTarget = intPreferencesKey("tTarget")
        val tLabel = stringPreferencesKey("tLabel")
    }

    val settings: Flow<Settings> = ctx.store.data.map { p ->
        val d = Settings()
        Settings(p[K.aSize] ?: d.arabicSize, p[K.mSize] ?: d.malaySize, p[K.line] ?: d.lineSpacing,
            p[K.wsp] ?: d.wordSpacing, p[K.bg] ?: d.background, p[K.naskh] ?: d.useNaskh,
            p[K.sA] ?: d.showArabic, p[K.sR] ?: d.showRumi, p[K.sT] ?: d.showTranslation,
            p[K.sE] ?: d.showExplanation, p[K.awake] ?: d.keepScreenOn)
    }

    suspend fun update(s: Settings) = ctx.store.edit {
        it[K.aSize] = s.arabicSize; it[K.mSize] = s.malaySize; it[K.line] = s.lineSpacing
        it[K.wsp] = s.wordSpacing; it[K.bg] = s.background; it[K.naskh] = s.useNaskh
        it[K.sA] = s.showArabic; it[K.sR] = s.showRumi; it[K.sT] = s.showTranslation
        it[K.sE] = s.showExplanation; it[K.awake] = s.keepScreenOn
    }

    val position: Flow<ReadingPos> = ctx.store.data.map { ReadingPos(it[K.posC] ?: "", it[K.posI] ?: 0) }
    suspend fun savePosition(c: String, i: Int) = ctx.store.edit { it[K.posC] = c; it[K.posI] = i }

    val tasbih: Flow<Tasbih> = ctx.store.data.map { Tasbih(it[K.tCount] ?: 0, it[K.tTarget] ?: 33, it[K.tLabel] ?: "Zikir") }
    suspend fun saveTasbih(t: Tasbih) = ctx.store.edit { it[K.tCount] = t.count; it[K.tTarget] = t.target; it[K.tLabel] = t.label }
}
