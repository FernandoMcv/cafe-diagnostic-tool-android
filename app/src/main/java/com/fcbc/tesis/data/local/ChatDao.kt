package com.fcbc.tesis.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fcbc.tesis.data.local.entity.ChatMessageEntity
import com.fcbc.tesis.data.local.entity.ChatRoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_rooms ORDER BY updatedAt DESC")
    fun getAllChatRooms(): Flow<List<ChatRoomEntity>>

    @Query("SELECT * FROM chat_rooms WHERE participantUserId = :userId LIMIT 1")
    fun getChatRoomByParticipantId(userId: String): ChatRoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChatRooms(rooms: List<ChatRoomEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRoom(room: ChatRoomEntity): Long

    @Query("SELECT * FROM chat_messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessagesForRoom(roomId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE isSent = 0")
    fun getUnsentMessages(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(messages: List<ChatMessageEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_rooms")
    fun deleteAllRooms(): Int

    @Query("DELETE FROM chat_messages WHERE roomId = :roomId")
    fun deleteMessagesByRoom(roomId: String): Int
}
