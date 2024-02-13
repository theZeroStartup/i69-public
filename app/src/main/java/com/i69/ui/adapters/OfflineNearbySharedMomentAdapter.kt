package com.i69.ui.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.ViewDataBinding
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.i69.BuildConfig
import com.i69.GetAllUserMomentsQuery
import com.i69.R
import com.i69.applocalization.AppStringConstant1
import com.i69.data.models.Moment
import com.i69.databinding.ItemSharedUserMomentBinding
import com.i69.utils.ApiUtil
import com.i69.utils.loadCircleImage
import com.i69.utils.loadImage
import com.i69.utils.setViewGone
import com.i69.utils.setViewVisible
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class OfflineNearbySharedMomentAdapter(
    private val ctx: Context,
//    private val listener: NearbySharedMomentListener,
    private var allusermoments2: ArrayList<Moment>,
    var userId: String?,
    var isShownearByUser: Boolean = true
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedItemPosition: Int = -1

    private val differCallback = object : DiffUtil.ItemCallback<Moment>() {
        override fun areItemsTheSame(
            oldItem: Moment,
            newItem: Moment
        ): Boolean {
            return oldItem.node?.node?.pk == newItem.node?.node?.pk
        }

        override fun areContentsTheSame(
            oldItem: Moment,
            newItem: Moment
        ): Boolean {
            return when {
                oldItem.node?.node?.like != newItem.node?.node?.like -> false
                oldItem.node?.node?.comment != newItem.node?.node?.comment -> false
                oldItem.node?.node?.momentDescription != newItem.node?.node?.momentDescription -> false
                oldItem.node?.node?.momentDescriptionPaginated != newItem.node?.node?.momentDescriptionPaginated -> false
                //oldList[oldItemPosition].node?.id != newList[newItemPosition].node?.id -> false
                else -> true
            }
            //return oldItem.node?.like == newItem.node?.like && oldItem.node?.comment == newItem.node?.comment
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private var allusermoments = emptyList<Moment>()
    var allusermoments4 = ArrayList(allusermoments)

    private fun getViewHolderByType(type: Int, viewBinding: ViewDataBinding) =
        NearbySharedMomentHolder(viewBinding as ItemSharedUserMomentBinding)

    private fun getViewDataBinding(viewType: Int, parent: ViewGroup) =
        ItemSharedUserMomentBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder =
        getViewHolderByType(type, getViewDataBinding(type, parent))

    var holder: NearbySharedMomentHolder? = null
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as NearbySharedMomentHolder
        this.holder = holder
        val item = differ.currentList.get(position)
        holder.bind(position, item, ctx)
    }

    fun submitList(data: List<Moment>) {
        differ.submitList(data)
    }

    fun submitList1(list: MutableList<Moment>) {
        Log.e("notify_adapter","submitList1 98")
        val x = mutableListOf<Moment>().apply {
            addAll(list)
        }
        differ.submitList(x)
    }

    fun getArrayList(): ArrayList<Moment> {
        val cardList: List<Moment> = differ.getCurrentList()
        return ArrayList<Moment>(cardList)
    }

    fun setData(newWordList: List<Moment>) {
        /*if (allusermoments4.size!=0){
            val fromIndex = allusermoments4.size-11
            val toIndex = allusermoments4.size-1
            allusermoments4.subList(fromIndex, toIndex + 1).clear()
        }*/
        val diffUtil = OfflineNearByMomentsAdapterDiff(allusermoments4, newWordList)
        val diffResults = DiffUtil.calculateDiff(diffUtil)
        allusermoments4 = newWordList as ArrayList<Moment>
        diffResults.dispatchUpdatesTo(this@OfflineNearbySharedMomentAdapter)
    }

    fun updateList(updatedList: ArrayList<Moment>, uid: String?) {
        userId = uid
        //allusermoments.clear()
        //allusermoments.addAll(updatedList)
        // allusermoments=updatedList
        Log.e("notify_adapter","updateList")
        notifyDataSetChanged()
    }


    fun add(r: Moment?) {
        //allusermoments.add(r!!)
        Log.e("notify_adapter","add 134")
        notifyItemInserted(differ.currentList.size - 1)
    }

    fun addAll(newdata: ArrayList<Moment>) {
        newdata.indices.forEach { i ->
            Log.e("notify_adapter","addAll 140")
            add(newdata[i])
        }
    }

    fun pauseAll() {
        selectedItemPosition = -1
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    inner class NearbySharedMomentHolder(val viewBinding: ItemSharedUserMomentBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {

        fun bind(position: Int, item_data: Moment?, ctx: Context) {
            if (item_data!!.node!!.node?.user == null) {
                Log.e("obj_node", "data is null ${differ.currentList.size}")
                return
            }
           // Log.e("obj_node", Gson().toJson(item_data!!))
            val title = item_data!!.node!!.node?.user!!.fullName

//            val s1 = SpannableString( AppStringConstant1.near_by_user)
            val s2 = SpannableString(title!!.toUpperCase())
            val s3 = SpannableString(AppStringConstant1.has_shared_moment)


//            s1.setSpan(
//                ForegroundColorSpan(Color.WHITE),
//                0,
//                s1.length,
//                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
//            )
            s2.setSpan(
                ForegroundColorSpan(this@OfflineNearbySharedMomentAdapter.ctx.resources.getColor(R.color.colorPrimary)),
                0,
                s2.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            s2.setSpan(StyleSpan(Typeface.BOLD), 0, s2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            s3.setSpan(
                ForegroundColorSpan(Color.WHITE),
                0,
                s3.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            // build the string
            val builder = SpannableStringBuilder()
//            builder.append(s1)
//            builder.append(" ")
            builder.append(s2)
            builder.append(" ")
            builder.append(s3)

            viewBinding.lblItemNearbyName.text = builder


            val url = if (!BuildConfig.USE_S3) {
                if (item_data.node?.node?.file.toString().startsWith(BuildConfig.BASE_URL))
                    item_data.node?.node?.file.toString()
                else
                    "${BuildConfig.BASE_URL}${item_data.node?.node?.file.toString()}"
            }
            else if (item_data.node?.node?.file.toString().startsWith(ApiUtil.S3_URL)) item_data?.node?.node?.file.toString()
            else ApiUtil.S3_URL.plus(item_data?.node?.node?.file.toString())
            Log.d("NSMA", "bind: $url, ${item_data.node?.node?.file.toString()}")
            Timber.d("binnd user avatar= ${item_data.node?.node?.user?.avatar}")

            if (isImageFile(item_data)){
                viewBinding.playerView.visibility = View.INVISIBLE
                viewBinding.ivPlay.setViewGone()
                viewBinding.imgSharedMoment.setViewVisible()
                viewBinding.imgSharedMoment.loadImage(url, placeHolderType = 1)
            }
            else {
                if (position == selectedItemPosition && selectedItemPosition != -1) {
                    Log.d("NSMA", "play: $position")

                    viewBinding.ivPlay.setViewGone()
                    viewBinding.imgSharedMoment.visibility = View.INVISIBLE
                    viewBinding.playerView.setViewVisible()

                    val uri: Uri = Uri.parse(url)
                    val mediaItem = MediaItem.Builder()
                        .setUri(uri)
                        .setMimeType(MimeTypes.VIDEO_MP4)
                        .build()
                    playView(mediaItem, true)
                }
                else {
                    viewBinding.playerView.visibility = View.INVISIBLE
                    viewBinding.imgSharedMoment.setViewVisible()
                    viewBinding.imgSharedMoment.loadImage(url)
                    viewBinding.ivPlay.setViewVisible()
                    Log.d("NSMA", "dont play: $position ${viewBinding.playerView?.player == null}")
                }

                viewBinding.ivPlay.setOnClickListener {
                    selectedItemPosition = position
                    notifyDataSetChanged()
                }
            }

            val avatarUrl = item_data.node?.node?.user?.avatar
            if (avatarUrl != null) {
                avatarUrl.url?.replace(
                    "http://95.216.208.1:8000/media/",
                    "${BuildConfig.BASE_URL}media/"
                )?.let {
                    Log.d("NSMA", "avatarUrl: $it")
                    viewBinding.imgNearbyUser.loadCircleImage(it)
                }
            } else {
                viewBinding.imgNearbyUser.loadImage(R.drawable.ic_default_user)
            }

            val sb = StringBuilder()
            item_data.node?.node?.momentDescriptionPaginated!!.forEach { sb.append(it) }
            val descstring = sb.toString().replace("", "")

            if (descstring.isNullOrEmpty()) {
                viewBinding.txtMomentDescription.visibility = View.GONE
            } else {
                viewBinding.txtMomentDescription.text = descstring
                viewBinding.txtMomentDescription.visibility = View.VISIBLE
            }
            var text = item_data.node?.node?.createdDate.toString()
            text = text.replace("T", " ").substring(0, text.indexOf("."))
            val momentTime = formatter.parse(text)

            val times = DateUtils.getRelativeTimeSpanString(
                momentTime.time,
                Date().time,
                DateUtils.MINUTE_IN_MILLIS
            )

            var publishAt = item_data.node?.node?.publishAt.toString()
            Log.d("UMSDF", "setStory: $publishAt")
            var publishTimeInMillis = ""
            if (publishAt.isNotEmpty() && publishAt != "null") {
                publishAt = publishAt.replace("T", " ").substring(0, publishAt.indexOf("+"))
                val momentPublishTime = formatter.parse(publishAt)
                publishTimeInMillis = DateUtils.getRelativeTimeSpanString(
                    momentPublishTime.time,
                    Date().time,
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
            }

            viewBinding.txtTimeAgo.text = publishTimeInMillis.ifEmpty { times }

            viewBinding.txtNearbyUserLikeCount.setText("" + item_data?.node!!.node?.like)

            viewBinding.lblItemNearbyUserCommentCount.setText("" + item_data?.node!!.node?.comment)
            if (item_data.node.node?.user!!.gender != null) {
                if (item_data.node.node?.user!!.gender!!.name.equals("A_0")) {

                    viewBinding.imgNearbyUserGift.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            this@OfflineNearbySharedMomentAdapter.ctx.resources,
                            R.drawable.yellow_gift_male,
                            null
                        )
                    )

                } else if (item_data.node!!.node?.user!!.gender!!.name.equals("A_1")) {
                    viewBinding.imgNearbyUserGift.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            this@OfflineNearbySharedMomentAdapter.ctx.resources,
                            R.drawable.red_gift_female,
                            null
                        )
                    )

                } else if (item_data.node!!.node?.user!!.gender!!.name.equals("A_2")) {
                    viewBinding.imgNearbyUserGift.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            this@OfflineNearbySharedMomentAdapter.ctx.resources,
                            R.drawable.purple_gift_nosay,
                            null
                        )
                    )

                }
//            else
//            {
//                viewBinding.imgNearbyUserGift.setImageDrawable(ResourcesCompat.getDrawable(ctx.resources,R.drawable.pink_gift_noavb,null))
//
//            }
            }

            if (isShownearByUser && (item_data?.node!!.node?.like!! > 0)) {
                viewBinding.lblViewAllLikes.visibility = View.VISIBLE
            } else {
                viewBinding.lblViewAllLikes.visibility = View.GONE
            }

            if (item_data?.node!!.node?.comment!! > 0) {
                viewBinding.lblViewAllComments.visibility = View.VISIBLE
            } else {
                viewBinding.lblViewAllComments.visibility = View.GONE
            }


            viewBinding.lblViewAllLikes.setOnClickListener {
//                listener.onLikeofMomentshowClick(bindingAdapterPosition, item_data)
            }
            viewBinding.imgNearbyUserLikes.setOnClickListener(View.OnClickListener {
//                listener.onLikeofMomentClick(bindingAdapterPosition, item_data)
            })

            viewBinding.ivFullscreen.setOnClickListener(View.OnClickListener {
//                if (listener.isPlaying() && !isImageFile(item_data)) {
//                    listener.pauseVideo()
//                    viewBinding.ivPlay.setViewVisible()
//                }
//                if (!isImageFile(item_data)) {
//                    listener.pauseVideo()
//                    viewBinding.ivPlay.setViewVisible()
//                }
//                selectedItemPosition = -1
//                notifyDataSetChanged()
//                listener.onCommentofMomentClick(bindingAdapterPosition, item_data)
            })

            viewBinding.imgNearbyUserComment.setOnClickListener(View.OnClickListener {
//                if (listener.isPlaying() && !isImageFile(item_data)) {
//                    listener.pauseVideo()
//                    viewBinding.ivPlay.setViewVisible()
//                }
//                else {
//                    if (!isImageFile(item_data)) {
//                        listener.pauseVideo()
//                        viewBinding.ivPlay.setViewVisible()
//                    }
//                    selectedItemPosition = -1
//                    notifyDataSetChanged()
//                    listener.onCommentofMomentClick(bindingAdapterPosition, item_data)
//                }
            })

            viewBinding.lblViewAllComments.setOnClickListener(View.OnClickListener {
//                if (listener.isPlaying() && !isImageFile(item_data)) {
//                    listener.pauseVideo()
//                    viewBinding.ivPlay.setViewVisible()
//                }
//                else {
//                    if (!isImageFile(item_data)) {
//                        listener.pauseVideo()
//                        viewBinding.ivPlay.setViewVisible()
//                    }
//                    selectedItemPosition = -1
//                    notifyDataSetChanged()
//                    listener.onCommentofMomentClick(bindingAdapterPosition, item_data)
//                }
            })
            viewBinding.itemCell.setOnClickListener(View.OnClickListener {
//                if (listener.isPlaying() && !isImageFile(item_data)) {
//                    listener.pauseVideo()
//                    viewBinding.ivPlay.setViewVisible()
//                }
//                else {
//                    if (!isImageFile(item_data)) {
//                        listener.pauseVideo()
//                        viewBinding.ivPlay.setViewVisible()
//                    }
//                    selectedItemPosition = -1
//                    notifyDataSetChanged()
//                    listener.onCommentofMomentClick(bindingAdapterPosition, item_data)
//                }
            })

            viewBinding.imgNearbyUserGift.setOnClickListener(View.OnClickListener {
//                if (listener.isPlaying() && !isImageFile(item_data)) {
//                    listener.pauseVideo()
//                    viewBinding.ivPlay.setViewVisible()
//                }
//                else {
//                    if (!isImageFile(item_data)) {
////                        listener.pauseVideo()
//                        viewBinding.ivPlay.setViewVisible()
//                    }
//                    selectedItemPosition = -1
//                    notifyDataSetChanged()
////                    listener.onCommentofMomentClick(bindingAdapterPosition, item_data)
//                }
            })

            viewBinding.imgNearbySharedMomentOption.setOnClickListener(View.OnClickListener {


                if (userId!!.equals(item_data!!.node!!.node?.user!!.id)) {
                    //creating a popup menu

                    val popup = PopupMenu(this@OfflineNearbySharedMomentAdapter.ctx, viewBinding.imgNearbySharedMomentOption)
                    popup.getMenuInflater().inflate(R.menu.more_options, popup.getMenu());

                    //adding click listener
                    popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->

                        when (item!!.itemId) {
                            R.id.nav_item_delete -> {
//                                listener.onDotMenuofMomentClick(
//                                    bindingAdapterPosition,
//                                    item_data,
//                                    "delete"
//                                )

                            }
                            /* R.id.nav_item_report -> {
                             listener.onDotMenuofMomentClick(bindingAdapterPosition,item_data,"report")

                             }*/
                            R.id.nav_item_edit -> {
//                                listener.onDotMenuofMomentClick(
//                                    bindingAdapterPosition,
//                                    item_data,
//                                    "edit"
//                                )

                            }

                        }

                        true
                    })
                    popup.show()
                } else {
                    val popup = PopupMenu(this@OfflineNearbySharedMomentAdapter.ctx, viewBinding.imgNearbySharedMomentOption)
                    popup.getMenuInflater().inflate(R.menu.more_options1, popup.getMenu());

                    //adding click listener
                    popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->

                        when (item!!.itemId) {

                            R.id.nav_item_report -> {
//                                listener.onDotMenuofMomentClick(
//                                    bindingAdapterPosition,
//                                    item_data,
//                                    "report"
//                                )

                            }

                        }

                        true
                    })
                    popup.show()
                }
            })

        }

        private fun isImageFile(itemData: Moment): Boolean {
            return itemData?.node?.node?.file.toString().endsWith(".jpg") || itemData?.node?.node?.file.toString().endsWith(".jpeg") ||
                    itemData?.node?.node?.file.toString().endsWith(".png") || itemData?.node?.node?.file.toString().endsWith(".webp")
        }

        @OptIn(UnstableApi::class) private fun playView(mediaItem: MediaItem, playWhenReady: Boolean) {
//            val exoPlayer = listener.playVideo(mediaItem, playWhenReady)
//            viewBinding.playerView.player = exoPlayer
            viewBinding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    }

    interface NearbySharedMomentListener {
        fun playVideo(mediaItem: MediaItem, playWhenReady: Boolean): ExoPlayer

        fun isPlaying(): Boolean

        fun pauseVideo()

        fun onSharedMomentClick(
            position: Int,
            item: Moment?
        )

        fun onMoreShareMomentClick()

        fun onLikeofMomentshowClick(
            position: Int,
            item: Moment?
        )

        fun onLikeofMomentClick(
            position: Int,
            item: Moment?
        )

        fun onCommentofMomentClick(
            position: Int,
            item: Moment?
        )

        fun onMomentGiftClick(
            position: Int,
            item: Moment?
        )

        fun onDotMenuofMomentClick(position: Int, item: Moment?, types: String)

    }

}