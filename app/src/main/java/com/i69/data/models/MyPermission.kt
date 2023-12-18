package com.i69.data.models

data class MyPermission(
    var hasPermission: Boolean,
    var coinsToUnlock: Int = 0,
    var freeUserLimit: Int = 0
)
