package com.i69.data.models

import com.stfalcon.chatkit.commons.models.MessageContentType

class MediaChatMessage(user: User?, timeStampMillis: Long, private val path: String) :
    com.i69.data.models.ChatMessage(user, timeStampMillis, ""), MessageContentType.Image {
    override fun getImageUrl(): String = path
}