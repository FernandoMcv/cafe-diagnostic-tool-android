package com.fcbc.tesis.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fcbc.tesis.data.local.entity.FollowEntity
import com.fcbc.tesis.data.local.entity.SavedPostEntity

@Dao
@JvmSuppressWildcards
interface InteractionDao {
    // ---- Saved Posts ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPost(savedPost: SavedPostEntity): Long

    @Query("SELECT * FROM saved_posts_table WHERE userId = :userId AND isDeletedOffline = 0")
    suspend fun getSavedPostsForUser(userId: String): List<SavedPostEntity>

    @Query("SELECT * FROM saved_posts_table WHERE userId = :userId AND postId = :postId")
    suspend fun getSavedPost(userId: String, postId: String): SavedPostEntity?

    @Query("DELETE FROM saved_posts_table WHERE userId = :userId AND postId = :postId")
    suspend fun deleteSavedPost(userId: String, postId: String): Int

    @Query("SELECT * FROM saved_posts_table WHERE isSynced = 0 OR isDeletedOffline = 1")
    suspend fun getUnsyncedSavedPosts(): List<SavedPostEntity>

    @Query("DELETE FROM saved_posts_table")
    suspend fun deleteAllSavedPosts(): Int

    // ---- Follows ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity): Long

    @Query("SELECT * FROM follows_table WHERE followerId = :followerId AND isDeletedOffline = 0")
    suspend fun getFollowingForUser(followerId: String): List<FollowEntity>

    @Query("SELECT * FROM follows_table WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun getFollow(followerId: String, followingId: String): FollowEntity?

    @Query("DELETE FROM follows_table WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun deleteFollow(followerId: String, followingId: String): Int

    @Query("SELECT * FROM follows_table WHERE isSynced = 0 OR isDeletedOffline = 1")
    suspend fun getUnsyncedFollows(): List<FollowEntity>

    @Query("DELETE FROM follows_table")
    suspend fun deleteAllFollows(): Int
}
