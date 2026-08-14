package com.fcbc.tesis.data.mapper

import com.fcbc.tesis.data.local.entity.CommentEntity
import com.fcbc.tesis.domain.model.Comment
import kotlinx.serialization.Serializable

@Serializable
data class CommentDto(
    val id: String? = null,
    val post_id: String? = null,
    val parent_id: String? = null,
    val author: String? = null,
    val content: String? = null,
    val timestamp: String? = null,
    val user_id: String? = null,
    val author_avatar_url: String? = null
)

fun CommentEntity.toDomain() = Comment(id, postId, parentId, author, content, timestamp, authorAvatarUrl)
fun Comment.toEntity() = CommentEntity(id, postId, parentId, author, content, timestamp, true, authorAvatarUrl)

fun CommentDto.toEntity() = CommentEntity(id ?: "", post_id ?: "", parent_id, author ?: "Usuario Anónimo", content ?: "", timestamp ?: "", true, author_avatar_url)
fun CommentEntity.toDto(userId: String? = null) = CommentDto(
    id = if (id == "") null else id,
    post_id = postId,
    parent_id = parentId,
    author = author,
    content = content,
    timestamp = timestamp,
    user_id = null,
    author_avatar_url = authorAvatarUrl
)
