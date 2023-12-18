package com.i69.data.remote.requests

data class LoginRequest(
    val accessToken: String,
    val accessVerifier: String = "",
    val provider: String,
)