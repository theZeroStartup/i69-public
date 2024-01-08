package com.i69.ui.screens.main.camera

import android.net.Uri

data class Preview(
    var imageUri: Uri? = null,
    var isSelected: Boolean = false
)
