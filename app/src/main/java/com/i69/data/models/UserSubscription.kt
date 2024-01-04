package com.i69.data.models

data class UserSubscription(
    val isActive: Boolean = false,
    val isCancelled: Boolean = false
) : java.io.Serializable