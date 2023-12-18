package com.i69.ui.screens.main.messenger.list

import android.graphics.Typeface
import android.os.Build
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.i69.GetAllRoomsQuery
import com.i69.R
import com.i69.databinding.ItemMessageBinding
import com.i69.type.MessageMessageType
import com.i69.utils.*
import java.text.SimpleDateFormat
import java.util.*

class MessengerListAdapter(
    private val listener: MessagesListListener,
    private val userId: String?
) : RecyclerView.Adapter<MessengerListAdapter.ViewHolder>() {

    private val itemColors = listOf(
//        R.color.message_list_container_1, R.color.message_list_container_2,
        R.color.message_list_container_6, R.color.message_list_container_2,
        R.color.message_list_container_3, R.color.message_list_container_4,
        R.color.message_list_container_5
    )

    //    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
//        timeZone = TimeZone.getTimeZone("UTC")
//    }
    private val formatter =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZ", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private val formatterNew =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZ", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    private val textPairColors = listOf(
//        Pair(R.color.message_list_text_title_color_1, R.color.message_list_description_color_1),
//        Pair(R.color.message_list_text_title_color_2, R.color.message_list_description_color_2),
//        Pair(R.color.message_list_text_title_color_2, R.color.message_list_description_color_2),
//        Pair(R.color.message_list_text_title_color_3, R.color.message_list_description_color_3),
//        Pair(R.color.message_list_text_title_color_4, R.color.message_list_description_color_4),
//        Pair(R.color.message_list_text_title_color_5, R.color.message_list_description_color_5)


        Pair(R.color.message_list_text_title_color_2, R.color.message_list_text_title_color_2),
        Pair(R.color.message_list_text_title_color_2, R.color.message_list_text_title_color_2),
        Pair(R.color.message_list_text_title_color_3, R.color.message_list_text_title_color_2),
        Pair(R.color.message_list_text_title_color_4, R.color.message_list_text_title_color_2),
        Pair(R.color.message_list_text_title_color_5, R.color.message_list_text_title_color_2)
    )

    private val diffUtilCallBack = object : DiffUtil.ItemCallback<GetAllRoomsQuery.Edge>() {
        override fun areItemsTheSame(
            oldItem: GetAllRoomsQuery.Edge,
            newItem: GetAllRoomsQuery.Edge
        ): Boolean {
            return oldItem.node?.id?.toInt() == newItem.node?.id?.toInt()
        }

        override fun areContentsTheSame(
            oldItem: GetAllRoomsQuery.Edge,
            newItem: GetAllRoomsQuery.Edge
        ): Boolean {
            return when {
                oldItem.node?.id != newItem.node?.id -> false
                oldItem.node?.unread != newItem.node?.unread -> false
                else -> true
            }
            // return oldItem.node?.id?.toInt()==newItem.node?.id?.toInt()
        }
    }

    private val differ = AsyncListDiffer(this, diffUtilCallBack)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MessengerListAdapter.ViewHolder =
        ViewHolder(ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: MessengerListAdapter.ViewHolder, position: Int) {
        val context = holder.viewBinding.root.context
        val roomData = differ.currentList[position]
        val msg = if (roomData.node?.messageSet?.edges?.isEmpty() == true) {
            null
        } else {
            roomData.node?.messageSet?.edges?.get(0)?.node
        }

        // color toggle

        val typeface_regular = Typeface.createFromAsset(context.assets, "fonts/poppins_semibold.ttf")
        val typeface_light = Typeface.createFromAsset(context.assets, "fonts/poppins_light.ttf")


        holder.viewBinding.title.typeface = typeface_regular;
        holder.viewBinding.subtitle.typeface = typeface_light;
        holder.viewBinding.time.typeface = typeface_light;
        holder.viewBinding.unseenMessages.typeface = typeface_regular;

//        if (position % 2 == 0) {
//            holder.viewBinding.rootChiled.background = context.getDrawable(R.color.transparent);
//            holder.viewBinding.title.setTextColor(
//                ContextCompat.getColor(
//                    context,
//                    R.color.white
//                )
//            )
//            holder.viewBinding.subtitle.setTextColor(
//                ContextCompat.getColor(
//                    context,
//                    R.color.white
//                )
//            )
//            holder.viewBinding.time.setTextColor(
//                ContextCompat.getColor(
//                    context,
//                    R.color.white
//                )
//            )
//            holder.viewBinding.continueDialog.setColorFilter(
//                ContextCompat.getColor(
//                    context,
//                    R.color.white
//                )
//            )
//        } else {
//            holder.viewBinding.time.setTextColor(
//                ContextCompat.getColor(
//                    context,
//                    R.color.black
//                )
//            )
//            holder.viewBinding.rootChiled.background = context.getDrawable(R.color.white);
//            holder.viewBinding.continueDialog.setColorFilter(
//                ContextCompat.getColor(
//                    context,
//                    R.color.black
//                )
//            )
//
//            holder.viewBinding.title.setTextColor(
//                ContextCompat.getColor(
//                    context,
//                    R.color.black
//                )
//            )
//            holder.viewBinding.subtitle.setTextColor(
//                ContextCompat.getColor(
//                    context,
//                    R.color.black
//                )
//            )
//
//
//        }


        LogUtil.debug("MessageType : : : ${msg?.messageType}")
        var option: Int
        holder.viewBinding.apply {
            this.obj = roomData
            subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, null, null, null
            )
            if (roomData.node?.userId?.id.equals(userId)) {
                roomData.node?.target?.avatar.let { imgSrc ->
                    if (imgSrc != null) {
                        imgSrc.url?.let { img.loadCircleImage(it) }
                    } else {
                        img.loadCircleImage("")
                    }
                }
                title.text = roomData.node?.target?.fullName
                if (roomData.node?.target?.isOnline == true) {
                    onlineStatus.setImageResource(R.drawable.round_green)
                } else {
                    onlineStatus.setImageResource(R.drawable.round_yellow)
                }
                if (!roomData.node?.unread.equals("0")) {
                    option = 1
                    unseenMessages.text = roomData.node?.unread
                    unseenMessages.visibility = View.VISIBLE
                    continueDialog.visibility = View.GONE


                    holder.viewBinding.time.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.black
                        )
                    )
                    holder.viewBinding.rootChiled.background = context.getDrawable(R.color.white);
                    holder.viewBinding.continueDialog.setColorFilter(
                        ContextCompat.getColor(
                            context,
                            R.color.black
                        )
                    )

                    holder.viewBinding.title.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.black
                        )
                    )
                    holder.viewBinding.subtitle.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.black
                        )
                    )



                } else {
                    option = 0
                    unseenMessages.visibility = View.GONE
                    continueDialog.visibility = View.VISIBLE

                    holder.viewBinding.rootChiled.background = context.getDrawable(android.R.color.transparent);
                    holder.viewBinding.title.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    )
                    holder.viewBinding.subtitle.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    )
                    holder.viewBinding.time.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    )
                    holder.viewBinding.continueDialog.setColorFilter(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    )

                }
                //  root.setBackgroundColor(ContextCompat.getColor(context, itemColors[option]))
//                title.setTextColor(
//                    ContextCompat.getColor(
//                        context,
//                        textPairColors[option].first
//                    )
//                )
//                subtitle.setTextColor(
//                    ContextCompat.getColor(
//                        context,
//                        textPairColors[option].second
//                    )
//                )
//                continueDialog.setColorFilter(
//                    ContextCompat.getColor(
//                        context,
//                        textPairColors[option].second
//                    )
//                )
                if (msg?.content?.contains("media/chat_files") == true) {
                    val ext = msg.content.findFileExtension()
                    val stringResId =
                        if (ext.isImageFile()) {
                            R.string.photo
                        } else if (ext.isVideoFile()) {
                            R.string.video
                        } else {
                            R.string.file
                        }
                    var icon =
                        if (ext.isImageFile()) {
                            R.drawable.ic_photo
                        } else if (ext.isVideoFile()) {
                            R.drawable.ic_video
                        } else {
                            R.drawable.ic_baseline_attach_file_24
                        }
                    subtitle.text = context.getString(stringResId)

                    if (stringResId.equals(R.string.file) && (msg?.messageType
                            ?: "") == MessageMessageType.G
                    ) {
//                        icon = R.drawable.ic_baseline_card_giftcard_24
//                        icon = R.drawable.icon_gifts
                        icon = R.drawable.ic_gift_card
                        subtitle.text = context.getString(R.string.gifts)
                    } else {
                        subtitle.text = context.getString(stringResId)
                    }
                    subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        ContextCompat.getDrawable(
                            context,
                            icon
                        ), null, null, null
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        subtitle.compoundDrawableTintList = ContextCompat.getColorStateList(
                            context,
                            textPairColors[option].second
                        )
                    }
                } else {
                    if (roomData.node?.id == "001" || roomData.node?.id == "000") {
                        subtitle.text = roomData.node.name
                        subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            null, null, null, null
                        )
                    } else {
                        if ((msg?.messageType ?: "") == MessageMessageType.GL) {
//                        if ((msg?.messageType ?: "") == MessageMessageType.C) {
                            subtitle.text = context.resources.getString(R.string.location)
                            subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                ContextCompat.getDrawable(
                                    context,
                                    R.drawable.location_icon_new
                                ), null, null, null
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                subtitle.compoundDrawableTintList = null
                            }
                        } else {
                            subtitle.text = msg?.content
                            subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                null, null, null, null
                            )
                        }
                    }
                }
                if (roomData.node?.lastModified != null) {
                    var text = roomData.node?.lastModified.toString()
//                    text = text.replace("T", " ").substring(0, text.indexOf("."))
                    val momentTime = formatter.parse(text)

//                    val messageTime = formatter.parse(text)
//
//                    val messageTimeToFormat = formatterNew.format(messageTime)
//                    val momentTime = formatterNew.parse(messageTimeToFormat) as Date

                    time.text = DateUtils.getRelativeTimeSpanString(
                        momentTime.time,
                        Date().time,
                        DateUtils.MINUTE_IN_MILLIS
                    )
                }
            } else {
                roomData.node?.userId?.avatar.let { imgSrc ->
                    when {
                        imgSrc != null -> {
                            img.loadCircleImage(imgSrc.url!!)
                        }
                        roomData.node?.id == "001" || roomData.node?.id == "000" -> {
                            img.loadCircleImage(R.drawable.ic_chat_item_logo_new)
                        }
                        else -> {
                            img.loadCircleImage("")
                        }
                    }
                }
                title.text = roomData.node?.userId?.fullName
                if (roomData.node?.userId?.isOnline == true) {
                    onlineStatus.setImageResource(R.drawable.round_green)
                } else {
                    onlineStatus.isVisible =
                        !(roomData.node?.id == "001" || roomData.node?.id == "000")
                    onlineStatus.setImageResource(R.drawable.round_yellow)
                }
                if (!roomData.node?.unread.equals("0")) {
                    option = 1
                    unseenMessages.text = roomData.node?.unread
                    unseenMessages.visibility = View.VISIBLE
                    continueDialog.visibility = View.GONE

                    holder.viewBinding.time.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.black
                        )
                    )
                    holder.viewBinding.rootChiled.background = context.getDrawable(R.color.white);
                    holder.viewBinding.continueDialog.setColorFilter(
                        ContextCompat.getColor(
                            context,
                            R.color.black
                        )
                    )

                    holder.viewBinding.title.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.black
                        )
                    )
                    holder.viewBinding.subtitle.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.black
                        )
                    )

                } else {
                    option = 0
                    unseenMessages.visibility = View.GONE
                    continueDialog.visibility = View.VISIBLE


                    holder.viewBinding.rootChiled.background = context.getDrawable(android.R.color.transparent);
                    holder.viewBinding.title.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    )
                    holder.viewBinding.subtitle.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    )
                    holder.viewBinding.time.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    )
                    holder.viewBinding.continueDialog.setColorFilter(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    )





                }
                //root.setBackgroundColor(ContextCompat.getColor(context, itemColors[option]))
//                title.setTextColor(
//                    ContextCompat.getColor(
//                        context,
//                        textPairColors[option].first
//                    )
//                )
//                subtitle.setTextColor(
//                    ContextCompat.getColor(
//                        context,
//                        textPairColors[option].second
//                    )
//                )
//                continueDialog.setColorFilter(
//                    ContextCompat.getColor(
//                        context,
//                        textPairColors[option].second
//                    )
//                )
                if (msg?.content?.contains("media/chat_files") == true) {
                    val ext = msg.content.findFileExtension()
                    val stringResId =
                        if (ext.isImageFile()) {
                            R.string.photo
                        } else if (ext.isVideoFile()) {
                            R.string.video
                        } else {
                            R.string.file
                        }
                    var icon =
                        when {
                            ext.isImageFile() -> {
                                R.drawable.ic_photo
                            }
                            ext.isVideoFile() -> {
                                R.drawable.ic_video
                            }
                            else -> {
                                R.drawable.ic_baseline_attach_file_24
                            }
                        }

                    if (stringResId.equals(R.string.file) && (msg?.messageType
                            ?: "") == MessageMessageType.G
                    ) {
//                        icon = R.drawable.ic_baseline_card_giftcard_24
//                        icon = R.drawable.icon_gifts
                        icon = R.drawable.ic_gift_card
                        subtitle.text = context.getString(R.string.gifts)
                    } else {
                        subtitle.text = context.getString(stringResId)
                    }



                    subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        ContextCompat.getDrawable(
                            context,
                            icon
                        ), null, null, null
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        subtitle.compoundDrawableTintList = ContextCompat.getColorStateList(
                            context,
                            textPairColors[option].second
                        )
                    }
                } else {
                    if (roomData.node?.id == "001" || roomData.node?.id == "000") {
                        subtitle.text = roomData.node?.name
                        subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            null, null, null, null
                        )
                    } else {
                        if ((msg?.messageType ?: "") == MessageMessageType.GL) {
//                        if ((msg?.messageType ?: "") == MessageMessageType.C) {
                            subtitle.text = context.resources.getString(R.string.location)
                            subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                ContextCompat.getDrawable(
                                    context,
                                    R.drawable.location_icon_new
                                ), null, null, null
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                subtitle.compoundDrawableTintList = null
                            }
                        } else {
                            subtitle.text = msg?.content
                            subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                null, null, null, null
                            )
                        }
                    }
                }
                if (roomData.node?.lastModified != null) {
                    var text = roomData.node.lastModified.toString()
//                    text = text.replace("T", " ").substring(0, text.indexOf("."))
                    val momentTime = formatter.parse(text)

//                    val messageTime = formatter.parse(text)
//                    val messageTimeToFormat = formatterNew.format(messageTime)
//                    val momentTime = formatterNew.parse(messageTimeToFormat) as Date

                    time.text = DateUtils.getRelativeTimeSpanString(
                        momentTime.time,
                        Date().time,
                        DateUtils.MINUTE_IN_MILLIS
                    )
                }
            }
            root.setOnClickListener {
                LogUtil.debug("Clicked")
                listener.onItemClick(roomData, position)
            }
            executePendingBindings()
        }
    }

    override fun getItemCount() = differ.currentList.size

    fun updateList(updatedList: List<GetAllRoomsQuery.Edge?>) {
        if (updatedList.isEmpty()) {
            differ.submitList(emptyList())
            return
        }
        differ.submitList(updatedList)
        notifyDataSetChanged()
    }

    fun submitList1(list: MutableList<GetAllRoomsQuery.Edge?>) {
        val newList = list.toSet()
        val x = mutableListOf<GetAllRoomsQuery.Edge?>().apply {
            addAll(newList)
        }
        differ.submitList(x)

    }

    inner class ViewHolder(val viewBinding: ItemMessageBinding) :
        RecyclerView.ViewHolder(viewBinding.root)

    fun interface MessagesListListener {
        fun onItemClick(userId: GetAllRoomsQuery.Edge, position: Int)
    }

}
