package com.i69.data.remote.responses

data class ResponseBody<T>(
    val data: T?,
    val errorMessage: String? = null
)