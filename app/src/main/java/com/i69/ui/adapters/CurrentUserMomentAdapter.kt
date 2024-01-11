package com.i69.ui.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
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
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.i69.BuildConfig
import com.i69.GetUserMomentsQuery
import com.i69.R
import com.i69.applocalization.AppStringConstant1
import com.i69.databinding.ItemSharedUserMomentBinding
import com.i69.utils.ApiUtil
import com.i69.utils.loadCircleImage
import com.i69.utils.loadImage
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CurrentUserMomentAdapter(
    private val ctx: Context,
    private val listener: CurrentUserMomentListener,
    private var currentUserMoments: ArrayList<GetUserMomentsQuery.Edge>,
    var userId: String?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun getViewHolderByType(type: Int, viewBinding: ViewDataBinding) = NearbySharedMomentHolder(viewBinding as ItemSharedUserMomentBinding)

    private fun getViewDataBinding(viewType: Int, parent: ViewGroup) = ItemSharedUserMomentBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder = getViewHolderByType(type, getViewDataBinding(type, parent))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        holder as NearbySharedMomentHolder
        val item = currentUserMoments.get(position)
        holder.bind(position, item)
    }
    fun add(r: GetUserMomentsQuery.Edge?) {
        currentUserMoments.add(r!!)
        notifyItemInserted(currentUserMoments.size - 1)
    }

    fun addAll(newdata: ArrayList<GetUserMomentsQuery.Edge>) {


        newdata.indices.forEach { i ->


            add(newdata[i])
        }
    }
    override fun getItemCount(): Int {
        return if (currentUserMoments == null) 0 else currentUserMoments?.size!!
    }

    inner class NearbySharedMomentHolder(val viewBinding: ItemSharedUserMomentBinding) : RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(position: Int, item_data: GetUserMomentsQuery.Edge) {
            val title = item_data.node!!.user?.fullName


            val s2 = SpannableString(title.plus(" "))
            val s3 = SpannableString(AppStringConstant1.has_shared_moment)


            s2.setSpan(
                ForegroundColorSpan(ctx.resources.getColor(R.color.colorPrimary)),
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
            builder.append(s2)
            builder.append(s3)



            viewBinding.lblItemNearbyName.text = builder
            val url = if (!BuildConfig.USE_S3) {
                "${BuildConfig.BASE_URL}${item_data?.node!!.file}"
            }
            else if (item_data?.node!!.file.toString().startsWith(ApiUtil.S3_URL)) item_data?.node.file.toString()
            else ApiUtil.S3_URL.plus(item_data?.node.file.toString())
            Log.d("CUMA", "bind: $url")

            val avatarUrl = item_data?.node!!.user?.avatar
            if (avatarUrl != null) {
                viewBinding.imgNearbyUser.loadCircleImage(avatarUrl.url!!)
            }
            else {
                viewBinding.imgNearbyUser.loadImage(R.drawable.ic_default_user)
            }
            viewBinding.imgSharedMoment.loadImage(url)
            viewBinding.txtMomentDescription.text = item_data?.node!!.momentDescriptionPaginated.toString()
            var text = item_data?.node!!.createdDate.toString()
            text = text.replace("T", " ").substring(0, text.indexOf("."))
            val momentTime = formatter.parse(text)

            viewBinding.txtTimeAgo.text = DateUtils.getRelativeTimeSpanString(momentTime.time, Date().time, DateUtils.MINUTE_IN_MILLIS)

            viewBinding.txtNearbyUserLikeCount.setText(""+item_data?.node!!.like)

            viewBinding.lblItemNearbyUserCommentCount.setText(""+item_data?.node!!.comment)
            if(item_data.node.user!!.gender != null) {

                if (item_data?.node!!.user!!.gender!!.name.equals("A_0")) {

                    viewBinding.imgNearbyUserGift.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            ctx.resources,
                            R.drawable.yellow_gift_male,
                            null
                        )
                    )

                } else if (item_data.node!!.user!!.gender!!.name.equals("A_1")) {
                    viewBinding.imgNearbyUserGift.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            ctx.resources,
                            R.drawable.red_gift_female,
                            null
                        )
                    )

                } else if (item_data.node!!.user!!.gender!!.name.equals("A_2")) {
                    viewBinding.imgNearbyUserGift.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            ctx.resources,
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

            viewBinding.root.setOnClickListener {
                //holder.viewBinding.photoCover.transitionName = "profilePhoto"
                //listener.onSharedMomentClick(bindingAdapterPosition, item)
            }
            viewBinding.imgNearbyUserLikes.setOnClickListener(View.OnClickListener {
                listener.onLikeofMomentClick(bindingAdapterPosition, item_data)
            })

            viewBinding.imgNearbyUserComment.setOnClickListener(View.OnClickListener {
                listener.onCommentofMomentClick(bindingAdapterPosition,item_data)

            })

            viewBinding.lblViewAllComments.setOnClickListener(View.OnClickListener {
                listener.onCommentofMomentClick(bindingAdapterPosition,item_data)

            })
            viewBinding.itemCell.setOnClickListener(View.OnClickListener {
                listener.onCommentofMomentClick(bindingAdapterPosition,item_data)

            })
            viewBinding.imgNearbyUserGift.setOnClickListener(View.OnClickListener {
                listener.onCommentofMomentClick(bindingAdapterPosition,item_data)
//                listener.onMomentGiftClick(bindingAdapterPosition,item_data)

            })

            viewBinding.imgNearbySharedMomentOption.setOnClickListener(View.OnClickListener {

                if (userId!!.equals(item_data!!.node!!.user!!.id)) {
                    //creating a popup menu

                    val popup = PopupMenu(ctx, viewBinding.imgNearbySharedMomentOption)
                    popup.getMenuInflater().inflate(R.menu.more_options, popup.getMenu());

                    //adding click listener
                    popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->

                        when (item!!.itemId) {
                            R.id.nav_item_delete -> {
                                listener.onDotMenuofMomentClick(bindingAdapterPosition,item_data,"delete")

                            }
                            R.id.nav_item_report -> {
                                listener.onDotMenuofMomentClick(bindingAdapterPosition,item_data,"report")

                            }

                        }

                        true
                    })
                    popup.show()
                }
                else
                {
                    val popup = PopupMenu(ctx, viewBinding.imgNearbySharedMomentOption)
                    popup.getMenuInflater().inflate(R.menu.more_options1, popup.getMenu());

                    //adding click listener
                    popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->

                        when (item!!.itemId) {

                            R.id.nav_item_report -> {
                                listener.onDotMenuofMomentClick(bindingAdapterPosition,item_data,"report")

                            }

                        }

                        true
                    })
                    popup.show()
                }
            })
        }
    }

    interface CurrentUserMomentListener {
        fun onSharedMomentClick(
            position: Int,
            item: GetUserMomentsQuery.Edge
        )
        fun onMoreShareMomentClick()
        fun onLikeofMomentClick(position: Int,
                                item: GetUserMomentsQuery.Edge)
        fun onCommentofMomentClick(position: Int,
                                   item: GetUserMomentsQuery.Edge)
        fun onDotMenuofMomentClick(position: Int,
                                   item: GetUserMomentsQuery.Edge,types: String)

        fun onMomentGiftClick(position: Int,
                              item: GetUserMomentsQuery.Edge?)
    }

}