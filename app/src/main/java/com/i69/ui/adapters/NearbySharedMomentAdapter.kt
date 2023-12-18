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
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.i69.BuildConfig
import com.i69.GetAllUserMomentsQuery
import com.i69.R
import com.i69.applocalization.AppStringConstant1
import com.i69.data.models.Edge
import com.i69.databinding.ItemSharedUserMomentBinding
import com.i69.utils.loadCircleImage
import com.i69.utils.loadImage
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class NearbySharedMomentAdapter(
    private val ctx: Context,
    private val listener: NearbySharedMomentListener,
    private var allusermoments2: ArrayList<GetAllUserMomentsQuery.Edge>,
    var userId: String?,
    var isShownearByUser: Boolean = true
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val differCallback = object : DiffUtil.ItemCallback<GetAllUserMomentsQuery.Edge>() {
        override fun areItemsTheSame(
            oldItem: GetAllUserMomentsQuery.Edge,
            newItem: GetAllUserMomentsQuery.Edge
        ): Boolean {
            return oldItem.node?.pk == newItem.node?.pk
        }

        override fun areContentsTheSame(
            oldItem: GetAllUserMomentsQuery.Edge,
            newItem: GetAllUserMomentsQuery.Edge
        ): Boolean {
            return when {
                oldItem.node?.like != newItem.node?.like -> false
                oldItem.node?.comment != newItem.node?.comment -> false
                oldItem.node?.momentDescription != newItem.node?.momentDescription -> false
                oldItem.node?.momentDescriptionPaginated != newItem.node?.momentDescriptionPaginated -> false
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
    private var allusermoments = emptyList<GetAllUserMomentsQuery.Edge>()
    var allusermoments4 = ArrayList(allusermoments)

    private fun getViewHolderByType(type: Int, viewBinding: ViewDataBinding) =
        NearbySharedMomentHolder(viewBinding as ItemSharedUserMomentBinding)

    private fun getViewDataBinding(viewType: Int, parent: ViewGroup) =
        ItemSharedUserMomentBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder =
        getViewHolderByType(type, getViewDataBinding(type, parent))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        holder as NearbySharedMomentHolder
        val item = differ.currentList.get(position)
        holder.bind(position, item)
    }

    fun submitList(data: List<GetAllUserMomentsQuery.Edge>) {
        differ.submitList(data)
    }

    fun submitList1(list: MutableList<GetAllUserMomentsQuery.Edge>) {
        Log.e("notify_adapter","submitList1 98")
        val x = mutableListOf<GetAllUserMomentsQuery.Edge>().apply {
            addAll(list)
        }
        differ.submitList(x)
    }

    fun getArrayList(): ArrayList<GetAllUserMomentsQuery.Edge> {
        val cardList: List<GetAllUserMomentsQuery.Edge> = differ.getCurrentList()
        return ArrayList<GetAllUserMomentsQuery.Edge>(cardList)
    }

    fun setData(newWordList: List<GetAllUserMomentsQuery.Edge>) {
        /*if (allusermoments4.size!=0){
            val fromIndex = allusermoments4.size-11
            val toIndex = allusermoments4.size-1
            allusermoments4.subList(fromIndex, toIndex + 1).clear()
        }*/
        val diffUtil = NearByMomentsAdapterDiff(allusermoments4, newWordList)
        val diffResults = DiffUtil.calculateDiff(diffUtil)
        allusermoments4 = newWordList as ArrayList<GetAllUserMomentsQuery.Edge>
        diffResults.dispatchUpdatesTo(this@NearbySharedMomentAdapter)
    }

    fun updateList(updatedList: ArrayList<GetAllUserMomentsQuery.Edge>, uid: String?) {
        userId = uid
        //allusermoments.clear()
        //allusermoments.addAll(updatedList)
        // allusermoments=updatedList
        Log.e("notify_adapter","updateList")
        notifyDataSetChanged()
    }


    fun add(r: GetAllUserMomentsQuery.Edge?) {
        //allusermoments.add(r!!)
        Log.e("notify_adapter","add 134")
        notifyItemInserted(differ.currentList.size - 1)
    }

    fun addAll(newdata: ArrayList<GetAllUserMomentsQuery.Edge>) {
        newdata.indices.forEach { i ->
            Log.e("notify_adapter","addAll 140")
            add(newdata[i])
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    inner class NearbySharedMomentHolder(val viewBinding: ItemSharedUserMomentBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(position: Int, item_data: GetAllUserMomentsQuery.Edge?) {
            if (item_data!!.node!!.user == null) {
                Log.e("obj_node", "data is null ${differ.currentList.size}")
                return
            }
           // Log.e("obj_node", Gson().toJson(item_data!!))
            val title = item_data!!.node!!.user!!.fullName

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
//            builder.append(s1)
//            builder.append(" ")
            builder.append(s2)
            builder.append(" ")
            builder.append(s3)

            viewBinding.lblItemNearbyName.text = builder


            val url = "${BuildConfig.BASE_URL}media/${item_data.node!!.file}"
            Timber.d("binnd user avatar= ${item_data.node.user?.avatar}")
            /*if (item?.user?.avatarPhotos?.size!! > 0) {
                Timber.d("binnd user avatar= ${item?.user?.avatarPhotos?.get(0)?.url}")
                val avatarUrl = item?.user?.avatarPhotos?.get(0)?.url!!
                viewBinding.imgNearbyUser.loadCircleImage(avatarUrl)
            }*/

            viewBinding.imgSharedMoment.loadImage(url)
//            Glide.with(contcex).load(url).into(viewBinding.imgSharedMoment);
            val avatarUrl = item_data.node.user?.avatar
            if (avatarUrl != null) {
                avatarUrl.url?.replace(
                    "http://95.216.208.1:8000/media/",
                    "${BuildConfig.BASE_URL}media/"
                )
                    ?.let { viewBinding.imgNearbyUser.loadCircleImage(it) }
            } else {
                viewBinding.imgNearbyUser.loadImage(R.drawable.ic_default_user)
            }

            val sb = StringBuilder()
            item_data.node.momentDescriptionPaginated!!.forEach { sb.append(it) }
            val descstring = sb.toString().replace("", "")

            if (descstring.isNullOrEmpty()) {
                viewBinding.txtMomentDescription.visibility = View.GONE
            } else {
                viewBinding.txtMomentDescription.text = descstring
                viewBinding.txtMomentDescription.visibility = View.VISIBLE
            }
            var text = item_data.node.createdDate.toString()
            text = text.replace("T", " ").substring(0, text.indexOf("."))
            val momentTime = formatter.parse(text)

            viewBinding.txtTimeAgo.text = DateUtils.getRelativeTimeSpanString(
                momentTime.time,
                Date().time,
                DateUtils.MINUTE_IN_MILLIS
            )

            viewBinding.txtNearbyUserLikeCount.setText("" + item_data?.node!!.like)

            viewBinding.lblItemNearbyUserCommentCount.setText("" + item_data?.node!!.comment)
            if (item_data.node.user!!.gender != null) {
                if (item_data.node.user!!.gender!!.name.equals("A_0")) {

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


            /*viewBinding.imgNearbyShare.setImageBitmap(bmp.borderedCircularBitmap())*/
            viewBinding.root.setOnClickListener {
                //holder.viewBinding.photoCover.transitionName = "profilePhoto"
                //listener.onSharedMomentClick(bindingAdapterPosition, item)
            }

            if (isShownearByUser && (item_data?.node!!.like!! > 0)) {
                viewBinding.lblViewAllLikes.visibility = View.VISIBLE
            } else {
                viewBinding.lblViewAllLikes.visibility = View.GONE
            }

            if (item_data?.node!!.comment!! > 0) {
                viewBinding.lblViewAllComments.visibility = View.VISIBLE
            } else {
                viewBinding.lblViewAllComments.visibility = View.GONE
            }


            viewBinding.lblViewAllLikes.setOnClickListener {
                listener.onLikeofMomentshowClick(bindingAdapterPosition, item_data)
            }
            viewBinding.imgNearbyUserLikes.setOnClickListener(View.OnClickListener {
                listener.onLikeofMomentClick(bindingAdapterPosition, item_data)
            })

            viewBinding.imgNearbyUserComment.setOnClickListener(View.OnClickListener {
                listener.onCommentofMomentClick(bindingAdapterPosition, item_data)
            })

            viewBinding.lblViewAllComments.setOnClickListener(View.OnClickListener {
                listener.onCommentofMomentClick(bindingAdapterPosition, item_data)

            })
            viewBinding.itemCell.setOnClickListener(View.OnClickListener {
                listener.onCommentofMomentClick(bindingAdapterPosition, item_data)

            })

            viewBinding.imgNearbyUserGift.setOnClickListener(View.OnClickListener {
                listener.onCommentofMomentClick(bindingAdapterPosition, item_data)
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
                                listener.onDotMenuofMomentClick(
                                    bindingAdapterPosition,
                                    item_data,
                                    "delete"
                                )

                            }
                            /* R.id.nav_item_report -> {
                             listener.onDotMenuofMomentClick(bindingAdapterPosition,item_data,"report")

                             }*/
                            R.id.nav_item_edit -> {
                                listener.onDotMenuofMomentClick(
                                    bindingAdapterPosition,
                                    item_data,
                                    "edit"
                                )

                            }

                        }

                        true
                    })
                    popup.show()
                } else {
                    val popup = PopupMenu(ctx, viewBinding.imgNearbySharedMomentOption)
                    popup.getMenuInflater().inflate(R.menu.more_options1, popup.getMenu());

                    //adding click listener
                    popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->

                        when (item!!.itemId) {

                            R.id.nav_item_report -> {
                                listener.onDotMenuofMomentClick(
                                    bindingAdapterPosition,
                                    item_data,
                                    "report"
                                )

                            }

                        }

                        true
                    })
                    popup.show()
                }

            })

        }
    }

    interface NearbySharedMomentListener {
        fun onSharedMomentClick(
            position: Int,
            item: GetAllUserMomentsQuery.Edge?
        )

        fun onMoreShareMomentClick()

        fun onLikeofMomentshowClick(
            position: Int,
            item: GetAllUserMomentsQuery.Edge?
        )

        fun onLikeofMomentClick(
            position: Int,
            item: GetAllUserMomentsQuery.Edge?
        )

        fun onCommentofMomentClick(
            position: Int,
            item: GetAllUserMomentsQuery.Edge?
        )

        fun onMomentGiftClick(
            position: Int,
            item: GetAllUserMomentsQuery.Edge?
        )

        fun onDotMenuofMomentClick(position: Int, item: GetAllUserMomentsQuery.Edge?, types: String)

    }

}