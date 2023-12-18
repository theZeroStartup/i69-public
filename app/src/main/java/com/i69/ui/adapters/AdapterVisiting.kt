package com.i69.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.i69.GetUserQuery
import com.i69.R
import com.i69.applocalization.AppStringConstant
import com.i69.data.models.BaseUserVisitingModel
import com.i69.databinding.ItemVisitorBinding
import com.i69.databinding.VisitorDateItemLayoutBinding
import com.i69.utils.loadImageWithPlaceHolder

class AdapterVisiting(var context: Context,
                      var items: MutableList<BaseUserVisitingModel>,
                      var appStringConstant: AppStringConstant,
                      private val listener: FollowerListListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val cView = VisitorDateItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            DateViewHolder(cView)
        } else {
            val cView = ItemVisitorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            MyViewHolder(cView)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].viewType
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MyViewHolder) {
            holder.bind(items[position].userVisiting,items[position].dateTime)
        } else if (holder is DateViewHolder) {
            holder.bind(items[position].dateTime)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class DateViewHolder(var viewBinding : VisitorDateItemLayoutBinding):RecyclerView.ViewHolder(viewBinding.root) {

        fun bind(dateTime: CharSequence) {
            viewBinding.title.setText(dateTime)
        }
    }

    inner class MyViewHolder(var viewBinding: ItemVisitorBinding) : RecyclerView.ViewHolder(viewBinding.root){
        init {

        }
        fun bind(item: GetUserQuery.UserVisiting?,dateTime: CharSequence){

            viewBinding.title.text = item!!.fullName
            viewBinding.tvDateTime.text = dateTime
            viewBinding.subtitle.text = String.format(appStringConstant.following_count_follower_count,item.followingCount,item.followersCount)

            if (item.avatarPhotos!!.size > 0) {
                var imgSrc = item.avatarPhotos.get(0)

                if (imgSrc != null) {
                    viewBinding.img.loadImageWithPlaceHolder(imgSrc.url!!)
                } else {
                    viewBinding.img.setImageResource(R.drawable.ic_default_user)
                }
            }

            if (item.isConnected != null && item.isConnected) {
                viewBinding.tvConnect.text = appStringConstant.following_tab
            } else {
                viewBinding.tvConnect.text = appStringConstant.follow
            }

            viewBinding.tvConnect.setOnClickListener {
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
        fun onItemClick(followinfUser: GetUserQuery.UserVisiting?)

        fun onUserProfileClick(followinfUser: GetUserQuery.UserVisiting?)
    }
}
