package com.i69.ui.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.i69.BuildConfig
import com.i69.GetChatMessagesByRoomIdQuery
import com.i69.R
import com.i69.applocalization.AppStringConstant1
import com.i69.databinding.ItemNewIncomingTextMessageBinding
import com.i69.databinding.ItemNewOutcomingTextMessageBinding
import com.i69.type.MessageMessageType
import com.i69.ui.screens.main.MainActivity.Companion.getMainActivity
import com.i69.utils.*
import com.skydoves.powermenu.MenuAnimation
import com.skydoves.powermenu.PowerMenu
import com.skydoves.powermenu.PowerMenuItem
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


private const val TYPE_NEW_INCOMING = 0
private const val TYPE_NEW_OUTGOING = 1
var translationUtils: TranslationUtils? = null

class NewChatMessagesAdapter(
    private val ctx: Context,
    private val userId: String?,
    private val listener: ChatMessageListener,
    //private val chatMessages: ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var lastSeenMessageId = ""
    var otherUserAvtar = ""
    private var lastMessageItem: GetChatMessagesByRoomIdQuery.Edge? = null

//    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
//        timeZone = TimeZone.getTimeZone("UTC")
//    }

//    private val formatterNew = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
//        timeZone = TimeZone.getTimeZone("UTC")
//    }


//    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault()).apply {
//        timeZone = TimeZone.getTimeZone("UTC")
//    }

    private val formatter =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZ", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

//    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ").apply {
//        timeZone = TimeZone.getTimeZone("UTC")
//    }


    private val formatterNew = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    private val displayTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    private fun getViewHolderByType(type: Int, viewBinding: ViewDataBinding) =
        if (type == TYPE_NEW_INCOMING) IncomingHolder(viewBinding as ItemNewIncomingTextMessageBinding)
        else OutgoingHolder(viewBinding as ItemNewOutcomingTextMessageBinding)

    private fun getViewDataBinding(viewType: Int, parent: ViewGroup) =
        if (viewType == TYPE_NEW_INCOMING)
            ItemNewIncomingTextMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        else
            ItemNewOutcomingTextMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

    override fun getItemViewType(position: Int): Int {
        val item = differ.currentList[position]
        return if (item?.node?.userId?.id == userId) TYPE_NEW_OUTGOING else TYPE_NEW_INCOMING
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder =
        getViewHolderByType(type, getViewDataBinding(type, parent))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = differ.currentList[holder.absoluteAdapterPosition]
        //LogUtil.debug("Position 1 Data : : : ${differ.currentList[position].node?.content} \n Position : : $position")
        if (getItemViewType(position) == TYPE_NEW_INCOMING) {
            holder as IncomingHolder
            holder.bind(item, position)
        } else {
            holder as OutgoingHolder
            holder.bind(item, position)
        }
    }

    override fun getItemCount() = differ.currentList.size

    private val diffUtilCallBack =
        object : DiffUtil.ItemCallback<GetChatMessagesByRoomIdQuery.Edge>() {
            override fun areItemsTheSame(
                oldItem: GetChatMessagesByRoomIdQuery.Edge,
                newItem: GetChatMessagesByRoomIdQuery.Edge
            ): Boolean {
                return oldItem.node?.id?.toInt() == newItem.node?.id?.toInt()
            }

            override fun areContentsTheSame(
                oldItem: GetChatMessagesByRoomIdQuery.Edge,
                newItem: GetChatMessagesByRoomIdQuery.Edge
            ): Boolean {
                return when {
                    oldItem.node?.id?.toInt() != newItem.node?.id?.toInt() -> false
                    oldItem.node?.content != newItem.node?.content -> false
                    else -> true
                }
//                 return oldItem.node?.content == newItem.node?.content
            }
        }

    private val differ = AsyncListDiffer(this, diffUtilCallBack)

    fun updateList(
        updatedList: MutableList<GetChatMessagesByRoomIdQuery.Edge?>?,
        isNew: Boolean = false,
        isRemove: Boolean = false,
        isUpdate : Boolean = false
    ) {
        val old = differ.currentList

//        if (updatedList != null) {
//            old.addAll(updatedList)
//        }

        val x = mutableListOf<GetChatMessagesByRoomIdQuery.Edge?>().apply {
            if (updatedList != null) {
                if (isRemove) {
                    addAll(updatedList)
                } else if (isNew) {
                    addAll(updatedList)
                    addAll(old)
                }else if(isUpdate){
                    addAll(updatedList)
                }
                else {
                    addAll(old)
                    addAll(updatedList)
                }
            }
        }

        if (differ.currentList.size > 0) {
            differ.submitList(null)
        }

//        old.addAll(x)
//        differ.submitList(old)
        differ.submitList(x)
//        notifyDataSetChanged()

    }

    fun addItmToList(updatedList: GetChatMessagesByRoomIdQuery.Edge?) {
        val old = differ.currentList

        val x = mutableListOf<GetChatMessagesByRoomIdQuery.Edge?>().apply {
            if (updatedList != null) {
                add(updatedList)
                addAll(old)
            }
        }
        differ.submitList(x)
    }

    fun addList(updatedList: MutableList<GetChatMessagesByRoomIdQuery.Edge?>) {
        val old = differ.currentList
        val x = mutableListOf<GetChatMessagesByRoomIdQuery.Edge?>().apply {
            addAll(updatedList)
            addAll(old)
        }
        differ.submitList(x)
    }

    /*@SuppressLint("NotifyDataSetChanged")
    fun addMessage(message: ChatSubscription.Message?) {
        var url: String
        try {
            url = message?.sender?.avatarPhotos?.get(0)?.url!!
        } catch (e:Exception) {url = ""}
        val avatar = GetChatMessagesByChatIdQuery.AvatarPhoto(url)
        val avatarPhoto = arrayListOf<GetChatMessagesByChatIdQuery.AvatarPhoto>()
        avatarPhoto.add(avatar)
        val sender = GetChatMessagesByChatIdQuery.Sender(message?.sender?.id!!, avatarPhoto, message.sender.username, message.sender.fullName, message.sender.email)
        val node = GetChatMessagesByChatIdQuery.Node(message.id, true, message.created, message.text, sender)
        val edge = Any(node)
        chatMessages?.add(edge)
        notifyDataSetChanged()
    }*/

    inner class IncomingHolder(val viewBinding: ItemNewIncomingTextMessageBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(item: GetChatMessagesByRoomIdQuery.Edge?, position: Int) {
            val content = item?.node?.content
            val mcontent = item?.node
//            LogUtil.debug("Data : : ${mcontent.toString()}")
//            LogUtil.debug("Data : : $content")
//            Log.e("inPtypemessageareComes", mcontent?.messageType.toString() )

//            if (item?.node?.messageType == MessageMessageType.P) {
            if (mcontent?.messageType == MessageMessageType.P) {
                viewBinding.ctRequestview.visibility = View.VISIBLE
                when (mcontent?.requestStatus) {
                    "PENDING" -> {
                        viewBinding.cdAccept.visibility = View.VISIBLE
                        viewBinding.cdReject.visibility = View.VISIBLE
//                        viewBinding.cdView.visibility = View.GONE
                    }
                    "APPROVE" -> {
                        viewBinding.cdAccept.visibility = View.GONE
                        viewBinding.cdReject.visibility = View.GONE
                        viewBinding.cdCancel.visibility = View.VISIBLE
//                        viewBinding.cdView.visibility = View.GONE
                    }
                    else -> {
                        viewBinding.cdAccept.visibility = View.GONE
                        viewBinding.cdReject.visibility = View.GONE
                        viewBinding.cdCancel.visibility = View.GONE
//                        viewBinding.cdView.visibility = View.VISIBLE
                    }
                }
                viewBinding.cdAccept.setOnClickListener {
                    listener.onPrivatePhotoAccessResult("A", item.node.privatePhotoRequestId!!)
                }
                viewBinding.cdReject.setOnClickListener {
                    listener.onPrivatePhotoAccessResult("R", item.node.privatePhotoRequestId!!)
                }
                viewBinding.cdCancel.setOnClickListener {
                    listener.onPrivatePhotoAccessResult("C", item.node.privatePhotoRequestId!!)
                }


            } else {
//                Log.e("inPicturMessageHide", mcontent?.messageType.toString())
                viewBinding.ctRequestview.visibility = View.GONE
            }
            if (content?.contains("media/chat_files") == true) {
                var fullUrl = content
                var message = ""
                if (fullUrl.contains(" ")) {
                    val link = fullUrl.substring(0, fullUrl.indexOf(" "))
                    val giftMessage = content.substring(fullUrl.indexOf(" ") + 1)
                    fullUrl = link
                    message = giftMessage
                } else {
                    if (content.startsWith("/media/chat_files/")) {
                        fullUrl = "${BuildConfig.BASE_URL}$content"
                    }
                }
/*
                if (content.startsWith("/media/chat_files/")) {
                    fullUrl = "${BuildConfig.BASE_URL}$content"
                }*/
                val uri = Uri.parse(fullUrl)
                val lastSegment = uri.lastPathSegment
                val ext = lastSegment?.substring(lastSegment.lastIndexOf(".") + 1)

                val typeface_regular = Typeface.createFromAsset(
                    viewBinding.root.context.assets,
                    "fonts/poppins_semibold.ttf"
                )
                val typeface_light = Typeface.createFromAsset(
                    viewBinding.root.context.assets,
                    "fonts/poppins_light.ttf"
                )

                viewBinding.messageText.typeface = typeface_regular
                viewBinding.messageTime.typeface = typeface_light

                if (ext?.isImageFile() == true || ext?.isVideoFile() == true) {
                    if (message.isNotEmpty()) {
                        viewBinding.messageText.text = message
                    } else {
                        viewBinding.messageText.text = ""
                    }
                    //  viewBinding.messageText.text = ""
                    viewBinding.messageImage.loadImage(
                        fullUrl,
                        RequestOptions().centerCrop(),
                        null,
                        null
                    )

                    viewBinding.messageImage.visibility = View.VISIBLE
                    viewBinding.mapView.visibility = View.GONE
                    viewBinding.messageFileIcon.visibility = View.GONE
                    viewBinding.messagePlayIcon.visibility =
                        if (ext.isVideoFile()) View.VISIBLE else View.GONE
                } else {
                    viewBinding.messageText.text = lastSegment
                    viewBinding.messageImage.setImageBitmap(null)
                    viewBinding.messageFileIcon.visibility = View.VISIBLE
                    viewBinding.messageImage.visibility = View.GONE
                    viewBinding.messagePlayIcon.visibility = View.GONE
                    viewBinding.mapView.visibility = View.GONE
                }
            } else {


                if (mcontent?.messageType == MessageMessageType.GL) {
//                if (mcontent?.messageType == MessageMessageType.C) {
                    viewBinding.ctRequestview.visibility = View.VISIBLE
                    viewBinding.messageText.text = ""
                    viewBinding.mapView.visibility = View.VISIBLE
                    viewBinding.messageImage.setImageBitmap(null)
                    viewBinding.messageImage.visibility = View.GONE
                    viewBinding.messageFileIcon.visibility = View.GONE
                    viewBinding.messagePlayIcon.visibility = View.GONE
                    viewBinding.cdReject.visibility = View.GONE
                    viewBinding.cdAccept.visibility = View.GONE
                    var latitude = 0.0
                    var longitude = 0.0
                    try {
                        val items = content?.split(",")?.toTypedArray()
                        latitude = items?.get(0)?.toDouble() ?: 0.0
                        longitude = items?.get(1)?.toDouble() ?: 0.0

                    } catch (throwable: Throwable) {
                        throwable.printStackTrace()
                    }
                    // Initialise the MapView
                    viewBinding.mapView.onCreate(null)
                    viewBinding.mapView.onResume()
                    // Set the map ready callback to receive the GoogleMap object
                    viewBinding.mapView.getMapAsync { googleMap ->
                        googleMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(latitude, longitude),
                                13f
                            )
                        )
                        googleMap.addMarker(
                            MarkerOptions().position(
                                LatLng(latitude, longitude)
                            )
                        )
                        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                        googleMap.uiSettings.setAllGesturesEnabled(false)
                    }
                } else {
                    viewBinding.messageText.text = content
                    viewBinding.messageImage.setImageBitmap(null)
                    viewBinding.messageImage.visibility = View.GONE
                    viewBinding.messageFileIcon.visibility = View.GONE
                    viewBinding.messagePlayIcon.visibility = View.GONE
                    viewBinding.mapView.visibility = View.GONE
                }
            }
            var avatarUrl: String? = ""
            avatarUrl = try {
                item?.node?.userId?.avatarPhotos?.get(item.node.userId.avatarIndex)?.url
            } catch (e: Exception) {
                ""
            }
            if (avatarUrl != null) {
                viewBinding.messageUserAvatar.loadCircleImage(avatarUrl)
            } else {
                viewBinding.messageUserAvatar.loadCircleImage(R.drawable.ic_chat_item_logo_new)
            }
            var text = item?.node?.timestamp.toString()
//            LogUtil.debug("Current  : : : $text")
            val messageTime: Date
            try {
//                text = text.replace("T", " ").substring(0, text.indexOf("."))
//                text = text.substring(0, text.indexOf("."))

                messageTime = formatter.parse(text) as Date
                val messageTimeToFormat = formatterNew.format(messageTime)
//                val messageTimeToCheck = formatterNew.format(messageTime) as Date
                val messageTimeToCheck = formatterNew.parse(messageTimeToFormat) as Date
                viewBinding.messageTime.text = displayTime.format(messageTime)
                val displayDate = formatDayDate(ctx, messageTime.time)
                // code for display date label
//                if (bindingAdapterPosition == 0) {

//                    if (itemCount == 1) {
//                        val displayDate = formatDayDate(ctx, messageTime.time)
//                        viewBinding.lblDate.visibility = View.VISIBLE
//                        viewBinding.lblDate.text = displayDate
//                    }
//                } else {
//                    Log.e("MyCurrentPositions", "${bindingAdapterPosition}")
//                    Log.e("MyCurrentPositionsSizes", "${differ.currentList.size}")

                if (differ.currentList.size > bindingAdapterPosition + 1) {
//                        Log.e("MyCurrentPositions111", "${bindingAdapterPosition+1}")

//                        val preItem = differ.currentList[bindingAdapterPosition - 1]
                    val preItem = differ.currentList[bindingAdapterPosition + 1]

                    var preTime = preItem?.node?.timestamp.toString()
                    LogUtil.debug("Pre  : : : $preTime")

                    val preDateToCheck = formatter.parse(preTime)
                    val preDateToFormat = formatterNew.format(preDateToCheck)
                    val preDate = formatterNew.parse(preDateToFormat) as Date

//                        preTime = preTime.replace("T", " ").substring(0, preTime.indexOf("."))
//                        preTime = preTime.substring(0, preTime.indexOf("."))
//                        val preDate = formatterNew.parse(preTime)
//                        val preDate = formatterNew.parse(preTime)
                    LogUtil.debug("preDate  $preDate")
//                        val preDisplayDate = formatDayDate(ctx, preDate.time)
//                    LogUtil.debug("preDisplayDate $preDisplayDate")
//                    LogUtil.debug("displayDate $displayDate")

//
//                        Log.e("myOutGoindmsgg", "ItemNewOutcomingTextMessageBinding")
//                        Log.e("preDate", "$preDate")
//                        Log.e("preMsges", "${preItem.node?.content}")
//                        Log.e("messageTimeToCheck", "$messageTimeToCheck")
//                        Log.e("currentMsges", "$content")


                    if (messageTimeToCheck == preDate) {
                        viewBinding.lblDate.text = ""
                        viewBinding.llDateLayout.visibility = View.GONE
                    } else {

                        viewBinding.llDateLayout.visibility = View.VISIBLE
                        viewBinding.lblDate.text = displayDate
                    }
                } else {
//                        val preItem = differ.currentList[bindingAdapterPosition ]
//
//                        var preTime = preItem?.node?.timestamp.toString()
//                        LogUtil.debug("Pre  : : : $preTime")
//
//                        val preDateToCheck = formatter.parse(preTime)
//                        val preDateToFormat = formatterNew.format(preDateToCheck)
//                        val preDate = formatterNew.parse(preDateToFormat) as Date
//                        if (messageTimeToCheck == preDate) {
//                            viewBinding.lblDate.text = ""
//                            viewBinding.lblDate.visibility = View.GONE
//                        } else {
                    viewBinding.llDateLayout.visibility = View.VISIBLE
                    viewBinding.lblDate.text = displayDate
//                        }
//                        Log.e("MyCurrentPositions222", "${bindingAdapterPosition}")

//                        viewBinding.lblDate.text = ""
//                        viewBinding.lblDate.visibility = View.GONE
                }
//                }
            } catch (e: Exception) {
                viewBinding.messageTime.text = e.message
                viewBinding.llDateLayout.visibility = View.GONE
            }
            viewBinding.root.setOnClickListener {
                //holder.viewBinding.photoCover.transitionName = "profilePhoto"
//                listener.onChatMessageClick(bindingAdapterPosition, item)
                if (item!!.node!!.roomId.name != "" || item.node!!.roomId.id != "") {
                    listener.onChatMessageClick(bindingAdapterPosition, item)
                }

            }
            viewBinding.messageUserAvatar.setOnClickListener {
                //holder.viewBinding.photoCover.transitionName = "profilePhoto"
//                listener.onChatUserAvtarClick()
                if (item!!.node!!.roomId.name != "" || item.node!!.roomId.id != "") {
                    listener.onChatUserAvtarClick()
                }
            }
            /*  viewBinding.ivHorizontal.setOnClickListener {
                  setPowerMenu()
              }*/
            viewBinding.ivMenu.setOnClickListener {
                setPowerMenu()
            }
            viewBinding.root.setOnLongClickListener(OnLongClickListener { v ->
                val powerMenu = PowerMenu.Builder(ctx)
//                    .addItemList(list) // list has "Novel", "Poetry", "Art"
                    // aad an item list.
                    .setAnimation(MenuAnimation.SHOWUP_TOP_LEFT) // Animation start point (TOP | LEFT).
                    .setMenuRadius(10f) // sets the corner radius.
                    .setMenuShadow(10f) // sets the shadow.
                    .setTextColor(ctx.resources.getColor(R.color.black))
                    .setTextGravity(Gravity.CENTER)
                    .setTextTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
                    .setSelectedTextColor(Color.WHITE)
                    .setMenuColor(Color.WHITE)
                    .setShowBackground(false)
                    .setSelectedMenuColor(ctx.resources.getColor(R.color.white))
                    .build();
//                Translation
                powerMenu.addItem(
                    PowerMenuItem(
//                        viewBinding.root.context.resources.getString(R.string.translation),
                        AppStringConstant1.translation,
                        false
                    )
                ) // add an item.
                powerMenu.addItem(
                    PowerMenuItem(
                        AppStringConstant1.copy,
                        false
                    )
                )
                if (!viewBinding.translatedMessageText.text.toString().isNullOrEmpty()) {
                    powerMenu.addItem(
                        PowerMenuItem(
                            AppStringConstant1.copy_translated_message,
                            false
                        )
                    )
                }
//                powerMenu.showAsAnchorLeftTop(viewBinding.root)
                powerMenu.showAsDropDown(
                    viewBinding.root,
                    viewBinding.root.getMeasuredWidth() / 2,
                    -viewBinding.root.getMeasuredHeight() / 2
                )
                powerMenu.setOnMenuItemClickListener { position, item ->
//                        moreMenu.selectedPosition = position
                    when (position) {
                        0 -> {
                            callTranslation(
                                viewBinding.translationDivider,
                                viewBinding.translatedMessageText,
                                viewBinding.messageText.text.toString(),
                                /*        getMainActivity()?.pref?.getString("language","en").toString(),
                                        mcontent!!.appLanguageCode*/
                                "auto",
                                getMainActivity()?.pref?.getString("language", "en").toString()
                            )
                            //  viewBinding.ivHorizontal.visibility = View.GONE
                            //    viewBinding.ivMenu.visibility = View.VISIBLE
                            powerMenu.dismiss()
                        }
                        1 -> {
                            powerMenu.dismiss()
                            copyToClipboard(viewBinding.messageText.text.toString(), ctx)
                        }
                        2 -> {
                            powerMenu.dismiss()

                            copyToClipboard(viewBinding.translatedMessageText.text.toString(), ctx)
                        }
                    }
//                        viewBinding.messageText.text = content
                }
//                Toast.makeText(v.context, "Position is $bindingAdapterPosition", Toast.LENGTH_SHORT)
//                    .show()
                false
            })
            lastMessageItem = item
        }

        private fun setPowerMenu() {
            var powerMenu = PowerMenu.Builder(ctx)
                //                    .addItemList(list) // list has "Novel", "Poetry", "Art"
                // aad an item list.
                .setAnimation(MenuAnimation.SHOWUP_TOP_LEFT) // Animation start point (TOP | LEFT).
                .setMenuRadius(10f) // sets the corner radius.
                .setMenuShadow(10f) // sets the shadow.
                .setTextColor(ctx.resources.getColor(R.color.black))
                .setTextGravity(Gravity.CENTER)
                .setTextTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
                .setSelectedTextColor(Color.WHITE)
                .setMenuColor(Color.WHITE)
                .setShowBackground(false)
                .setSelectedMenuColor(ctx.resources.getColor(R.color.white))
                .build();
            powerMenu.addItem(
                PowerMenuItem(
                    AppStringConstant1.translation,
                    false
                )
            ) // add an item.
            powerMenu.addItem(
                PowerMenuItem(
                    AppStringConstant1.copy,
                    false
                )
            )
            if (!viewBinding.translatedMessageText.text.toString().isNullOrEmpty()) {
                powerMenu.addItem(
                    PowerMenuItem(
                        AppStringConstant1.copy_translated_message,
                        false
                    )
                )
            }
            //                powerMenu.showAsAnchorLeftTop(viewBinding.root)
            powerMenu.showAsDropDown(
                viewBinding.root,
                viewBinding.root.getMeasuredWidth() / 2,
                -viewBinding.root.getMeasuredHeight() / 2
            )
            powerMenu.setOnMenuItemClickListener { position, item ->
                //                        moreMenu.selectedPosition = position
                when (position) {
                    0 -> {
                        callTranslation(
                            viewBinding.translationDivider,
                            viewBinding.translatedMessageText,
                            viewBinding.messageText.text.toString(),
                            /*        getMainActivity()?.pref?.getString("language","en").toString(),
                                        mcontent!!.appLanguageCode*/
                            "auto",
                            getMainActivity()?.pref?.getString("language", "en").toString()
                        )
                        //   viewBinding.ivHorizontal.visibility = View.GONE
                        //   viewBinding.ivMenu.visibility = View.VISIBLE
                        powerMenu.dismiss()
                    }
                    1 -> {
                        powerMenu.dismiss()
                        copyToClipboard(viewBinding.messageText.text.toString(), ctx)
                    }
                    2 -> {
                        powerMenu.dismiss()

                        copyToClipboard(viewBinding.translatedMessageText.text.toString(), ctx)
                    }
                }

                //                        viewBinding.messageText.text = content
            }
        }
    }

    inner class OutgoingHolder(val viewBinding: ItemNewOutcomingTextMessageBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(item: GetChatMessagesByRoomIdQuery.Edge?, position: Int) {
            val content = item?.node?.content
            val mainContent = item?.node
            Log.e("inPtypemessageareComes", mainContent?.messageType.toString())

            if (content?.contains("media/chat_files") == true) {
                var fullUrl = content
                var message = ""
                if (fullUrl.contains(" ")) {
                    val link = fullUrl.substring(0, fullUrl.indexOf(" "))
                    val giftMessage = content.substring(fullUrl.indexOf(" ") + 1)
                    fullUrl = link
                    message = giftMessage
                } else {
                    if (content.startsWith("/media/chat_files/")) {
                        fullUrl = "${BuildConfig.BASE_URL}$content"
                    }
                }

                val uri = Uri.parse(fullUrl)
                val lastSegment = uri.lastPathSegment
                val ext = lastSegment?.substring(lastSegment.lastIndexOf(".") + 1)
                Timber.d("clickk $lastSegment $ext")
                if (ext?.isImageFile() == true || ext?.isVideoFile() == true) {
                    if (message.isNotEmpty()) {
                        viewBinding.messageText.text = message
                    } else {
                        viewBinding.messageText.text = ""
                    }
                    viewBinding.messageImage.loadImage(
                        fullUrl,
                        RequestOptions().centerCrop(),
                        null,
                        null
                    )
                    viewBinding.messageImage.visibility = View.VISIBLE
                    viewBinding.messageFileIcon.visibility = View.GONE
                    viewBinding.mapView.visibility = View.GONE
                    viewBinding.messagePlayIcon.visibility =
                        if (ext.isVideoFile()) View.VISIBLE else View.GONE
                } else {
                    viewBinding.messageText.text = lastSegment
                    viewBinding.messageImage.setImageBitmap(null)
                    viewBinding.messageFileIcon.visibility = View.VISIBLE
                    viewBinding.messageImage.visibility = View.GONE
                    viewBinding.messagePlayIcon.visibility = View.GONE
                    viewBinding.mapView.visibility = View.GONE
                }

                viewBinding.ctRequestview.visibility = View.GONE
            } else {
                if (mainContent?.messageType == MessageMessageType.GL) {
//                if (mainContent?.messageType == MessageMessageType.C) {

                    viewBinding.messageText.text = ""
                    viewBinding.mapView.visibility = View.VISIBLE
                    viewBinding.messageImage.setImageBitmap(null)
                    viewBinding.messageImage.visibility = View.GONE
                    viewBinding.messageFileIcon.visibility = View.GONE
                    viewBinding.messagePlayIcon.visibility = View.GONE
                    viewBinding.ctRequestview.visibility = View.GONE
                    val items = content?.split(",")?.toTypedArray()
                    val latitude = items?.get(0)?.toDouble()
                    val longitude = items?.get(1)?.toDouble()
                    // Initialise the MapView
                    viewBinding.mapView.onCreate(null)
                    viewBinding.mapView.onResume()
                    // Set the map ready callback to receive the GoogleMap object
                    viewBinding.mapView.getMapAsync(object : OnMapReadyCallback{

                        override fun onMapReady(googleMap: GoogleMap) {
                            googleMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(latitude!!, longitude!!),
                                    13f
                                )
                            )
                            googleMap.addMarker(
                                MarkerOptions().position(
                                    LatLng(latitude, longitude)
                                )
                            )
                            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                            googleMap.uiSettings.setAllGesturesEnabled(false)
                        }


                    })
//                    viewBinding.mapView.getMapAsync {
//
////                        viewBinding.mapView.onResume()
//                    }
                } else if (mainContent?.messageType == MessageMessageType.P) {
                    viewBinding.ctRequestview.visibility = View.VISIBLE
                    when (mainContent?.requestStatus) {
                        "PENDING" -> {
                            viewBinding.cdView.visibility = View.GONE
                        }
                        "APPROVE" -> {
                            viewBinding.cdView.visibility = View.VISIBLE
                        }
                        else -> {
                            viewBinding.cdView.visibility = View.GONE
                        }
                    }

                    viewBinding.messageText.text = content
                    viewBinding.mapView.visibility = View.GONE
                    viewBinding.messageImage.setImageBitmap(null)
                    viewBinding.messageImage.visibility = View.GONE
                    viewBinding.messageFileIcon.visibility = View.GONE
                    viewBinding.messagePlayIcon.visibility = View.GONE
                } else {
                    viewBinding.ctRequestview.visibility = View.GONE
                    viewBinding.messageText.text = content
                    viewBinding.mapView.visibility = View.GONE
                    viewBinding.messageImage.setImageBitmap(null)
                    viewBinding.messageImage.visibility = View.GONE
                    viewBinding.messageFileIcon.visibility = View.GONE
                    viewBinding.messagePlayIcon.visibility = View.GONE
                }
            }

            viewBinding.cdView.setOnClickListener {
                listener.onChatUserAvtarClick()
            }
            Log.d(
                "NewChatFragment",
                "lastSeenMessageId $lastSeenMessageId Item ${item?.node?.id}\n"
            )
            if (lastSeenMessageId == item?.node?.id) {
                /*    var avatarUrl: String? = ""
                    try {

                        avatarUrl = item.node.userId.avatarPhotos?.get(item.node.userId.avatarIndex)?.url

                    } catch (e: Exception) {
                        avatarUrl = ""
                    }*/
                Log.d("NewChatFragment", "")
                if (otherUserAvtar.isNotEmpty()) {
                    viewBinding.ivSeenimage.loadCircleImage(otherUserAvtar)
                } else {
                    viewBinding.ivSeenimage.loadCircleImage(R.drawable.ic_chat_item_logo_new)
                }
                viewBinding.cdSeenimage.visibility = View.VISIBLE
            } else {
                viewBinding.cdSeenimage.visibility = View.GONE
            }

            var avatarUrl: String? = ""
            avatarUrl = try {
                item?.node?.userId?.avatarPhotos?.get(item.node.userId.avatarIndex)?.url
            } catch (e: Exception) {
                ""
            }
            if (avatarUrl != null) {
                viewBinding.messageUserAvatar.loadCircleImage(avatarUrl)
            } else {
                viewBinding.messageUserAvatar.loadCircleImage(R.drawable.ic_chat_item_logo_new)
            }
            //Timber.d("messgg $content ${content?.contains("media/chat_files")}")
            var text = item?.node?.timestamp.toString()
            LogUtil.debug("Current  : : : $text")
            val messageTime: Date?
            try {
//                text = text.replace("T", " ").substring(0, text.indexOf("."))
//                text = text.substring(0, text.indexOf("."))
                messageTime = formatter.parse(text)
//                val messageTimeToCheck = formatterNew.parse(text)
//                val messageTimeToCheck = formatter.parse(text)
                val messageTimeToFormat = formatterNew.format(messageTime)
                val messageTimeToCheck = formatterNew.parse(messageTimeToFormat) as Date

                viewBinding.messageTime.text = displayTime.format(messageTime)
                val displayDate = formatDayDate(ctx, messageTime.time)
//                if (bindingAdapterPosition == 0) {

//                    if (itemCount == 1) {

//                        viewBinding.lblDate.visibility = View.VISIBLE
//                        viewBinding.lblDate.text = displayDate
//                    }
//                } else {


                if (differ.currentList.size > bindingAdapterPosition + 1) {

//                    val preItem = differ.currentList[bindingAdapterPosition - 1]
                    val preItem = differ.currentList[bindingAdapterPosition + 1]


                    var preTime = preItem?.node?.timestamp.toString()
                    LogUtil.debug("Pre  : : : $preTime")
                    val preDateToCheck = formatter.parse(preTime)
                    val preDateToFormat = formatterNew.format(preDateToCheck)
                    val preDate = formatterNew.parse(preDateToFormat) as Date

//                        preTime = preTime.replace("T", " ").substring(0, preTime.indexOf("."))
//                        preTime = preTime.substring(0, preTime.indexOf("."))
//                        val preDate = formatterNew.parse(preTime)

//                        val preDisplayDate = formatDayDate(ctx, preDate.time)
//                    LogUtil.debug("AbcdpreDisplayDate $preDisplayDate")
//                    LogUtil.debug("AbcddisplayDate $displayDate")

                    if (messageTimeToCheck == preDate) {
                        viewBinding.lblDate.text = ""
                        viewBinding.llDateLayout.visibility = View.GONE
                    } else {
                        viewBinding.llDateLayout.visibility = View.VISIBLE
                        viewBinding.lblDate.text = displayDate
                    }

                } else {
//                        val preItem = differ.currentList[bindingAdapterPosition ]
//
//                        var preTime = preItem?.node?.timestamp.toString()
//                        LogUtil.debug("Pre  : : : $preTime")
//
//                        val preDateToCheck = formatter.parse(preTime)
//                        val preDateToFormat = formatterNew.format(preDateToCheck)
//                        val preDate = formatterNew.parse(preDateToFormat) as Date
//                        if (messageTimeToCheck == preDate) {
//                            viewBinding.lblDate.text = ""
//                            viewBinding.lblDate.visibility = View.GONE
//                        } else {
                    viewBinding.llDateLayout.visibility = View.VISIBLE
                    viewBinding.lblDate.text = displayDate
//                        }


//                        viewBinding.lblDate.text = ""
//                        viewBinding.lblDate.visibility = View.GONE
                }
//                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewBinding.messageTime.text = e.message
            }

            viewBinding.root.setOnClickListener {
                //holder.viewBinding.photoCover.transitionName = "profilePhoto"
//                listener.onChatMessageClick(bindingAdapterPosition, item)
                if (!item!!.node!!.roomId.name.equals("") || !item.node!!.roomId.id.equals("")) {
                    listener.onChatMessageClick(bindingAdapterPosition, item)
                }
            }
            viewBinding.ivMenu.setOnClickListener {
                var powerMenu = PowerMenu.Builder(ctx)
//                    .addItemList(list) // list has "Novel", "Poetry", "Art"
                    // aad an item list.
                    .setAnimation(MenuAnimation.SHOWUP_TOP_LEFT) // Animation start point (TOP | LEFT).
                    .setMenuRadius(10f) // sets the corner radius.
                    .setMenuShadow(10f) // sets the shadow.
                    .setTextColor(ctx.resources.getColor(R.color.black))
                    .setTextGravity(Gravity.CENTER)
                    .setTextTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
                    .setSelectedTextColor(Color.WHITE)
                    .setMenuColor(Color.WHITE)
                    .setShowBackground(false)
                    .setSelectedMenuColor(ctx.resources.getColor(R.color.white))
                    .build();

                powerMenu.addItem(
                    PowerMenuItem(
                        AppStringConstant1.copy,
                        false
                    )
                )

                if (mainContent?.messageType != MessageMessageType.G) {
                    powerMenu.addItem(
                        PowerMenuItem(
                            AppStringConstant1.delete,
                            false
                        )
                    ) // add an item.
                }
                if (!viewBinding.translatedMessageText.text.toString().isNullOrEmpty()) {
                    powerMenu.addItem(
                        PowerMenuItem(
                            AppStringConstant1.copy_translated_message,
                            false
                        )
                    )
                }


//                powerMenu.showAsAnchorLeftTop(viewBinding.root)
                powerMenu.showAsDropDown(
                    viewBinding.root,
                    viewBinding.root.getMeasuredWidth() / 2,
                    -viewBinding.root.getMeasuredHeight() / 2
                )
                powerMenu.setOnMenuItemClickListener { position, menuItem ->
//                        moreMenu.selectedPosition = position
                    when (position) {
                        0 -> {
                            Log.d("ChatAdapter", "Called")

                            powerMenu.dismiss()
                            copyToClipboard(viewBinding.messageText.text.toString(), ctx)
//                            listener.onChatMessageDelete(item)
                            /*        callTranslation(viewBinding.translationDivider,viewBinding.translatedMessageText,viewBinding.messageText.text.toString(),
                                        getMainActivity()?.pref?.getString("language","en").toString(),"fr")*/
                            powerMenu.dismiss()
                        }
                        1 -> {
                            powerMenu.dismiss()
                            listener.onChatMessageDelete(item)
//                            copyToClipboard(viewBinding.messageText.text.toString(), ctx)
                        }
                        2 -> {
                            powerMenu.dismiss()
                            copyToClipboard(viewBinding.translatedMessageText.text.toString(), ctx)
                        }
                    }

//                        viewBinding.messageText.text = content
                }

            }

            viewBinding.root.setOnLongClickListener { v ->
                val powerMenu = PowerMenu.Builder(ctx)
//                    .addItemList(list) // list has "Novel", "Poetry", "Art"
                    // aad an item list.
                    .setAnimation(MenuAnimation.SHOWUP_TOP_LEFT) // Animation start point (TOP | LEFT).
                    .setMenuRadius(10f) // sets the corner radius.
                    .setMenuShadow(10f) // sets the shadow.
                    .setTextColor(ctx.resources.getColor(R.color.black))
                    .setTextGravity(Gravity.CENTER)
                    .setTextTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
                    .setSelectedTextColor(Color.WHITE)
                    .setMenuColor(Color.WHITE)
                    .setShowBackground(false)
                    .setSelectedMenuColor(ctx.resources.getColor(R.color.white))
                    .build();
                powerMenu.addItem(
                    PowerMenuItem(
                        AppStringConstant1.delete,
                        false
                    )
                ) // add an item.
                powerMenu.addItem(
                    PowerMenuItem(
                        AppStringConstant1.copy,
                        false
                    )
                )
                if (!viewBinding.translatedMessageText.text.toString().isNullOrEmpty()) {
                    powerMenu.addItem(
                        PowerMenuItem(
                            AppStringConstant1.copy_translated_message,
                            false
                        )
                    )
                }
//                powerMenu.showAsAnchorLeftTop(viewBinding.root)
                powerMenu.showAsDropDown(
                    viewBinding.root,
                    viewBinding.root.measuredWidth / 2,
                    -viewBinding.root.measuredHeight / 2
                )
                powerMenu.setOnMenuItemClickListener { position, menuItem ->
//                        moreMenu.selectedPosition = position
                    when (position) {
                        0 -> {
                            Log.d("ChatAdapter", "Called")
                            listener.onChatMessageDelete(item)
                            /*        callTranslation(viewBinding.translationDivider,viewBinding.translatedMessageText,viewBinding.messageText.text.toString(),
                                        getMainActivity()?.pref?.getString("language","en").toString(),"fr")*/
                            powerMenu.dismiss()
                        }
                        1 -> {
                            powerMenu.dismiss()
                            copyToClipboard(viewBinding.messageText.text.toString(), ctx)
                        }
                        2 -> {
                            powerMenu.dismiss()
                            copyToClipboard(viewBinding.translatedMessageText.text.toString(), ctx)
                        }
                    }
//                        viewBinding.messageText.text = content
                }
                false
            }
            lastMessageItem = item
        }
    }

    interface ChatMessageListener {
        fun onChatMessageClick(position: Int, message: GetChatMessagesByRoomIdQuery.Edge?)
        fun onChatUserAvtarClick()
        fun onChatMessageDelete(message: GetChatMessagesByRoomIdQuery.Edge?)
        fun onPrivatePhotoAccessResult(decision: String, requestId: Int)
    }

    private fun callTranslation(
        divider: View,
        translatedtoTextview: TextView,
        textfortranslation: String,
        fromLangCodeSupport: String,
        toLangCodeSupport: String
    ) {
        if (NetworkUtils.isNetworkConnected(ctx)) {
            val textToTranslate = textfortranslation.trim()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                progress_search.progressTintList = ColorStateList.valueOf(Color.WHITE)
            } else {
//                progress_search.indeterminateDrawable.setColorFilter(
//                    ctx.resources.getColor(R.color.white),
//                    PorterDuff.Mode.SRC_ATOP
//                )
            }
//            progress_search.visibility = View.VISIBLE
            if (translationUtils != null) {
                translationUtils?.StopBackground()
            }
            translationUtils = TranslationUtils(object : TranslationUtils.ResultCallBack {
                override fun onReceiveResult(result: String?) {
//                    setTranslation(result!!)
//                    insertHistory(textToTranslate, result)
                    translatedtoTextview.visibility = View.VISIBLE
                    divider.visibility = View.VISIBLE
                    translatedtoTextview.text = result.toString()
                    Log.d("TranslationText", result.toString())
                }

                override fun onFailedResult() {
                    Toast.makeText(
                        ctx,
                        AppStringConstant1.translation_failed,
                        Toast.LENGTH_LONG
                    ).show()
//                    handleErrorResponse("Network error, please check the network and try again")
                }

            }, textToTranslate, fromLangCodeSupport, toLangCodeSupport)
            translationUtils?.execute()
            Log.d(
                "ChatAdapter",
                "fromLangCodeSupport $fromLangCodeSupport toLangCodeSupport $toLangCodeSupport $textToTranslate"
            )
        } else {
            Toast.makeText(ctx, "Turn on Internet Connection", Toast.LENGTH_LONG).show()
        }
    }
}

