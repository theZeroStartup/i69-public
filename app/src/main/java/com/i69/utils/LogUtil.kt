package com.i69.utils

import android.util.Log

object LogUtil {

    val TAG = "I69Logs"

    fun debug(msg: String?) {
        if (msg != null) {
            Log.d(TAG, msg)
        }
    }
}