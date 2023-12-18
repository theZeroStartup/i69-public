package com.i69.data.models

import com.i69.GetAllPackagesQuery


data class BaseAllPackageModel(var isExpanded : Boolean, val allPackage: GetAllPackagesQuery.AllPackage?)
