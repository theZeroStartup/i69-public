package com.i69.ui.screens.main.moment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Rect
import com.i69.BuildConfig
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.exception.ApolloException
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textview.MaterialTextView
import com.i69.*
import com.i69.R
import com.i69.applocalization.AppStringConstant
import com.i69.data.models.ModelGifts
import com.i69.di.modules.AppModule
import com.i69.gifts.FragmentRealGifts
import com.i69.gifts.FragmentVirtualGifts
import com.i69.ui.adapters.*
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.screens.main.search.userProfile.SearchUserProfileFragment
import com.i69.ui.viewModels.CommentsModel
import com.i69.ui.viewModels.ReplysModel
import com.i69.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import timber.log.Timber


const val STATE_RESUME_WINDOW = "resumeWindow"
const val STATE_RESUME_POSITION = "resumePosition"
const val STATE_PLAYER_FULLSCREEN = "playerFullscreen"
const val STATE_PLAYER_PLAYING = "playerOnPlay"
class PlayUserStoryDialogFragment(val listener: UserStoryDetailFragment.DeleteCallback?)  :
    DialogFragment(), StoryCommentListAdapter.ClickPerformListener, CommentReplyListAdapter.ClickonListener{

    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var isFullscreen = false
    private var isPlayerPlaying = true

    private var countUp: Int = 100
    private lateinit var loadingDialog: Dialog

    private lateinit var views: View
    var progressBar1: ProgressBar? = null


    private lateinit var exoPlayer: SimpleExoPlayer
    private lateinit var dataSourceFactory: DataSource.Factory


    var player_view: PlayerView? = null

    var adapters: StoryLikesAdapter? = null

    private var timer1: CountDownTimerExt? = null

    lateinit var pause : ImageView


    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var GiftbottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var LikebottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    var txtNearbyUserLikeCount: MaterialTextView? = null
    var txtMomentRecentComment: MaterialTextView? = null
    var lblItemNearbyUserCommentCount: MaterialTextView? = null
    var msg_write : EditText? = null
    var rvSharedMoments: RecyclerView? = null
    var nodata: MaterialTextView? = null

    var items: ArrayList<CommentsModel> = ArrayList()

    lateinit var glide : RequestManager


    var rvLikes: RecyclerView? = null



    var no_data: MaterialTextView? = null
    var txtheaderlike: MaterialTextView? = null

    var giftUserid: String? = null

    var fragVirtualGifts: FragmentVirtualGifts?= null
    var fragRealGifts: FragmentRealGifts?= null

    var userToken: String? = null

    var Uid: String? = null

    var objectID: Int? = null
    var Replymodels: CommentsModel? = null



    var adapter: VideoStoryCommentListAdapter? = null

    override fun getTheme(): Int {
        return R.style.DialogTheme
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        views = inflater.inflate(R.layout.fragment_user_story_detail, container, false)
        if (savedInstanceState != null) {
            currentWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW)
            playbackPosition = savedInstanceState.getLong(STATE_RESUME_POSITION)
            isFullscreen = savedInstanceState.getBoolean(STATE_PLAYER_FULLSCREEN)
            isPlayerPlaying = savedInstanceState.getBoolean(STATE_PLAYER_PLAYING)
        }
        return views
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        pausetimerandplayer()
    }


    fun pausetimerandplayer()

    {
        if(timer1 != null)
        {
            timer1!!.pause()

        }
        if (exoPlayer != null){
            exoPlayer.playWhenReady = false
            pause.visibility = View.VISIBLE
        }
    }

    fun resumetimerandplayer()
    {

        if(timer1 != null)
        {
            timer1!!.start()

        }

        if (exoPlayer != null){
            exoPlayer.playWhenReady = true
            pause.visibility = View.GONE
        }
    }


    override fun onStart() {
        super.onStart()




        loadingDialog = requireContext().createLoadingDialog()
        val userPic = views.findViewById<ImageView>(R.id.userPic)
        val lblName = views.findViewById<MaterialTextView>(R.id.lblName)
        val ctParent = views.findViewById<RelativeLayout>(R.id.ct_parent)
        val txtTimeAgo = views.findViewById<MaterialTextView>(R.id.txtTimeAgo)
        val sendgiftto = views.findViewById<MaterialTextView>(R.id.sendgiftto)
        progressBar1 = views.findViewById<ProgressBar>(R.id.progressBar1)
        val imgUserStory = views.findViewById<ImageView>(R.id.imgUserStory)
        val imgNearbyUserComment = views.findViewById<ImageView>(R.id.imgNearbyUserComment)
        val img_close = views.findViewById<ImageView>(com.i69.R.id.img_close)
        val imgNearbyUserGift = views.findViewById<ImageView>(R.id.imgNearbyUserGift)
        val giftsTabs = views.findViewById<TabLayout>(R.id.giftsTabs)
        val giftsPager = views.findViewById<ViewPager>(R.id.gifts_pager)
        txtNearbyUserLikeCount = views.findViewById<MaterialTextView>(R.id.txtNearbyUserLikeCount)
        txtMomentRecentComment = views.findViewById<MaterialTextView>(R.id.txtMomentRecentComment)
        lblItemNearbyUserCommentCount = views.findViewById(R.id.lblItemNearbyUserCommentCount)
        val thumbnail = views.findViewById<ImageView>(R.id.thumbnail)
        val send_btn = views.findViewById<ImageView>(R.id.send_btn)
         msg_write = views.findViewById<EditText>(R.id.msg_write)
        val imgNearbyUserLikes = views.findViewById<ImageView>(R.id.imgNearbyUserLikes)
        rvSharedMoments = views.findViewById<RecyclerView>(R.id.rvSharedMoments)
        nodata = views.findViewById<MaterialTextView>(R.id.no_data)
        player_view = views.findViewById<PlayerView>(com.i69.R.id.player_view)
        player_view!!.setShutterBackgroundColor(Color.TRANSPARENT);
        player_view!!.setKeepContentOnPlayerReset(true)
        dataSourceFactory = DefaultDataSourceFactory(requireActivity(), Util.getUserAgent(requireActivity(), "i69"))

        rvLikes = views.findViewById<RecyclerView>(R.id.rvLikes)
        no_data = views.findViewById<MaterialTextView>(R.id.no_datas)
        txtheaderlike = views.findViewById<MaterialTextView>(R.id.txtheaderlike)
        val likes_l = views.findViewById<LinearLayout>(R.id.likes_l)
        val likes_view = views.findViewById<LinearLayout>(R.id.likes_view)
        val comment_l = views.findViewById<LinearLayout>(R.id.comment_l)
        val gift_l = views.findViewById<LinearLayout>(R.id.gift_l)
        val delete_story = views.findViewById<ImageView>(R.id.delete_story)
         pause = views.findViewById<ImageView>(R.id.iv_pause)
        val showDelete = arguments?.getBoolean("showDelete") ?: false
        val report_l = views.findViewById<LinearLayout>(R.id.report_l)

        if (showDelete) {
            delete_story.visibility = View.VISIBLE
            report_l.visibility = View.GONE
        } else {
            report_l.visibility = View.VISIBLE
        }

        /*if(Uid.equals(giftUserid))
        {
            report_l.visibility = View.GONE
        }else{
            report_l.visibility = View.VISIBLE
        }*/


        glide = Glide.with(requireContext())

        Uid = arguments?.getString("Uid","")
        val url = arguments?.getString("url", "")

        Timber.d("playview ${arguments?.getString("url")}")
        // Uri object to refer the
        // resource from the videoUrl

        val uri: Uri = Uri.parse(url)
        //val uri: Uri = Uri.fromFile(File(arguments?.getString("path")))


        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(MimeTypes.VIDEO_MP4)
            .build()



        playView(mediaItem)

        val userurl = arguments?.getString("userurl", "")
        val username = arguments?.getString("username", "")
        val times = arguments?.getString("times", "")
        userToken = arguments?.getString("token", "")
        objectID= arguments?.getInt("objectID", 0)


        getStories()



        ctParent.setOnClickListener {

            if (exoPlayer.playWhenReady){
                pausetimerandplayer()
            }else{
                resumetimerandplayer()
            }

        }



        player_view!!.visibility = View.VISIBLE
        imgUserStory.visibility = View.GONE


        userPic.loadCircleImage(userurl!!)
        lblName.text = username
        txtTimeAgo.text = times
        sendgiftto.text = context?.resources?.getString(R.string.send_git_to)+" "+ username!!



        userPic.setOnClickListener(View.OnClickListener {
            var bundle = Bundle()
            bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
            bundle.putString("userId", giftUserid)

            pausetimerandplayer()


            findNavController().navigate(
                destinationId = R.id.action_global_otherUserProfileFragment,
                popUpFragId = null,
                animType = AnimationTypes.SLIDE_ANIM,
                inclusive = true,
                args = bundle
            )
            dismiss()
        })

        lblName.setOnClickListener(View.OnClickListener {
            val bundle = Bundle()
            bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
            bundle.putString("userId", giftUserid)

            pausetimerandplayer()

            findNavController().navigate(
                destinationId = R.id.action_global_otherUserProfileFragment,
                popUpFragId = null,
                animType = AnimationTypes.SLIDE_ANIM,
                inclusive = true,
                args = bundle
            )
            dismiss()
        })



        img_close.setOnClickListener(View.OnClickListener {
            pausetimerandplayer()
            dismiss()
        })


        comment_l.setOnClickListener(View.OnClickListener {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                pausetimerandplayer()

            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED;
                resumetimerandplayer()


            }
        })


        gift_l.setOnClickListener(View.OnClickListener {

            if (Uid.equals(giftUserid)) {
                Toast.makeText(requireContext(), getString(R.string.user_cant_bought_gift_yourseld), Toast.LENGTH_LONG)
                    .show()
            }else {
                if (GiftbottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                    GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            buttonSlideUp.text = "Slide Down";
                    pausetimerandplayer()


                } else {
                    GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED;
//            buttonSlideUp.text = "Slide Up"
                    resumetimerandplayer()


                }
            }
        })

        likes_view.setOnClickListener {
            if (LikebottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            buttonSlideUp.text = "Slide Down";
                pausetimerandplayer()


            } else {
                LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED;
//            buttonSlideUp.text = "Slide Up"
                resumetimerandplayer()


            }
        }

        likes_l.setOnClickListener(View.OnClickListener {
            if(Uid.equals(giftUserid))
            {
                if (LikebottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                    LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            buttonSlideUp.text = "Slide Down";
                    pausetimerandplayer()


                } else {
                    LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED;
//            buttonSlideUp.text = "Slide Up"
                    resumetimerandplayer()


                }
            }
            else
            {
                //            showProgressView()
                lifecycleScope.launchWhenResumed {
                    val res = try {
                        apolloClient(
                            requireContext(),
                            userToken!!
                        ).mutation(LikeOnStoryMutation(objectID!!,"story"))
                            .execute()
                    } catch (e: ApolloException) {
                        Timber.d("apolloResponseException ${e.message}")
//                    binding.root.snackbar("Exception ${e.message}")
                        Toast.makeText(requireContext()," ${e.message}",Toast.LENGTH_LONG).show()

//                    hideProgressView()
                        return@launchWhenResumed
                    }

//                hideProgressView()
                    val usermoments = res.data?.genericLike
//                    fireLikeStoryNotification(objectID.toString(),giftUserid)
                    Timber.d("LIKE")

                    RefreshStories()



                }
            }
        })





        val bottomSheet = views.findViewById<ConstraintLayout>(com.i69.R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from<ConstraintLayout>(bottomSheet)
        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        Timber.d("Slide Up")
                        resumetimerandplayer()


                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        Timber.d("Slide Hidden")

                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        Timber.d("Slide Down")
                        pausetimerandplayer()



                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {
                        Timber.d("Slide Dragging")

                    }
                    BottomSheetBehavior.STATE_SETTLING -> {

                    }
                }
            }
        })

        val giftbottomSheet = views.findViewById<ConstraintLayout>(R.id.giftbottomSheet)
        GiftbottomSheetBehavior = BottomSheetBehavior.from<ConstraintLayout>(giftbottomSheet)
        GiftbottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        Timber.d("Slide Up")
                        resumetimerandplayer()


                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {

                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        Timber.d("Slide Down")
                        pausetimerandplayer()


                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {

                    }
                    BottomSheetBehavior.STATE_SETTLING -> {

                    }
                }
            }
        })
        val likebottomSheet = views.findViewById<ConstraintLayout>(R.id.likebottomSheet)
        LikebottomSheetBehavior = BottomSheetBehavior.from<ConstraintLayout>(likebottomSheet)
        LikebottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        Timber.d("Slide Up")
                        resumetimerandplayer()

                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {

                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        Timber.d("Slide Down")
                        pausetimerandplayer()


                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {

                    }
                    BottomSheetBehavior.STATE_SETTLING -> {

                    }
                }
            }
        })
        sendgiftto.setOnClickListener(View.OnClickListener {

            val items: MutableList<ModelGifts.Data.AllRealGift> = mutableListOf()
            fragVirtualGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }
            fragRealGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }

            lifecycleScope.launchWhenCreated() {
                if (items.size > 0) {
                    showProgressView()
                    items.forEach { gift ->

                        var res: ApolloResponse<GiftPurchaseMutation.Data>? = null
                        try {
                            res = apolloClient(
                                requireContext(),
                                userToken!!
                            ).mutation(GiftPurchaseMutation(gift.id, giftUserid!! , Uid!!)).execute()
                        } catch (e: ApolloException) {
                            Timber.d("apolloResponseException ${e.message}")
                            Toast.makeText(requireContext()," ${e.message}", Toast.LENGTH_LONG).show()
//                                views.snackbar("Exception ${e.message}")
                            //hideProgressView()
                            //return@launchWhenResumed
                        }

                        if (res?.hasErrors() == false) {
//                                views.snackbar("You bought ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} successfully!")
                            Toast.makeText(requireContext(),context?.resources?.getString(R.string.you_bought)+" ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} "+context?.resources?.getString(R.string.successfully),
                                Toast.LENGTH_LONG).show()

                         //   fireGiftBuyNotificationforreceiver(gift.id,giftUserid)

                        }
                        if(res!!.hasErrors())
                        {
//                                views.snackbar(""+ res.errors!![0].message)
                            Toast.makeText(requireContext(),""+ res.errors!![0].message, Toast.LENGTH_LONG).show()

                        }
                        Timber.d("apolloResponse ${res.hasErrors()} ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName}")
                    }
                    hideProgressView()
                }
            }

        })


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


        send_btn.setOnClickListener(View.OnClickListener {

            if (msg_write?.text.toString().equals("")) {
//                binding.root.snackbar(getString(R.string.you_cant_add_empty_msg))
                Toast.makeText(requireContext(),getString(R.string.you_cant_add_empty_msg),Toast.LENGTH_LONG).show()

                return@OnClickListener

            }


            if (msg_write?.text.toString().startsWith("@") && msg_write?.text.toString()
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

                            CommentOnStoryMutation(
                                msg_write?.text.toString().replace("@" + Replymodels!!.username!!, "").trim(),
                                Replymodels!!.cmtID!!.toInt(),"genericcomment"
                            )
                        )
                            .execute()
                    } catch (e: ApolloException) {
                        Timber.d("apolloResponse ${e.message}")
//                        Toast.makeText(requireContext(),"Exception ${e.message}",Toast.LENGTH_LONG).show()

//                        hideProgressView()
//                        return@launchWhenResumed
                    }

                    hideProgressView()
//                    fireCommntStoryNotification(objectID!!.toString(),giftUserid!!)
                    msg_write?.setText("")

//                    val usermoments = res.data?.genericComment

                    Timber.d("CMNT")
                    if (items.size > 0) {
                        RefreshStories()

                    } else {

                        getStories()
                    }


                }

            }else{

            showProgressView()
            lifecycleScope.launchWhenResumed {
                val res = try {
                    apolloClient(
                        requireContext(),
                        userToken!!
                    ).mutation(
                        CommentOnStoryMutation(
                            msg_write?.text.toString(),
                            objectID!!,"story"
                        )
                    )
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponse ${e.message}")
                    Toast.makeText(requireContext(),"Exception ${e.message}",Toast.LENGTH_LONG).show()

                    hideProgressView()
                    return@launchWhenResumed
                }

                hideProgressView()
//                fireCommntStoryNotification(objectID!!.toString(),giftUserid!!)

                msg_write?.setText("")

                val usermoments = res.data?.genericComment

                Timber.d("CMNT")
                if (items.size > 0) {
                    RefreshStories()

                } else {

                    getStories()
                }

            }

            }

        })





        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetUserDataQuery(Uid!!)).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                Toast.makeText(requireContext(),"Exception ${e.message}",Toast.LENGTH_LONG).show()

                hideProgressView()
                return@launchWhenResumed
            }

            hideProgressView()
            val UserData = res.data?.user

            try {
                if (UserData!!.avatar!! != null) {

                    try {
                        thumbnail.loadCircleImage(
                            UserData.avatar!!.url!!.replace(
                                "http://95.216.208.1:8000/media/",
                                "${BuildConfig.BASE_URL}media/"))
                        Timber.d("URL "+ UserData.avatar!!.url!!.replace(
                            "http://95.216.208.1:8000/media/",
                            "${BuildConfig.BASE_URL}media/"))
                    } catch (e: Exception) {
                        e.stackTrace
                    }


                }
            } catch (e: Exception) {
            }


        }

        delete_story.setOnClickListener {
            dismiss()
            listener?.deleteCallback(objectID ?: 0)
        }

        report_l.setOnClickListener {

//            if (exoPlayer.playWhenReady){
                pausetimerandplayer()

            reportDialog()

//            val popup = PopupMenu(requireContext(), report_l)
//            popup.getMenuInflater().inflate(R.menu.more_options1, popup.getMenu());
//
//            //adding click listener
//            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->
//
//                when (item!!.itemId) {
//
//                    R.id.nav_item_report -> {
//
//                        Log.e("clickReport", "clickReport")
////                        listener.onDotMenuofMomentClick(bindingAdapterPosition,item_data,"report")
//                        reportDialog()
//                    }
//
//                }
//
//                true
//            })
//            popup.setOnDismissListener {
//
//                resumetimerandplayer()
//            }
//            popup.show()
        }
    }


    private fun reportDialog() {

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
            lifecycleScope.launchWhenResumed {
                val res = try {
                    apolloClient(
                        requireContext(),
                        userToken!!
                    ).mutation(ReportStoryMutation(objectID.toString(), message))
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponse Exception ${e.message}")
                    Toast.makeText(requireContext(), " ${e.message}", Toast.LENGTH_LONG).show()

                    hideProgressView()
                    dialog.dismiss()
                    return@launchWhenResumed
                }

                if(res.hasErrors()){

                }else{
                    Log.e("rsponceSuccess", res.data!!.reportStory!!.storyReport!!.story.pk.toString())
                }
                resumetimerandplayer()
                hideProgressView()
                dialog.dismiss()
            }
        }

        cancleButton.setOnClickListener {
            resumetimerandplayer()
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun subscribeForUpdateStory(stories: List<GetAllUserStoriesQuery.Edge?>) {

        var storiesPkList = java.util.ArrayList<Int>()

        stories.indices.forEach {
            var pkss = stories[it]?.node!!.pk
            if (pkss != null) {
                storiesPkList.add(pkss)
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                apolloClientSubscription(requireActivity(), userToken!!).subscription(
                    OnUpdateStorySubscription(storiesPkList)
                ).toFlow().catch {
                    it.printStackTrace()
                    Timber.d("reealltime exception= ${it.message}")
                }
                    .retryWhen { cause, attempt ->
                        Timber.d("reealltime retry $attempt ${cause.message}")
//                        Log.d(
//                            "UserMomentsFragmentSubscription",
//                            "realtime retry  $attempt ${cause.message}"
//                        )
                        delay(attempt * 1000)
                        true
                    }.collect { newStory ->
                        if (newStory.hasErrors()) {
//                            Log.d(
//                                "UserMomentsFragmentSubscription",
//                                "realtime response error = ${newStory.errors?.get(0)?.message}"
//                            )
                            Timber.d("reealltime response error = ${newStory.errors?.get(0)?.message}")
                        } else {
                               Timber.d("reealltime NewStoryData ${newStory.data?.onUpdateStory}")
//                            Log.d(
//                                "UserMultiStoryDetailFragment",
//                                " story realtime NewStoryData content ${newStory.data?.onUpdateStory}"
//                            )
//                            Log.d(
//                                "UserMultiStoryDetailFragment",
//                                "story realtime NewStoryData ${newStory.data}"
//                            )
//                            Log.d(
//                                "UserMultiStoryDetailFragment",
//                                "story realtime Batchnumber ${newStory.data?.onUpdateStory?.storyId}"
//                            )
//                            val user =
//                                stories.filter { it?.user?.id == newStory.data?.onNewStory?.user?.id }
//                                    .filter { it?.batchNumber == newStory.data?.onNewStory?.batchNumber }

//                            Log.d(
//                                "UserMultiStoryDetailFragment",
//                                "story realtime NewStoryData ${newStory.data}"
//                            )

//                            val replaceStory =
//                                stories.filter { it!!.node?.pk.toString() == newStory.data?.onUpdateStory?.storyId.toString() }

                            if (items.size > 0) {
                                RefreshStories()

                            } else {

                                getStories()
                            }

//                            val position = stories.indexOf(replaceStory[0])
//                          var storiesUpdate =  stories.get(position)

//                            storiesUpdate?.node?.comments=newStory.data.onUpdateStory.comments
                        }
                    }
                Timber.d("reealltime 2")
            } catch (e2: Exception) {
                e2.printStackTrace()
//                Log.d("UserMomentsFragmentSubscription", "story realtime exception= ${e2.message}")
                Timber.d("reealltime exception= ${e2.message}")
            }
        }
    }


    private fun getStories() {

//        showProgressView()
   lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetAllUserStoriesQuery(100,"",
                    objectID.toString(), ""
                ))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception${e.message}")
                Toast.makeText(requireContext()," ${e.message}",Toast.LENGTH_LONG).show()

//                hideProgressView()
                return@launchWhenResumed
            }
            Timber.d("apolloResponse allUserStories stories ${res.hasErrors()}")

//            hideProgressView()
            val allUserStories = res.data?.allUserStories!!.edges

            if(allUserStories.size != 0)
            {
                subscribeForUpdateStory(allUserStories)
                val likeCount = allUserStories.get(0)!!.node!!.likesCount.toString()
                val commentCount = allUserStories.get(0)!!.node!!.commentsCount.toString()
                txtNearbyUserLikeCount!!.text = likeCount +" ${ requireActivity().resources.getString(R.string.like) }"
                txtMomentRecentComment!!.text = commentCount +" ${ requireActivity().resources.getString(R.string.comments) }"
                lblItemNearbyUserCommentCount!!.text = commentCount + " ${ requireActivity().resources.getString(R.string.comments) }"
                txtheaderlike!!.text = likeCount +" ${ requireActivity().resources.getString(R.string.like) }"
                giftUserid = allUserStories.get(0)!!.node!!.user!!.id.toString()

                val Likedata= allUserStories.get(0)!!.node!!.likes!!.edges

                if (rvLikes!!.itemDecorationCount == 0) {
                    rvLikes!!.addItemDecoration(object : RecyclerView.ItemDecoration() {
                        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                            outRect.top = 25
                        }
                    })
                }
                if (Likedata.size > 0) {
                    Timber.d("apolloResponse: ${Likedata[0]!!.node!!.id}")
                    no_data!!.visibility = View.GONE
                    rvLikes!!.visibility = View.VISIBLE

                    val items1: java.util.ArrayList<CommentsModel> = java.util.ArrayList()

                    Likedata.indices.forEach { i ->

                        val models = CommentsModel()

                        models.commenttext = Likedata[i]!!.node!!.user.fullName

//                        models.userurl = Likedata[i]!!.node!!.user.avatar!!.url
                        if(Likedata[i]!!.node!!.user.avatar!=null&& !Likedata[i]!!.node!!.user.avatar!!.url.isNullOrEmpty()) {
                            models.userurl = Likedata[i]!!.node!!.user.avatar!!.url
                        }else{
                            models.userurl = ""
                        }

                        items1.add(models)

                    }

                    adapters = StoryLikesAdapter(requireActivity(), items1,glide)
                    adapters?.userProfileClicked {
//                        Log.d("UserStoryDetailsFragment", "$it")
                        var bundle = Bundle()
                        bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
                        bundle.putString("userId", it.uid)
                        if (Uid == it.uid) {
                            MainActivity.getMainActivity()?.binding?.bottomNavigation?.selectedItemId =
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


                    rvLikes!!.adapter = adapters
                    rvLikes!!.layoutManager = LinearLayoutManager(activity)
                } else {
                    no_data!!.visibility = View.VISIBLE
                    rvLikes!!.visibility = View.GONE
                }

                val Commentdata= allUserStories.get(0)!!.node!!.comments!!.edges

                if (rvSharedMoments!!.itemDecorationCount == 0) {
                    rvSharedMoments!!.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
                if (Commentdata!!.size > 0) {
                    Timber.d("apolloResponse: ${Commentdata?.get(0)?.node!!.commentDescription}")
                    nodata!!.visibility = View.GONE
                    rvSharedMoments!!.visibility = View.VISIBLE


                    items = ArrayList()






                    Commentdata.indices.forEach { i ->
                        val hm: MutableList<ReplysModel> = ArrayList()


                        val models = CommentsModel()

                        models.commenttext = Commentdata[i]!!.node!!.commentDescription

//                        if (Commentdata[i]!!.node!!.user.avatarPhotos!!.size > 0) {
                        if(Commentdata[i]!!.node!!.user.avatar!=null && Commentdata[i]!!.node!!.user.avatar!!.url!!.isNotEmpty()){

                            models.userurl = Commentdata[i]!!.node!!.user.avatar!!.url
                        } else {
                            models.userurl = ""
                        }
                        models.username = Commentdata[i]!!.node!!.user.fullName
                        models.timeago = Commentdata[i]!!.node!!.createdDate.toString()
                        models.cmtID = Commentdata[i]!!.node!!.pk.toString()
                        models.momentID = objectID?.toString()
                        models.uid = Commentdata[i]!!.node!!.user.id.toString()
                        models.cmtlikes = Commentdata[i]!!.node!!.likesCount.toString()


                        for (f in 0 until Commentdata[i]!!.node!!.replys!!.edges.size) {


                            val md = ReplysModel()

                            md.replytext = Commentdata[i]!!.node!!.replys!!.edges[f]!!.node!!.commentDescription
                            md.userurl =
                                Commentdata[i]!!.node!!.replys!!.edges[f]!!.node!!.user.avatarPhotos?.get(0)?.url
                            md.usernames = Commentdata[i]!!.node!!.replys!!.edges[f]!!.node!!.user.fullName
                            md.timeago = Commentdata[i]!!.node!!.replys!!.edges[f]!!.node!!.createdDate.toString()
                            md.uid = Commentdata[i]!!.node!!.replys!!.edges[f]!!.node!!.user.id.toString()


                            hm.add(f, md)


                        }
                        models.replylist = hm
                        models.isExpanded = true



                        items.add(models)
                    }




                    adapter =
                        VideoStoryCommentListAdapter(
                            requireActivity(),
                            this@PlayUserStoryDialogFragment,
                            items,
                            this@PlayUserStoryDialogFragment
                        )
                    rvSharedMoments!!.adapter = adapter
                    rvSharedMoments!!.layoutManager = LinearLayoutManager(activity)
                } else {
                    nodata!!.visibility = View.VISIBLE
                    rvSharedMoments!!.visibility = View.GONE

                }

            }


        }
    }

    private fun RefreshStories() {

//        showProgressView()
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetAllUserStoriesQuery(100,"",
                    objectID.toString(), ""
                ))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                Toast.makeText(requireContext()," ${e.message}",Toast.LENGTH_LONG).show()

//                hideProgressView()
                return@launchWhenResumed
            }
            Timber.d("apolloResponse allUserStories stories ${res.hasErrors()}")

//            hideProgressView()
            val allUserStories = res.data?.allUserStories!!.edges
            if(allUserStories != null && allUserStories.size != 0)
            {

                val likeCount = allUserStories.get(0)!!.node!!.likesCount.toString()
                val commentCount = allUserStories.get(0)!!.node!!.commentsCount.toString()
                txtNearbyUserLikeCount!!.text = likeCount +" ${ requireActivity().resources.getString(R.string.like) }"
                txtMomentRecentComment!!.text = commentCount +" ${ requireActivity().resources.getString(R.string.comments) }"
                lblItemNearbyUserCommentCount!!.text = commentCount + " ${ requireActivity().resources.getString(R.string.comments) }"
                txtheaderlike!!.text = likeCount +" ${ requireActivity().resources.getString(R.string.like) }"

                /*txtNearbyUserLikeCount!!.text = ""+ allUserStories.get(0)!!.node!!.likesCount
                txtMomentRecentComment!!.text = ""+ allUserStories.get(0)!!.node!!.commentsCount+" ${ requireActivity().resources.getString(R.string.comments) }"
                lblItemNearbyUserCommentCount!!.text =
                    allUserStories.get(0)!!.node!!.commentsCount.toString() + " ${ requireActivity().resources.getString(R.string.comments) }"
                txtheaderlike!!.text = allUserStories.get(0)!!.node!!.likesCount.toString()+" ${ requireActivity().resources.getString(R.string.like) }"*/
                giftUserid = allUserStories.get(0)!!.node!!.user!!.id.toString()



                val Likedata= allUserStories.get(0)!!.node!!.likes!!.edges
                if (Likedata.size > 0) {
                    Timber.d("apolloResponse: ${Likedata[0]!!.node!!.id}")
                    no_data!!.visibility = View.GONE
                    rvLikes!!.visibility = View.VISIBLE

                    val items1: java.util.ArrayList<CommentsModel> = java.util.ArrayList()

                    Likedata.indices.forEach { i ->

                        val models = CommentsModel()

                        models.commenttext = Likedata[i]!!.node!!.user.fullName

//                        models.userurl = Likedata[i]!!.node!!.user.avatar!!.url
                        if(Likedata[i]!!.node!!.user.avatar!=null&& !Likedata[i]!!.node!!.user.avatar!!.url.isNullOrEmpty()) {
                            models.userurl = Likedata[i]!!.node!!.user.avatar!!.url
                        }else{
                            models.userurl = ""
                        }

                        items1.add(models)

                    }

                    if(adapters==null) {
                        adapters = StoryLikesAdapter(requireActivity(), items1, glide)
                        adapters?.userProfileClicked {
//                            Log.d("UserStoryDetailsFragment", "$it")
                            var bundle = Bundle()
                            bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
                            bundle.putString("userId", it.uid)
                            if (Uid == it.uid) {
                                MainActivity.getMainActivity()?.binding?.bottomNavigation?.selectedItemId =
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


                        rvLikes!!.adapter = adapters
                        rvLikes!!.layoutManager = LinearLayoutManager(activity)
                    }else{
                        adapters!!.addAll(items1)
                        adapters!!.notifyDataSetChanged()
                    }
                } else {
                    no_data!!.visibility = View.VISIBLE
                    rvLikes!!.visibility = View.GONE
                }

                val Commentdata= allUserStories.get(0)!!.node!!.comments!!.edges
                if (rvSharedMoments!!.itemDecorationCount == 0) {
                    rvSharedMoments!!.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
                if (Commentdata!!.size > 0) {
                    Timber.d("apolloResponse: ${Commentdata?.get(0)?.node!!.commentDescription}")
                    nodata!!.visibility = View.GONE
                    rvSharedMoments!!.visibility = View.VISIBLE


                    val items1: ArrayList<CommentsModel> = ArrayList()

                    Commentdata.indices.forEach { i ->
                        val hm: MutableList<ReplysModel> = ArrayList()


                        val models = CommentsModel()

                        models.commenttext = Commentdata[i]!!.node!!.commentDescription

//                        if (Commentdata[i]!!.node!!.user.avatarPhotos!!.size > 0) {
                        if(Commentdata[i]!!.node!!.user.avatar!=null && Commentdata[i]!!.node!!.user.avatar!!.url!!.isNotEmpty()){

                            models.userurl = Commentdata[i]!!.node!!.user.avatar!!.url
                        } else {
                            models.userurl = ""
                        }
                        models.username = Commentdata[i]!!.node!!.user.fullName
                        models.timeago = Commentdata[i]!!.node!!.createdDate.toString()
                        models.cmtID = Commentdata[i]!!.node!!.pk.toString()
                        models.momentID = objectID?.toString()
                        models.uid = Commentdata[i]!!.node!!.user.id.toString()
                        models.cmtlikes = Commentdata[i]!!.node!!.likesCount.toString()


                        for (f in 0 until Commentdata[i]!!.node!!.replys!!.edges.size) {


                            val md = ReplysModel()

                            md.replytext = Commentdata[i]!!.node!!.replys!!.edges[f]!!.node!!.commentDescription
                            md.userurl =
                                Commentdata[i]!!.node!!.replys!!.edges[f]!!.node!!.user.avatarPhotos?.get(0)?.url
                            md.usernames = Commentdata[i]!!.node!!.replys!!.edges[f]!!.node!!.user.fullName
                            md.timeago = Commentdata[i]!!.node!!.replys!!.edges[f]!!.node!!.createdDate.toString()
                            md.uid = Commentdata[i]!!.node!!.replys!!.edges[f]!!.node!!.user.id.toString()


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
                    nodata!!.visibility = View.VISIBLE
                    rvSharedMoments!!.visibility = View.GONE

                }

            }


        }
    }
    fun fireLikeStoryNotification(pkids: String, userid: String?) {

        lifecycleScope.launchWhenResumed {


            val queryName = "sendNotification"
            val query = StringBuilder()
                .append("mutation {")
                .append("$queryName (")
                .append("userId: \"${userid}\", ")
                .append("notificationSetting: \"STLIKE\", ")
                .append("data: {pk:${pkids}}")
                .append(") {")
                .append("sent")
                .append("}")
                .append("}")
                .toString()

            val result= AppModule.provideGraphqlApi().getResponse<Boolean>(
                query,
                queryName, userToken)
            Timber.d("RSLT",""+result.message)

        }








    }
    fun fireCommntStoryNotification(pkids: String, userid: String?) {

        lifecycleScope.launchWhenResumed {


            val queryName = "sendNotification"
            val query = StringBuilder()
                .append("mutation {")
                .append("$queryName (")
                .append("userId: \"${userid}\", ")
                .append("notificationSetting: \"STCMNT\", ")
                .append("data: {pk:${pkids}}")
                .append(") {")
                .append("sent")
                .append("}")
                .append("}")
                .toString()

            val result= AppModule.provideGraphqlApi().getResponse<Boolean>(
                query,
                queryName, userToken)
            Timber.d("RSLT",""+result.message)

        }








    }

    fun fireGiftBuyNotificationforreceiver(gid: String, userid: String?) {

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

            val result= AppModule.provideGraphqlApi().getResponse<Boolean>(
                query,
                queryName, userToken)
            Timber.d("RSLT",""+result.message)

        }








    }
    protected fun showProgressView() {
        loadingDialog.show()
    }

    protected fun hideProgressView() {
        loadingDialog.dismiss()
    }

    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = UserItemsAdapter(childFragmentManager)
        fragRealGifts = FragmentRealGifts()
        fragVirtualGifts = FragmentVirtualGifts()

        adapter.addFragItem(fragRealGifts!!, getString(R.string.real_gifts))
        adapter.addFragItem(fragVirtualGifts!!, getString(R.string.virtual_gifts))
        viewPager.adapter = adapter
    }

    private fun initPlayer(){

        val mediaItem = MediaItem.Builder()
            .setUri("")
            .setMimeType(MimeTypes.VIDEO_MP4)
            .build()
        exoPlayer = SimpleExoPlayer.Builder(requireActivity()).build().apply {
            playWhenReady = isPlayerPlaying
            seekTo(currentWindow, playbackPosition)
            setMediaItem(mediaItem, false)
            prepare()
        }
        player_view!!.player = exoPlayer
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_RESUME_WINDOW, exoPlayer.currentWindowIndex)
        outState.putLong(STATE_RESUME_POSITION, exoPlayer.currentPosition)
        outState.putBoolean(STATE_PLAYER_FULLSCREEN, isFullscreen)
        outState.putBoolean(STATE_PLAYER_PLAYING, isPlayerPlaying)
        super.onSaveInstanceState(outState)
    }
    private fun releasePlayer(){
        isPlayerPlaying = exoPlayer.playWhenReady
        playbackPosition = exoPlayer.currentPosition
        currentWindow = exoPlayer.currentWindowIndex
        exoPlayer.release()
    }

    private fun playView(mediaItem: MediaItem) {

        showProgressView()

        exoPlayer = SimpleExoPlayer.Builder(requireActivity()).build().apply {
            playWhenReady = isPlayerPlaying
            seekTo(currentWindow, playbackPosition)
            setMediaItem(mediaItem, false)
            prepare()
        }
        player_view!!.player = exoPlayer

        var durationSet = false

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_READY && !durationSet) {
                    val realDurationMillis: Long = exoPlayer.getDuration()
                    durationSet = true
                    val duration = realDurationMillis
                    Timber.d("filee ${duration}")
                    progressBar1!!.max = realDurationMillis.toInt()
                    val millisInFuture = duration.toLong()

                    timer1 = object : CountDownTimerExt(millisInFuture, 100) {
                        override fun onTimerTick(millisUntilFinished: Long) {
                            Log.d("MainActivity", "onTimerTick $millisUntilFinished")
                            onTickProgressUpdate()

                        }

                        override fun onTimerFinish() {
                            Log.d("MainActivity", "onTimerFinish")
                            dismiss()

                        }

                    }
                    hideProgressView()
                    timer1.run { this!!.start() }

                }
            }

            fun onPlayWhenReadyCommitted() {
                // No op.
            }

             fun onPlayerError(error: ExoPlaybackException) {
                // No op.
            }
        })


    }

    private fun onTickProgressUpdate() {

        countUp += 100
        val progress = countUp
        Timber.d("prggress $progress")
        progressBar1!!.smoothProgress(progress)
    }

    override fun onreply(position: Int, models: CommentsModel) {


        Replymodels = models

        msg_write?.setText("")
        msg_write?.setText("")
        msg_write?.requestFocus()
        msg_write?.postDelayed(Runnable {

            val inputMethodManager =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?

            inputMethodManager!!.showSoftInput(
                msg_write,
                InputMethodManager.SHOW_IMPLICIT
            )
            msg_write?.append("@" + models.username + " ")

        }, 150)

    }

    override fun oncommentLike(position: Int, models: CommentsModel) {
//        Log.e("OnCommentLikeInMultiStoryVideo", "${models.commenttext}")

        showProgressView()
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(
                    requireContext(),
                    userToken!!
                ).mutation(LikeOnStoryMutation(models.cmtID!!.toInt(), "genericcomment"))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
//                    binding.root.snackbar("Exception ${e.message}")
                Toast.makeText(requireContext(), " ${e.message}", Toast.LENGTH_LONG).show()

                hideProgressView()
                return@launchWhenResumed
            }

            Timber.d("LIKE")
            RefreshStories()
            hideProgressView()
        }
    }

    override fun onUsernameClick(position: Int, models: CommentsModel) {

        var bundle = Bundle()
        bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
        bundle.putString("userId", models.uid)
        if (Uid == models.uid) {
            MainActivity.getMainActivity()?.binding?.bottomNavigation?.selectedItemId =
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
        if (Uid == models.uid) {
            MainActivity.getMainActivity()?.binding?.bottomNavigation?.selectedItemId =
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
