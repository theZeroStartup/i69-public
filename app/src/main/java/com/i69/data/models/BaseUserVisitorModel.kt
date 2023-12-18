package com.i69.data.models

import com.i69.GetUserQuery

data class BaseUserVisitorModel(val viewType : Int,val userVisitor: GetUserQuery.UserVisitor,val dateTime: CharSequence = "")
