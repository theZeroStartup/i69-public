package com.i69.data.models

data class BlockedUser(
    val id: String,
    val username: String,
    val avatarPhotos: List<com.i69.data.models.Photo>? = emptyList(),
    val fullName:String
)
