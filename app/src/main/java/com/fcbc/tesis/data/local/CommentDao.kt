package com.fcbc.tesis.data.local

import androidx.room.*
import com.fcbc.tesis.data.local.entity.CommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {

    @Query("SELECT * FROM forum_comments ORDER BY timestamp ASC")
    fun getAll(): Flow<List<CommentEntity>>

    @Query("SELECT * FROM forum_comments WHERE isSynced = 0")
    fun getUnsyncedComments(): List<CommentEntity>

    @Query("SELECT * FROM forum_comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(comment: CommentEntity): Long

    @Update
    fun update(comment: CommentEntity): Int

    @Delete
    fun delete(comment: CommentEntity): Int

    @Query("DELETE FROM forum_comments WHERE postId = :postId")
    fun deleteByPostId(postId: String): Int

    @Query("SELECT * FROM forum_comments WHERE id = :id LIMIT 1")
    fun getCommentById(id: String): CommentEntity?
}
