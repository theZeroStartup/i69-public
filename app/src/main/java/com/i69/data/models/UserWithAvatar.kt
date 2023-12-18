package com.i69.data.models

data class UserWithAvatar(
    var id: String = "",
    val username: String = "",
    var fullName: String = "",
    var email: String = "",
    var avatar: UserAvatar?,
)