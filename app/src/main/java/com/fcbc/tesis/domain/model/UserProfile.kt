package com.fcbc.tesis.domain.model

data class UserProfile(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val location: String? = null
)
