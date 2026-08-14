package com.fcbc.tesis.domain.repository

import com.fcbc.tesis.domain.model.UserProfile
import com.fcbc.tesis.domain.util.Resource

interface UserRepository {
    suspend fun getPublicProfile(userId: String): Resource<UserProfile>
    suspend fun followUser(userId: String): Resource<Unit>
    suspend fun unfollowUser(userId: String): Resource<Unit>
    suspend fun isFollowing(userId: String): Resource<Boolean>
    suspend fun getFollowingList(): Resource<List<UserProfile>>
    suspend fun updateProfileData(newName: String, newLocation: String?): Resource<Unit>
}
