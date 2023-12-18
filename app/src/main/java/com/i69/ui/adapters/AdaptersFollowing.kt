package com.i69.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.i69.GetUserQuery
import com.i69.applocalization.AppStringConstant
import com.i69.databinding.ItemFollowingBinding
import com.i69.utils.loadImage

class AdaptersFollowing(var context: Context,
                        var items: MutableList<GetUserQuery.FollowingUser?>,
                        private val listener: AdaptersFollowing.FollowingListListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val cView = ItemFollowingBinding.inflate(
            LayoutInflater.from(
                parent.context
            ), parent, false
        )
//        val dm = parent.context.resources.displayMetrics
//        var size = dm.widthPixels/3
//        size -= (5 * dm.density).toInt()
//        cView.lytGift.layoutParams = FrameLayout.LayoutParams (size, size)
//        cView.lytGift.requestLayout()
//        size -= (2 * dm.density).toInt()
//        cView.B.layoutParams = FrameLayout.LayoutParams (size, size)
//        cView.BB.layoutParams = FrameLayout.LayoutParams (size, size)
        return MyViewHolder(cView)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MyViewHolder) {
            holder.bind(items[position])
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

//    fun getSelected(): List<ModelGifts.Data.AllRealGift> {
//        return items.filter { it.isSelected }
//    }

    inner class MyViewHolder(var viewBinding: ItemFollowingBinding) : RecyclerView.ViewHolder(viewBinding.root){
        init {

        }
        fun bind(item: GetUserQuery.FollowingUser?){

            viewBinding.title.text = item!!.fullName
            viewBinding.subtitle.text = item!!.firstName
//            viewBinding.subtitle.text = item!!.email


            if(item.avatarPhotos!!.size>0) {
                var imgSrc =item.avatarPhotos.get(0)

                if (imgSrc != null) {
                    viewBinding.img.loadImage(imgSrc.url!!)
                } else {
                    viewBinding. img.loadImage("")
                }
            }

            if (item.isConnected == true) {
                viewBinding.tvFollowing.text =   AppStringConstant(context).following_tab
//                viewBinding.tvFollowing.text =
//                    context.resources.getString(com.i69app.R.string.following_tab)
            } else {

                viewBinding.tvFollowing.text =   AppStringConstant(context).follow
//                viewBinding.tvFollowing.text =
//                    context.resources.getString(com.i69app.R.string.follow)
            }
            viewBinding.tvFollowing.setOnClickListener {
                listener.onItemClick(item)

            }


            viewBinding.title.setOnClickListener {
                listener.onUserProfileClick(item)

            }

            viewBinding.img.setOnClickListener {
                listener.onUserProfileClick(item)

            }
//            viewBinding.vm = item

        }
    }


     interface FollowingListListener {
        fun onItemClick(followinfUser: GetUserQuery.FollowingUser?)

        fun onUserProfileClick(followinfUser: GetUserQuery.FollowingUser?)
    }
}
