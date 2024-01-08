package com.i69.ui.screens.main.camera

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CameraVM  @Inject constructor(
    app : Application
) : ViewModel(){

    private var imagePreviewAdapter: ImagePreviewAdapter? = null

    var onPreviewSelectionChanged = MutableLiveData<Pair<Int, Preview>>()
    var onImagePreview = MutableLiveData<Preview>()

    var uploadPosition = 0
    var isUploadPending = false

    fun getPreviewAdapter(context: Context): ImagePreviewAdapter {
        return imagePreviewAdapter ?: ImagePreviewAdapter(context, mutableListOf()) { pos, type, item ->
            if (type == "tap") onPreviewSelectionChanged.postValue(Pair(pos, item))
            else if (type == "hold") onImagePreview.postValue(item)
        }.apply { imagePreviewAdapter = this }
    }

}