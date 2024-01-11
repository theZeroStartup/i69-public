package com.i69.ui.screens.main.moment.momentcomment


import android.R.attr.button
import android.app.AlertDialog
import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.exception.ApolloException
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textview.MaterialTextView
import com.google.gson.Gson
import com.i69.*
import com.i69.applocalization.AppStringConstant
import com.i69.applocalization.AppStringConstantViewModel
import com.i69.data.models.ModelGifts
import com.i69.data.remote.responses.MomentLikes
import com.i69.databinding.FragmentMomentsAddcommentsBinding
import com.i69.di.modules.AppModule
import com.i69.gifts.FragmentRealGifts
import com.i69.gifts.FragmentReceivedGifts
import com.i69.gifts.FragmentVirtualGifts
import com.i69.ui.adapters.CommentListAdapter
import com.i69.ui.adapters.CommentReplyListAdapter
import com.i69.ui.adapters.StoryLikesAdapter
import com.i69.ui.adapters.UserItemsAdapter
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.screens.main.MainActivity.Companion.getMainActivity
import com.i69.ui.screens.main.search.userProfile.SearchUserProfileFragment
import com.i69.ui.viewModels.CommentsModel
import com.i69.ui.viewModels.ReplysModel
import com.i69.ui.viewModels.UserMomentsModelView
import com.i69.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*


class MomentAddCommentFragment : BaseFragment<FragmentMomentsAddcommentsBinding>(),
    CommentListAdapter.ClickPerformListener, CommentReplyListAdapter.ClickonListener {

    private lateinit var giftbottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var receivedGiftbottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var LikebottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var momentLikesAdapters: StoryLikesAdapter

    private var userToken: String? = null
    private var userName: String? = null
    private var userId: String? = null
    private lateinit var fragVirtualGifts: FragmentVirtualGifts
    private lateinit var fragRealGifts: FragmentRealGifts
    var giftUserid: String? = null
    var itemPosition: Int? = 0

    private var muserID: String? = ""
    private var mID: String? = ""
    private var filesUrl: String? = ""
    private var likess: String? = ""
    private var Comments: String? = ""
    private var Desc_: String? = ""
    private var Desc1_: List<String?>? = ArrayList()
    private var fullnames: String? = ""
    private var gender: String? = ""
    var builder: SpannableStringBuilder? = null
    var adapter: CommentListAdapter? = null
    var items: ArrayList<CommentsModel> = ArrayList()
    var Replymodels: CommentsModel? = null
    private val mViewModel: UserMomentsModelView by activityViewModels()

    var momentLikesUsers: ArrayList<MomentLikes> = ArrayList()
    private val viewStringConstModel: AppStringConstantViewModel by activityViewModels()

    //    imgNearbyUserComment
    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentMomentsAddcommentsBinding.inflate(inflater, container, false).apply {
            stringConstant = AppStringConstant(requireContext())
        }

    override fun setupTheme() {
        navController = findNavController()
        viewStringConstModel.data.observe(this@MomentAddCommentFragment) { data ->

            binding.stringConstant = data

        }
        viewStringConstModel.data?.also {
            binding.stringConstant = it.value
//            Log.e("MydataBasesss", it.value!!.messages)
        }
        //getMainActivity()?.binding?.mainNavView?.visibility=View.GONE
        binding.model = mViewModel
        lifecycleScope.launch {
            userToken = getCurrentUserToken()!!
            userName = getCurrentUserName()!!
            userId = getCurrentUserId()
            Timber.i("usertoken $userToken")
        }
        Timber.i("usertoken 2 $userToken")

//binding.momentDetailScrollView.sc

        binding.imgNearbyUserComment.setOnClickListener {
            Log.e("itemClicked", "itemClicked1")

            binding.momentDetailScrollView.post {
                binding.momentDetailScrollView.fullScroll(View.FOCUS_DOWN)
            }

//            binding.momentDetailScrollView.scrollTo(binding.momentDetailScrollView.getScrollX() as Int, binding.momentDetailScrollView.getScrollY() as Int + 50)
//            binding.momentDetailScrollView.getParent().requestChildFocus(binding.momentDetailScrollView, binding.momentDetailScrollView);
        }

        binding.lblItemNearbyUserCommentCount.setOnClickListener {
            Log.e("itemClicked", "itemClicked2")

            binding.momentDetailScrollView.post {
                binding.momentDetailScrollView.fullScroll(View.FOCUS_DOWN)
            }
//            binding.momentDetailScrollView.scrollTo(binding.momentDetailScrollView.getScrollX() as Int, binding.momentDetailScrollView.getScrollY() as Int + 50)
//            binding.momentDetailScrollView.getParent().requestChildFocus(binding.momentDetailScrollView, binding.momentDetailScrollView);
        }

        binding.imgback.setOnClickListener(View.OnClickListener {
            //activity?.onBackPressed()
            findNavController().popBackStack()
        })

        setSendGiftLayout()
        showLikeBottomSheet()

        binding.imgNearbyUserGift.setOnClickListener(View.OnClickListener {

            if (userId!! != muserID) {
                if (giftbottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                    giftbottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                } else {
                    giftbottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            } else {
                //TODO: show receive gifts
                childFragmentManager.beginTransaction()
                    .replace(R.id.receivedGiftContainer, FragmentReceivedGifts())
                    .commitAllowingStateLoss()

                receivedGiftbottomSheetBehavior =
                    BottomSheetBehavior.from(binding.receivedGiftBottomSheet)
                receivedGiftbottomSheetBehavior.setBottomSheetCallback(object :
                    BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {

                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        when (newState) {
                            BottomSheetBehavior.STATE_COLLAPSED -> {
                                Timber.d("Slide Up")
                            }

                            BottomSheetBehavior.STATE_HIDDEN -> {

                            }

                            BottomSheetBehavior.STATE_EXPANDED -> {
                                Timber.d("Slide Down")
                            }

                            BottomSheetBehavior.STATE_DRAGGING -> {

                            }

                            BottomSheetBehavior.STATE_SETTLING -> {

                            }
                        }
                    }
                })

                if (receivedGiftbottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                    receivedGiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                } else {
                    receivedGiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
            /* var bundle = Bundle()
             bundle.putString("userId", userId)
             navController.navigate(R.id.action_userProfileFragment_to_userGiftsFragment, bundle)*/
        })



        binding.sendBtn.setOnClickListener(View.OnClickListener {


            if (binding.msgWrite.text.toString().equals("")) {
                binding.root.snackbar(getString(R.string.you_cant_add_empty_msg))
                return@OnClickListener

            }

            if (binding.msgWrite.text.toString().startsWith("@") && binding.msgWrite.text.toString()
                    .trim().contains(
                        Replymodels!!.username!!, true
                    )
            ) {
                showProgressView()
                lifecycleScope.launchWhenResumed {
                    val res = try {
                        apolloClient(
                            requireContext(),
                            userToken!!
                        ).mutation(
                            CommentReplyOnMomentMutation(
                                binding.msgWrite.text.toString()
                                    .replace("@" + Replymodels!!.username!!, "").trim(),
                                Replymodels!!.momentID!!, Replymodels!!.cmtID!!
                            )
                        )
                            .execute()
                    } catch (e: ApolloException) {
                        Timber.d("apolloResponse Exception ${e.message}")
                        binding.root.snackbar("${e.message}")
                        hideProgressView()
                        return@launchWhenResumed
                    }
                    if (res.hasErrors()) {
                        val error = res.errors?.get(0)?.message
                        Timber.d("Exception momentUpdateDesc $error")
                        binding.root.snackbar(" $error")
                        hideProgressView()
                        return@launchWhenResumed
                    } else {
                        Log.e("TAG", "CommentReplyOnMomentMutation: "+ res.data.toString())
                        //val allmoments = res.data?.allUserMoments!!.edges
                        if(res.data!!.commentMoment != null && res.data!!.commentMoment!!.comment != null) {
                            val usermoments = res.data?.commentMoment!!.comment!!.momemt
                            //mViewModel.userMomentsList[pos] = allmoments[i]!!
                            // sharedMomentAdapter.submitList1(mViewModel.userMomentsList)


                            binding.lblItemNearbyUserCommentCount.setText("" + usermoments.comment!!)
                        }
                            binding.msgWrite.setText("")
                            itemPosition?.let { it1 ->
                                getMainActivity()?.pref?.edit()
                                    ?.putString("checkUserMomentUpdate", "true")
                                    ?.putString("mID", mID)
                                    ?.putInt(
                                        "itemPosition",
                                        it1
                                    )?.apply()
                            }

                            getAllCommentsofMomentsRefresh()
//                        }

                        hideProgressView()

                    }

                }
            } else {
                showProgressView()
                lifecycleScope.launchWhenResumed {
                    val res = try {
                        apolloClient(
                            requireContext(),
                            userToken!!
                        ).mutation(
                            CommentonmomentMutation(
                                mID.toString(),
                                binding.msgWrite.text.toString()
                            )
                        )
                            .execute()
                    } catch (e: ApolloException) {
                        Timber.d("apolloResponse Exception${e.message}")
                        binding.root.snackbar("${e.message}")
                        hideProgressView()
                        return@launchWhenResumed
                    }
                    if (res.hasErrors()) {
                        val error = res.errors?.get(0)?.message
                        Timber.d("Exception momentUpdateDesc $error")
                        binding.root.snackbar(" $error")
                        hideProgressView()
                        return@launchWhenResumed
                    } else {
                        hideProgressView()
                        Log.e("TAG", "setupTheme: " + res.data)
                        binding.msgWrite.setText("")
                        if (res.data?.commentMoment != null) {
                            if (res.data?.commentMoment!!.comment != null) {

                                val usermoments = res.data?.commentMoment!!.comment!!.momemt

                                binding.lblItemNearbyUserCommentCount.setText("" + usermoments.comment!!)
                            }
                        }
                        itemPosition?.let { it1 ->
                            getMainActivity()?.pref?.edit()
                                ?.putString("checkUserMomentUpdate", "true")?.putString("mID", mID)
                                ?.putInt(
                                    "itemPosition",
                                    it1
                                )?.apply()
                        }

                        if (items.size > 0) {
                            getAllCommentsofMomentsRefresh()

                        } else {
                            getAllCommentsofMoments();
                        }
                        //fireCommentNotificationforreceiver(muserID, mID)

                    }
                }
            }
        })

        binding.txtMomentViewLike.visibility = View.VISIBLE
        binding.txtMomentViewLike.setOnClickListener {

            getMomentLikes()
        }

        binding.imgNearbyUserLikes.setOnClickListener(View.OnClickListener {

            lifecycleScope.launchWhenResumed {
                userId = getCurrentUserId()!!
//            userName = getCurrentUserName()!!
                Log.i("TAG", "onLikeofMomentClick: UserId: $userId   Username: $userName")
                val selectedUserId = muserID
                if (selectedUserId == userId) {
                    getMomentLikes()
                } else {
                    showProgressView()
//                lifecycleScope.launchWhenResumed {
                    Log.e("mID", "" + mID)
                    Log.e("userToken", "" + userToken)
                    val res = try {
                        apolloClient(
                            requireContext(),
                            userToken!!
                        ).mutation(LikeOnMomentMutation(mID!!)).execute()
                    } catch (e: ApolloException) {
                        Timber.d("apolloResponse Exception ${e.message}")
                        binding.root.snackbar("${e.message}")
                        hideProgressView()
                        return@launchWhenResumed
                    }
                    hideProgressView()
                    // Log.e("ress", Gson().toJson(res))
//                    if (res != null) {
                    Log.e("ress", Gson().toJson(res))
                    if (res.hasErrors()) {
                        //binding.txtNearbyUserLikeCount.setText("0")
                    } else {
                        if (res.data != null && res.data!!.likeMoment!!.like != null) {
                            val usermoments = res.data?.likeMoment!!.like!!.momemt
                            binding.txtNearbyUserLikeCount.setText("" + usermoments.like!!)
                        }
                    }
//                    }
                    itemPosition?.let { it1 ->
                        getMainActivity()?.pref?.edit()
                            ?.putString("checkUserMomentUpdate", "true")?.putString("mID", mID)
                            ?.putInt(
                                "itemPosition",
                                it1
                            )?.apply()
                    }
//                    fireLikeNotificationforreceiver(muserID, mID)
                }
            }
        })

        binding.lblItemNearbyName.setOnClickListener(View.OnClickListener {
            binding.lblItemNearbyName.hideKeyboard()
            var bundle = Bundle()
            bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
            bundle.putString("userId", muserID)
            if (userId == muserID) {
                getMainActivity()?.binding?.bottomNavigation?.selectedItemId =
                    R.id.nav_user_profile_graph
            } else {
                findNavController().navigate(
                    destinationId = R.id.action_global_otherUserProfileFragment,
                    popUpFragId = null,
                    animType = AnimationTypes.SLIDE_ANIM,
                    inclusive = true,
                    args = bundle
                )
            }
        })

        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(
                    requireContext(),
                    userToken!!
                ).query(GetUserDataQuery(userId!!))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception${e.message}")
                binding.root.snackbar(" ${e.message}")
                hideProgressView()
                return@launchWhenResumed
            }

            hideProgressView()
            val UserData = res.data?.user

            try {
                if (UserData!!.avatar != null) {

                    try {
                        binding.thumbnail.loadCircleImage(
                            UserData.avatar!!.url!!.replace(
                                "${BuildConfig.BASE_URL_REP}media/",
                                "${BuildConfig.BASE_URL}media/"
                            )
                        )
                        Timber.d(
                            "URL " + UserData.avatar.url!!.replace(
                                "${BuildConfig.BASE_URL_REP}media/",
                                "${BuildConfig.BASE_URL}media/"
                            )
                        )
                    } catch (e: Exception) {
                        e.stackTrace
                    }


                }
            } catch (e: Exception) {
            }


        }


        val url = if (!BuildConfig.USE_S3)
            "${BuildConfig.BASE_URL}${filesUrl}" else filesUrl.toString()

        Log.d("NSMA", "setupTheme: $url")
        if (url.endsWith(".jpg") || url.endsWith(".jpeg") ||
            url.endsWith(".png") || url.endsWith(".webp")){
            binding.playerView.visibility = View.INVISIBLE
            binding.imgSharedMoment.setViewVisible()

            val params = binding.layoutSharedMomentInfo.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.ALIGN_BOTTOM, com.i69.R.id.imgSharedMoment)
            binding.layoutSharedMomentInfo.layoutParams = params

            binding.imgSharedMoment.loadImage(url)
        }
        else {
            binding.playerView.setViewVisible()
            binding.imgSharedMoment.visibility = View.INVISIBLE

            val params = binding.layoutSharedMomentInfo.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.ALIGN_BOTTOM, com.i69.R.id.playerView)

            binding.layoutSharedMomentInfo.layoutParams = params

            binding.playerView.setOnClickListener {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    binding.ivPlay.setViewVisible()
                }
                else {
                    binding.ivPlay.setViewGone()
                    if (exoPlayer.currentPosition > 0L) {
                        exoPlayer.seekTo(exoPlayer.currentPosition)
                        exoPlayer.play()
                    }
                    else {
                        val uri: Uri = Uri.parse(url)
                        val mediaItem = MediaItem.Builder()
                            .setUri(uri)
                            .setMimeType(MimeTypes.VIDEO_MP4)
                            .build()
                        playView(mediaItem)
                    }
                }
            }

            val uri: Uri = Uri.parse(url)
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMimeType(MimeTypes.VIDEO_MP4)
                .build()
            playView(mediaItem)
        }

        binding.txtNearbyUserLikeCount.setText(likess)

        binding.lblItemNearbyUserCommentCount.setText(Comments)

        binding.lblItemNearbyName.setText(fullnames!!.uppercase())

        binding.txtMomentRecentComment.setText(builder);
        binding.txtMomentRecentComment.setMovementMethod(LinkMovementMethod.getInstance())

        if (gender != null) {
            if (gender.equals("A_0")) {
                binding.imgNearbyUserGift.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        requireActivity().resources,
                        R.drawable.yellow_gift_male,
                        null
                    )
                )

            } else if (gender.equals("A_1")) {
                binding.imgNearbyUserGift.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        requireActivity().resources,
                        R.drawable.red_gift_female,
                        null
                    )
                )

            } else if (gender.equals("A_2")) {
                binding.imgNearbyUserGift.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        requireActivity().resources,
                        R.drawable.purple_gift_nosay,
                        null
                    )
                )

            }
//        else
//        {
//            binding.imgNearbyUserGift.setImageDrawable(ResourcesCompat.getDrawable(requireActivity().resources,R.drawable.pink_gift_noavb,null))
//
//        }
        }
        getAllCommentsofMoments()

    }

    private lateinit var exoPlayer: ExoPlayer
    private fun playView(mediaItem: MediaItem) {
        exoPlayer = ExoPlayer.Builder(getMainActivity()!!).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ALL
            setMediaItem(mediaItem, false)
            prepare()
        }
        binding.playerView.player = exoPlayer
        binding.playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
        var durationSet = false
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_READY && !durationSet) {

                }
            }
        })
    }

    override fun onStop() {
        super.onStop()
        if (this::exoPlayer.isInitialized) {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    private fun getMomentLikes() {

        momentLikesUsers.clear()
        lifecycleScope.launch(Dispatchers.Main) {
            userToken?.let {
                Log.d("UserMomentsFragment", "UserMomentNextPage Calling")
                mViewModel.getMomentLikes(it, ((mID ?: "").toString())) { error ->
                    if (error == null) {
                        activity?.runOnUiThread({
                                loadLikesDialog(mViewModel.coinPrice)
                        })

                    } else {

                    }
                }
            }
        }
//        lifecycleScope.launch(Dispatchers.Main) {
//        lifecycleScope.launchWhenResumed {
//
//            val token = getCurrentUserToken()!!
//            mViewModel.getMomentLikes(token, (mID?: "").toString())
//                .observe(viewLifecycleOwner) {
////                .observe(life, observer {
//                    it?.let { momentLikes ->
//                        loadLikesDialog(momentLikes)
//                    }
//                }
//        }
    }

    private fun showLikeBottomSheet() {
//        Log.i(TAG, "showLikeBottomSheet: UserId: ${item?.node?.pk}")

        binding.rvLikes.layoutManager = LinearLayoutManager(activity)
        if (binding.rvLikes.itemDecorationCount == 0) {
            binding.rvLikes.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.top = 25
                }
            })
        }


        val items1: ArrayList<CommentsModel> = ArrayList()
//        momentLikesUsers.forEach { i ->
//            val models = CommentsModel()
//            models.commenttext = i.user.fullName
//            models.uid = i.user.id
//            models.userurl = i.user.avatar?.url
//            items1.add(models)
//        }
        momentLikesAdapters =
            StoryLikesAdapter(requireActivity(), items1, Glide.with(requireContext()))

        momentLikesAdapters.userProfileClicked {
            LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            LogUtil.debug("UserStoryDetailsFragment : : : $it")
            val bundle = Bundle()
            bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
            bundle.putString("userId", it.uid)
            if (userId == it.uid) {
                getMainActivity()?.binding?.bottomNavigation?.selectedItemId =
                    R.id.nav_user_profile_graph
            } else {
                findNavController().navigate(
                    destinationId = R.id.action_global_otherUserProfileFragment,
                    popUpFragId = null,
                    animType = AnimationTypes.SLIDE_ANIM,
                    inclusive = true,
                    args = bundle
                )
            }
        }
        binding.rvLikes.adapter = momentLikesAdapters

        val likebottomSheet = binding.likebottomSheet
        LikebottomSheetBehavior = BottomSheetBehavior.from(likebottomSheet)
//        LikebottomSheetBehavior.setBottomSheetCallback(object :
        LikebottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        Timber.d("STATE_COLLAPSED")
//                        binding.rvSharedMoments.visibility = View.VISIBLE
                    }

                    BottomSheetBehavior.STATE_HIDDEN -> {
                        Timber.d("STATE_HIDDEN")
                    }

                    BottomSheetBehavior.STATE_EXPANDED -> {
                        Timber.d("STATE_EXPANDED")
//                        binding.rvSharedMoments.visibility = View.INVISIBLE
                    }

                    BottomSheetBehavior.STATE_DRAGGING -> {
                        Timber.d("STATE_DRAGGING")
                    }

                    BottomSheetBehavior.STATE_SETTLING -> {
                        Timber.d("STATE_SETTLING")
                    }
                }
            }
        })
    }

    private fun loadLikesDialog(momentLikes: ArrayList<MomentLikes>) {
        momentLikesUsers.clear()
        momentLikesUsers.addAll(momentLikes)
//        showLikeBottomSheet()
//        val dialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
//        val view =
//            LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_layout2, null)
//        dialog.setContentView(view)

//        val rvLikes = binding.rvLikes
//        val no_data = binding.noDatas
//        val txtHeaderLike = binding.txtheaderlike
//
        binding.txtheaderlike.text = momentLikes.size.toString() + " Likes"

        if (momentLikesUsers.isNotEmpty()) {
            binding.noDatas.visibility = View.GONE
            binding.rvLikes.visibility = View.VISIBLE

            val items1: ArrayList<CommentsModel> = ArrayList()
            momentLikesUsers.forEach { i ->
                val models = CommentsModel()
                models.commenttext = i.user.fullName
                models.uid = i.user.id
                models.userurl = i.user.avatar?.url
                items1.add(models)
            }

            momentLikesAdapters.addAll(items1)
            momentLikesAdapters.notifyDataSetChanged()
//            val adapters =
//                StoryLikesAdapter(requireActivity(), items1, Glide.with(requireContext()))

//            adapters.userProfileClicked {
//                LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//                LogUtil.debug("UserStoryDetailsFragment : : : $it")
//                val bundle = Bundle()
//                bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
//                bundle.putString("userId", it.uid)
//                if (userId == it.uid) {
//                    MainActivity.getMainActivity()?.binding?.bottomNavigation?.selectedItemId =
//                        R.id.nav_user_profile_graph
//                } else {
//                    findNavController().navigate(
//                        destinationId = R.id.action_global_otherUserProfileFragment,
//                        popUpFragId = null,
//                        animType = AnimationTypes.SLIDE_ANIM,
//                        inclusive = true,
//                        args = bundle
//                    )
//                }
//            }
//            binding.rvLikes.adapter = adapters

        } else {
            binding.noDatas.visibility = View.VISIBLE
            binding.rvLikes.visibility = View.GONE
        }
        if (LikebottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.rvSharedMoments.visibility = View.INVISIBLE
//            Log.d("UserStoryDetailsFragment", "STATE_EXPANDED")
        } else {
            LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            binding.rvSharedMoments.visibility = View.VISIBLE
//            Log.d("UserStoryDetailsFragment", "STATE_COLLAPSED")
        }
    }


    private fun setSendGiftLayout() {
        val giftBottomSheet = binding.root.findViewById<ConstraintLayout>(R.id.giftbottomSheet)
        val sendgiftto = giftBottomSheet.findViewById<MaterialTextView>(R.id.sendgiftto)
        val giftsTabs = giftBottomSheet.findViewById<TabLayout>(R.id.giftsTabs)
        val giftsPager = giftBottomSheet.findViewById<ViewPager>(R.id.gifts_pager)
        giftsTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {
                giftsPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
            }
        })
        giftsTabs.setupWithViewPager(giftsPager)
        setupViewPager(giftsPager)
        sendgiftto.text = context?.resources?.getString(R.string.send_git_to) + " " + fullnames
        sendgiftto.setOnClickListener {

            val items: MutableList<ModelGifts.Data.AllRealGift> = mutableListOf()
            fragVirtualGifts.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }
            fragRealGifts.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }

            lifecycleScope.launchWhenCreated() {
                if (items.size > 0) {
                    showProgressView()
                    items.forEach { gift ->
                        Log.e("gift.id", gift.id)
                        Log.e("giftUserid", muserID.toString())
                        var res: ApolloResponse<GiftPurchaseMutation.Data>? = null
                        try {
                            res = apolloClient(
                                requireContext(),
                                userToken!!
                            ).mutation(GiftPurchaseMutation(gift.id, muserID!!, userId!!)).execute()
                        } catch (e: ApolloException) {
                            Timber.d("apolloResponse Exception ${e.message}")
                            Toast.makeText(
                                requireContext(),
                                " ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            //                                views.snackbar("Exception ${e.message}")
                            //hideProgressView()
                            //return@launchWhenResumed
                        }
                        Log.e("resee", Gson().toJson(res))

                        if (res?.hasErrors() == false) {
                            //                                views.snackbar("You bought ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} successfully!")
                            Toast.makeText(
                                requireContext(),
                                context?.resources?.getString(R.string.you_bought) + " ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} " + context?.resources?.getString(
                                    R.string.successfully
                                ),
                                Toast.LENGTH_LONG
                            ).show()

                            //fireGiftBuyNotificationforreceiver(gift.id, giftUserid)

                        }
                        if (res!!.hasErrors()) {
                            //                                views.snackbar(""+ res.errors!![0].message)
                            Toast.makeText(
                                requireContext(),
                                "" + res.errors!![0].message,
                                Toast.LENGTH_LONG
                            ).show()

                        }
                        Timber.d("apolloResponse ${res.hasErrors()} ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName}")
                    }
                    hideProgressView()
                }
            }

        }
        giftbottomSheetBehavior = BottomSheetBehavior.from(giftBottomSheet)
//        giftbottomSheetBehavior.setBottomSheetCallback(object :
        giftbottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        Timber.d("Slide Up")
                    }

                    BottomSheetBehavior.STATE_HIDDEN -> {

                    }

                    BottomSheetBehavior.STATE_EXPANDED -> {
                        Timber.d("Slide Down")
                    }

                    BottomSheetBehavior.STATE_DRAGGING -> {

                    }

                    BottomSheetBehavior.STATE_SETTLING -> {

                    }

                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {

                    }
                }
            }
        })
    }

    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = UserItemsAdapter(childFragmentManager)
        fragRealGifts = FragmentRealGifts()
        fragVirtualGifts = FragmentVirtualGifts()

        adapter.addFragItem(fragRealGifts, getString(R.string.real_gifts))
        adapter.addFragItem(fragVirtualGifts, getString(R.string.virtual_gifts))
        viewPager.adapter = adapter
    }


    private fun fireGiftBuyNotificationforreceiver(gid: String, userid: String?) {
        lifecycleScope.launchWhenResumed {

            val queryName = "sendNotification"
            val query = StringBuilder()
                .append("mutation {")
                .append("$queryName (")
                .append("userId: \"${userid}\", ")
                .append("notificationSetting: \"GIFT RLVRTL\", ")
                .append("data: {giftId:${gid}}")
                .append(") {")
                .append("sent")
                .append("}")
                .append("}")
                .toString()

            val result = AppModule.provideGraphqlApi().getResponse<Boolean>(
                query,
                queryName, userToken
            )
            Timber.d("RSLT", "" + result.message)

        }
    }

    private fun getAllCommentsofMoments() {
        items = ArrayList()

        showProgressView()
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(
                    requireContext(),
                    userToken!!
                ).query(GetAllCommentOfMomentQuery(mID!!))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("getAllComments ${e.message}")
                hideProgressView()
                return@launchWhenResumed
            }

            hideProgressView()
            val allusermoments = res.data?.allComments

            if (binding.rvSharedMoments.itemDecorationCount == 0) {
                binding.rvSharedMoments.addItemDecoration(object :
                    RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.top = 25
                    }
                })
            }
            if (allusermoments?.size!! > 0) {
                Timber.d("apolloResponse: ${allusermoments.get(0)?.commentDescription}")
                binding.noData.visibility = View.GONE
                binding.rvSharedMoments.visibility = View.VISIBLE



                allusermoments.indices.forEach { i ->
                    val hm: MutableList<ReplysModel> = ArrayList()


                    val models = CommentsModel()

                    models.commenttext = allusermoments[i]!!.commentDescription

                    if (allusermoments[i]!!.user.avatarPhotos?.size!! > 0) {
                        models.userurl = allusermoments[i]!!.user.avatarPhotos?.get(0)?.url

                    } else {
                        models.userurl = ""
                    }
                    models.username = allusermoments[i]!!.user.fullName
                    models.timeago = allusermoments[i]!!.createdDate.toString()
                    models.cmtID = allusermoments[i]!!.pk.toString()
                    models.momentID = mID
                    models.cmtlikes = allusermoments[i]!!.like.toString()
                    models.uid = allusermoments[i]!!.user.id.toString()

                    for (f in 0 until allusermoments[i]!!.replys!!.size) {


                        val md = ReplysModel()

                        md.replytext = allusermoments[i]!!.replys!![f]!!.commentDescription
                        md.userurl =
                            allusermoments[i]!!.replys!![f]!!.user.avatarPhotos?.get(0)?.url
                        md.usernames = allusermoments[i]!!.replys!![f]!!.user.fullName
                        md.timeago = allusermoments[i]!!.createdDate.toString()
                        md.uid = allusermoments[i]!!.user.id.toString()


                        hm.add(f, md)


                    }

                    models.replylist = hm
                    models.isExpanded = true



                    items.add(models)
                }




                adapter =
                    CommentListAdapter(
                        requireActivity(),
                        this@MomentAddCommentFragment,
                        items,
                        this@MomentAddCommentFragment,
                        userId == muserID,
                        userId!!
                    )
                binding.rvSharedMoments.adapter = adapter
                binding.rvSharedMoments.layoutManager = LinearLayoutManager(activity)
            } else {
                binding.noData.visibility = View.VISIBLE
                binding.rvSharedMoments.visibility = View.GONE

            }
        }
    }


    private fun getAllCommentsofMomentsRefresh() {

        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(
                    requireContext(),
                    userToken!!
                ).query(GetAllCommentOfMomentQuery(mID!!))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse getAllComments ${e.message}")
                binding.root.snackbar(" ${e.message}")
                return@launchWhenResumed
            }
            if (res.hasErrors()) {
                val error = res.errors?.get(0)?.message
                Timber.d("Exception momentUpdateDesc $error")
                binding.root.snackbar(" $error")
                hideProgressView()
                return@launchWhenResumed
            } else {
                val allusermoments = res.data?.allComments

                binding.lblItemNearbyUserCommentCount.setText("" + res.data?.allComments?.size)
                if (binding.rvSharedMoments.itemDecorationCount == 0) {
                    binding.rvSharedMoments.addItemDecoration(object :
                        RecyclerView.ItemDecoration() {
                        override fun getItemOffsets(
                            outRect: Rect,
                            view: View,
                            parent: RecyclerView,
                            state: RecyclerView.State
                        ) {
                            outRect.top = 25
                        }
                    })
                }
                if (allusermoments?.size!! > 0) {
                    Timber.d("apolloResponse: ${allusermoments.get(0)?.commentDescription}")
                    binding.noData.visibility = View.GONE
                    binding.rvSharedMoments.visibility = View.VISIBLE


                    val items1: ArrayList<CommentsModel> = ArrayList()



                    allusermoments.indices.forEach { i ->
                        val hm: MutableList<ReplysModel> = ArrayList()


                        val models = CommentsModel()

                        models.commenttext = allusermoments[i]!!.commentDescription
                        if (allusermoments[i]!!.user.avatarPhotos?.size!! > 0) {
                            models.userurl = allusermoments[i]!!.user.avatarPhotos?.get(0)?.url

                        } else {
                            models.userurl = ""
                        }
                        models.username = allusermoments[i]!!.user.fullName
                        models.timeago = allusermoments[i]!!.createdDate.toString()
                        models.cmtID = allusermoments[i]!!.pk.toString()
                        models.momentID = mID
                        models.cmtlikes = allusermoments[i]!!.like.toString()
                        models.uid = allusermoments[i]!!.user.id.toString()

                        for (f in 0 until allusermoments[i]!!.replys!!.size) {


                            val md = ReplysModel()

                            md.replytext = allusermoments[i]!!.replys!![f]!!.commentDescription
                            md.userurl =
                                allusermoments[i]!!.replys!![f]!!.user.avatarPhotos?.get(0)?.url
                            md.usernames = allusermoments[i]!!.replys!![f]!!.user.fullName
                            md.timeago = allusermoments[i]!!.createdDate.toString()
                            md.uid = allusermoments[i]!!.user.id.toString()


                            hm.add(f, md)


                        }

                        models.replylist = hm
                        models.isExpanded = true




                        items1.add(models)

                        if (items1.size - 1 == i) {
                            if (items.size != 0) {
                                if (adapter != null) {
                                    adapter!!.updateList(items1)
                                }

                            }
                        }
                    }


                } else {
                    binding.noData.visibility = View.VISIBLE
                    binding.rvSharedMoments.visibility = View.GONE

                }
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val arguments = arguments
        if (arguments != null) {
            mID = arguments.getString("momentID")
            filesUrl = arguments.getString("filesUrl")
            likess = arguments.getString("Likes")
            Comments = arguments.getString("Comments")
            Desc_ = arguments.getString("Desc")
            fullnames = arguments.getString("fullnames")
            muserID = arguments.getString("momentuserID")
            gender = arguments.getString("gender")
            giftUserid = arguments.getString("userId")
            itemPosition = arguments.getInt("itemPosition")

            Desc1_ = Arrays.asList(Desc_)


            if (Desc_!!.split(",").size > 1) {
                val s1 =
                    SpannableString(
                        Desc_!!.split(",")[0].replace("[\"", "").replace("\"]", "")
                            .replace("\"", "")
                    )
//                val s2 = SpannableString("READ MORE")
                val s2 = SpannableString(resources.getString(R.string.read_more))

                s1.setSpan(
                    ForegroundColorSpan(Color.WHITE),
                    0,
                    s1.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                s2.setSpan(
                    ForegroundColorSpan(resources.getColor(R.color.colorPrimary)),
                    0,
                    s2.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                s2.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    s2.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )


                s2.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        builder = SpannableStringBuilder()
                        Desc1_!!.forEach { builder!!.append(it).toString() }

                        binding.txtMomentRecentComment.setText(
                            builder.toString().replace("[\"", "").replace("\"]", "")
                                .replace("\",\"", "")
                        )

                    }
                }, 0, s2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)


                // build the string
                builder = SpannableStringBuilder()
                builder!!.append(s1)
                builder!!.append(s2)
            } else {
                builder = SpannableStringBuilder()
                builder!!.append(Desc_!!.replace("[\"", "").replace("\"]", ""))
            }


        }
        return super.onCreateView(inflater, container, savedInstanceState)

    }


    override fun setupClickListeners() {

    }

    fun fireLikeNotificationforreceiver(muserID: String?, mID: String?) {
        lifecycleScope.launchWhenResumed {


            val queryName = "sendNotification"
            val query = StringBuilder()
                .append("mutation {")
                .append("$queryName (")
                .append("userId: \"${muserID}\", ")
                .append("notificationSetting: \"LIKE\", ")
                .append("data: {momentId:${mID}}")
                .append(") {")
                .append("sent")
                .append("}")
                .append("}")
                .toString()

            val result = AppModule.provideGraphqlApi().getResponse<Boolean>(
                query,
                queryName, userToken
            )
            Timber.d("RSLT", "" + result.message)

        }


    }


    fun fireCommentNotificationforreceiver(muserID: String?, mID: String?) {


        lifecycleScope.launchWhenResumed {


            val queryName = "sendNotification"
            val query = StringBuilder()
                .append("mutation {")
                .append("$queryName (")
                .append("userId: \"${muserID}\", ")
                .append("notificationSetting: \"CMNT\", ")
                .append("data: {momentId:${mID}}")
                .append(") {")
                .append("sent")
                .append("}")
                .append("}")
                .toString()

            val result = AppModule.provideGraphqlApi().getResponse<Boolean>(
                query,
                queryName, userToken
            )
            Timber.d("RSLT", "" + result.message)

        }


    }


    override fun onreply(position: Int, models: CommentsModel) {

        Replymodels = models

//        binding.msgWrite.performClick()
        binding.msgWrite.setText("")
        binding.msgWrite.requestFocus()
        binding.msgWrite.postDelayed(Runnable {

            val inputMethodManager =
                requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?

            inputMethodManager!!.showSoftInput(
                binding.msgWrite,
                InputMethodManager.SHOW_IMPLICIT
            )
            binding.msgWrite.append("@" + models.username + " ")

        }, 150)

    }


    override fun oncommentLike(position: Int, models: CommentsModel) {
        showProgressView()
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(
                    requireContext(),
                    userToken!!
                ).mutation(
                    LikeOnCommentMutation(
                        models.cmtID!!
                    )
                )
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception${e.message}")
                binding.root.snackbar(" ${e.message}")
                hideProgressView()
                return@launchWhenResumed
            }

            if (res.hasErrors()) {

            }
            hideProgressView()

            getAllCommentsofMomentsRefresh()

        }
    }

    private fun reportDialog(position: Int, models: CommentsModel) {

        val dialogLayout = layoutInflater.inflate(R.layout.dialog_report, null)
        val reportView = dialogLayout.findViewById<TextView>(R.id.report_view)
        val reportMessage = dialogLayout.findViewById<EditText>(R.id.report_message)
        val okButton = dialogLayout.findViewById<TextView>(R.id.ok_button)
        val cancleButton = dialogLayout.findViewById<TextView>(R.id.cancel_button)

//        reportView.text = "${AppStringConstant(getMainActivity()).are_you_sure_you_want_to_delete_story}"
        okButton.text = "${AppStringConstant(requireActivity()).ok}"
        cancleButton.text = "${AppStringConstant(requireActivity()).cancel}"

        val builder = AlertDialog.Builder(activity,R.style.DeleteDialogTheme)
        builder.setView(dialogLayout)
        builder.setCancelable(false)
        val dialog = builder.create()

        okButton.setOnClickListener {
            val message = reportMessage.text.toString()
            showProgressView()
            Log.e("MyCommmentPk", models?.cmtID!!)
            lifecycleScope.launchWhenResumed {
                val res = try {
                    apolloClient(
                        requireContext(),
                        userToken!!
                    ).mutation(ReportCommentMutation(message, models?.cmtID!!.toInt()))
//                    ).mutation(ReportCommentMutation(models?.cmtID!!,message))
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponse Exception ${e.message}")
                    Log.e("reportsOnComens11", "${e}")
                    Log.e("reportsOnComens", "Exception : ${e.message}")
                    binding.root.snackbar(" ${e.message}")
                    hideProgressView()
                    dialog.dismiss()
                    return@launchWhenResumed
                }

                if (res.hasErrors()) {
                    val error = res.errors?.get(0)?.message
                    Timber.d("Exception momentCommentDelete $error")
                    Log.e("reportsOnComens", "Error : ${error}")
                    Log.e("reportsOnComens111", "${res.errors}")
                    binding.root.snackbar(" $error")
                    hideProgressView()
                    dialog.dismiss()
                    return@launchWhenResumed
                } else {
                    hideProgressView()
                    getAllCommentsofMomentsRefresh()
                }

                dialog.dismiss()

            }
        }

        cancleButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun oncommentReport(position: Int, models: CommentsModel) {
        Log.e("myCommentReportClick", models!!.commenttext!!)

        reportDialog(position, models)
//        showProgressView()
//        lifecycleScope.launchWhenResumed {
//            val res = try {
//                apolloClient(
//                    requireContext(),
//                    userToken!!
//                ).mutation(
//                    CommentDeleteMutation(
//                        models.cmtID!!
//                    )
//                )
//                    .execute()
//            } catch (e: ApolloException) {
//                Timber.d("apolloResponse Exception${e.message}")
//                binding.root.snackbar(" ${e.message}")
//                hideProgressView()
//                return@launchWhenResumed
//            }
//
//            if (res.hasErrors()) {
//                val error = res.errors?.get(0)?.message
//                Timber.d("Exception momentCommentDelete $error")
//                binding.root.snackbar(" $error")
//                hideProgressView()
//                return@launchWhenResumed
//            }
//            hideProgressView()
//
//            getAllCommentsofMomentsRefresh()
//
//        }
    }


    override fun oncommentDelete(position: Int, models: CommentsModel) {
        showProgressView()
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(
                    requireContext(),
                    userToken!!
                ).mutation(
                    CommentDeleteMutation(
                        models.cmtID!!
                    )
                )
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception${e.message}")
                binding.root.snackbar(" ${e.message}")
                hideProgressView()
                return@launchWhenResumed
            }

            if (res.hasErrors()) {
                val error = res.errors?.get(0)?.message
                Timber.d("Exception momentCommentDelete $error")
                binding.root.snackbar(" $error")
                hideProgressView()
                return@launchWhenResumed
            }
            hideProgressView()

            getAllCommentsofMomentsRefresh()

        }
    }

    override fun onUsernameClick(position: Int, models: CommentsModel) {
        var bundle = Bundle()
        bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
        bundle.putString("userId", models.uid)
        if (userId == models.uid) {
            getMainActivity()?.binding?.bottomNavigation?.selectedItemId =
                R.id.nav_user_profile_graph

        } else {
            findNavController().navigate(
                destinationId = R.id.action_global_otherUserProfileFragment,
                popUpFragId = null,
                animType = AnimationTypes.SLIDE_ANIM,
                inclusive = true,
                args = bundle
            )
        }
    }

    override fun onUsernameClick(position: Int, models: ReplysModel) {
        var bundle = Bundle()
        bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
        bundle.putString("userId", models.uid)
        if (userId == models.uid) {
            getMainActivity()?.binding?.bottomNavigation?.selectedItemId =
                R.id.nav_user_profile_graph

        } else {
            findNavController().navigate(
                destinationId = R.id.action_global_otherUserProfileFragment,
                popUpFragId = null,
                animType = AnimationTypes.SLIDE_ANIM,
                inclusive = true,
                args = bundle
            )
        }
    }
}