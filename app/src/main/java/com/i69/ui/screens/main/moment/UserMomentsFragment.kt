package com.i69.ui.screens.main.moment

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window.FEATURE_NO_TITLE
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.DefaultUpload
import com.apollographql.apollo3.api.content
import com.apollographql.apollo3.exception.ApolloException
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.i69.BuildConfig
import com.i69.DeleteStoryMutation
import com.i69.DeletemomentMutation
import com.i69.GetAllUserMomentsQuery
import com.i69.GetAllUserMultiStoriesQuery
import com.i69.GetNotificationCountQuery
import com.i69.GiftPurchaseMutation
import com.i69.LikeOnMomentMutation
import com.i69.MomentMutation
import com.i69.OnDeleteMomentSubscription
import com.i69.OnDeleteStorySubscription
import com.i69.OnNewMomentSubscription
import com.i69.OnNewStorySubscription
import com.i69.OnUpdateMomentSubscription
import com.i69.R
import com.i69.ReportonmomentMutation
import com.i69.ScheduleMomentMutation
import com.i69.ScheduleStoryMutation
import com.i69.StoryMutation
import com.i69.applocalization.AppStringConstant
import com.i69.data.models.ModelGifts
import com.i69.data.models.User
import com.i69.data.remote.responses.MomentLikes
import com.i69.databinding.BottomsheetShareOptionsBinding
import com.i69.databinding.DialogBuySubscriptionOrCoinsBinding
import com.i69.databinding.DialogPreviewImageBinding
import com.i69.databinding.FragmentUserMomentsBinding
import com.i69.di.modules.AppModule.provideGraphqlApi
import com.i69.gifts.FragmentRealGifts
import com.i69.gifts.FragmentReceivedGifts
import com.i69.gifts.FragmentVirtualGifts
import com.i69.ui.adapters.NearbySharedMomentAdapter
import com.i69.ui.adapters.StoryLikesAdapter
import com.i69.ui.adapters.UserItemsAdapter
import com.i69.ui.adapters.UserMultiStoriesAdapter
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.ImagePickerActivity
import com.i69.ui.screens.SplashActivity
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.screens.main.notification.NotificationDialogFragment
import com.i69.ui.screens.main.search.userProfile.SearchUserProfileFragment
import com.i69.ui.viewModels.CommentsModel
import com.i69.ui.viewModels.UserMomentsModelView
import com.i69.ui.viewModels.UserViewModel
import com.i69.ui.views.InsLoadingView
import com.i69.utils.AnimationTypes
import com.i69.utils.LogUtil
import com.i69.utils.apolloClient
import com.i69.utils.apolloClientSubscription
import com.i69.utils.getResponse
import com.i69.utils.getVideoFilePath
import com.i69.utils.loadImage
import com.i69.utils.navigate
import com.i69.utils.setViewGone
import com.i69.utils.setViewVisible
import com.i69.utils.showOkAlertDialog
import com.i69.utils.snackbar
import com.i69.utils.snackbarOnTop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

class UserMomentsFragment : BaseFragment<FragmentUserMomentsBinding>(),
    NearbySharedMomentAdapter.NearbySharedMomentListener,
    UserMultiStoriesAdapter.UserMultiStoryListener {

    companion object {
        private const val TAG = "UserMomentsFragment"
    }

    private var tempStories = mutableListOf<ApolloResponse<OnNewStorySubscription.Data>>()
    private var stories: ArrayList<GetAllUserMultiStoriesQuery.AllUserMultiStory?> = ArrayList()
    private val mViewModel: UserMomentsModelView by activityViewModels()
    private val viewModel: UserViewModel by activityViewModels()
    private var ShowNotification: String? = ""
    var width = 0
    var size = 0
    private var userToken: String? = null

    //  private lateinit var usersAdapter: UserStoriesAdapter
    private lateinit var usersMultiStoryAdapter: UserMultiStoriesAdapter
    private lateinit var sharedMomentAdapter: NearbySharedMomentAdapter
    private var mFilePath: String? = null
    lateinit var file: File
    var layoutManager: LinearLayoutManager? = null
    var allUserMoments: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()
    var allUserMomentsNew: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()
    var allUserMoments2: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()
    var allUserMoments1: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()
    private var userId: String? = null
    private var userName: String? = null
    var endCursor: String = ""
    var hasNextPage: Boolean = false
    var hasLoaded: Boolean = false
    private lateinit var GiftbottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var LikebottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    var momentLikesUsers: ArrayList<CommentsModel> = ArrayList()
    var likeMomentItemPK: String? = null

    var momentLikeUserAdapters:
            StoryLikesAdapter? = null

    var giftUserid: String? = null

    var fragVirtualGifts: FragmentVirtualGifts? = null
    var fragRealGifts: FragmentRealGifts? = null
    private var mUser: User? = null

    private lateinit var receivedGiftbottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private lateinit var exoPlayer: ExoPlayer

    override fun playVideo(mediaItem: MediaItem, playWhenReady: Boolean): ExoPlayer {
        exoPlayer.apply {
            setMediaItem(mediaItem, false)
            this.playWhenReady = playWhenReady
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
        }
        return exoPlayer
    }

    override fun isPlaying(): Boolean {
        return exoPlayer.isPlaying
    }

    override fun pauseVideo() {
        if (isPlaying())
            exoPlayer.pause()
    }

    override fun onPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause()
        if (this::sharedMomentAdapter.isInitialized) {
            sharedMomentAdapter.pauseAll()
        }
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        exoPlayer = ExoPlayer.Builder(requireContext()).build()
    }

    override fun onStop() {
        super.onStop()
        exoPlayer.stop()
        exoPlayer.release()
    }

    private val photosLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            val data = activityResult.data
            if (activityResult.resultCode == Activity.RESULT_OK) {
                mFilePath = data?.getStringExtra("result")
                file = File(mFilePath)
                Timber.d("fileBase64 $mFilePath")
                Log.d("UserMomentFragment", "Photo Choosed")
                showFilePreview(file, ".jpg")
            }
        }
    val galleryImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            val data = activityResult.data
            if (activityResult.resultCode == Activity.RESULT_OK) {
                mFilePath = data.toString()
                val result = data?.data?.path
                val openInputStream =
                    requireActivity().contentResolver?.openInputStream(data?.data!!)
                val type = if (result?.contains("video") == true) ".mp4" else ".jpg"
                val outputFile =
                    requireContext().filesDir.resolve("${System.currentTimeMillis()}$type")
                openInputStream?.copyTo(outputFile.outputStream())
                file = File(outputFile.toURI())
                Timber.d("fileBase64 $mFilePath")
                showFilePreview(file, type)
            }
        }

    private fun showShareOptions(onShared: () -> Unit) {
        val shareOptionsDialog = BottomSheetDialog(requireContext())
        val bottomsheet = BottomsheetShareOptionsBinding.inflate(layoutInflater, null, false)

        var shareAt = ""

        bottomsheet.llShareLaterRoot.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.profileTransBlackOverlayColor, null))
        bottomsheet.rbShareLater.setViewGone()
        bottomsheet.ivLocked.setViewVisible()

        if ((requireActivity() as MainActivity).isUserAllowedToPostStory()) {
            bottomsheet.llShareNowRoot.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.CE4F3FF, null))
            bottomsheet.rbShareNow.setViewVisible()
            bottomsheet.ivPostLocked.setViewGone()
            if ((requireActivity() as MainActivity).isUserHasStoryQuota()) {
                bottomsheet.tvShareNowCoins.setViewGone()
            }
            else {
                (requireActivity() as MainActivity).getRequiredCoins("POST_STORY_COINS") {
                    bottomsheet.tvShareNowCoins.setViewVisible()
                    bottomsheet.tvShareNowCoins.text = it.toString()
                }
            }
        }
        else {
            bottomsheet.llShareNowRoot.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.profileTransBlackOverlayColor, null))
            bottomsheet.rbShareNow.setViewGone()
            bottomsheet.ivPostLocked.setViewVisible()

            bottomsheet.llShareLaterRoot.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.profileTransBlackOverlayColor, null))
            bottomsheet.rbShareLater.setViewGone()
            bottomsheet.ivLocked.setViewVisible()

            bottomsheet.cvShareNow.setOnClickListener {
                if (shareOptionsDialog.isShowing)
                    shareOptionsDialog.dismiss()
                onShared.invoke()
                showUpgradePlanDialog()
            }

            bottomsheet.cvShareLater.setOnClickListener {
                if (shareOptionsDialog.isShowing)
                    shareOptionsDialog.dismiss()
                onShared.invoke()
                showUpgradePlanDialog()
            }

            shareOptionsDialog.setContentView(bottomsheet.root)
            shareOptionsDialog.show()

            return
        }

        if ((requireActivity() as MainActivity).isUserAllowedToScheduleStory()) {
            if ((requireActivity() as MainActivity).isUserHasSubscription()){
                bottomsheet.llShareLaterRoot.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.CE4F3FF, null))
                bottomsheet.rbShareLater.setViewVisible()
                bottomsheet.ivLocked.setViewGone()
                bottomsheet.tvShareLaterCoins.setViewGone()
            }
            else{
                (requireActivity() as MainActivity).getRequiredCoins("SCHEDULE_STORY_COINS") {
                    bottomsheet.llShareLaterRoot.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.CE4F3FF, null))
                    bottomsheet.rbShareLater.setViewVisible()
                    bottomsheet.ivLocked.setViewGone()
                    bottomsheet.tvShareLaterCoins.setViewVisible()
                    bottomsheet.tvShareLaterCoins.text = it.toString()
                }
            }
        }
        bottomsheet.cvShareNow.setOnClickListener {
            bottomsheet.rbShareNow.isChecked = true
            bottomsheet.rbShareLater.isChecked = false
        }

        bottomsheet.cvShareLater.setOnClickListener {
            if ((requireActivity() as MainActivity).isUserAllowedToScheduleStory()) {
                bottomsheet.rbShareLater.isChecked = true
                bottomsheet.rbShareNow.isChecked = false
                showDateTimePicker { displayTime, apiTime ->
                    if (displayTime.isNotEmpty() && apiTime.isNotEmpty()) {
                        bottomsheet.tvShareLater.text = "Scheduled for $displayTime"
                        shareAt = apiTime
                    }
                    else {
                        bottomsheet.rbShareLater.isChecked = false
                        bottomsheet.rbShareNow.isChecked = true
                    }
                }
            }
            else {
                if (shareOptionsDialog.isShowing)
                    shareOptionsDialog.dismiss()
                onShared.invoke()
                showUpgradePlanDialog()
            }
        }

        bottomsheet.rbShareNow.setOnClickListener { bottomsheet.cvShareNow.performClick() }
        bottomsheet.rbShareLater.setOnClickListener { bottomsheet.cvShareLater.performClick() }

        bottomsheet.btnShareMoment.setOnClickListener {
            if (shareOptionsDialog.isShowing) shareOptionsDialog.dismiss()
            onShared.invoke()
            if (bottomsheet.rbShareNow.isChecked) {
                uploadStory()
            }
            else if (bottomsheet.rbShareLater.isChecked) {
                if (shareAt.isNotEmpty()) {
                    uploadStoryLater(shareAt)
                }
            }
        }

        shareOptionsDialog.setContentView(bottomsheet.root)
        shareOptionsDialog.show()
    }

    private fun showUpgradePlanDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(FEATURE_NO_TITLE)
        val dialogBinding = DialogBuySubscriptionOrCoinsBinding.inflate(layoutInflater)

        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window?.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT

        dialogBinding.ivCross.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.clBuyCoins.setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.action_global_purchase)
        }

        dialogBinding.clBuySubscription.setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.action_global_plan)
        }

        dialog.setContentView(dialogBinding.root)
        dialog.show()
        dialog.window?.attributes = lp
    }

    private fun showDateTimePicker(onDateAndTimePicked: (String, String) -> Unit) {
        val currentDate: Calendar = Calendar.getInstance()
        val date = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { view, year, monthOfYear, dayOfMonth ->
                date.set(year, monthOfYear, dayOfMonth)
                TimePickerDialog(context,
                    { view, hourOfDay, minute ->
                        if (getMainActivity().isValidTime(year, monthOfYear, dayOfMonth, hourOfDay, minute)) {
                            date.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            date.set(Calendar.MINUTE, minute)

                            val sdf1 = SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault())
                            val sdf2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                            sdf2.timeZone = TimeZone.getTimeZone("UTC")
                            val now = date.time
                            val displayTime = sdf1.format(now)
                            val formattedTime = sdf2.format(now)
                            onDateAndTimePicked.invoke(displayTime, formattedTime)

                            Log.v("UMF", "The choosen one $formattedTime")
                        }
                        else {
                            onDateAndTimePicked.invoke("", "")
                            binding.root.snackbarOnTop(getString(R.string.please_select_a_future_time))
                        }
                    },
                    currentDate.get(Calendar.HOUR_OF_DAY),
                    currentDate.get(Calendar.MINUTE),
                    false
                ).show()
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DATE)
        ).show()
    }

    private val videoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            val contentURI = activityResult.data?.data
            mFilePath = contentURI?.getVideoFilePath(requireContext())
            val f = File(mFilePath!!)
            Timber.d("filee ${f.exists()} ${f.length()} ${f.absolutePath}")
            if (f.exists()) {
                val sizeInMb = (f.length() / 1000) / 1000
                if (sizeInMb < 2) {
                } else {
                    mFilePath = null
                    val ok = resources.getString(R.string.pix_ok)
                    requireContext().showOkAlertDialog(
                        ok,
                        "${requireActivity().resources.getString(R.string.file_size)} ${sizeInMb}MB",
                        getString(R.string.your_video_file_should_be_less_than_11mb)
                    ) { dialog, which -> }
                }
            } else {

                binding.root.snackbar("${requireActivity().resources.getString(R.string.wrong_path)} $mFilePath")
            }
        }

    private fun showFilePreview(file: File?, fileType: String) {
        (requireActivity() as MainActivity).loadUser()
        val dialogBinding = DialogPreviewImageBinding.inflate(layoutInflater, null, false)

        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(R.color.color_transparant_80)

        if (fileType == ".jpg") {
            dialogBinding.ivPreview.setViewVisible()
            dialogBinding.vvPreview.setViewGone()
            Glide.with(requireContext())
                .load(file)
                .into(dialogBinding.ivPreview)
        }
        else {
            dialogBinding.ivPreview.setViewGone()
            dialogBinding.vvPreview.setViewVisible()
            dialogBinding.vvPreview.setVideoPath(file?.path)
            dialogBinding.vvPreview.start()
            dialogBinding.vvPreview.setOnCompletionListener {
                if (dialog.isShowing && it != null) dialogBinding.vvPreview.start()
            }
        }

        dialogBinding.ibClose.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnShareMoment.setOnClickListener {
            showShareOptions {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentUserMomentsBinding.inflate(inflater, container, false)

    var scrollY1 = 0
    var height = 0
    override fun setupTheme() {
        Log.e("setupTheme", "setupTheme")
//        if(stories.isNotEmpty()){
////        if (mViewModel.userMomentsList.size != 0) {
//            sharedMomentAdapter.notifyDataSetChanged()
//            usersMultiStoryAdapter.notifyDataSetChanged()
//
//
//        }
//        val extras = arguments
        if (getMainActivity().isShare) {
            getMainActivity().isShare = false
            binding.llSharing.visibility = View.VISIBLE
            momentSharing(
                getMainActivity().filePath,
                getMainActivity().description,
                getMainActivity().checked
            )
        } else if (getMainActivity().isShareLater) {
            getMainActivity().isShareLater = false
//            binding.llSharing.visibility = View.VISIBLE
            scheduleMomentForLater(
                getMainActivity().filePath,
                getMainActivity().description,
                getMainActivity().checked,
                getMainActivity().publishAt
            )
        }

        val displayMetrics = DisplayMetrics()
//            requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        height = displayMetrics.heightPixels
//        val height = displayMetrics.heightPixels

        width = displayMetrics.widthPixels
        val densityMultiplier = resources.displayMetrics.density
        val scaledPx = 14 * densityMultiplier
        val paint = Paint()
        paint.textSize = scaledPx
        size = paint.measureText("s").roundToInt()
        if (!hasInitializedRootView) {
         //  hasInitializedRootView = true

//        else{
           /* if (TempConstants.LanguageChanged) {
                (activity as MainActivity).openSettingsScreen()
            }*/

            navController = findNavController()
            binding.model = mViewModel
            lifecycleScope.launch {
                userId = getCurrentUserId()!!
                userToken = getCurrentUserToken()!!
                Timber.i("usertokenn $userToken")
//        }
                Timber.i("userID $userId")
//        Log.d("UserMomentFragment", "userID $userId")
//        val displayMetrics = DisplayMetrics()
//        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)

                //allUserMoments = ArrayList()
                layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
                binding.rvSharedMoments.layoutManager = layoutManager
                //   getAllUserStories()
                Log.e("TAG", "try")
                getAllUserMultiStories()
                subscribeForNewStory()
                subscribeForDeleteStory()
                Log.e("obj_node", "try")
                subscribeForNewMoment()
                subscribeForDeleteMoment()
                subscribeForUpdateMoment()

                if (mViewModel.userMomentsList.size == 0) {
                    endCursor = ""
                    sharedMomentAdapter = NearbySharedMomentAdapter(
                        requireActivity(),
                        this@UserMomentsFragment,
                        allUserMoments1,
                        userId
                    )
                }
                if (::sharedMomentAdapter.isInitialized) {
                    binding.rvSharedMoments.adapter = sharedMomentAdapter
                }
                //binding.rvSharedMoments.setHasFixedSize(true)
                binding.rvSharedMoments.isNestedScrollingEnabled = false
                //getAllUserMoments(width,size)
                if (mViewModel.userMomentsList.size == 0) {
                    getMainActivity().pref.edit().putString("checkUserMomentUpdate", "false")
                        .apply()
                    Log.d("UserMomentsSub", "usermomentnextpage")
                    getUserMomentNextPage(width, size, 10, endCursor)
                }
                if (getMainActivity().pref.getString("checkUserMomentUpdate", "false")
                        .equals("true")
                ) {
                    getMainActivity().pref.edit().putString("checkUserMomentUpdate", "false")
                        .apply()
                    getMainActivity().pref.getString("mID", "")
                        ?.let {
                            Log.d("UserMomentsSub", "getParticularMoments")
                            getParticularMoments(
                                getMainActivity().pref.getInt("itemPosition", 0),
                                it
                            )
                        }
                }
            }
            binding.bubble.setOnClickListener {
                try {
                    val layabouts: LinearLayoutManager =
                        binding.rvSharedMoments.layoutManager as LinearLayoutManager
                    layabouts.scrollToPositionWithOffset(0, 0)
                    binding.scrollView.fullScroll(View.FOCUS_UP)
                    binding.bubble.isVisible = false
                } catch (e: Exception) {
                    Log.e("TAG_Click", "setupTheme: " + e.message.toString())
                }
            }
            binding.btnNewPostCheck.setOnClickListener {
                try {
                    val layabouts: LinearLayoutManager =
                        binding.rvSharedMoments.layoutManager as LinearLayoutManager
                    layabouts.scrollToPositionWithOffset(0, 0)
                    binding.scrollView.fullScroll(View.FOCUS_UP)
                    binding.bubble.isVisible = false
                } catch (e: Exception) {
                    Log.e("TAG_Click", "setupTheme: " + e.message.toString())
                }
            }
            if (binding.rvSharedMoments.itemDecorationCount == 0) {
                binding.rvSharedMoments.addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.top = 10
                    }
                })
            }

            binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                if (hasNextPage) {
                    binding.rvSharedMoments.let {
                        val diff = it.bottom - (binding.scrollView.height + binding.scrollView.scrollY);
                        Log.e("difference",""+diff)
                        if (!alreadyFetching && (diff) < 7000) {
                            alreadyFetching = true
                            getUserMomentNextPage(width, size, 20, endCursor)
                        }
                        //allusermoments1(width,size,10,endCursor)
                        scrollY1 = scrollY
                        binding.btnNewPostCheck.isVisible = scrollY > height
                        if (binding.bubble.isVisible) {
                            if (scrollY == 0) {
                                binding.bubble.isVisible = false
                            }
                        }
//                   binding.bubble.isVisible=scrollY==height
                    }
                }
            })
//        }
        }
    }

    override fun setupClickListeners() {
        binding.toolbarHamburger.setOnClickListener {
            getMainActivity().drawerSwitchState()
        }
        binding.bell.setOnClickListener {
            val dialog = NotificationDialogFragment(
                userToken,
                binding.counter,
                userId,
                binding.bell
            )
//            dialog.show(childFragmentManager, getString(R.string.notifications))
            getMainActivity()?.notificationDialog(
                dialog,
                childFragmentManager,
                getString(R.string.notifications)
            )

        }

        showLikeBottomSheet()

        GiftbottomSheetBehavior = BottomSheetBehavior.from(binding.giftbottomSheet)
        GiftbottomSheetBehavior.setBottomSheetCallback(object :
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
        binding.sendgiftto.setOnClickListener {
//            val items: MutableList<ModelGifts.Data.AllRealGift> = mutableListOf()
//            fragVirtualGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }
//            fragRealGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }


            Log.e("callGiftClick", "callGiftClick")

            lifecycleScope.launchWhenCreated {
                val items: MutableList<ModelGifts.Data.AllRealGift> = mutableListOf()
                fragVirtualGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }
                fragRealGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }

                if (items.size > 0) {
                    showProgressView()
                    items.forEach { gift ->
                        var res: ApolloResponse<GiftPurchaseMutation.Data>? = null
                        try {
                            res = apolloClient(
                                requireContext(),
                                userToken!!
                            ).mutation(
                                GiftPurchaseMutation(
                                    gift.id,
                                    giftUserid!!,
                                    getCurrentUserId()!!
                                )
                            ).execute()
                        } catch (e: ApolloException) {
                            Timber.d("apolloResponseException ${e.message}")
                            Toast.makeText(
                                requireContext(),
                                " ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            //views.snackbar("Exception ${e.message}")
                            //hideProgressView()
                            //return@launchWhenResumed
                        }
                        if (res?.hasErrors() == false) {
                            //views.snackbar("You bought ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} successfully!")
                            Toast.makeText(
                                requireContext(),
                                context?.resources?.getString(R.string.you_bought) + " ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} " + context?.resources?.getString(
                                    R.string.successfully
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                            // fireGiftBuyNotificationforreceiver(gift.id,giftUserid)
                        }
                        if (res!!.hasErrors()) {
//                                views.snackbar(""+ res.errors!![0].message)
                            Toast.makeText(
                                requireContext(),
                                "" + res.errors!![0].message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        Log.e("rr3rr", "-->" + Gson().toJson(res))
                        Timber.d("apolloResponse ${res.hasErrors()} ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName}")
                    }
                    hideProgressView()
                } else {
                    Log.e("giftsSizearenotmoreThan", "giftsSizearenotmoreThan")
                }
            }
        }
        binding.giftsTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {
                binding.giftsPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
            }
        })
        binding.giftsTabs.setupWithViewPager(binding.giftsPager)
        setupViewPager(binding.giftsPager)

        childFragmentManager.beginTransaction()
            .replace(R.id.receivedGiftContainer, FragmentReceivedGifts()).commitAllowingStateLoss()

        receivedGiftbottomSheetBehavior = BottomSheetBehavior.from(binding.receivedGiftBottomSheet)
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
    }

    fun fireGiftBuyNotificationForReceiver(gid: String, userid: String?) {
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
            val result = provideGraphqlApi().getResponse<Boolean>(
                query,
                queryName, userToken
            )
            Timber.d("RSLT", "" + result.message)
        }
    }

    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = UserItemsAdapter(childFragmentManager)
        fragRealGifts = FragmentRealGifts()
        fragVirtualGifts = FragmentVirtualGifts()
        adapter.addFragItem(fragRealGifts!!, getString(R.string.real_gifts))
        adapter.addFragItem(fragVirtualGifts!!, getString(R.string.virtual_gifts))
        viewPager.adapter = adapter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val arguments = arguments
        if (arguments != null) {
//            ShowNotification = arguments.get("ShowNotification") as String?
            ShowNotification = arguments.getString("ShowNotification") as String?
            if (ShowNotification.equals("true")) {
//                Handler().postDelayed({ binding.bell.performClick() }, 500)
                Handler(Looper.getMainLooper()).postDelayed({ binding.bell.performClick() }, 500)
            }
        }
        ShowNotification = getMainActivity().pref.getString("ShowNotification", "false")
        if (ShowNotification.equals("true")) {
            getMainActivity().pref.edit().putString("ShowNotification", "false").apply()
            ShowNotification = "false"
            Handler(Looper.getMainLooper()).postDelayed({ binding.bell.performClick() }, 500)
//            Handler().postDelayed({ binding.bell.performClick() }, 500)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }


    private fun getAllUserMoments(width: Int, size: Int) {
        showProgressView()
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(
                    GetAllUserMomentsQuery(
                        width,
                        size,
                        10,
                        "",
                        ""
                    )
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponseException all moments ${e.message}")
                binding.root.snackbar(" ${e.message}")
                hideProgressView()
                return@launchWhenResumed
            }
            hideProgressView()
            Log.e("rr2rr", "-->" + Gson().toJson(res))
            if (res.hasErrors()) {
                Log.e(
                    "rr2rrr",
                    "-->" + res.errors!![0].nonStandardFields!!["code"].toString()
                )
                if (res.errors!![0].nonStandardFields!!["code"].toString() == "InvalidOrExpiredToken"
                ) {
                    // error("User doesn't exist")
                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        if (activity != null) {
                            //App.userPreferences.saveUserIdToken("","","")
                            val intent = Intent(activity, SplashActivity::class.java)
                            startActivity(intent)
                            requireActivity().finishAffinity()
                        }
                    }
                }
            }
            val allmoments = res.data?.allUserMoments!!.edges
            if (allmoments.isNotEmpty()) {
                endCursor = res.data?.allUserMoments!!.pageInfo.endCursor!!
                hasNextPage = res.data?.allUserMoments!!.pageInfo.hasNextPage
                val allUserMomentsFirst: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()
                allmoments.indices.forEach { i ->
                    allUserMomentsFirst.add(allmoments[i]!!)
                }
                sharedMomentAdapter = NearbySharedMomentAdapter(
                    requireActivity(),
                    this@UserMomentsFragment,
                    allUserMomentsFirst,
                    userId
                )
                /*if (!hasLoaded) {
                    sharedMomentAdapter.addAll(allUserMomentsFirst)
                    hasLoaded=true
               }else{
                   sharedMomentAdapter.updateList(allUserMomentsFirst,userId)
                }*/
                binding.rvSharedMoments.adapter = sharedMomentAdapter
            }
            if (binding.rvSharedMoments.itemDecorationCount == 0) {
                binding.rvSharedMoments.addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.top = 10
                    }
                })
            }
            if (allmoments.isNotEmpty()) {
                Timber.d("apolloResponse: ${allmoments[0]?.node!!.file}")
                Timber.d("apolloResponse: ${allmoments[0]?.node!!.id}")
                Timber.d("apolloResponse: ${allmoments[0]?.node!!.createdDate}")
                Timber.d("apolloResponse: ${allmoments[0]?.node!!.momentDescriptionPaginated}")
                Timber.d("apolloResponse: ${allmoments[0]?.node!!.user?.fullName}")
            }
            //binding.root.snackbar("apolloResponse ${allusermoments?.get(0)?.file}")
        }
    }

    private fun getNotificationIndex() {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetNotificationCountQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponseException NotificationIndex  ${e.message}")
                binding.root.snackbar("${e.message}")
                return@launchWhenResumed
            }
            Timber.d("apolloResponse NotificationIndex ${res.hasErrors()}")
            Log.e("rr1rr", "-->" + Gson().toJson(res))
            if (res.hasErrors()) {
                Log.e(
                    "rr1rrr",
                    "-->" + res.errors!![0].nonStandardFields!!["code"].toString()
                )
                if (res.errors!![0].nonStandardFields!!["code"].toString() == "InvalidOrExpiredToken"
                ) {
                    // error("User doesn't exist")
                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        if (activity != null) {
                            //App.userPreferences.saveUserIdToken("","","")
                            val intent = Intent(activity, SplashActivity::class.java)
                            startActivity(intent)
                            requireActivity().finishAffinity()
                        }
                    }
                }
            }
            val NotificationCount = res.data?.unseenCount
            if (NotificationCount == null || NotificationCount == 0) {
                binding.counter.visibility = View.GONE
                binding.bell.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.notification1
                    )
                )
            } else {
                binding.counter.visibility = View.VISIBLE

                if (NotificationCount > 10) {
                    binding.counter.text = "9+"
                } else {
                    binding.counter.text = "" + NotificationCount
                }
                binding.bell.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.notification2
                    )
                )
            }
        }
    }

    private fun subscribeForNewMoment() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                apolloClientSubscription(requireActivity(), userToken!!).subscription(
                    OnNewMomentSubscription("", userToken!!)
                ).toFlow().catch {
                    it.printStackTrace()
                    Timber.d("reealltime exception= ${it.message}")
                    Log.d(
                        "UserMomentsSub",
                        "realtime retry ${it.message}"
                    )
                }
                    .retryWhen { cause, attempt ->
                        Timber.d("reealltime retry $attempt ${cause.message}")
                        Log.d(
                            "UserMomentsSub",
                            "realtime retry  $attempt ${cause.message}"
                        )
                        delay(attempt * 1000)
                        false
                    }
                    .collect { newMoment ->
                        if (newMoment.hasErrors()) {
                            Log.d(
                                "UserMomentsSub",
                                "realtime response error = ${newMoment.errors?.get(0)?.message}"
                            )
                            Timber.d("reealltime response error = ${newMoment.errors?.get(0)?.message}")
                        } else {
                            Log.d(
                                "UserMomentsSub",
                                " moment realtime NewStoryData content ${newMoment.data?.onNewMoment?.moment}"
                            )
                            Log.d(
                                "UserMomentsSub",
                                "moment realtime NewStoryData ${newMoment.data}"
                            )

                            val newMomentToAdd = newMoment.data?.onNewMoment?.moment

                            if (newMomentToAdd != null && !allUserMomentsNew.any { it.node!!.id == newMomentToAdd.id }) {
                                val newMomentUser = newMoment.data?.onNewMoment?.moment?.user
                                val avatar = GetAllUserMomentsQuery.Avatar(
                                    newMomentUser?.avatar?.url,
                                    newMomentUser?.avatar?.id.toString(),
                                    newMomentUser?.avatar?.user
                                )
                                val avatarPhotos = newMomentUser?.avatarPhotos?.map {
                                    GetAllUserMomentsQuery.AvatarPhoto(
                                        it?.url,
                                        it.id,
                                        it.user
                                    )
                                }
                                val user = GetAllUserMomentsQuery.User(
                                    newMomentUser?.id.toString(),
                                    newMomentUser?.email.toString(),
                                    newMomentUser?.fullName.toString(),
                                    newMomentUser?.username.toString(),
                                    newMomentUser?.gender,
                                    avatar,
                                    newMomentUser?.onesignalPlayerId,
                                    avatarPhotos ?: listOf()
                                )
                                val node = GetAllUserMomentsQuery.Node(
                                    newMomentToAdd.pk,
                                    newMomentToAdd.comment,
                                    newMomentToAdd.createdDate,
                                    newMomentToAdd.publishAt,
//                                    isPlaying = false,
                                    newMomentToAdd.file,
                                    newMomentToAdd.id,
                                    newMomentToAdd.like,
                                    newMomentToAdd.momentDescription,
                                    newMomentToAdd.momentDescriptionPaginated ?: listOf(),
                                    user
                                )
                                val newMomentEdge = GetAllUserMomentsQuery.Edge("", node)

                                Log.d(
                                    "UserMomentsSub",
                                    "Initial Size  " + allUserMomentsNew.size.toString()
                                )
                                allUserMomentsNew.add(0, newMomentEdge)

                                Log.d(
                                    "UserMomentsSub",
                                    "After Size  " + allUserMomentsNew.size.toString()
                                )
                                Log.d(
                                    "UserMomentsSub",
                                    "Added Moment ${newMomentEdge}"
                                )
                                CoroutineScope(Dispatchers.Main).launch {
                                    Log.e("obj_node", "submitList1 801")
                                    sharedMomentAdapter.submitList1(allUserMomentsNew)
//                                           sharedMomentAdapter.notifyItemInserted(0)

                                    Log.e(
                                        "TAG",
                                        "subscribeForNewMoment: " + scrollY1 + " " + height
                                    )
                                    binding.bubble.isVisible = scrollY1 > height
                                }
                            }
                        }
                    }
            } catch (e2: Exception) {
                e2.printStackTrace()
                Log.d("UserMomentsSub", "moment realtime exception= ${e2.message}")
                Timber.d("reealltime exception= ${e2.message}")
            }
        }
    }

    private fun subscribeForDeleteMoment() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                apolloClientSubscription(requireActivity(), userToken!!).subscription(
                    OnDeleteMomentSubscription("", userToken!!)
                ).toFlow().catch {
                    it.printStackTrace()
                    Timber.d("reealltime exception= ${it.message}")
                }
                    .retryWhen { cause, attempt ->
                        Timber.d("reealltime retry $attempt ${cause.message}")
                        Log.d(
                            "UserMomentsSub",
                            "realtime retry  $attempt ${cause.message}"
                        )
                        delay(attempt * 1000)
                        true
                    }
                    .collect { newMoment ->
                        if (newMoment.hasErrors()) {
                            Log.d(
                                "UserMomentsSub",
                                "DeleteMoment Realtime response error = ${newMoment.errors?.get(0)?.message}"
                            )
                            Timber.d("reealltime response error = ${newMoment.errors?.get(0)?.message}")
                        } else {
                            Log.d(
                                "UserMomentsSub",
                                "DeleteMoment Realtime  ID ${newMoment.data?.onDeleteMoment?.id}"
                            )
                            Log.d(
                                "UserMomentsSub",
                                "DeleteMoment Realtime  ID ${newMoment.data}"
                            )
                            Log.d(
                                "UserMomentsSub",
                                "Before Delete ${allUserMomentsNew.size}"
                            )

                            val deletedMoment =
                                allUserMomentsNew.filter { it.node?.pk.toString() == newMoment.data?.onDeleteMoment?.id }

                            if (deletedMoment.isNotEmpty()) {
                                Log.d("UserMomentsSub", "Before Delete 2 ${deletedMoment.size}")
                                Log.d("UserMomentsSub", "Before Delete ${deletedMoment[0]}")

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    allUserMomentsNew.removeIf { it.node?.pk.toString() == newMoment.data?.onDeleteMoment?.id }
                                } else {
                                    val deletedMomentId = newMoment.data?.onDeleteMoment?.id
                                    val iterator = allUserMomentsNew.iterator()
                                    while (iterator.hasNext()) {
                                        val moment = iterator.next()
                                        if (moment.node?.pk.toString() == deletedMomentId) {
                                            iterator.remove()
                                        }
                                    }
                                }

                                CoroutineScope(Dispatchers.Main).launch {
                                    Log.e("obj_node", "submitList1 941")
                                    sharedMomentAdapter.submitList1(allUserMomentsNew)
                                    // sharedMomentAdapter.notifyItemInserted(0)
                                }
                            }

//                            val deletedMoment =
//                                allUserMomentsNew.filter { it.node?.pk.toString() == newMoment.data?.onDeleteMoment?.id }
//                            if (deletedMoment.isNotEmpty()) {
//                                Log.d(
//                                    "UserMomentsSub",
//                                    "Before Delete 2 ${deletedMoment.size}"
//                                )
//                                val position = allUserMomentsNew.indexOf(deletedMoment[0])
////                                allUserMomentsNew.removeAt(position)
////                                CoroutineScope(Dispatchers.Main).launch {
////                                    sharedMomentAdapter.submitList1(allUserMomentsNew)
////                                    //       sharedMomentAdapter.notifyItemInserted(0)
////                                }
//                            }
                        }
                    }
            } catch (e2: Exception) {
                e2.printStackTrace()
                Log.d(
                    "UserMomentSubsc",
                    "Delete moment realtime exception= ${e2.message}"
                )
                Timber.d("reealltime exception= ${e2.message}")
            }
        }
    }

    private fun subscribeForUpdateMoment() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                apolloClientSubscription(requireActivity(), userToken!!).subscription(
                    OnUpdateMomentSubscription("", userToken!!)
                ).toFlow().catch {
                    it.printStackTrace()
                    Timber.d("reealltime exception= ${it.message}")
                }
                    .retryWhen { cause, attempt ->
                        Timber.d("reealltime retry $attempt ${cause.message}")
                        Log.d(
                            "UserMomentsSubs Update",
                            "realtime retry  $attempt ${cause.message}"
                        )
                        delay(attempt * 1000)
                        true
                    }
                    .collect { newMoment ->
                        if (newMoment.hasErrors()) {
                            Log.d(
                                "UserMomentsSubs Update",
                                "realtime UpdateStoryData response error = ${newMoment.errors?.get(0)?.message}"
                            )
                            Timber.d("reealltime response error = ${newMoment.errors?.get(0)?.message}")
                        } else {
                            Log.d(
                                "UserMomentsSubs Update",
                                " moment realtime UpdateStoryData content ${newMoment.data?.onUpdateMoment?.moment}"
                            )
                            Log.d(
                                "UserMomentsSubs Update",
                                "moment realtime UpdateStoryData ${newMoment.data}"
                            )
                            val newMomentToAdd = newMoment.data?.onUpdateMoment?.moment
                            val newMomentUser = newMoment.data?.onUpdateMoment?.moment?.user
                            val likedUser = newMoment.data?.onUpdateMoment?.likedByUsersList

                            if (LikebottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {

                                Log.d("UMFrag", "subscribeForUpdateMoment: if")
                                if (likeMomentItemPK.toString() == newMomentToAdd!!.pk.toString()) {
                                    momentLikesUsers.clear()

                                    Log.d("UMFrag", "subscribeForUpdateMoment: iff")
                                    likedUser!!.map {

//                                      var avtrat =  UserAvatar(url = it?.avatar?.url!!)
//                                     var userAvtar=   UserWithAvatar(
//                                            id= it?.id.toString(),
//                                         username= it?.username,
//                                         fullName =it?.fullName,
//                                         email =it?.email,
//                                         avatar = avtrat,
//                                        )
//                                        momentLikesUsers.add(MomentLikes(userAvtar))

                                        val models = CommentsModel()
                                        models.commenttext = it?.username
                                        models.uid = it?.id.toString()
                                        models.userurl = it?.avatar?.url
                                        momentLikesUsers.add(models)

                                    }

                                    if (momentLikeUserAdapters != null) {
                                        momentLikeUserAdapters?.notifyDataSetChanged()
                                    }
                                }

                                Log.d("UserUpdateMomentLike", "STATE_EXPANDED")

                            }


                            val avatar = GetAllUserMomentsQuery.Avatar(
                                newMomentUser?.avatar?.url,
                                newMomentUser?.avatar?.id.toString(),
                                newMomentUser?.avatar?.user
                            )
                            val avatarPhotos = newMomentUser?.avatarPhotos?.map {
                                GetAllUserMomentsQuery.AvatarPhoto(
                                    it?.url,
                                    it.id,
                                    it.user
                                )
                            }
                            val user = GetAllUserMomentsQuery.User(
                                newMomentUser?.id.toString(),
                                newMomentUser?.email.toString(),
                                newMomentUser?.fullName.toString(),
                                newMomentUser?.username.toString(),
                                newMomentUser?.gender,
                                avatar,
                                newMomentUser?.onesignalPlayerId,
                                avatarPhotos ?: listOf()
                            )
                            val node = GetAllUserMomentsQuery.Node(
                                newMomentToAdd?.pk,
                                newMomentToAdd?.comment,
                                newMomentToAdd?.createdDate!!,
                                newMomentToAdd.publishAt,
                                newMomentToAdd.file,
                                newMomentToAdd.id,
                                newMomentToAdd.like,
                                newMomentToAdd.momentDescription,
                                newMomentToAdd.momentDescriptionPaginated ?: listOf(),
                                user
                            )

                            val newMomentEdge = GetAllUserMomentsQuery.Edge("", node)
                            val replaceMoment =
                                allUserMomentsNew.filter { it.node?.pk.toString() == newMomentToAdd.pk.toString() }
                            Log.d("UMFrag", "replaceMoment: $replaceMoment")
                            Log.d(
                                "UserMomentSubsc Update",
                                "1. ${replaceMoment.isEmpty()}"
                            )
                            if (replaceMoment.isNotEmpty()) {

                                Log.d("UMFrag", "subscribeForUpdateMoment: replace")
                                Log.d(
                                    "UserMomentSubsc Update",
                                    "2. Item found before update ${replaceMoment[0].node?.momentDescriptionPaginated}"
                                )
                                val position = allUserMomentsNew.indexOf(replaceMoment[0])
                                allUserMomentsNew.removeAt(position)
                                allUserMomentsNew.add(position, newMomentEdge)
                                Log.d(
                                    "UserMomentSubsc Update",
                                    "3. Item found after update $position ${allUserMomentsNew[position].node?.momentDescriptionPaginated}"
                                )
                                CoroutineScope(Dispatchers.Main).launch {
                                    Log.e("obj_node", "submitList1 1098")
                                    sharedMomentAdapter.submitList1(allUserMomentsNew)
                                    sharedMomentAdapter.notifyItemRemoved(position)
                                    sharedMomentAdapter.notifyItemChanged(position)
                                }
                            } else {
                                Log.d("UserMomentSubsc Update", "Item not found")
                            }
                        }
                    }
            } catch (e2: Exception) {
                e2.printStackTrace()
                Log.d("UserMomentSubsc Update", "moment realtime exception= ${e2.message}")
                Timber.d("reealltime exception= ${e2.message}")
            }
        }
    }

    private fun subscribeForNewStory() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                apolloClientSubscription(requireActivity(), userToken!!).subscription(
                    OnNewStorySubscription()
                ).toFlow().catch {
                    it.printStackTrace()
                    Timber.d("reealltime exception= ${it.message}")
                    Log.e(
                        "UserMomentSubsc",
                        "reealltime exception= ${it.message}"
                    )
                }
                    .retryWhen { cause, attempt ->
                        Timber.d("reealltime retry $attempt ${cause.message}")
                        Log.e(
                            "UserMomentSubsc",
                            "reealltime retry $attempt ${cause.message}"
                        )
                        delay(attempt * 1000)
                        true
                    }.collect { newStory ->
                        if (newStory.hasErrors()) {

                            Log.e(
                                "UserMomentSubsc",
                                "realtime response error = ${newStory.errors?.get(0)?.message}"
                            )
                            Timber.d("reealltime response error = ${newStory.errors?.get(0)?.message}")
                        } else {
                            //   Timber.d("reealltime onNewMessage ${newMessage.data?.onNewMessage?.message?.timestamp}")

                            Log.d(
                                "UserMomentSubsc",
                                " story realtime NewStoryData content ${tempStories?.contains(newStory)}"
                            )
                            if (!tempStories?.contains(newStory)) {
                                tempStories?.add(newStory)
                                Log.e(
                                    "UserMomentSubsc",
                                    " story realtime NewStoryData content ${!stories.any { it?.user?.id == newStory.data?.onNewStory?.user?.id }}"
                                )

                                val user =
                                    stories.filter { it?.user?.id == newStory.data?.onNewStory?.user?.id }.filter{ it?.batchNumber == newStory.data?.onNewStory?.batchNumber }

                                Log.e(
                                    "UserMomentSubsc",
                                    "Filter, story realtime NewStoryData content ${user.toString()} ${user.isEmpty()}"
                                )
                                if (user.isEmpty()) {
                                    val newStoryData = newStory.data?.onNewStory
                                    val newStoryUser = newStory.data?.onNewStory?.user
                                    val avatar = newStory.data?.onNewStory?.user?.avatar
                                    val listOfAvatar = newStory.data?.onNewStory?.user?.avatarPhotos
                                    val storiesTemp = newStory.data?.onNewStory?.stories
                                    val newStoryCollection =
                                        GetAllUserMultiStoriesQuery.AllUserMultiStory(
                                            GetAllUserMultiStoriesQuery.User(
                                                newStoryUser?.id!!,
                                                newStoryUser.fullName,
                                                GetAllUserMultiStoriesQuery.Avatar(
                                                    avatar?.url,
                                                    avatar?.id!!
                                                ),
                                                newStoryUser.avatarIndex,
                                                convertAvatarListQueryToSubscription(listOfAvatar)
                                            ),
                                            batchNumber = newStoryData?.batchNumber!!,
                                            stories = GetAllUserMultiStoriesQuery.Stories(
                                                convertEdgeListQueryToSubscrption(storiesTemp),
                                                GetAllUserMultiStoriesQuery.PageInfo2(
                                                    storiesTemp?.pageInfo?.endCursor,
                                                    storiesTemp?.pageInfo?.hasNextPage!!,
                                                    storiesTemp.pageInfo.hasPreviousPage,
                                                    storiesTemp.pageInfo.startCursor
                                                )
                                            )
                                        )
                                    Log.d(
                                        "UserMomentSubsc",
                                        "If, Before Add TotalUserStories ${stories.size} and newStory ${storiesTemp.edges.size}"
                                    )
                                    stories.add(0, newStoryCollection)
                                    Log.d("UserMomentSubsc", "Add UserStories $stories")
                                    Log.d(
                                        "UserMomentSubsc",
                                        "After Add TotalUserStories ${stories.size}"
                                    )
                                    CoroutineScope(Dispatchers.Main).launch {
                                        usersMultiStoryAdapter.storyList = stories
                                        usersMultiStoryAdapter.notifyItemInserted(1)
                                    }
                                } else {
                                    val position = stories.indexOf(user[0])
                                    val newStoryData = newStory.data?.onNewStory
                                    val newStoryUser = newStory.data?.onNewStory?.user
                                    val avatar = newStory.data?.onNewStory?.user?.avatar
                                    val listOfAvatar = newStory.data?.onNewStory?.user?.avatarPhotos
                                    val storiesTemp = newStory.data?.onNewStory?.stories
                                    val newStoryCollection =
                                        GetAllUserMultiStoriesQuery.AllUserMultiStory(
                                            GetAllUserMultiStoriesQuery.User(
                                                newStoryUser?.id!!,
                                                newStoryUser.fullName,
                                                GetAllUserMultiStoriesQuery.Avatar(
                                                    avatar?.url,
                                                    avatar?.id!!
                                                ),
                                                newStoryUser.avatarIndex,
                                                convertAvatarListQueryToSubscription(listOfAvatar)
                                            ),
                                            batchNumber = newStoryData?.batchNumber!!,
                                            stories = GetAllUserMultiStoriesQuery.Stories(
                                                convertEdgeListQueryToSubscrption(storiesTemp),
                                                GetAllUserMultiStoriesQuery.PageInfo2(
                                                    storiesTemp?.pageInfo?.endCursor,
                                                    storiesTemp?.pageInfo?.hasNextPage!!,
                                                    storiesTemp.pageInfo.hasPreviousPage,
                                                    storiesTemp.pageInfo.startCursor
                                                )
                                            )
                                        )
                                    Log.d(
                                        "UserMomentSubsc",
                                        "Else, Before Set TotalUserStories ${stories.size} and newStory ${storiesTemp.edges.size}"
                                    )
                                    stories.removeAt(position)
                                    stories.add(0, newStoryCollection)
                                    Log.d("UserMomentSubsc", "Set UserStories $stories")
                                    Log.d(
                                        "UserMomentSubsc",
                                        "After Set TotalUserStories ${stories.size}"
                                    )
                                    CoroutineScope(Dispatchers.Main).launch {
                                        usersMultiStoryAdapter.storyList = stories
                                        usersMultiStoryAdapter.notifyItemRemoved(position + 1)
                                        usersMultiStoryAdapter.notifyItemInserted(1)
                                    }
                                }
                                //  viewModel.onNewMessage(newMessage = newMessage.data?.onNewMessage?.message)
                            }
                        }
                    }
                Timber.d("reealltime 2")
            } catch (e2: Exception) {
                e2.printStackTrace()
                Log.d("UserMomentSubsc", "story realtime exception= ${e2.localizedMessage}")
                Timber.d("reealltime exception= ${e2.message}")
            }
        }
    }

    private fun subscribeForDeleteStory() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                apolloClientSubscription(requireActivity(), userToken!!).subscription(
                    OnDeleteStorySubscription()
                ).toFlow().catch {
                    it.printStackTrace()
                    Timber.d("reealltime exception= ${it.message}")
                }
                    .retryWhen { cause, attempt ->
                        Timber.d("reealltime retry $attempt ${cause.message}")
                        Log.d(
                            "UserMomentSubsc",
                            "realtime retry  $attempt ${cause.message}"
                        )
                        delay(attempt * 1000)
                        true
                    }.collect { newStory ->
                        if (newStory.hasErrors()) {
                            Log.d(
                                "UserMomentSubsc",
                                "realtime response error = ${newStory.errors?.get(0)?.message}"
                            )
                            Timber.d("reealltime response error = ${newStory.errors?.get(0)?.message}")
                        } else {
                            //   Timber.d("reealltime onNewMessage ${newMessage.data?.onNewMessage?.message?.timestamp}")
                            Log.d(
                                "UserMomentSubsc",
                                " story realtime DeleteStory content ${newStory.data?.onDeleteStory}"
                            )
                            Log.d(
                                "UserMomentSubsc",
                                "story realtime DeleteStory ${newStory.data}"
                            )
                            val oldBatches =
                                stories.filter { it?.user?.id == newStory.data?.onDeleteStory?.userId }
                                    .sortedBy { it?.batchNumber }
                            Log.d(
                                "UserMomentSubsc",
                                "Old Batches size ${oldBatches.size}"
                            )
                            var allStories = mutableListOf<GetAllUserMultiStoriesQuery.Edge?>()
                            val newBatches =
                                mutableListOf<GetAllUserMultiStoriesQuery.AllUserMultiStory>()
                            for (batch in oldBatches) {
                                batch?.stories?.edges?.filter { it?.node?.pk != newStory.data?.onDeleteStory?.storyId }
                                    ?.let { allStories.addAll(it) }
                            }
                            Log.d(
                                "UserMomentSubsc",
                                "allStories size ${oldBatches.size} after removed"
                            )
                            if (allStories.isNotEmpty()) {
                                if (oldBatches.size > 1) {
                                    val batchSize = oldBatches[0]?.stories?.edges?.size ?: 0
                                    Log.d(
                                        "UserMomentSubsc",
                                        "Batchsize more than story size $batchSize"
                                    )
                                    for (batch in oldBatches) {
                                        if (allStories.isNotEmpty()) {
                                            val newBatch =
                                                GetAllUserMultiStoriesQuery.AllUserMultiStory(
                                                    batch?.user!!,
                                                    batch.batchNumber,
                                                    GetAllUserMultiStoriesQuery.Stories(
                                                        pageInfo = batch.stories.pageInfo,
                                                        edges = allStories.take(batchSize)
                                                    )
                                                )
                                            newBatches.add(newBatch)
                                            allStories = allStories.drop(batchSize).toMutableList()
                                            Log.d(
                                                "UserMomentSubsc",
                                                "Newbatch added and remaining Stories ${allStories.size}"
                                            )
                                        }
                                    }
                                } else {
                                    val batch = oldBatches[0]
                                    val newBatch = GetAllUserMultiStoriesQuery.AllUserMultiStory(
                                        batch?.user!!,
                                        batch.batchNumber,
                                        GetAllUserMultiStoriesQuery.Stories(
                                            pageInfo = batch.stories.pageInfo,
                                            edges = allStories
                                        )
                                    )
                                    newBatches.add(newBatch)
                                    Log.d(
                                        "UserMomentSubsc",
                                        "Batch size less than one and newBatch added"
                                    )
                                }
                                val modifiedPositions = mutableListOf<Int>()
                                val removedPositions = mutableListOf<Int>()
                                newBatches.sortedBy { it.batchNumber }
                                    .forEachIndexed { index, newBatch ->
                                        val oldStoryPosition = stories.indexOf(oldBatches[index])
                                        if (oldStoryPosition > -1) {
                                            modifiedPositions.add(oldStoryPosition)
                                            stories[oldStoryPosition] = newBatch
                                        }
                                    }
                                if (oldBatches.size > newBatches.size) {
                                    val oldBatch = oldBatches[oldBatches.size - 1]
                                    val oldBatchPosition = stories.indexOf(oldBatch)
                                    stories.removeAt(oldBatchPosition)
                                    removedPositions.add(oldBatchPosition)
                                }
                                CoroutineScope(Dispatchers.Main).launch {
                                    usersMultiStoryAdapter.storyList = stories
                                    modifiedPositions.forEach {
                                        usersMultiStoryAdapter.notifyItemRemoved(it + 1)
                                        usersMultiStoryAdapter.notifyItemInserted(it + 1)
                                    }
                                    removedPositions.forEach {
                                        usersMultiStoryAdapter.notifyItemRemoved(it + 1)
                                    }
                                }
                            } else {
                                val oldBatchPosition = stories.indexOf(oldBatches[0])
                                stories.removeAt(oldBatchPosition)
                                CoroutineScope(Dispatchers.Main).launch {
                                    usersMultiStoryAdapter.storyList = stories
                                    usersMultiStoryAdapter.notifyItemRemoved(oldBatchPosition + 1)
                                }
                            }
                        }
                    }
                Timber.d("reealltime 2")
            } catch (e2: Exception) {
                e2.printStackTrace()
                Log.d("UserMomentSubsc", "story realtime exception= ${e2.message}")
                Timber.d("reealltime exception= ${e2.message}")
            }
        }
    }

    private fun convertEdgeListQueryToSubscrption(stories: OnNewStorySubscription.Stories?): List<GetAllUserMultiStoriesQuery.Edge?> {
        if (stories == null) return listOf()
        val newList = mutableListOf<GetAllUserMultiStoriesQuery.Edge>()
        for (story in stories.edges) {
            val node = story?.node
            val newStoryUser = story?.node?.user
            val avatar = story?.node?.user?.avatar
            val listOfAvatar = newStoryUser?.avatarPhotos
            newList.add(
                GetAllUserMultiStoriesQuery.Edge(
                    story?.cursor.toString(),
                    GetAllUserMultiStoriesQuery.Node(
                        node?.createdDate.toString(),
                        node?.publishAt.toString(),
                        node?.file.toString(),
                        node?.fileType,
                        node?.id.toString(),
                        node?.pk,
                        node?.thumbnail,
                        node?.commentsCount,
                        GetAllUserMultiStoriesQuery.Comments(
                            GetAllUserMultiStoriesQuery.PageInfo(
                                node?.comments?.pageInfo?.endCursor,
                                node?.comments?.pageInfo?.hasNextPage!!,
                                node?.comments.pageInfo.hasPreviousPage,
                                node?.comments.pageInfo.startCursor
                            ),
                            convertCommentListQueryToSubscription(node.comments.edges)
                        ),
                        node.likesCount,
                        GetAllUserMultiStoriesQuery.Likes(
                            GetAllUserMultiStoriesQuery.PageInfo1(
                                node.likes?.pageInfo?.endCursor,
                                node.likes?.pageInfo?.hasNextPage!!,
                                node.likes.pageInfo.hasPreviousPage,
                                node.likes.pageInfo.startCursor
                            ),
                            convertListListQueryToSubscription(node.likes.edges)
                        ),
                        GetAllUserMultiStoriesQuery.User3(
                            newStoryUser?.id!!,
                            newStoryUser.fullName,
                            GetAllUserMultiStoriesQuery.Avatar3(avatar?.url, avatar?.id!!),
                            newStoryUser.avatarIndex,
                            convertAvatar3ListQueryToSubscription(listOfAvatar)
                        )
                    )
                )
            )
        }
        return newList
    }

    override fun onResume() {
        getNotificationIndex()
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction("moment_added")
        activity?.registerReceiver(broadCastReceiver, intentFilter,
            Context.RECEIVER_NOT_EXPORTED)
        getMainActivity().setDrawerItemCheckedUnchecked(null)
    }

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            val extras = intent?.extras
            val state = extras!!.getString("extra")
            Log.e("Tag_Moment_added", "onReceive: $state")
//            val listSize = mViewModel.userMomentsList.size - 1
            //      mViewModel.userMomentsList.clear()
            binding.bubble.isVisible = binding.scrollView.scrollY != 0
            Log.d("UserMomentsFragment", "BroadCastReceiver Calling")
            //          getUserMomentNextpage(width, size, listSize, "")
//            updateView(state.toString())
        }
    }

    private fun convertAvatarListQueryToSubscription(list: List<OnNewStorySubscription.AvatarPhoto>?): List<GetAllUserMultiStoriesQuery.AvatarPhoto> {
        if (list == null) return listOf()
        val newList = mutableListOf<GetAllUserMultiStoriesQuery.AvatarPhoto>()
        for (item in list) {
            newList.add(GetAllUserMultiStoriesQuery.AvatarPhoto(item.url, item.id))
        }
        return newList
    }

    private fun convertAvatar3ListQueryToSubscription(list: List<OnNewStorySubscription.AvatarPhoto3>?): List<GetAllUserMultiStoriesQuery.AvatarPhoto3> {
        if (list == null) return listOf()
        val newList = mutableListOf<GetAllUserMultiStoriesQuery.AvatarPhoto3>()
        for (item in list) {
            newList.add(GetAllUserMultiStoriesQuery.AvatarPhoto3(item.url, item.id))
        }
        return newList
    }

    private fun convertCommentListQueryToSubscription(list: List<OnNewStorySubscription.Edge1?>?): List<GetAllUserMultiStoriesQuery.Edge1> {
        if (list == null) return listOf()
        val newList = mutableListOf<GetAllUserMultiStoriesQuery.Edge1>()
        return newList
    }

    private fun convertListListQueryToSubscription(list: List<OnNewStorySubscription.Edge2?>?): List<GetAllUserMultiStoriesQuery.Edge2> {
        if (list == null) return listOf()
        val newList = mutableListOf<GetAllUserMultiStoriesQuery.Edge2>()
        return newList
    }

    private fun getAllUserMultiStories() {
        //showProgressView()
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetAllUserMultiStoriesQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception all multi stories${e.message}")
                binding.root.snackbar(" ${e.message}")
                //hideProgressView()
                return@launchWhenResumed
            }
            Timber.d("apolloResponse allUserStories stories ${res.hasErrors()}")
            var body = Gson().toJson(res.data)
            Log.e("rr4rr", "-->" + Gson().toJson(res) + "   " + body)
            if (res.hasErrors()) {
                val errorMessage = res.errors?.get(0)?.message
                Toast.makeText(
                    requireContext(),
                    "$errorMessage",
                    Toast.LENGTH_SHORT
                ).show()
//                Toast.makeText(
//                    requireContext(),
//                    "MultiStory Exception $errorMessage",
//                    Toast.LENGTH_SHORT
//                ).show()
                //    Log.e("rr4rrrr","-->"+res.errors!!.get(0).nonStandardFields!!.get("code"))
                /*     if(res.errors!!.get(0).nonStandardFields!!.get("code").toString().equals("InvalidOrExpiredToken"))
                     {
                         // error("User doesn't exist")

                         lifecycleScope.launch(Dispatchers.Main) {
                             userPreferences.clear()
                             if(activity!=null)
                             {
                                 //App.userPreferences.saveUserIdToken("","","")
                                 val intent = Intent(activity, SplashActivity::class.java)
                                 startActivity(intent)
                                 requireActivity().finishAffinity()
                             }    }                   }*/
            }
            // hideProgressView()

            val allUserMultiStories = res.data?.allUserMultiStories!!.also {
                usersMultiStoryAdapter =
                    UserMultiStoriesAdapter(requireContext(), mUser, this@UserMomentsFragment)
                stories.clear()
                stories.addAll(it)
                usersMultiStoryAdapter.storyList = stories

                viewModel.getCurrentUser(userId!!, token = userToken!!, false)
                    .observe(viewLifecycleOwner) { userDetails ->
                        userDetails?.let {
                            mUser = it
                            usersMultiStoryAdapter.mUser = it
                            usersMultiStoryAdapter.notifyItemChanged(0)
                        }
                    }
            }
            /*     val allUserStories = res.data?.allUserStories!!.edges.also {
                     usersAdapter = UserStoriesAdapter(requireActivity(), this@UserMomentsFragment, it)
                 }*/
            binding.rvUserStories.adapter = usersMultiStoryAdapter
            if (binding.rvUserStories.itemDecorationCount == 0) {
                binding.rvUserStories.addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.top = 20
                        outRect.bottom = 10
                        outRect.left = 20
                    }
                })
            }
            if (allUserMultiStories.isNotEmpty()) {
                /*                Timber.d("apolloResponse: stories ${allUserMultiStories?.size}")
                                Timber.d("apolloResponse: stories ${allUserMultiStories?.get(0)?.node!!.file}")
                                Timber.d("apolloResponse: stories ${allUserMultiStories?.get(0)?.node!!.id}")
                                Timber.d("apolloResponse: stories ${allUserMultiStories?.get(0)?.node!!.createdDate}")*/
            }
        }
    }

    private fun uploadStoryLater(publishAt: String) {
//        showProgressView()
        startLoader();
        lifecycleScope.launchWhenCreated {
            val f = file
            val buildder = DefaultUpload.Builder()
            Log.e("UserStory", "story upload ")
            buildder.contentType("Content-Disposition: form-data;")
            buildder.fileName(f.name)

            val upload = buildder.content(f).build()
            Timber.d("filee ${f.exists()} ${f.length()}")
            val userToken = getCurrentUserToken()!!
            Log.d("UserMomentFragment", "userToken $userToken File ${upload.fileName} Publish at: $publishAt")
            Log.e("UserMomentFragment", "userToken $userToken File ${upload.fileName}")
            val response: ApolloResponse<ScheduleStoryMutation.Data> = try {
                Log.e("UserStory", "story upload 1")
                apolloClient(
                    context = requireContext(),
                    token = userToken,
                    isMultipart = true
                ).mutation(
                    ScheduleStoryMutation(file = upload, publishAt)
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("filee Apollo Exception ${e.message}")
                Log.e("UsermomentsStory", Gson().toJson(e))

                Log.e("UsermomentsStory", "ApolloException==> ${e.message}")
//                binding.root.snackbar("ApolloException ${e.message}")
                binding.root.snackbar("${e.message}")
                return@launchWhenCreated
            } catch (e: Exception) {
                Log.e("UsermomentsStory", "Exception==> ${e.message}")

                Log.d("UserMomentFragment", "Exception $e")
                Timber.d("filee General Exception ${e.message} $userToken")
                binding.root.snackbar(" ${e.message}")
                return@launchWhenCreated
            } finally {
                stopLoader();
//                hideProgressView()
            }
            Log.d("NewUserMomentFragment", "uploadStoryLater: ${response.data}")
            if (response.hasErrors()) {
                Log.d("NewUserMomentFragment", "${response.errors}")
                Log.e("UsermomentsStory", "ResponceError==> ${response.errors}")
                Log.e("UsermomentsStory", Gson().toJson(response))

                if(response.errors?.get(0)?.message != null) {
                    if (response.errors?.get(0)?.message!!
                            .contains("purchase a package", true)) {
                        binding.root.snackbarOnTop(
                            "${response.errors?.get(0)?.message}",
                            Snackbar.LENGTH_INDEFINITE,
                            callback = {
                                findNavController().navigate(R.id.action_global_plan)
                            })
                    }
                    else {
                        binding.root.snackbarOnTop(
                            "${response.errors?.get(0)?.message}",
                            Snackbar.LENGTH_INDEFINITE,
                            callback = {
                                //TODO: navigate to package/subscription screen
                            })
                    }
                }
            }
            else {
                binding.root.snackbar("New Story scheduled for later")
            }
            Timber.d("filee hasError= $response")
            Log.e("UsermomentsSuces", Gson().toJson(response))
            Log.e("UserStory", "story upload 2")

            //   getAllUserStories()
            getAllUserMultiStories()
        }
    }

    private fun uploadStory() {
//        showProgressView()
        startLoader();
        lifecycleScope.launchWhenCreated {
            val f = file
            val buildder = DefaultUpload.Builder()
            Log.e("UserStory", "story upload ")
            buildder.contentType("Content-Disposition: form-data;")
            buildder.fileName(f.name)

            val upload = buildder.content(f).build()
            Timber.d("filee ${f.exists()} ${f.length()}")
            val userToken = getCurrentUserToken()!!
            Log.d("UserMomentFragment", "userToken $userToken File ${upload.fileName}")
            Log.e("UserMomentFragment", "userToken $userToken File ${upload.fileName}")
            val response: ApolloResponse<StoryMutation.Data> = try {
                Log.e("UserStory", "story upload 1")
                apolloClient(
                    context = requireContext(),
                    token = userToken,
                    isMultipart = true
                ).mutation(
                    StoryMutation(file = upload)
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("filee Apollo Exception ${e.message}")
                Log.e("UsermomentsStory", Gson().toJson(e))

                Log.e("UsermomentsStory", "ApolloException==> ${e.message}")
//                binding.root.snackbar("ApolloException ${e.message}")
                binding.root.snackbar(" ${e.message}")
                return@launchWhenCreated
            } catch (e: Exception) {
                Log.e("UsermomentsStory", "Exception==> ${e.message}")

                Log.d("UserMomentFragment", "Exception $e")
                Timber.d("filee General Exception ${e.message} $userToken")
                binding.root.snackbar(" ${e.message}")
                return@launchWhenCreated
            } finally {
                stopLoader();
//                hideProgressView()
            }
            if (response.hasErrors()) {
                Log.d("NewUserMomentFragment", "${response.errors}")
                Log.e("UsermomentsStory", "ResponceError==> ${response.errors}")
                Log.e("UsermomentsStory", Gson().toJson(response))

                if(response.errors?.get(0)?.message != null) {
                    if (response.errors?.get(0)?.message!!
                            .contains("purchase a package", true)) {
                        binding.root.snackbarOnTop(
                            "${response.errors?.get(0)?.message}",
                            Snackbar.LENGTH_INDEFINITE,
                            callback = {
                                findNavController().navigate(R.id.action_global_plan)
                            })
                    }
                    else {
                        binding.root.snackbarOnTop(
                            "${response.errors?.get(0)?.message}",
                            Snackbar.LENGTH_INDEFINITE,
                            callback = {
                                //TODO: navigate to package/subscription screen
                            })
                    }
                }
            }
            Timber.d("filee hasError= $response")
            Log.e("UsermomentsSuces", Gson().toJson(response))
            Log.e("UserStory", "story upload 2")

            //   getAllUserStories()
            getAllUserMultiStories()
        }
    }

    private fun startLoader() {
        val viewHolder: RecyclerView.ViewHolder? =
            binding.rvUserStories.findViewHolderForAdapterPosition(0)
        if (viewHolder != null) {
            viewHolder as UserMultiStoriesAdapter.NewUserStoryHolder
            viewHolder.viewBinding.loadingView.status = InsLoadingView.Status.LOADING
            viewHolder.viewBinding.root.isClickable = false
        }
    }

    private fun stopLoader() {
        val viewHolder: RecyclerView.ViewHolder? =
            binding.rvUserStories.findViewHolderForAdapterPosition(0)
        if (viewHolder != null) {
            viewHolder as UserMultiStoriesAdapter.NewUserStoryHolder
            viewHolder.viewBinding.loadingView.status = InsLoadingView.Status.CLICKED
            viewHolder.viewBinding.root.isClickable = true
        }
    }

    override fun onUserMultiStoryClick(
        position: Int,
        userStory: GetAllUserMultiStoriesQuery.AllUserMultiStory
    ) {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val node = userStory.stories.edges.get(0)?.node
        val url = "${BuildConfig.BASE_URL}media/${node?.file}"

        var userurl = if (node!!.user!!.avatar != null && node.user!!.avatar!!.url != null) {
            node.user.avatar!!.url!!
        } else {
            ""
        }
        val username = node.user!!.fullName
        val UserID = userId
        val objectId = node.pk
        var text = node.createdDate.toString()
        text = text.replace("T", " ").substring(0, text.indexOf("."))
        val momentTime = formatter.parse(text)
        val times = DateUtils.getRelativeTimeSpanString(
            momentTime.time,
            Date().time,
            DateUtils.MINUTE_IN_MILLIS
        )

        val dialog = UserMultiStoryDetailFragment(
            object : UserMultiStoryDetailFragment.DeleteCallback {
                override fun deleteCallback(objectId: Int) {
                    // call api for delete
                    showProgressView()
                    Handler(Looper.getMainLooper()).postDelayed({
                        lifecycleScope.launch {
                            try {
                                apolloClient(requireContext(), userToken!!)
                                    .mutation(
                                        DeleteStoryMutation("$objectId")
                                    ).execute()
                            } catch (e: ApolloException) {
                                Timber.d("apolloResponse Exception ${e.message}")
                                binding.root.snackbar(" ${e.message}")
                                hideProgressView()
                                return@launch
                            }
                        }
                        hideProgressView()
                        getAllUserMultiStories()
                    }, 1000)
                }
            }
        )
        val b = Bundle()
        b.putString("Uid", UserID)
        b.putString("url", url)
        b.putString("userurl", userurl)
        b.putString("username", username)
        b.putString("times", times.toString())
        b.putString("token", userToken)
        b.putInt("objectID", objectId!!)
        b.putBoolean("showDelete", userId == node.user.id)
        val gson = Gson()
        val json = gson.toJson(userStory.stories)
        b.putString("stories", json)
        dialog.arguments = b
        dialog.show(childFragmentManager, "story")
        // }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    var mediaPermissions = arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO
    )

    override fun onAddNewUserStoryClick(isCamera: Boolean) {

        if (isCamera) {
            val intent = Intent(requireActivity(), ImagePickerActivity::class.java)
            intent.putExtra("video_duration_limit", 60)
            intent.putExtra("withCrop", false)
            photosLauncher.launch(intent)

        } else {

            galleryImageLauncher.launch(
                Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI
                )
            )

        }

//        val dialog = Dialog(requireContext())
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        dialog.setCancelable(true)
//        dialog.setContentView(R.layout.dialog_image_option)
//        dialog.findViewById<TextView>(R.id.header_title).text =
//            resources.getString(R.string.select_story_pic)
//        dialog.findViewById<LinearLayoutCompat>(R.id.ll_camera).setOnClickListener {
//            val intent = Intent(requireActivity(), ImagePickerActivity::class.java)
//            intent.putExtra("withCrop", false)
//            photosLauncher.launch(intent)
//            dialog.dismiss()
//        }
//        dialog.findViewById<LinearLayoutCompat>(R.id.ll_gallery).setOnClickListener {
//            galleryImageLauncher.launch(
//                Intent(
//                    Intent.ACTION_PICK,
//                    MediaStore.Images.Media.INTERNAL_CONTENT_URI
//                )
//            )
//            dialog.dismiss()
//        }
//        dialog.show()

        /*        val intent = Intent(requireActivity(), ImagePickerActivity::class.java)
                photosLauncher.launch(intent)*/
        //val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        // videoLauncher.launch(intent)
    }

    override fun onSharedMomentClick(position: Int, item: GetAllUserMomentsQuery.Edge?) {

    }

    override fun onLikeofMomentshowClick(position: Int, item: GetAllUserMomentsQuery.Edge?) {
        getMomentLikes(item)
    }

    override fun onLikeofMomentClick(position: Int, item: GetAllUserMomentsQuery.Edge?) {
        showProgressView()
        lifecycleScope.launchWhenResumed {
            userId = getCurrentUserId()!!
            userName = getCurrentUserName()!!
            Log.i(TAG, "onLikeofMomentClick: UserId: $userId   Username: $userName")
            val selectedUserId = item?.node?.user?.id
            if (selectedUserId == userId) {
                getMomentLikes(item)
//                val rvLikes = binding.rvLikes
//                val no_data = binding.noDatas
//                val txtHeaderLike = binding.txtheaderlike
//                txtHeaderLike.text = "${item?.node?.like.toString()} Likes"
//                val Likedata = item?.node!!.like
//                if (rvLikes.itemDecorationCount == 0) {
//                    rvLikes.addItemDecoration(object : RecyclerView.ItemDecoration() {
//                        override fun getItemOffsets(
//                            outRect: Rect,
//                            view: View,
//                            parent: RecyclerView,
//                            state: RecyclerView.State
//                        ) {
//                            outRect.top = 25
//                        }
//                    })
//                }
////                if (Likedata.isNotEmpty()) {
////                    Timber.d("apolloResponse: ${Likedata[0]!!.node!!.id}")
////                    no_data.visibility = View.GONE
////                    rvLikes.visibility = View.VISIBLE
////                    val items1: ArrayList<CommentsModel> = ArrayList()
////                    Likedata.indices.forEach { i ->
////                        val models = CommentsModel()
////                        models.commenttext = Likedata[i]!!.node!!.user.fullName
////                        models.userurl = Likedata[i]!!.node!!.user.avatar!!.url
////                        items1.add(models)
////                    }
//                    adapters = StoryLikesAdapter(requireActivity(), items1, glide)
////                    rvLikes.adapter = adapters
////                    rvLikes.layoutManager = LinearLayoutManager(activity)
////                } else {
//                    no_data.visibility = View.VISIBLE
//                    rvLikes.visibility = View.GONE
////                }
//                showLikeBottomSheet(item)
//
//                if (LikebottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
//                    LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//                } else {
//                    LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//                }
            } else {
                val res = try {
                    apolloClient(
                        requireContext(),
                        userToken!!
                    ).mutation(LikeOnMomentMutation(item?.node!!.pk!!.toString()))
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponseException ${e.message}")
                    binding.root.snackbar(" ${e.message}")
                    hideProgressView()
                    return@launchWhenResumed
                }
                if (res.hasErrors()) {
                    Timber.tag("UserMomentFragment").e(res.errors?.get(0)?.message.toString())
                } else {
                    getParticularMoments(position, item.node.pk.toString())
                }
            }
            hideProgressView()
            Timber.d("Size", "" + allUserMoments.size)
            //fireLikeNotificationforreceiver(item)
           //getParticularMoments(position, item?.node?.pk.toString())
        }
    }

    private fun getMomentLikes(item: GetAllUserMomentsQuery.Edge?) {
        likeMomentItemPK = (item?.node?.pk ?: "").toString()
        lifecycleScope.launchWhenResumed {
            userToken?.let {
                Log.d("UserMomentsFragment", "UserMomentNextPage Calling")
                mViewModel.getMomentLikes(it, (item?.node?.pk ?: "").toString()) { error ->
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
//        lifecycleScope.launchWhenResumed{
//            val token = getCurrentUserToken()!!
//            mViewModel.getMomentLikes(token, (item?.node?.pk ?: "").toString())
//                .observe(viewLifecycleOwner) {
//                    it?.let { momentLikes ->
//                        loadLikesDialog(momentLikes)
//                    }
//                }
//        }
    }

    private fun loadLikesDialog(momentLikes: ArrayList<MomentLikes>) {
//        lifecycleScope.launch(Dispatchers.Main) {
        lifecycleScope.launchWhenResumed {
            momentLikesUsers.clear()
//        momentLikesUsers.addAll(momentLikes)
//            showLikeBottomSheet()
//        val dialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
//        val view =
//            LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_layout2, null)
//        dialog.setContentView(view)

            val rvLikes = binding.rvLikes
            val no_data = binding.noDatas
            val txtHeaderLike = binding.txtheaderlike
//
            txtHeaderLike.text =
                momentLikes.size.toString() + " ${requireActivity().resources.getString(R.string.like)}"
            if (momentLikes.isNotEmpty()) {
                no_data.visibility = View.GONE
                rvLikes.visibility = View.VISIBLE
                if (rvLikes.itemDecorationCount == 0) {
                    rvLikes.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
//            val items1: ArrayList<CommentsModel> = ArrayList()
                momentLikes.forEach { i ->
                    val models = CommentsModel()
                    models.commenttext = i.user.fullName
                    models.uid = i.user.id
                    models.userurl = i.user.avatar?.url
                    momentLikesUsers.add(models)
                }

//                if (momentLikeUserAdapters != null) {
//                    momentLikeUserAdapters?.notifyDataSetChanged()
//                } else {
                momentLikeUserAdapters =
                    StoryLikesAdapter(
                        requireActivity(),
                        momentLikesUsers,
                        Glide.with(requireContext())
                    )
                rvLikes.adapter = momentLikeUserAdapters
                momentLikeUserAdapters?.notifyDataSetChanged()
                momentLikeUserAdapters?.userProfileClicked {
                    LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    LogUtil.debug("UserStoryDetailsFragment : : : $it")
                    val bundle = Bundle()
                    bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
                    bundle.putString("userId", it.uid)
                    if (userId == it.uid) {
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
                rvLikes.layoutManager = LinearLayoutManager(activity)
//                }
            } else {
                no_data.visibility = View.VISIBLE
                rvLikes.visibility = View.GONE
            }
            if (LikebottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                Log.d("UserStoryDetails", "STATE_EXPANDED")
            } else {
                LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                Log.d("UserStoryDetails", "STATE_COLLAPSED")
                likeMomentItemPK = ""

//                momentLikesUsers.clear()
//                momentLikeUserAdapters?.addAll(momentLikesUsers)
            }
        }
    }


    private fun getParticularMoments(pos: Int, ids: String) {
        Log.e("callPerticulareMoments", "callPerticulareMoments")

        lifecycleScope.launchWhenResumed {
            val res = try {
                Log.d("UMF", "getParticularMoments: $width $size $ids")
                apolloClient(requireContext(), userToken!!).query(
                    GetAllUserMomentsQuery(
                        width,
                        size,
                        1,
                        "",
                        ids
                    )
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponseException all moments ${e.message}")
                binding.root.snackbar(" ${e.message}")
                return@launchWhenResumed
            }
            val allmoments = res.data?.allUserMoments!!.edges
            allmoments.indices.forEach { i ->
                if (ids.equals(allmoments[i]!!.node!!.pk.toString())) {
//                    Log.d("UserMomentSubsc", "${allmoments[i]}")
                    Log.d("USMV", "getParticularMoments: ${mViewModel.userMomentsList[pos] == null}")
                    if (mViewModel.userMomentsList[pos] != null) {
                        mViewModel.userMomentsList[pos] = allmoments[i]!!
                        Log.e("callPerticulareMoments", "${mViewModel.userMomentsList.size}")
                        Log.e("obj_node", "submitList1 2084")
                        sharedMomentAdapter.submitList1(mViewModel.userMomentsList)
                    }
                    sharedMomentAdapter.notifyItemChanged(pos)
                    return@forEach
                }
            }
        }
    }

    fun fireLikeNotificationForReceiver(item: GetAllUserMomentsQuery.Edge) {
        lifecycleScope.launchWhenResumed {
            val queryName = "sendNotification"
            val query = StringBuilder()
                .append("mutation {")
                .append("$queryName (")
                .append("userId: \"${item.node!!.user!!.id}\", ")
                .append("notificationSetting: \"LIKE\", ")
                .append("data: {momentId:${item.node.pk}}")
                .append(") {")
                .append("sent")
                .append("}")
                .append("}")
                .toString()
            val result = provideGraphqlApi().getResponse<Boolean>(
                query,
                queryName, userToken
            )
            Timber.d("RSLT", "" + result.message)
        }
    }
    var alreadyFetching = false
    private fun getUserMomentNextPage(width: Int, size: Int, i: Int, endCursors: String) {
        userToken?.let {
            Log.d("UserMomentsFragment", "UserMomentNextPage Calling")
            mViewModel.getAllMoments(requireContext(), it, width, size, i, endCursors) { error ->
                alreadyFetching = false
                if (error == null) {
                    activity?.runOnUiThread {
                        //allUserMoments2.addAll(mViewModel.userMomentsList)
                        //sharedMomentAdapter.setData(mViewModel.userMomentsList)
                        Log.d("UserMomentSubsc", "${mViewModel.userMomentsList}")
                        allUserMomentsNew = mViewModel.userMomentsList
                        Log.e("obj_node", "submitList1 2126")
                        sharedMomentAdapter.submitList1(mViewModel.userMomentsList)
                    }
                    hasNextPage = mViewModel.hasNextPageN
                    endCursor = mViewModel.endCursorN
                } else {

                }
            }
        }
        /* mViewModel.arrayListLiveData.observe(viewLifecycleOwner) { arrayList ->
             allUserMoments2.addAll(arrayList)
             sharedMomentAdapter.setData(allUserMoments2)
         }*/
    }

    fun allusermoments1(width: Int, size: Int, i: Int, endCursors: String) {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(
                    GetAllUserMomentsQuery(
                        width,
                        size,
                        i,
                        endCursors,
                        ""
                    )
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponseException all moments ${e.message}")
                binding.root.snackbar(" ${e.message}")
                return@launchWhenResumed
            }

            val allusermoments = res.data?.allUserMoments!!.edges

            endCursor = res.data?.allUserMoments!!.pageInfo.endCursor!!
            hasNextPage = res.data?.allUserMoments!!.pageInfo.hasNextPage

            if (allusermoments.size != 0) {
                val allUserMomentsNext: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()

                allusermoments.indices.forEach { i ->
                    allUserMomentsNext.add(allusermoments[i]!!)
                }
                sharedMomentAdapter.addAll(allUserMomentsNext)
            }
            if (binding.rvSharedMoments.itemDecorationCount == 0) {
                binding.rvSharedMoments.addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.top = 15
                    }
                })
            }
            if (allusermoments.size > 0) {
                Timber.d("apolloResponse: ${allusermoments.get(0)?.node!!.file}")
                Timber.d("apolloResponse: ${allusermoments.get(0)?.node!!.id}")
                Timber.d("apolloResponse: ${allusermoments.get(0)?.node!!.createdDate}")
                Timber.d("apolloResponse: ${allusermoments.get(0)?.node!!.momentDescriptionPaginated}")
                Timber.d("apolloResponse: ${allusermoments.get(0)?.node!!.user?.fullName}")
            }
        }
    }

    override fun onCommentofMomentClick(
        position: Int, item: GetAllUserMomentsQuery.Edge?
    ) {
        val bundle = Bundle().apply {
            putString("momentID", item?.node!!.pk.toString())
            putInt("itemPosition", position)
            putString("filesUrl", item.node.file!!)
            putString("Likes", item.node.like!!.toString())
            putString("Comments", item.node.comment!!.toString())
            val gson = Gson()
            putString("Desc", gson.toJson(item.node.momentDescriptionPaginated))
            putString("fullnames", item.node.user!!.fullName)
            if (item.node.user.gender != null) {
                putString("gender", item.node.user.gender.name)
            } else {
                putString("gender", null)
            }
            putString("momentuserID", item.node.user.id.toString())
        }
        navController.navigate(R.id.momentsAddCommentFragment, bundle)
    }

    override fun onMomentGiftClick(position: Int, item: GetAllUserMomentsQuery.Edge?) {
//        var bundle = Bundle()
//        bundle.putString("userId", userId)
//        navController.navigate(R.id.action_userProfileFragment_to_userGiftsFragment,bundle)
        if (userId!! != item!!.node!!.user!!.id) {
            giftUserid = item.node!!.user!!.id.toString()
            binding.sendgiftto.text =
                context?.resources?.getString(R.string.send_git_to) + " " + item.node.user!!.fullName
            if (GiftbottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            buttonSlideUp.text = "Slide Down";
            } else {
                GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//            buttonSlideUp.text = "Slide Up"
            }
        } else {
            //TODO: show receive gifts
            /*Toast.makeText(
                requireContext(),
                getString(R.string.user_cant_bought_gift_yourseld),
                Toast.LENGTH_LONG
            ).show()*/
            if (receivedGiftbottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                receivedGiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                receivedGiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    private fun deleteConfirmation(item: GetAllUserMomentsQuery.Edge?) {

        val dialogLayout = layoutInflater.inflate(R.layout.dialog_delete, null)
        val headerTitle = dialogLayout.findViewById<TextView>(R.id.header_title)
        val noButton = dialogLayout.findViewById<TextView>(R.id.no_button)
        val yesButton = dialogLayout.findViewById<TextView>(R.id.yes_button)

        headerTitle.text = "${AppStringConstant(getMainActivity()).are_you_sure_you_want_to_delete_moment}"
        noButton.text = "${AppStringConstant(getMainActivity()).no}"
        yesButton.text = "${AppStringConstant(getMainActivity()).yes}"

        val builder = AlertDialog.Builder(getMainActivity(),R.style.DeleteDialogTheme)
        builder.setView(dialogLayout)
        val dialog = builder.create()

        noButton.setOnClickListener {
            dialog.dismiss();
        }

        yesButton.setOnClickListener {
            dialog.dismiss();
            showProgressView()
            lifecycleScope.launchWhenResumed {
                val res = try {
                    apolloClient(
                        requireContext(),
                        userToken!!
                    ).mutation(DeletemomentMutation(item?.node!!.pk!!.toString()))
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponseException ${e.message}")
                    binding.root.snackbar(" ${e.message}")
                    hideProgressView()
                    return@launchWhenResumed
                }
                hideProgressView()
            }
        }

        dialog.show()
    }

    override fun onDotMenuofMomentClick(
        position: Int,
        item: GetAllUserMomentsQuery.Edge?, types: String
    ) {
        if (types == "delete") {

            deleteConfirmation(item)
//            showProgressView()
//            lifecycleScope.launchWhenResumed {
//                val res = try {
//                    apolloClient(
//                        requireContext(),
//                        userToken!!
//                    ).mutation(DeletemomentMutation(item?.node!!.pk!!.toString()))
//                        .execute()
//                } catch (e: ApolloException) {
//                    Timber.d("apolloResponseException ${e.message}")
//                    binding.root.snackbar(" ${e.message}")
//                    hideProgressView()
//                    return@launchWhenResumed
//                }
//                hideProgressView()
//                if (res.hasErrors()) {
//                    Log.d("UserMomentFragment", res.errors?.get(0)?.message.toString())
//                } else {
////                    val positionss = allUserMomentsNew.indexOf(item)
//                    allUserMomentsNew.remove(item)
//                    sharedMomentAdapter.notifyItemRemoved(position)
//                }
//            }
        } else if (types == "report") {
            reportDialog(item)
        } else if (types == "edit") {
            val sb = StringBuilder()
            item?.node?.momentDescriptionPaginated?.forEach { sb.append(it) }
            val desc = sb.toString().replace("", "")
            val args = Bundle()
            args.putString("moment_desc", desc)
            args.putInt("moment_pk", item?.node?.pk ?: -1)
            findNavController().navigate(
                destinationId = R.id.userMomentUpdateFragment,
                popUpFragId = null,
                animType = AnimationTypes.SLIDE_ANIM,
                inclusive = true,
                args = args
            )
        }
    }

    private fun reportDialog(item: GetAllUserMomentsQuery.Edge?) {

        val dialogLayout = layoutInflater.inflate(R.layout.dialog_report, null)
        val reportView = dialogLayout.findViewById<TextView>(R.id.report_view)
        val reportMessage = dialogLayout.findViewById<EditText>(R.id.report_message)
        val okButton = dialogLayout.findViewById<TextView>(R.id.ok_button)
        val cancleButton = dialogLayout.findViewById<TextView>(R.id.cancel_button)

//        reportView.text = "${AppStringConstant(getMainActivity()).are_you_sure_you_want_to_delete_story}"
        okButton.text = "${AppStringConstant(getMainActivity()).ok}"
        cancleButton.text = "${AppStringConstant(getMainActivity()).cancel}"

        val builder = AlertDialog.Builder(getMainActivity(),R.style.DeleteDialogTheme)
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
                    ).mutation(ReportonmomentMutation(item?.node!!.pk!!.toString(), message))
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponse Exception ${e.message}")
                    binding.root.snackbar(" ${e.message}")
                    hideProgressView()
                    dialog.dismiss()
                    return@launchWhenResumed
                }

                if (res.hasErrors()) {

                } else {

                }
                hideProgressView()
                dialog.dismiss()
            }
        }

        cancleButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onMoreShareMomentClick() {

    }

    //    private fun showLikeBottomSheet(item: GetAllUserMomentsQuery.Edge?) {
    private fun showLikeBottomSheet() {
//        Log.i(TAG, "showLikeBottomSheet: UserId: ${item?.node?.pk}")
        val likebottomSheet = binding.likebottomSheet
        LikebottomSheetBehavior = BottomSheetBehavior.from(likebottomSheet)
        LikebottomSheetBehavior.setBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        Timber.d("STATE_COLLAPSED")
                    }

                    BottomSheetBehavior.STATE_HIDDEN -> {
                        Timber.d("STATE_HIDDEN")
                    }

                    BottomSheetBehavior.STATE_EXPANDED -> {
                        Timber.d("STATE_EXPANDED")
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

    fun getMainActivity() = activity as MainActivity

    fun momentSharing(filePath: File?, description: String?, commentAllowed: Boolean) {
        binding.imageviewSharing.loadImage(filePath!!)
        binding.momentSharing.text = "Uploading..."
        lifecycleScope.launchWhenCreated {

            val f = filePath!!
            val buildder = DefaultUpload.Builder()
            buildder.contentType("Content-Disposition: form-data;")
            buildder.fileName(f.name)
            val upload = buildder.content(f).build()
            Timber.d("filee ${f.exists()}")
            val userToken = getCurrentUserToken()!!

            Timber.d("useriddd ${mUser?.id}")
            if (mUser?.id != null) {
                val response = try {

                    apolloClient(context = requireContext(), token = userToken).mutation(
                        MomentMutation(
                            file = upload,
                            detail = description!!,
                            userField = mUser?.id!!,
                            allowComment = commentAllowed
                        )
                    ).execute()

                } catch (e: ApolloException) {
                    hideProgressView()
                    Timber.d("filee Apollo Exception ApolloException ${e.message}")
                    binding.root.snackbar(" ${e.message}")
                    return@launchWhenCreated
                } catch (e: Exception) {
                    hideProgressView()
                    Timber.d("filee General Exception ${e.message} $userToken")
                    binding.root.snackbar(" ${e.message}")
                    return@launchWhenCreated
                }
                Log.e("222", "--->" + Gson().toJson(response))
                hideProgressView()

                if (response.hasErrors()) {
                    if (response.errors?.get(0)?.message!!.contains("purchase a package", true)) {
                        binding.root.snackbarOnTop(
                            "${response.errors?.get(0)?.message}",
                            Snackbar.LENGTH_INDEFINITE,
                            callback = { findNavController().navigate(R.id.action_global_plan) })
                    }
                    else {
                        binding.root.snackbar("${response.errors?.get(0)?.message}")
                    }
                } else {
                    binding.root.snackbar("New Moment Shared")
                    binding.llSharing.visibility = View.GONE
                }
            } else {

            }
            binding.llSharing.visibility = View.GONE
        }
    }


    private fun scheduleMomentForLater(filePath: File?, description: String?, commentAllowed: Boolean, publishAt: String) {
        binding.imageviewSharing.loadImage(filePath!!)
        binding.momentSharing.text = "Uploading..."
        lifecycleScope.launchWhenCreated {

            val f = filePath!!
            val buildder = DefaultUpload.Builder()
            buildder.contentType("Content-Disposition: form-data;")
            buildder.fileName(f.name)
            val upload = buildder.content(f).build()
            Timber.d("filee ${f.exists()}")
            val userToken = getCurrentUserToken()!!

            Timber.d("useriddd ${mUser?.id}")
            if (mUser?.id != null) {
                val response = try {

                    apolloClient(context = requireContext(), token = userToken).mutation(
                        ScheduleMomentMutation(
                            file = upload,
                            detail = description!!,
                            userField = mUser?.id!!,
                            allowComment = commentAllowed,
                            publishAt = publishAt
                        )
                    ).execute()

                } catch (e: ApolloException) {
                    hideProgressView()
                    Timber.d("filee Apollo Exception ApolloException ${e.message}")
                    binding.root.snackbar(" ${e.message}")
                    return@launchWhenCreated
                } catch (e: Exception) {
                    hideProgressView()
                    Timber.d("filee General Exception ${e.message} $userToken")
                    binding.root.snackbar(" ${e.message}")
                    return@launchWhenCreated
                }
                Log.e("222", "--->" + Gson().toJson(response))
                hideProgressView()

                if (response.hasErrors()) {
                    if (response.errors?.get(0)?.message!!.contains("purchase a package", true)) {
                        binding.root.snackbarOnTop(
                            "${response.errors?.get(0)?.message}",
                            Snackbar.LENGTH_INDEFINITE,
                            callback = { findNavController().navigate(R.id.action_global_plan) })
                    }
                    else {
                        binding.root.snackbar("${response.errors?.get(0)?.message}")
                    }
                } else {
                    binding.root.snackbar("New Moment scheduled for later")
                    binding.llSharing.visibility = View.GONE
                }
            } else {

            }
            binding.llSharing.visibility = View.GONE
        }
    }

}


