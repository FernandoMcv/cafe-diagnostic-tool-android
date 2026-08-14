package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.repository.UserRepository

class GetPublicProfileUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(userId: String) = repository.getPublicProfile(userId)
}
class FollowUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(userId: String) = repository.followUser(userId)
}
class UnfollowUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(userId: String) = repository.unfollowUser(userId)
}
class IsFollowingUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(userId: String) = repository.isFollowing(userId)
}
class GetFollowingListUseCase(private val repository: UserRepository) {
    suspend operator fun invoke() = repository.getFollowingList()
}
class UpdateProfileDataUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(newName: String, newLocation: String?) = repository.updateProfileData(newName, newLocation)
}
