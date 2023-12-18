package com.i69.ui.adapters

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.i69.R
import com.i69.ui.viewModels.CommentsModel
import com.i69.databinding.ItemCommentMomentBinding
import com.i69.utils.loadCircleImage
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CommentListAdapter(
    private val ctx: Context,
    private val listener: ClickPerformListener,
    private var allusermoments: ArrayList<CommentsModel>?,
    private var momentAddCommentFragment: CommentReplyListAdapter.ClickonListener,
    private var isAdminOfMoments : Boolean,
    private  val userId : String
) : RecyclerView.Adapter<CommentListAdapter.ViewHolder>() {
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onBindViewHolder(holder: CommentListAdapter.ViewHolder, position: Int) {

        val item = allusermoments?.get(position)
        holder.bind(position, item)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CommentListAdapter.ViewHolder =
        ViewHolder(
            ItemCommentMomentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )


    override fun getItemCount(): Int {
        return if (allusermoments == null) 0 else allusermoments?.size!!
    }

    fun updateList(updatedList: List<CommentsModel>) {
        allusermoments!!.clear()
        allusermoments!!.addAll(updatedList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val viewBinding: ItemCommentMomentBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(position: Int, item: CommentsModel?) {

            viewBinding.txtMomentDescription.setText(item!!.commenttext)
            viewBinding.imgNearbyUser.loadCircleImage(item!!.userurl.toString())
            viewBinding.lblItemNearbyName.setText(item.username?.uppercase())
            viewBinding.txtNearbyUserLikeCount.setText(item.cmtlikes)


            var text = item.timeago
            text = text!!.replace("T", " ").substring(0, text.indexOf("."))
            val momentTime = formatter.parse(text)

            viewBinding.txtTimeAgo.text = DateUtils.getRelativeTimeSpanString(
                momentTime.time,
                Date().time,
                DateUtils.MINUTE_IN_MILLIS
            )


            val headlineAdapter = CommentReplyListAdapter(ctx,momentAddCommentFragment, item.replylist)
            viewBinding.rvReplies.adapter = headlineAdapter
            viewBinding.rvReplies.layoutManager = LinearLayoutManager(ctx)
            viewBinding.txtMomentDescription.setOnClickListener { onItemClicked(item) }
            if (item.isExpanded!!) {
                viewBinding.rvReplies.visibility = View.VISIBLE
//                holder.ivArrow.setImageResource(R.drawable.ic_arrow_up)
            } else {
                viewBinding.rvReplies.visibility = View.GONE
//                holder.ivArrow.setImageResource(R.drawable.ic_arrow_down)
            }





            viewBinding.imgNearbySharedMomentOption.setOnClickListener {
//                if(isAdminOfMoments) {
                    val popup = PopupMenu(ctx, viewBinding.imgNearbySharedMomentOption)
                    popup.getMenuInflater().inflate(R.menu.comment_more_option, popup.getMenu())
//                        .apply {
//                        stringConstant = AppStringConstant(requireContext())
//                    }
                    if(isAdminOfMoments  || userId.equals(item.uid) ){
                        popup.getMenu().findItem(R.id.nav_item_delete).setVisible(true)
                        popup.getMenu().findItem(R.id.nav_item_report).setVisible(false)

                    }else{
                        popup.getMenu().findItem(R.id.nav_item_delete).setVisible(false)
                        popup.getMenu().findItem(R.id.nav_item_report).setVisible(true)
                    }
                    popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { itemd: MenuItem? ->

                        when (itemd!!.itemId) {
                            R.id.nav_item_delete -> {
                                listener.oncommentDelete(bindingAdapterPosition, item)

                            }
                           R.id.nav_item_report -> {
                         listener.oncommentReport(bindingAdapterPosition, item)

                         }
//                        R.id.nav_item_edit -> {
//                            listener.onDotMenuofMomentClick(bindingAdapterPosition,item_data,"edit")
//
//                        }

                        }

                        true
                    })
                    popup.show()
                }

//            }
            viewBinding.imgNearbyUserComment.setOnClickListener(View.OnClickListener {
                listener.onreply(bindingAdapterPosition, item)

            })
            viewBinding.imgNearbyUserLikes.setOnClickListener(View.OnClickListener {
                listener.oncommentLike(bindingAdapterPosition, item)

            })
            viewBinding.lblItemNearbyName.setOnClickListener(View.OnClickListener {
                listener.onUsernameClick(bindingAdapterPosition, item)


            })

            viewBinding.imgNearbyUser.setOnClickListener(View.OnClickListener {
                listener.onUsernameClick(bindingAdapterPosition, item)


            })


        }

        private fun onItemClicked(newspaperModel: CommentsModel?) {
            newspaperModel?.isExpanded = !newspaperModel?.isExpanded!!
            notifyDataSetChanged()
        }
    }

    interface ClickPerformListener {
        fun onreply(position: Int, models: CommentsModel)
        fun oncommentLike(position: Int, models: CommentsModel)
        fun oncommentDelete(position: Int, models: CommentsModel)
        fun oncommentReport(position: Int, models: CommentsModel)
        fun onUsernameClick(position: Int,
                            models: CommentsModel)
    }


}