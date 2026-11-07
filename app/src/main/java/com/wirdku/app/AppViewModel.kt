package com.wirdku.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wirdku.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repo = ContentRepository(app)
    val prefs = Prefs(app)
    private val dao = WirdDb.get(app).dao()
    val audio = AudioController(app)

    val settings = prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val position = prefs.position.stateIn(viewModelScope, SharingStarted.Eagerly, ReadingPos())
    val tasbih = prefs.tasbih.stateIn(viewModelScope, SharingStarted.Eagerly, Tasbih())
    val bookmarks = dao.bookmarks().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val history = dao.history().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSettings(f: (Settings) -> Settings) = viewModelScope.launch { prefs.update(f(settings.value)) }
    fun savePosition(c: String, i: Int) = viewModelScope.launch {
        prefs.savePosition(c, i)
    }
    fun logHistory(c: String, s: String, i: Int) = viewModelScope.launch { dao.addHistory(HistoryEntity(collectionId = c, sectionId = s, sectionIndex = i)) }
    fun toggleBookmark(c: String, s: String, i: Int, label: String) = viewModelScope.launch {
        if (bookmarks.value.any { it.collectionId == c && it.sectionId == s }) dao.removeBookmark(c, s)
        else dao.addBookmark(BookmarkEntity(collectionId = c, sectionId = s, sectionIndex = i, label = label))
    }
    fun clearHistory() = viewModelScope.launch { dao.clearHistory() }
    fun saveTasbih(t: Tasbih) = viewModelScope.launch { prefs.saveTasbih(t) }
    override fun onCleared() { audio.release() }
}
