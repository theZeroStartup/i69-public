package com.i69.data.models

data class PlanBnefits(
    val name: String,
    var planId : String,
    var isPlatnium: Boolean,
    var isSilver: Boolean,
    var isGold : Boolean,
    var selectedPackageId : String
)


