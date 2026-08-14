package com.fcbc.tesis.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fcbc.tesis.data.local.entity.CommentEntity
import com.fcbc.tesis.data.local.entity.ForumPostEntity
import com.fcbc.tesis.data.local.entity.HistoryEntity
import com.fcbc.tesis.data.local.entity.ChatRoomEntity
import com.fcbc.tesis.data.local.entity.ChatMessageEntity
import com.fcbc.tesis.data.local.entity.SavedPostEntity
import com.fcbc.tesis.data.local.entity.FollowEntity

/**
 * Base de Datos Blindada (Senior).
 * Se ha incrementado a la Versión 7 para soportar Ubicación.
 */
@Database(
    entities = [
        HistoryEntity::class, 
        ForumPostEntity::class, 
        CommentEntity::class,
        ChatRoomEntity::class,
        ChatMessageEntity::class,
        SavedPostEntity::class,
        FollowEntity::class
    ], 
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun forumDao(): ForumDao
    abstract fun commentDao(): CommentDao
    abstract fun chatDao(): ChatDao
    abstract fun interactionDao(): InteractionDao

    companion object {
        const val DATABASE_NAME = "apptess_db"

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE history_table ADD COLUMN location TEXT")
                db.execSQL("ALTER TABLE forum_posts ADD COLUMN location TEXT")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE forum_posts ADD COLUMN authorAvatarUrl TEXT")
                db.execSQL("ALTER TABLE forum_posts ADD COLUMN aiConfidence REAL")
                db.execSQL("ALTER TABLE forum_comments ADD COLUMN authorAvatarUrl TEXT")
            }
        }
    }
}
