package com.i69.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.i69.BuildConfig
import com.i69.R
import com.i69.data.models.MyPermission
import com.i69.data.models.User
import com.i69.data.remote.responses.DefaultPicker
import com.i69.databinding.ListItemSearchedUserBinding
import com.i69.databinding.ListItemUnlockFeatureBinding
import com.i69.utils.getSelectedValueFromDefaultPicker
import com.i69.utils.loadCircleImage
import timber.log.Timber

private const val TYPE_UNLOCK_PAID = 0
private const val TYPE_DEFAULT = 1

class UsersSearchListAdapter(
    private val listener: UserSearchListener,
    private val defaultPicker: DefaultPicker,
    val items: ArrayList<User>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //private  = ArrayList()

    private var userhashPermission: MyPermission = MyPermission(false)

    private fun getViewHolderByType(type: Int, viewBinding: ViewDataBinding) =
        if (type == TYPE_DEFAULT) SearchedUserHolder(viewBinding as ListItemSearchedUserBinding)
        else UnlockHolder(viewBinding as ListItemUnlockFeatureBinding)

    private fun getViewDataBinding(viewType: Int, parent: ViewGroup) = if (viewType == TYPE_DEFAULT)
        ListItemSearchedUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    else
        ListItemUnlockFeatureBinding.inflate(LayoutInflater.from(parent.context), parent, false)

//    override fun getItemViewType(position: Int): Int = if (position == 0 && !userhashPermission.hasPermission) TYPE_UNLOCK_PAID else TYPE_DEFAULT

    override fun getItemViewType(position: Int): Int = TYPE_DEFAULT


    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder =
        getViewHolderByType(type, getViewDataBinding(type, parent))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_UNLOCK_PAID) {
            holder as UnlockHolder
            holder.viewBinding.root.setOnClickListener {
                listener.onUnlockFeatureClick()
            }
        } else {
            holder as SearchedUserHolder
            val item = items[position]
            holder.bind(item)

            if (item.avatarPhotos != null) {
                if (item.avatarPhotos!!.size != 0) {

                    if (item.avatarIndex!! < item.avatarPhotos!!.size) {
                        val imageUrl = item.avatarPhotos!!.get(item.avatarIndex!!).url?.replace(
                            "http://95.216.208.1:8000/media/",
                            "${BuildConfig.BASE_URL}media/"
                        )
                        Log.e("userprofile","${item.fullName} --> $imageUrl")
                        if (imageUrl != null) {
                            holder.viewBinding.photoCover.loadCircleImage(
                                imageUrl, true
                            )
                        }
                    }
                }
            }

            holder.viewBinding.root.setOnClickListener {
                holder.viewBinding.photoCover.transitionName = "profilePhoto"
                listener.onItemClick(holder.bindingAdapterPosition, item)
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(updated: List<User>, userhashPermission: MyPermission) {
        this@UsersSearchListAdapter.items.clear()
        this.userhashPermission = userhashPermission
        if (!userhashPermission.hasPermission) {
//            items.add(0, User())
        }
        Log.e("updatadataList", "${updated.size}")
        this@UsersSearchListAdapter.items.addAll(updated)
        Log.e("updatadataList1", "${items.size}")
        this@UsersSearchListAdapter.notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class UnlockHolder(val viewBinding: ListItemUnlockFeatureBinding) :
        RecyclerView.ViewHolder(viewBinding.root)

    inner class SearchedUserHolder(val viewBinding: ListItemSearchedUserBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(user: User) {
            var title = ""
            var subTitle = ""
            if (user.fullName.isNotEmpty()) title += user.fullName
            if (user.age != null) {
                val age = defaultPicker.agePicker.getSelectedValueFromDefaultPicker(user.age)
                if (age.isNotEmpty()) title += ", $age"
            }
            if (!user.work.isNullOrEmpty()) subTitle += (user.work + "\n")
            if (!user.education.isNullOrEmpty()) subTitle += user.education




            if (user.isOnline == true) {
                viewBinding.onlineIndicator.setImageResource(R.drawable.round_green)
            } else {
                viewBinding.onlineIndicator.setImageResource(R.drawable.round_yellow)
            }

            viewBinding.userTitle.text = title
            if (subTitle.isEmpty()) {
                viewBinding.userSubTitle.visibility = View.GONE
            } else {
                viewBinding.userSubTitle.text = subTitle
                viewBinding.userSubTitle.visibility = View.VISIBLE
            }
        }
    }

    interface UserSearchListener {
        fun onItemClick(position: Int, user: User)
        fun onUnlockFeatureClick()
    }

}