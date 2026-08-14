package com.fcbc.tesis.data.local.entity

import androidx.room.Entity

@Entity(tableName = "saved_posts_table", primaryKeys = ["userId", "postId"])
data class SavedPostEntity(
    val userId: String,
    val postId: String,
    val isSynced: Boolean = true,
    val isDeletedOffline: Boolean = false
)
