package com.i69.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.i69.GetUserQuery
import com.i69.applocalization.AppStringConstant
import com.i69.databinding.ItemFollowersBinding
import com.i69.utils.loadImage


class AdapterFollowers(var context: Context,
                       var items: MutableList<GetUserQuery.FollowerUser?>,
                       private val listener: AdapterFollowers.FollowerListListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val cView = ItemFollowersBinding.inflate(
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



    inner class MyViewHolder(var viewBinding: ItemFollowersBinding) : RecyclerView.ViewHolder(viewBinding.root){
        init {

        }
        fun bind(item: GetUserQuery.FollowerUser?){

            viewBinding.title.text = item!!.fullName
//            viewBinding.subtitle.text = item!!.email

            viewBinding.subtitle.text = item!!.firstName

            if(item.avatarPhotos!!.size>0) {
                var imgSrc =item.avatarPhotos.get(0)

                if (imgSrc != null) {
                    viewBinding.img.loadImage(imgSrc.url!!)
                } else {
                    viewBinding. img.loadImage("")
                }

                if (item.isConnected!=null && item.isConnected) {

                    viewBinding.tvFolllow.text =   AppStringConstant(context).following_tab
//                    viewBinding.tvFolllow.text =
//                        context.resources.getString(com.i69app.R.string.following_tab)
                } else {
                    viewBinding.tvFolllow.text =
                        AppStringConstant(context).follow
//                    viewBinding.tvFolllow.text =
//                        context.resources.getString(com.i69app.R.string.follow)
                }
            }
            viewBinding.tvFollowing.text = AppStringConstant(context).remove

            viewBinding.tvFollowing.setOnClickListener {
                listener.onItemClick(item, true)
            }

            viewBinding.tvFolllow.setOnClickListener {
                listener.onItemClick(item)

            }


            viewBinding.title.setOnClickListener {
                listener.onUserProfileClick(item)

            }

            viewBinding.img.setOnClickListener {
                listener.onUserProfileClick(item)

            }

        }
    }

    interface FollowerListListener {
        fun onItemClick(followinfUser: GetUserQuery.FollowerUser?, isRemove : Boolean = false)

        fun onUserProfileClick(followinfUser: GetUserQuery.FollowerUser?)
    }
}
