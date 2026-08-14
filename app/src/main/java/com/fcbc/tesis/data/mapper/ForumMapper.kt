package com.fcbc.tesis.data.mapper

import com.fcbc.tesis.data.local.entity.ForumPostEntity
import com.fcbc.tesis.domain.model.ForumPost
import kotlinx.serialization.Serializable

@Serializable
data class ForumPostDto(
    val id: String? = null,
    val author: String? = null,
    val crop_context: String? = null,
    val content: String? = null,
    val timestamp: String? = null,
    val disease_tags: String? = null,
    val image_url: String? = null,
    val likes: Int? = 0,
    val comments_count: Int? = 0,
    val user_id: String? = null,
    val location: String? = null,
    val author_avatar_url: String? = null,
    val ai_confidence: Float? = null
)

fun ForumPostEntity.toDomain() = ForumPost(id, author, cropContext, content, timestamp, diseaseTag?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(), imageUrl, likes, commentsCount, userId = userId, location = location, authorAvatarUrl = authorAvatarUrl, aiConfidence = aiConfidence)
fun ForumPost.toEntity() = ForumPostEntity(id, author, cropContext, content, timestamp, if (diseaseTags.isEmpty()) null else diseaseTags.joinToString(","), imageUrl, likes, commentsCount, userId, true, location, authorAvatarUrl, aiConfidence)

fun ForumPostDto.toEntity() = ForumPostEntity(id ?: "", author ?: "Usuario Anónimo", crop_context ?: "", content ?: "", timestamp ?: "", disease_tags, image_url, likes ?: 0, comments_count ?: 0, user_id, true, location, author_avatar_url, ai_confidence)
fun ForumPostEntity.toDto(userIdArg: String? = null) = ForumPostDto(
    id = if (id == "") null else id,
    author = author,
    crop_context = cropContext,
    content = content,
    timestamp = timestamp,
    disease_tags = diseaseTag,
    image_url = imageUrl,
    likes = likes,
    comments_count = commentsCount,
    user_id = userIdArg ?: this.userId,
    location = location,
    author_avatar_url = authorAvatarUrl,
    ai_confidence = aiConfidence
)
