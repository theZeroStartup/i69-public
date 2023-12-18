package com.i69.data.models

import com.i69.GetUserQuery

data class BaseUserVisitingModel(val viewType : Int, val userVisiting: GetUserQuery.UserVisiting, val dateTime: CharSequence = "")
