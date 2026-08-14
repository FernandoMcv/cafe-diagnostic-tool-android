package com.fcbc.tesis.data.local

import androidx.room.*
import androidx.paging.PagingSource
import com.fcbc.tesis.data.local.entity.ForumPostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ForumDao {

    @Query("SELECT * FROM forum_posts ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ForumPostEntity>>

    @Query("SELECT * FROM forum_posts ORDER BY timestamp DESC")
    fun getPagedPosts(): PagingSource<Int, ForumPostEntity>

    @Query("SELECT * FROM forum_posts WHERE diseaseTag LIKE '%' || :filter || '%' ORDER BY timestamp DESC")
    fun getPagedPostsByFilter(filter: String): PagingSource<Int, ForumPostEntity>

    @Query("SELECT * FROM forum_posts WHERE isSynced = 0")
    fun getUnsyncedPosts(): List<ForumPostEntity>

    @Query("SELECT * FROM forum_posts WHERE isSynced = 1")
    fun getAllSyncedPosts(): List<ForumPostEntity>

    @Query("SELECT * FROM forum_posts WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPostsByUserId(userId: String): Flow<List<ForumPostEntity>>

    @Query("SELECT * FROM forum_posts WHERE id = :id LIMIT 1")
    fun getPostById(id: String): ForumPostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(post: ForumPostEntity): Long

    @Update
    fun update(post: ForumPostEntity): Int

    @Query("DELETE FROM forum_posts")
    fun deleteAll(): Int

    @Query("DELETE FROM forum_posts WHERE id = :postId")
    fun deletePost(postId: String): Int
}
