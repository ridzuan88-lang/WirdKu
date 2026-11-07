package com.wirdku.app.data

import android.content.Context
import kotlinx.serialization.json.Json

class ContentRepository(private val ctx: Context) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var cache: List<WirdCollection>? = null
    private var makhrajCache: MakhrajData? = null

    fun collections(): List<WirdCollection> = cache ?: load().also { cache = it }

    private fun load(): List<WirdCollection> {
        val idx = runCatching {
            json.decodeFromString<CollectionIndex>(read("collections/index.json"))
        }.getOrDefault(CollectionIndex())
        return idx.collections.mapNotNull { f ->
            runCatching { prepare(json.decodeFromString<WirdCollection>(read("collections/$f"))) }.getOrNull()
        }
    }

    /** Ensure every section has stable word ids; merge JSON-supplied word data by index. */
    fun prepare(c: WirdCollection): WirdCollection = c.copy(sections = c.sections.map { s ->
        val tokens = ArabicText.segment(s.arabic)
        val words = tokens.mapIndexed { i, t ->
            val id = "${s.id}_w${(i + 1).toString().padStart(3, '0')}"
            val supplied = s.words.firstOrNull { it.id == id } ?: s.words.getOrNull(i)?.takeIf { it.arabic == t }
            (supplied ?: Word()).copy(id = id, arabic = t)
        }
        s.copy(words = words)
    })

    fun collection(id: String) = collections().firstOrNull { it.id == id }

    fun makhraj(): MakhrajData = makhrajCache ?: runCatching {
        json.decodeFromString<MakhrajData>(read("makhraj/letters.json"))
    }.getOrDefault(MakhrajData()).also { makhrajCache = it }

    fun assetExists(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return runCatching { ctx.assets.open(path).close(); true }.getOrDefault(false)
    }

    private fun read(path: String) = ctx.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
}
