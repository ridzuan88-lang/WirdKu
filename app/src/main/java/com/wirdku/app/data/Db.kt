package com.wirdku.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarks", indices = [Index(value = ["collectionId", "sectionId"], unique = true)])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionId: String, val sectionId: String, val sectionIndex: Int,
    val label: String = "", val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionId: String, val sectionId: String, val sectionIndex: Int,
    val readAt: Long = System.currentTimeMillis(),
)

@Dao
interface WirdDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC") fun bookmarks(): Flow<List<BookmarkEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addBookmark(b: BookmarkEntity)
    @Query("DELETE FROM bookmarks WHERE collectionId=:c AND sectionId=:s") suspend fun removeBookmark(c: String, s: String)
    @Query("SELECT * FROM history ORDER BY readAt DESC LIMIT 50") fun history(): Flow<List<HistoryEntity>>
    @Insert suspend fun addHistory(h: HistoryEntity)
    @Query("DELETE FROM history") suspend fun clearHistory()
}

@Database(entities = [BookmarkEntity::class, HistoryEntity::class], version = 1, exportSchema = false)
abstract class WirdDb : RoomDatabase() {
    abstract fun dao(): WirdDao
    companion object {
        @Volatile private var inst: WirdDb? = null
        fun get(ctx: Context): WirdDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, WirdDb::class.java, "wirdku.db").build().also { inst = it }
        }
    }
}
