package com.i69.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.gson.Gson
import com.i69.*
import com.i69.applocalization.AppStringConstant1
import com.i69.data.models.User
import com.i69.databinding.ItemAddNewNearbyThumbBinding
import com.i69.databinding.ItemNearbyThumbBinding
import com.i69.utils.loadCircleImage


private const val TYPE_NEW_STORY = 0
private const val TYPE_DEFAULT = 1

class UserMultiStoriesAdapter(
    private val ctx: Context,
    var mUser: User?,
    private val listener: UserMultiStoryListener,
//    private val allUserStories: List<GetAllUserMultiStoriesQuery.AllUserMultiStory?>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private fun getViewHolderByType(type: Int, viewBinding: ViewDataBinding) =
        if (type == TYPE_DEFAULT)
            UserStoryHolder(viewBinding as ItemNearbyThumbBinding)
        else NewUserStoryHolder(
            viewBinding as ItemAddNewNearbyThumbBinding
        )

    private fun getViewDataBinding(viewType: Int, parent: ViewGroup) = if (viewType == TYPE_DEFAULT)
        ItemNearbyThumbBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    else
        ItemAddNewNearbyThumbBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun getItemViewType(position: Int): Int =
        if (position == 0) TYPE_NEW_STORY else TYPE_DEFAULT

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
/*
        if (type==TYPE_DEFAULT) {
            val itemView = getViewHolderByType(
                type,
                getViewDataBinding(type, parent)
            ).itemView.findViewById<ConstraintLayout>(R.id.parentLayout)
            itemView.layoutParams.width = (getScreenWidth(parent.context) / 2)
        }
*/
        return getViewHolderByType(type, getViewDataBinding(type, parent))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_NEW_STORY) {
            holder as NewUserStoryHolder
            mUser?.let {
                it.avatarPhotos?.let { avtarPhoto->
                    if (it.avatarIndex != null && avtarPhoto.size > it.avatarIndex!!) {
                        val imageUrl = avtarPhoto[it.avatarIndex!!].url?.replace(
                            "${BuildConfig.BASE_URL}media/",
                            "${BuildConfig.BASE_URL}media/"
                        ).toString()
                        Glide.with(ctx)
                            .load(imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .optionalCircleCrop()
                            //.placeholder(R.drawable.ic_default_user)
                            .into(holder.viewBinding.ivProfile)
                    }
                }
            }

            holder.viewBinding.root.setOnClickListener {

                val inflater =
                    ctx.applicationContext
                        .getSystemService(
                            Context.LAYOUT_INFLATER_SERVICE
                        ) as LayoutInflater
                var view = inflater.inflate(R.layout.layout_attach_story, null)
                view.findViewById<TextView>(R.id.header_title).text = AppStringConstant1.select_story_pic

                var mypopupWindow =
                    PopupWindow(
                        view,
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        true
                    )

                var llCamera = view.findViewById<View>(R.id.llCamera) as LinearLayoutCompat
                var llGallary =
                    view.findViewById<View>(R.id.ll_gallery) as LinearLayoutCompat


                llCamera.setOnClickListener {
                    listener.onAddNewUserStoryClick(isCamera = true)
                    mypopupWindow.dismiss()
                }

                llGallary.setOnClickListener {
                    listener.onAddNewUserStoryClick(isCamera = false)
                    mypopupWindow.dismiss()
                }
                mypopupWindow.showAsDropDown(holder.viewBinding.root, -153, 0);
//                mypopupWindow.showAsDropDown( holder.viewBinding.root,-153,0);
//                listener.onAddNewUserStoryClick()


            }
            if (mUser?.userLanguageCode == "de" || mUser?.userLanguageCode == "ru" || mUser?.userLanguageCode == "no" || mUser?.userLanguageCode == "nl") {
                holder.viewBinding.addstorytext.text = AppStringConstant1.add_story.toLowerCase()
            } else {
                holder.viewBinding.addstorytext.text = AppStringConstant1.add_story
            }
        } else {
            holder as UserStoryHolder
            if (storyList.size > position - 1) {
                val item = storyList.get(position - 1)  //allUserStories?.get(position-1)
                holder.bind(item)

                holder.viewBinding.root.setOnClickListener {
                    //holder.viewBinding.photoCover.transitionName = "profilePhoto"
                    listener.onUserMultiStoryClick(holder.bindingAdapterPosition, item!!)
                }
            }

        }
    }

    //   override fun getItemCount() = (allUserStories?.size?.plus(1)) ?: 1
    override fun getItemCount() = storyList.size.plus(1)

    inner class NewUserStoryHolder(val viewBinding: ItemAddNewNearbyThumbBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
            init {
                if (mUser?.userLanguageCode == "de" || mUser?.userLanguageCode == "ru" || mUser?.userLanguageCode == "no" || mUser?.userLanguageCode == "nl") {
                    val layoutParam = viewBinding.addstorytext.layoutParams as ConstraintLayout.LayoutParams
                    //layoutParam.setMargins(0,ctx.resources.getDimensionPixelSize(R.dimen.add_story_text_margin),0,0)
                    layoutParam.setMargins(0,0,0,0)
                }
            }
        }

    inner class UserStoryHolder(val viewBinding: ItemNearbyThumbBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(item: GetAllUserMultiStoriesQuery.AllUserMultiStory?) {
            //  val title = item?.node!!.user?.fullName
            val title = item?.user?.fullName
            viewBinding.txtItemNearbyName.text = title
            val storyImage: String
            //  if (item?.node.fileType.equals("video")) {
            val node = item?.stories?.edges?.get(0)?.node
            val user = item?.user
            if (node?.fileType.equals("video")) {
                storyImage = "${BuildConfig.BASE_URL}media/${node?.thumbnail}"
                Log.e("thumbnail","thumbnail from node\n${Gson().toJson(node)}}")
            } else {
                Log.e("thumbnail","file from node \n" +
                        "${Gson().toJson(node)}")
                storyImage = "${BuildConfig.BASE_URL}media/${node?.file}"
            }

            Log.e("thumbnail",storyImage)
            viewBinding.imgUserStory.loadCircleImage(storyImage)

            var url: String? = ""

            if (user?.avatarPhotos != null && (user.avatarPhotos.isNotEmpty()) && (user.avatarIndex < user.avatarPhotos.size)) {
                url = user.avatarPhotos[user.avatarIndex].url
            }

            /*    if ((node?.user?.avatarPhotos?.size!! > 0) && (node.user.avatarIndex < node.user.avatarPhotos.size)) {
                    url = node.user.avatarPhotos[node.user.avatarIndex].url
                }*/

            viewBinding.progressindicatorStories.blockCount = item?.stories?.edges!!.size
            viewBinding.imgNearbyProfile.loadCircleImage(
                url?.replace(
                    "http://95.216.208.1:8000/media/",
                    "${BuildConfig.BASE_URL}media/"
                ).toString()
            )
        }
    }

    private val diffUtilCallBack =
        object : DiffUtil.ItemCallback<GetAllUserMultiStoriesQuery.AllUserMultiStory?>() {

            override fun areItemsTheSame(
                oldItem: GetAllUserMultiStoriesQuery.AllUserMultiStory,
                newItem: GetAllUserMultiStoriesQuery.AllUserMultiStory
            ): Boolean {
                return oldItem.user.id == newItem.user.id && oldItem.batchNumber == newItem.batchNumber

            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(
                oldItem: GetAllUserMultiStoriesQuery.AllUserMultiStory,
                newItem: GetAllUserMultiStoriesQuery.AllUserMultiStory
            ): Boolean {

                return if (oldItem.user.id == newItem.user.id) {
                    if (oldItem.batchNumber == newItem.batchNumber) {
                        oldItem.stories.edges.size == newItem.stories.edges.size
                    } else {
                        false
                    }
                } else {
                    false
                }
            }

        }

    private val differ = AsyncListDiffer(this, diffUtilCallBack)

    var storyList: List<GetAllUserMultiStoriesQuery.AllUserMultiStory?>
        get() = differ.currentList
        set(value) = differ.submitList(value)


    /*  fun updateList(updatedList: MutableList<GetAllUserMultiStoriesQuery.AllUserMultiStory??>?,pos : Int) {
          val old = differ.currentList
          val x = mutableListOf<GetAllUserMultiStoriesQuery.AllUserMultiStory??>().apply {
              if (updatedList != null) {
                  addAll(updatedList)
              }
          }
          differ.submitList(x)
          if (pos != 0){
              if (pos == 1){
                  notifyItemInserted(pos)
              }else{
                  notifyItemChanged(pos)
              }
          }

          /// differ.s
          // notifyDataSetChanged()
      }*/

//    interface UserStoryListener {
//        fun onUserStoryClick(position: Int, userStory: GetAllUserStoriesQuery.Edge?)
//        fun onAddNewUserStoryClick()
//    }

    interface UserMultiStoryListener {
        fun onUserMultiStoryClick(
            position: Int,
            userStory: GetAllUserMultiStoriesQuery.AllUserMultiStory
        )

        fun onAddNewUserStoryClick(isCamera: Boolean)
    }

    private fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size.x
    }

    fun startLoader() {

    }
}