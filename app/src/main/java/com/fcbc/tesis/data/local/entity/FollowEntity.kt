package com.fcbc.tesis.data.local.entity

import androidx.room.Entity

@Entity(tableName = "follows_table", primaryKeys = ["followerId", "followingId"])
data class FollowEntity(
    val followerId: String,
    val followingId: String,
    val isSynced: Boolean = true,
    val isDeletedOffline: Boolean = false
)
