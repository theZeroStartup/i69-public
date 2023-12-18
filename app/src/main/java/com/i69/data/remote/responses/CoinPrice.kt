package com.i69.data.remote.responses

data class CoinPrice(
    val coinsCount: String,
    val originalPrice: String,
    val discountedPrice: String,
    val currency: String,
)
