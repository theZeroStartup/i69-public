package com.i69.profile

//import androidx.lifecycle.ViewModelProviders
import android.R
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.apollographql.apollo3.exception.ApolloException
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textview.MaterialTextView
import com.google.gson.Gson
import com.i69.BuildConfig
import com.i69.GetNotificationCountQuery
import com.i69.GetUserMomentsQuery
import com.i69.UpdateCoinMutation
import com.i69.UserSubscriptionQuery
import com.i69.applocalization.AppStringConstant
import com.i69.applocalization.AppStringConstant1
import com.i69.applocalization.AppStringConstantViewModel
import com.i69.applocalization.getLoalizations
import com.i69.data.config.Constants
import com.i69.data.models.BlockedUser
import com.i69.data.models.Photo
import com.i69.data.models.User
import com.i69.databinding.AlertFullImageBinding
import com.i69.databinding.FragmentUserProfileBinding
import com.i69.gifts.FragmentReceivedGifts
import com.i69.gifts.FragmentSentGifts
import com.i69.profile.vm.VMProfile
import com.i69.ui.adapters.ImageSliderAdapter
import com.i69.ui.adapters.StoryLikesAdapter
import com.i69.ui.adapters.UserItemsAdapter
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.SplashActivity
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.screens.main.notification.NotificationDialogFragment
import com.i69.ui.screens.main.search.userProfile.SearchUserProfileFragment
import com.i69.ui.screens.main.search.userProfile.getimageSliderIntent
import com.i69.ui.viewModels.CommentsModel
import com.i69.ui.viewModels.UserMomentsModelView
import com.i69.utils.*
import com.paypal.pyplcheckout.sca.runOnUiThread
import com.synnapps.carouselview.ViewListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.math.roundToInt


@AndroidEntryPoint
class UserProfileFragment : BaseFragment<FragmentUserProfileBinding>(), OnPageChangeListener {
    private var userToken: String? = null
    private var userId: String? = null
    private var userFulNAme: String? = ""
    private var userData: User? = null
    var scrollPos = 0
    var userAvatarSize = 0
    private lateinit var GiftbottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var WalletGiftbottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var LikebottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var momentLikesAdapters: StoryLikesAdapter
    var fragReceivedGifts: FragmentReceivedGifts? = null
    var fragSentGifts: FragmentSentGifts? = null
    lateinit var adapter: ImageSliderAdapter

    var currentUserLikes: ArrayList<BlockedUser> = ArrayList()

    private val tabIcons = intArrayOf(
        com.i69.R.drawable.pink_gift_noavb,
        com.i69.R.drawable.pink_gift_noavb
    )
    private val viewModel: VMProfile by activityViewModels()
    private val mViewModel: UserMomentsModelView by activityViewModels()

    var width = 0
    var size = 0

    //    private val viewModel by lazy {
//        activity?.let { ViewModelProviders.of(it).get(VMProfile::class.java) }
//    }
    private val viewStringConstModel: AppStringConstantViewModel by activityViewModels()

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentUserProfileBinding.inflate(inflater, container, false).apply {
            viewModel.isMyUser = true
            this.vm = viewModel
            this.stringConstant = AppStringConstant(requireContext())

        }

    private val addSliderImageIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            Timber.d("RESULT $result")
        }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val config: Configuration = resources.configuration
        if (config.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            //in Right To Left layout
            binding.ownProfileLayout.llName.layoutDirection = View.LAYOUT_DIRECTION_RTL
            // toast("View is RTL")
        }
        //else
        //toast("View is LTR")

        val displayMetrics = DisplayMetrics()
        width = displayMetrics.widthPixels
        val densityMultiplier =getResources().getDisplayMetrics().density;
        val scaledPx = 14 * densityMultiplier;
        val paint = Paint()
        paint.setTextSize(scaledPx);
        size = paint.measureText("s").roundToInt();

    }

    private fun redirectVisitirPage() {

        var bundle = Bundle()
        bundle.putString("userId", userId)
        bundle.putString("userFulNAme", userFulNAme)

        findNavController().navigate(
            destinationId = com.i69.R.id.action_global__to_fragment_visitor,
            popUpFragId = null,
            animType = AnimationTypes.SLIDE_ANIM,
            inclusive = true,
            args = bundle
        )
    }

    private fun redirectToFolllowingPage() {

        var bundle = Bundle()
        bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
        bundle.putString("userId", userId)
        bundle.putString("userFulNAme", userFulNAme)

        findNavController().navigate(
            destinationId = com.i69.R.id.action_global__to_fragment_follower,
            popUpFragId = null,
            animType = AnimationTypes.SLIDE_ANIM,
            inclusive = true,
            args = bundle
        )
    }

    private fun getAllUserMoments(width: Int, size: Int, data: VMProfile.DataCombined) {
        Log.d("MFR", "getAllUserMoments: $width $size")

        lifecycleScope.launch {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(
                    GetUserMomentsQuery(width,size,10,"", userId.toString(),"")
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloException currentUserMoments ${e.message}")
                Log.e("getAllUserMoments", "${e.message}")

                return@launch
            }

            var isUserHasMoments = false

            val allmoments = res.data?.allUserMoments?.edges
            if(!allmoments.isNullOrEmpty()) {
                for (item in allmoments) {
                    Log.d("MFR", "getAllUserMoments: ${item?.node?.user?.id} ${userId}")
                    if (item?.node?.user?.id.toString() == userId){
                        isUserHasMoments = true
                        break
                    }
                }

                finalizeViewPagerSetup(isUserHasMoments, data)
            }
            else finalizeViewPagerSetup(false, data)
        }
    }

    private fun finalizeViewPagerSetup(userHasMoments: Boolean, data: VMProfile.DataCombined) {
        binding.profileTabs.setupWithViewPager(binding.userDataViewPager)
        binding.userDataViewPager.isSaveEnabled = false;
        binding.userDataViewPager.adapter =
            viewModel.setupViewPager(
                childFragmentManager,
                data.user,
                data.defaultPicker,
                requireContext(),
                userHasMoments
            )
        binding.userDataViewPager.offscreenPageLimit = 3
    }

    fun updateLanguageTranslation() {
        val sharedPref = SharedPref(requireContext())
        var consted = sharedPref.getAttrTranslater()
        if (consted == null) {
            consted = getLoalizations(requireContext(), isUpdate = true)
        }
        viewStringConstModel.data.postValue(consted)

    }


    override fun setupTheme() {
        showProgressView()
        navController = findNavController()
//        updateLanguageTranslation()
        Log.d("UPFrag", "setupTheme: ")
        binding.userDataViewPager.setViewGone()
        viewStringConstModel.data.observe(this@UserProfileFragment) { data ->
            binding.stringConstant = data
            Log.e("MYValuesAreChanged", "${data.feed}")
            Log.e("MYValuesAreChanged", "${data.wallet}")
            viewModel.viewStringConstModel = data
        }


        Log.e("callTranslation", "AppStr-Feed=User=>" + AppStringConstant1.feed)
        Log.e("callTranslation", "AppStr-Feed=User=>" + AppStringConstant1.wallet)
        viewStringConstModel.data?.also {
            Log.d("UPFrag", "setupTheme: ${it.value}")
            binding.stringConstant = it.value
//            Log.e("MydataBasesss", it.value!!.messages)
        }
//        if (TempConstants.LanguageChanged) {
//            navController.navigate(com.i69.R.id.action_userProfileFragment_to_userEditProfileFragment)
//        }

        binding.userBaseInfo.visibility = View.GONE
        binding.otherProfileLayout.rvOtherprofile.visibility = View.GONE

//        binding.ownProfileLayout.currentUserNotSubScribe.visibility = View.VISIBLE
//        binding.ownProfileLayout.llNotSubScribeCoinVallet.visibility = View.VISIBLE
//        binding.ownProfileLayout.llButtonSubscribe.visibility = View.VISIBLE
//
//
//        binding.ownProfileLayout.tvuserActiveSubscription.visibility = View.GONE
//        binding.ownProfileLayout.userSubScribeCoinWallet.visibility = View.GONE


        binding.ownProfileLayout.currentUserNotSubScribe.visibility = View.VISIBLE
        binding.ownProfileLayout.lyNotSubScribeCoinVallet.visibility = View.GONE
        binding.ownProfileLayout.llNotSubScribeCoinVallet.visibility = View.GONE
        binding.ownProfileLayout.llButtonSubscribe.visibility = View.GONE


        binding.ownProfileLayout.llButtonSubscribedPackage.visibility = View.VISIBLE
        binding.ownProfileLayout.tvuserActiveSubscription.visibility = View.VISIBLE
        binding.ownProfileLayout.lyUserSubScribeCoinWallet.visibility = View.VISIBLE
        binding.ownProfileLayout.userSubScribeCoinWallet.visibility = View.VISIBLE

        binding.ownProfileLayout.llButtonSubscribe.setOnClickListener {
            noActiveSubScription()
        }

        binding.ownProfileLayout.tvuserActiveSubscription.setOnClickListener {
            userSubScription(true)
        }



        binding.followerLayout.btnFollowing.setOnClickListener {
            redirectToFolllowingPage()
        }

        binding.followerLayout.btnFollower.setOnClickListener {
            redirectToFolllowingPage()
        }

        binding.followerLayout.btnVisitor.setOnClickListener {
            redirectVisitirPage()
        }

        lifecycleScope.launch {
            userToken = getCurrentUserToken()!!
            userId = getCurrentUserId()!!

            Timber.i("usertokenn $userToken")
//        }
            Timber.i("usertokenn 2 $userToken")

            getTypeActivity<MainActivity>()?.enableNavigationDrawer()
            // binding.ownProfileLayout.initChatMsgBtn.visibility = View.GONE
            // binding.ownProfileLayout.lytCoinsGifts.visibility = View.VISIBLE
            binding.actionGifts1.visibility = View.VISIBLE
            //binding.actionGifts.visibility = View.GONE
            binding.ownProfileLayout.actionCoins.visibility = View.VISIBLE

//        subscribeonUpdatePrivatePhotoRequest()

            Log.e("callUserProfile1", "callUserProfile1")
            viewModel.getProfile(userId) {
                Log.d("UPFrag", "datareceived: ")
                Handler(Looper.getMainLooper()).postDelayed({
                    runOnUiThread {
                        if (view != null)
                            binding.userDataViewPager.setViewVisible()
                    }
                }, 750)
            }


//        binding.actionCoins.setOnClickListener {
//
//            userCoinDetail()
//        }

            viewModel.data.observe(this@UserProfileFragment) { data ->
                Timber.d("observe: $data")

                if (data == null) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(context, SplashActivity::class.java)
                        startActivity(intent)
                        requireActivity().finishAffinity()
                    }
                }

                if (data != null) {
                    if (data.user != null) {
                        userFulNAme = data.user!!.fullName!!
                        userData = data.user
                        Log.e("inprofilename", userFulNAme!!)
                        val ava = Gson().toJson(data.user!!)
                        Log.e("inprofilusername", ava.toString())//data.user!!.username!!)
//                        userFulNAme =   data.user!!.username!!
                        if (data.user!!.avatarPhotos != null && data.user!!.avatarPhotos!!.size != 0) {
                            binding.userImgHeader.setIndicatorVisibility(View.GONE)
                            adapter = fragmentManager?.let {
                                ImageSliderAdapter(
                                    it,
                                    data.user!!.avatarPhotos!!
                                )
                            }!!
                            binding.container.adapter = adapter

                            binding.recyclerTabLayout.setupWithViewPager(binding.container, true)
                            binding.container.currentItem = scrollPos
                            userAvatarSize = data.user!!.avatarPhotos!!.size

                            if (data.user!!.giftCoins <= 0) {
                                binding.giftCounter.visibility = View.GONE
                                //binding.bell.setImageDrawable(ContextCompat.getDrawable(requireContext(), com.i69app.R.drawable.notification1))
                            } else {
                                binding.giftCounter.visibility = View.VISIBLE
                                binding.giftCounter.text = "${data.user!!.giftCoins}"
                                //binding.bell.setImageDrawable(ContextCompat.getDrawable(requireContext(), com.i69app.R.drawable.notification2))
                            }

                            binding.userImgHeader.addOnPageChangeListener(this@UserProfileFragment)

                            try {
                                binding.userImgHeader.setViewListener(ViewListener {


                                    var view: View = layoutInflater.inflate(
                                        com.i69.R.layout.custom_imageview,
                                        null
                                    )

                                    try {
                                        var pos = it
                                        var imageView =
                                            view.findViewById<ImageView>(com.i69.R.id.userIv)
                                        if (pos <= data.user!!.avatarPhotos!!.size) {
                                            data.user?.avatarPhotos?.get(pos)?.let { avatar ->

                                                val url = if (!BuildConfig.USE_S3) {
                                                    if (avatar?.url.toString().startsWith(BuildConfig.BASE_URL))
                                                        avatar?.url.toString()
                                                    else
                                                        "${BuildConfig.BASE_URL}${avatar?.url.toString()}"
                                                }
                                                else if (avatar?.url.toString().startsWith(ApiUtil.S3_URL)) avatar?.url.toString()
                                                else ApiUtil.S3_URL.plus(avatar?.url.toString())

                                                imageView.loadImage(
                                                    url
                                                )

//                                                val background: Drawable =
//                                                    imageView.getBackground()
//                                                if (background is ColorDrawable) {
//                                                    val color = (background as ColorDrawable).color
//                                                    val invertedColor = color.inv()
//                                                    binding.textFlag1.setTextColor(invertedColor)
//                                                    // Use color here
//                                                }
                                            }
                                        }
                                        imageView.setOnClickListener {

                                            if (data.user!!.avatarPhotos != null && data.user!!.avatarPhotos!!.size != 0) {

                                                val dataarray: ArrayList<Photo> = ArrayList()
                                                data.user!!.avatarPhotos!!.indices.forEach { i ->

                                                    val photo_ = data.user!!.avatarPhotos!![i]
                                                    dataarray.add(photo_)
                                                }
                                                addSliderImageIntent.launch(
                                                    getimageSliderIntent(
                                                        requireActivity(),
                                                        Gson().toJson(dataarray),
                                                        pos
                                                    )
                                                )
                                            }

                                        }
                                    } catch (e: Exception) {
                                        Timber.d(e.message)
                                    }




                                    view
                                })



                                binding.userImgHeader.pageCount = data.user!!.avatarPhotos!!.size

                            } catch (e: Exception) {
                            }
                        } else {

                            for (f in 0 until binding.userImgHeader.pageCount) {

                                binding.userImgHeader.removeViewAt(f)
                                binding.userImgHeader.setIndicatorVisibility(View.GONE)

                            }
                        }
                    }
                }

                if (data != null) {
                    if (data.user != null) {
                        if (data.user!!.avatarPhotos != null) {

                            if (data.user?.avatarPhotos?.size != 0 && data.user?.avatarIndex != null) {
                                if (data.user!!.avatarPhotos?.size!! > data.user?.avatarIndex!!) {
                                    binding.userProfileImg.loadCircleImage(
                                        data.user!!.avatarPhotos?.get(
                                            data.user?.avatarIndex!!
                                        )?.url?.replace(
                                            "${BuildConfig.BASE_URL_REP}media/",
                                            "${BuildConfig.BASE_URL}media/"
                                        ).toString()
                                    )
                                }
                            }
                        }
                        try {
                            binding.textFlag1.setText(data.user!!.city + ", " + data.user!!.countryCode)
                            binding.imgFlag.loadImage(data.user!!.countryFlag.toString())

                            Timber.d("Flag Image " + data.user!!.countryFlag)
                        } catch (e: Exception) {
                            Timber.d(e.message)
                        }
                    }
                }
                if (data != null) {
                    if (data.user != null) {
                        getAllUserMoments(width, size, data)
                    }
                }

//            binding.userImgHeader.pageCount = data?.user?.avatarPhotos?.size ?: 1

            }

            viewModel.removeMomentFromUserFeed.observe(this@UserProfileFragment) {
                viewModel.data.value?.let { it1 -> finalizeViewPagerSetup(false, it1) }
            }
        }
        userSubScription()
        showLikeBottomSheet()
        //adjustScreen()
        GiftbottomSheetBehavior =
            BottomSheetBehavior.from<ConstraintLayout>(binding.giftbottomSheet)
        GiftbottomSheetBehavior.addBottomSheetCallback(object :
//        )
//        GiftbottomSheetBehavior.setBottomSheetCallback(object :
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

        WalletGiftbottomSheetBehavior =
            BottomSheetBehavior.from<ConstraintLayout>(binding.walletGiftbottomSheet)
        WalletGiftbottomSheetBehavior.addBottomSheetCallback(object :
//        )
//        GiftbottomSheetBehavior.setBottomSheetCallback(object :
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

        binding.linearLayoutUserLikes.setOnClickListener {
            loadLikesDialog()
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
        binding.giftsTabs.tabIconTint = null
        binding.giftsTabs.getTabAt(0)!!.setIcon(com.i69.R.drawable.pink_gift_noavb)
        binding.giftsTabs.getTabAt(1)!!.setIcon(com.i69.R.drawable.pink_gift_noavb)
        binding.giftsTabs.addOnTabSelectedListener(
//        binding.giftsTabs.setOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {

                    // no where in the code it is defined what will happen when tab is tapped/selected by the user
                    // this is why the following line is necessary
                    // we need to manually set the correct fragment when a tab is selected/tapped
                    // and this is the problem in your code

                    if (tab.position == 0) {
//                        binding.unametitle.text =
//                            "${requireActivity().resources.getString(com.i69app.R.string.sender)}"
                        binding.unametitle.text =
                            "${AppStringConstant1.sender}"
                    } else if (tab.position == 1) {
                        binding.unametitle.text = AppStringConstant1.beneficiary_name
//                        binding.unametitle.text = getString(com.i69app.R.string.beneficiary_name)

                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {


                    // Reload your recyclerView here
                }
            })


        binding.giftsTabs1.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {
                binding.giftsPager1.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
            }
        })
        binding.giftsTabs1.setupWithViewPager(binding.giftsPager1)
        setupReceivedGiftViewPager(binding.giftsPager1)
        binding.giftsTabs1.tabIconTint = null
        binding.giftsTabs1.getTabAt(0)!!.setIcon(tabIcons[0])
//        binding.giftsTabs1.getTabAt(1)!!.setIcon(tabIcons[1])
        binding.giftsTabs1.addOnTabSelectedListener(
//        binding.giftsTabs.setOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {

                    // no where in the code it is defined what will happen when tab is tapped/selected by the user
                    // this is why the following line is necessary
                    // we need to manually set the correct fragment when a tab is selected/tapped
                    // and this is the problem in your code

                    if (tab.position == 0) {
//                        binding.unametitle.text =
//                            "${requireActivity().resources.getString(com.i69app.R.string.sender)}"
                        binding.unametitle1.text =
                            "${AppStringConstant1.sender}"
                    } else if (tab.position == 1) {
                        binding.unametitle1.text = AppStringConstant1.beneficiary_name
//                        binding.unametitle.text = getString(com.i69app.R.string.beneficiary_name)

                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {


                    // Reload your recyclerView here
                }
            })

        Handler(Looper.getMainLooper()).post { hideProgressView() }

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
                MainActivity.getMainActivity()?.binding?.bottomNavigation?.selectedItemId =
                    com.i69.R.id.nav_user_profile_graph
            } else {
                findNavController().navigate(
                    destinationId = com.i69.R.id.action_global_otherUserProfileFragment,
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


    private fun loadLikesDialog() {
        currentUserLikes.clear()
        userData?.likes?.let { currentUserLikes.addAll(it) }
//        showLikeBottomSheet()
//        val dialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
//        val view =
//            LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_layout2, null)
//        dialog.setContentView(view)

//        val rvLikes = binding.rvLikes
//        val no_data = binding.noDatas
//        val txtHeaderLike = binding.txtheaderlike
//
        binding.txtheaderlike.text = currentUserLikes.size.toString() + " Likes"

        if (currentUserLikes.isNotEmpty()) {
            binding.noDatas.visibility = View.GONE
            binding.rvLikes.visibility = View.VISIBLE

            val items1: ArrayList<CommentsModel> = ArrayList()
            currentUserLikes.forEach { i ->
                val models = CommentsModel()
                models.commenttext = i.fullName
                models.uid = i.id
                models.userurl = userData?.avatarIndex?.let {
                    if (i.avatarPhotos?.size!! > 0)
                        i.avatarPhotos?.get(it)?.url
                }.toString()
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
        } else {
            LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    fun userSubScription(isOpenDialog: Boolean = false) {

//        showProgressView()
        lifecycleScope.launchWhenResumed {

            val userToken = getCurrentUserToken()!!
            val response = try {
                apolloClient(requireContext(), userToken).query(
                    UserSubscriptionQuery()
                ).execute()

            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                hideProgressView()
                if (!isOpenDialog) {
                    binding.ownProfileLayout.currentUserNotSubScribe.visibility = View.VISIBLE
                    binding.ownProfileLayout.lyNotSubScribeCoinVallet.visibility = View.VISIBLE
                    binding.ownProfileLayout.llNotSubScribeCoinVallet.visibility = View.VISIBLE
                    binding.ownProfileLayout.llButtonSubscribe.visibility = View.VISIBLE


                    binding.ownProfileLayout.llButtonSubscribedPackage.visibility = View.GONE
                    binding.ownProfileLayout.tvuserActiveSubscription.visibility = View.GONE
                    binding.ownProfileLayout.lyUserSubScribeCoinWallet.visibility = View.GONE
                    binding.ownProfileLayout.userSubScribeCoinWallet.visibility = View.GONE

                }
                return@launchWhenResumed
            }


            if (response.hasErrors()) {
                hideProgressView()
                val errorMessage = response.errors?.get(0)?.message
                Log.e("errorAllPackage", "$errorMessage")
                if (!isOpenDialog) {

                    binding.ownProfileLayout.currentUserNotSubScribe.visibility = View.VISIBLE
                    binding.ownProfileLayout.lyNotSubScribeCoinVallet.visibility = View.VISIBLE
                    binding.ownProfileLayout.llNotSubScribeCoinVallet.visibility = View.VISIBLE
                    binding.ownProfileLayout.llButtonSubscribe.visibility = View.VISIBLE


                    binding.ownProfileLayout.llButtonSubscribedPackage.visibility = View.GONE
                    binding.ownProfileLayout.tvuserActiveSubscription.visibility = View.GONE
                    binding.ownProfileLayout.lyUserSubScribeCoinWallet.visibility = View.GONE
                    binding.ownProfileLayout.userSubScribeCoinWallet.visibility = View.GONE

                }
                if (errorMessage != null) {
                    binding.root.snackbar(errorMessage)
                }
            } else {


                Log.e(
                    "userCurentSubScription",
                    Gson().toJson(response.data)
                )
                hideProgressView()
                if (isOpenDialog) {

                    activeSubScriptionDetail(response.data!!.userSubscription!!)

                } else {
                    if (response.data!!.userSubscription!!.`package` != null) {
                        binding.ownProfileLayout.tvuserActiveSubscription.text =
                            response.data!!.userSubscription!!.`package`!!.name

                        binding.ownProfileLayout.currentUserNotSubScribe.visibility = View.VISIBLE
                        binding.ownProfileLayout.lyNotSubScribeCoinVallet.visibility = View.GONE
                        binding.ownProfileLayout.llNotSubScribeCoinVallet.visibility = View.GONE
                        binding.ownProfileLayout.llButtonSubscribe.visibility = View.GONE


                        binding.ownProfileLayout.llButtonSubscribedPackage.visibility = View.VISIBLE
                        binding.ownProfileLayout.tvuserActiveSubscription.visibility = View.VISIBLE
                        binding.ownProfileLayout.lyUserSubScribeCoinWallet.visibility = View.VISIBLE
                        binding.ownProfileLayout.userSubScribeCoinWallet.visibility = View.VISIBLE
//    your_subscription
                    } else {


                        binding.ownProfileLayout.currentUserNotSubScribe.visibility = View.VISIBLE
                        binding.ownProfileLayout.lyNotSubScribeCoinVallet.visibility = View.VISIBLE
                        binding.ownProfileLayout.llNotSubScribeCoinVallet.visibility = View.VISIBLE
                        binding.ownProfileLayout.llButtonSubscribe.visibility = View.VISIBLE


                        binding.ownProfileLayout.llButtonSubscribedPackage.visibility = View.GONE
                        binding.ownProfileLayout.tvuserActiveSubscription.visibility = View.GONE
                        binding.ownProfileLayout.lyUserSubScribeCoinWallet.visibility = View.GONE
                        binding.ownProfileLayout.userSubScribeCoinWallet.visibility = View.GONE

                    }
                }
            }

        }

    }

    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = UserItemsAdapter(childFragmentManager)
        fragReceivedGifts = FragmentReceivedGifts()
        fragSentGifts = FragmentSentGifts()

//        adapter.addFragItem(fragReceivedGifts!!, getString(com.i69app.R.string.rec_gifts))
//        adapter.addFragItem(fragSentGifts!!, getString(com.i69app.R.string.sent_gifts))
//

        adapter.addFragItem(fragReceivedGifts!!, AppStringConstant1.rec_gifts)
        adapter.addFragItem(fragSentGifts!!, AppStringConstant1.sent_gifts)


        viewPager.adapter = adapter

//        viewPager.addOnPageChangeListener()
    }

    private fun setupReceivedGiftViewPager(viewPager: ViewPager) {
        val adapter = UserItemsAdapter(childFragmentManager)
        fragReceivedGifts = FragmentReceivedGifts()
//        adapter.addFragItem(fragReceivedGifts!!, getString(com.i69app.R.string.rec_gifts))
//        adapter.addFragItem(fragSentGifts!!, getString(com.i69app.R.string.sent_gifts))
//

        adapter.addFragItem(fragReceivedGifts!!, AppStringConstant1.rec_gifts)


        viewPager.adapter = adapter

//        viewPager.addOnPageChangeListener()
    }

    private fun showImageDialog(imageUrl: String) {

        val dialog =
            Dialog(requireContext(), R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen)
        val dialogBinding = AlertFullImageBinding.inflate(layoutInflater, null, false)
        dialogBinding.fullImg.loadImage(imageUrl) {
            dialogBinding.alertTitle.setViewGone()
        }
        dialog.setContentView(dialogBinding.root)
        dialog.show()
        dialogBinding.root.setOnClickListener {
            dialog.cancel()
        }
        dialogBinding.alertTitle.setViewGone()

    }

    private fun getNotificationIndex() {

        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetNotificationCountQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception NotificationIndex${e.message}")
                Log.e("getNotificatapolo", " ${e.message}")
                binding.root.snackbar(" ${e.message}")
                return@launchWhenResumed
            }
            Timber.d("apolloResponse NotificationIndex ${res.hasErrors()}")
            if (res.hasErrors()) {
                Log.e("getNotificErrors", " ${res.errors!!.get(0).toString()}")
                if (JSONObject(res.errors!!.get(0).toString()).getString("code")
                        .equals("InvalidOrExpiredToken")
                ) {
                    // error("User doesn't exist")

                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        requireActivity().finishAffinity()
                    }
                }
//                return@launchWhenResumed
            }
            val NotificationCount = res.data?.unseenCount
            if (NotificationCount == null || NotificationCount == 0) {
                binding.counter.visibility = View.GONE
                binding.bell.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        com.i69.R.drawable.notification1
                    )
                )

            } else {
                binding.counter.visibility = View.VISIBLE
//                binding.counter.setText("" + NotificationCount)
                if (NotificationCount > 10) {
                    binding.counter.text = "9+"
                } else {
                    binding.counter.text = "" + NotificationCount
                }
                binding.bell.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        com.i69.R.drawable.notification2
                    )
                )
            }
        }
    }

    override fun onResume() {
        getNotificationIndex()
        //getReceivedGiftIndex()
        super.onResume()
//        getNotificationIndex()
//        val intentFilter = IntentFilter().apply {
//            addAction("gift_Received")
//            addAction("com.my.app.onMessageReceived")
//        }
//
//        activity?.registerReceiver(broadCastReceiver, intentFilter)

//        val intentFilter = IntentFilter()
//        intentFilter.addAction("")
//        intentFilter.addAction("gift_Received_2")
//        intentFilter.addAction("gift_Received_3")
//        activity?.registerReceiver(broadCastReceiver, intentFilter)
        getMainActivity().setDrawerItemCheckedUnchecked(null)
        Log.e("Sent_message", "onReceive: ")
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            localBroadcastReceiver,
            IntentFilter(Constants.INTENTACTION)
        )
    }

    private val localBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            Log.e("Sent_message", "Broadcast received 22")
            Log.e("Sent_message", "Broadcast received 22" + intent!!.getStringExtra("extra"))


            val title = intent!!.getStringExtra("extra")
            if ((title!!.toLowerCase().contains("gifted coins deduction") || title!!.toLowerCase()
                    .contains("gifted coins added"))
                && intent!!.hasExtra("coins")
            ) {

                val coins = intent!!.getStringExtra("coins")
                Log.e("Sent_message coins", "$coins")

                val obj = JSONObject(coins)
//            viewModel.onCoins!!.invoke(obj.getJSONObject("data").getString("coins"))
                updateView("")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the local broadcast receiver
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(localBroadcastReceiver)
    }

    public fun updateView(state: String) {
        Log.e(":updateViewState", "$state")
        viewModel.getProfile(userId) {

        }
    }

//    private val broadCastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(contxt: Context?, intent: Intent?) {
//            Log.e("Sent_message", "Broadcast received")
//            val extras = intent?.extras
//            val state = extras!!.getString("extra")
//            val coins = intent!!.getStringExtra("coins")
//
//            if(intent!!.action.equals("gift_Received_3"))
//                Log.e("Sent_message", "onReceive2: "+coins)
//            if(intent!!.action.equals("gift_Received_2"))
//                Log.e("Sent_message", "onReceive2: "+coins )
//
////            val coins = intent!!.getStringExtra("coins")
////            Log.e("Sent_message", "onReceive: "+coins )
////            Log.e("TAG_Notification_rece", "onReceive: $state")
//
//            val obj = JSONObject(coins)
//            viewModel.onCoins!!.invoke(obj.getJSONObject("data").getString("coins"))

//            updateView(state.toString())
//        }
//    }

//    private fun getReceivedGiftIndex() {
//        lifecycleScope.launchWhenResumed {
//            val res = try {
//                apolloClient(
//                    requireContext(),
//                    userToken!!
//                ).query(GetUserReceiveGiftQuery(receiverId = userId!!))
//                    .execute()
//            } catch (e: ApolloException) {
//                Timber.d("apolloResponse ${e.message}")
//                binding.root.snackbar("Exception getGiftIndex ${e.message}")
//                return@launchWhenResumed
//            }
//            Timber.d("apolloResponse getGiftIndex ${res.hasErrors()}")
//
//            val receiveGiftList = res.data?.allUserGifts?.edges
//            res.data?.allUserGifts?.edges?.forEach { it ->
//                Timber.d("apolloResponse getGiftIndex ${it?.node?.gift?.giftName}")
//            }
//
//            if (receiveGiftList?.size == null || receiveGiftList.isEmpty()) {
//                binding.giftCounter.visibility = View.GONE
//                //binding.bell.setImageDrawable(ContextCompat.getDrawable(requireContext(), com.i69app.R.drawable.notification1))
//            } else {
//                binding.giftCounter.visibility = View.VISIBLE
//                binding.giftCounter.text = "${receiveGiftList.size}"
//                var totalCount = 0
//                receiveGiftList.forEach {
//                    totalCount += it?.node?.gift?.cost?.toInt()!!
//                }
//                updateCoins(totalCount)
//                //binding.bell.setImageDrawable(ContextCompat.getDrawable(requireContext(), com.i69app.R.drawable.notification2))
//            }
//        }
//    }

    private fun updateCoins(totalCount: Int) {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(
                    requireContext(),
                    userToken!!
                ).mutation(UpdateCoinMutation(id = userId!!, coins = totalCount))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception updateCoins${e.message}")
                binding.root.snackbar(" ${e.message}")
                return@launchWhenResumed
            }
            Timber.d("apolloResponse updateCoins ${res.hasErrors()}")

            val success = res.data?.updateCoin?.success
            Timber.d("apolloResponse updateCoins ${success}")
            if (success == true) {
                viewModel.getProfile {

                }
            }
        }
    }

    override fun setupClickListeners() {
        var bundle = Bundle()
        bundle.putString("userId", userId)
        viewModel.onSendMsg = { requireActivity().onBackPressed() }
        binding.actionBack.setOnClickListener {
            //requireActivity().onBackPressed()
            findNavController().popBackStack()
        }

        viewModel.onCoins = { userCoinDetail(it) }

        viewModel.onDrawer = { (activity as MainActivity).drawerSwitchState() }

        viewModel.onEditProfile =
            { navController.navigate(com.i69.R.id.action_userProfileFragment_to_userEditProfileFragment) }
        viewModel.onGift = {
            //Toast.makeText(activity,"User can't bought gift yourself", Toast.LENGTH_LONG).show()
            //Toast.makeText(activity,"User can't bought gift yourself", Toast.LENGTH_LONG).show()
//            setupViewPager(binding.giftsPager)
            binding.purchaseButton.visibility = View.GONE
            binding.topl.visibility = View.VISIBLE

            if (GiftbottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            buttonSlideUp.text = "Slide Down";

            } else {
                GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED;
//            buttonSlideUp.text = "Slide Up"
            }
            //navController.navigate(com.i69app.R.id.action_userProfileFragment_to_userGiftsFragment,bundle)
        }

        binding.ownProfileLayout.walletIcon.setOnClickListener {

//            setupReceivedGiftViewPager(binding.giftsPager1)
            binding.purchaseButton1.visibility = View.GONE
            binding.topl1.visibility = View.VISIBLE


            if (WalletGiftbottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                WalletGiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            buttonSlideUp.text = "Slide Down";

            } else {
                WalletGiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED;
//            buttonSlideUp.text = "Slide Up"
            }
        }

        binding.ownProfileLayout.walletIcon1.setOnClickListener {
//            setupReceivedGiftViewPager(binding.giftsPager1)
            binding.purchaseButton1.visibility = View.GONE
            binding.topl1.visibility = View.VISIBLE

            if (WalletGiftbottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                WalletGiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            buttonSlideUp.text = "Slide Down";

            } else {
                WalletGiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED;
//            buttonSlideUp.text = "Slide Up"
            }
        }

        binding.bell.setOnClickListener {
            val dialog =
                NotificationDialogFragment(userToken, binding.counter, userId, binding.bell)
            dialog.show(
                childFragmentManager,
                "${AppStringConstant1.comments}"
            )
        }
    }

    private fun adjustScreen() {
        var height = Utils.getScreenHeight()
        var calculated = (height * 70) / 100
        Timber.d("Height: $height Calculated 75%: $calculated")

        var params = binding.userCollapseToolbar.layoutParams
        params.height = calculated
        binding.userCollapseToolbar.layoutParams = params
    }


    private fun userCoinDetail(coins: String) {
        val bottomsheetDialog = BottomSheetDialog(requireContext())
//        val customView = layoutInflater.inflate(R.layout.dialog_entermobilenumber, null, false)
        val customView =
            layoutInflater.inflate(com.i69.R.layout.dialog_user_coin_option, null, false)

//        val title = customView.findViewById<MaterialTextView>(com.i69app.R.id.description)
        val tvUserBalance =
            customView.findViewById<MaterialTextView>(com.i69.R.id.tv_user_balance)
        val tvUserBalanceCoins =
            customView.findViewById<MaterialTextView>(com.i69.R.id.tv_user_balance_coin)

        val upgrade_button =
            customView.findViewById<MaterialTextView>(com.i69.R.id.upgrade_button)

//        val cdUserBalance = customView.findViewById<ConstraintLayout>(com.i69app.R.id.cd_user_balance)

        val cdUpgradeBalance =
            customView.findViewById<LinearLayout>(com.i69.R.id.cd_upgrade_balance)

        val imageCross =
            customView.findViewById<ImageView>(com.i69.R.id.iv_cross)

        val typeface_regular =
            Typeface.createFromAsset(activity?.assets, "fonts/poppins_semibold.ttf")
        val typeface_light = Typeface.createFromAsset(activity?.assets, "fonts/poppins_light.ttf")

        tvUserBalance.typeface = typeface_light;
        upgrade_button.typeface = typeface_regular;

//        tvUserBalance.text =
//            requireContext().resources.getString(com.i69app.R.string.text_user_balance)
//                .plus(" $coins")

        tvUserBalanceCoins.text = " $coins".plus("\nCoin")


        cdUpgradeBalance.setOnClickListener {
            bottomsheetDialog.dismiss()
            navigateToPurchase()
        }

        imageCross.setOnClickListener {
            bottomsheetDialog.dismiss()
        }

//        cdUserBalance.setOnClickListener {
//
//        }
        bottomsheetDialog.setContentView(customView)
        bottomsheetDialog.show()
    }


    private fun noActiveSubScription() {
        val bottomsheetDialog = BottomSheetDialog(requireContext())
//        val customView = layoutInflater.inflate(R.layout.dialog_entermobilenumber, null, false)
        val customView =
            layoutInflater.inflate(com.i69.R.layout.dialog_user_coin_option, null, false)

        val title = customView.findViewById<TextView>(com.i69.R.id.description)
        val iv_gpay = customView.findViewById<ImageView>(com.i69.R.id.iv_gpay)
        val tvUserBalance =
            customView.findViewById<MaterialTextView>(com.i69.R.id.tv_user_balance)

        val upgrade_button =
            customView.findViewById<MaterialTextView>(com.i69.R.id.upgrade_button)

        val cdUserBalance =
            customView.findViewById<ConstraintLayout>(com.i69.R.id.cd_user_balance)

        val cdUpgradeBalance =
            customView.findViewById<LinearLayout>(com.i69.R.id.cd_upgrade_balance)

        val imageCross =
            customView.findViewById<ImageView>(com.i69.R.id.iv_cross)

        val typeface_regular =
            Typeface.createFromAsset(activity?.assets, "fonts/poppins_semibold.ttf")
        val typeface_light = Typeface.createFromAsset(activity?.assets, "fonts/poppins_light.ttf")

        tvUserBalance.typeface = typeface_light;
        upgrade_button.typeface = typeface_regular;

//        cdUpgradeBalance.visibility= View.GONE
//        cdUserBalance.visibility= View.GONE

//        ball

        iv_gpay.setImageResource(com.i69.R.drawable.subscription)
        title.visibility = View.GONE
        tvUserBalance.text = AppStringConstant1.no_active_subscription
        upgrade_button.text = AppStringConstant1.buy_subscription
//        titleSubscription.visibility= View.VISIBLE
//        tvUserBalance.text =
//            requireContext().resources.getString(com.i69app.R.string.text_user_balance)
//                .plus(" $coins")

//        tvUserBalanceCoins.text = " $coins".plus("\nCoin")


        cdUpgradeBalance.setOnClickListener {
            bottomsheetDialog.dismiss()
            navigatePlanPurchase()
        }

        imageCross.setOnClickListener {
            bottomsheetDialog.dismiss()
        }

//        cdUserBalance.setOnClickListener {
//
//        }
        bottomsheetDialog.setContentView(customView)
        bottomsheetDialog.show()


    }

    private val formatterDateTimeUTC =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZ", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private val formatterDateOnly = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        //timeZone = TimeZone.getTimeZone("UTC")
        timeZone = TimeZone.getDefault()
    }

    private fun activeSubScriptionDetail(userSubscription: UserSubscriptionQuery.UserSubscription) {
        val bottomsheetDialog = BottomSheetDialog(requireContext())
        val customView =
            layoutInflater.inflate(com.i69.R.layout.dialog_upgrage_subscription, null, false)
        var iv_close = customView.findViewById<ImageView>(com.i69.R.id.iv_cross)

        var tv_subscription_name =
            customView.findViewById<TextView>(com.i69.R.id.tv_subscription_name)
        var tv_subscription_price =
            customView.findViewById<TextView>(com.i69.R.id.tv_subscription_price)
        var tv_subscription_description =
            customView.findViewById<TextView>(com.i69.R.id.tv_subscription_description)

        var tv_subscription_date =
            customView.findViewById<TextView>(com.i69.R.id.tv_subscription_date)
        var tv_subscription_left =
            customView.findViewById<TextView>(com.i69.R.id.tv_subscription_left)


        var clUpGrade =
            customView.findViewById<LinearLayout>(com.i69.R.id.cd_upgrade_subscription)

        iv_close.setOnClickListener { bottomsheetDialog.dismiss() }

        if (userSubscription!!.`package` != null) {
            tv_subscription_name.text =
                userSubscription.`package`!!.name


        }
        if (userSubscription!!.plan != null) {
            tv_subscription_price.text = "${userSubscription.plan!!.priceInCoins}"
            if (userSubscription.plan!!.isOnDiscount) {
                tv_subscription_description.text =
                    "${userSubscription.plan!!.dicountedPriceInCoins}"
            } else {
                tv_subscription_description.visibility = View.GONE
            }
        }

        val startDate = formatterDateTimeUTC.parse(userSubscription!!.startsAt.toString())
        val endDate = formatterDateTimeUTC.parse(userSubscription!!.endsAt.toString())
        tv_subscription_date.text =
            formatterDateOnly.format(startDate).plus(" - ").plus(formatterDateOnly.format(endDate))
        val diffInMillisec: Long = endDate.time - Date().time
        val diffInDays: Long = TimeUnit.MILLISECONDS.toDays(diffInMillisec)
        tv_subscription_left.text = String.format(getString(com.i69.R.string.days_left), diffInDays)

        if (userSubscription.`package`?.name?.contains(AppStringConstant1.platinum, true) == true)
            clUpGrade.setViewGone()


        clUpGrade.setOnClickListener {
            bottomsheetDialog.dismiss()
            navigatePlanPurchase()
        }

        bottomsheetDialog.setContentView(customView)
        bottomsheetDialog.show()
    }


    private fun navigateToPurchase() {
        findNavController().navigate(com.i69.R.id.actionGoToPurchaseFragment)
    }

    private fun navigatePlanPurchase() {
        findNavController().navigate(com.i69.R.id.action_global_plan)
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        //com.synnapps.carouselview.CarouselView

//        Log.e("colorChange", "colorChange")
//        if (scrollPos == (userAvatarSize -1)){
//            scrollPos = 0
//            binding.container.currentItem = scrollPos
//        }else {
//            scrollPos += 1
//            binding.container.currentItem = scrollPos
//        }

        binding.container.currentItem = position

//        var view = binding.userImgHeader.getChildAt(position)
//        val imageView: ImageView = view.findViewById<View>(com.i69app.R.id.userIv) as ImageView
//        if (imageView != null) {
//            val bitmap =  getBitmapFromView(imageView)
////            val bitmap =
////                imageView.background.toBitmap()
//            val color = bitmap!!.getPixel(100, 100)
////        if (background is ColorDrawable) {
////            val color = (background as ColorDrawable).color
////            Integer.toHexString(color)
//            val invertedColor = color.inv()
//
//            val rgb = getRGBStringfromHex(getHex(invertedColor))!!.split(",".toRegex())
//                .dropLastWhile { it.isEmpty() }
//                .toTypedArray()
//            val desireinvertedcolor =
//                getColorFromRGBString(invertedRGBfromRGB(getRGBStringtoRGBInt(rgb)!!)!!)
//
//
//            binding.textFlag1.setTextColor(desireinvertedcolor)
////            binding.textFlag1.setTextColor(invertedColor)
//            // Use color here
////        }
//        }else{
//            Log.e("getNukllImageView", "getNukllImageView")
//        }

    }

    override fun onPageSelected(position: Int) {
        //com.synnapps.carouselview.CarouselView
        Log.e("onPageSelected", "onPageSelected")


//        val background: Drawable =
//            binding.userImgHeader.getBackground()
//        var view=   binding.userImgHeader.getChildAt(position)
//        var imageView =
//            view.findViewById<ImageView>(com.i69app.R.id.userIv)
////        val background: Drawable =
////            imageView.getBackground()
////
////        if (background is ColorDrawable) {
////            val color = (background as ColorDrawable).color
//        if (imageView != null) {
//            val bitmap =  getBitmapFromView(imageView)
////            val bitmap =
////                imageView.background.toBitmap()
//            val color = bitmap!!.getPixel(100, 100)
////
//
//            val invertedColor = color.inv()
//
//            val rgb = getRGBStringfromHex(getHex(invertedColor))!!.split(",".toRegex())
//                .dropLastWhile { it.isEmpty() }
//                .toTypedArray()
//            val desireinvertedcolor =
//                getColorFromRGBString(invertedRGBfromRGB(getRGBStringtoRGBInt(rgb)!!)!!)
//
//
//            binding.textFlag1.setTextColor(desireinvertedcolor)

//            val invertedColor = color.inv()
//            binding.textFlag1.setTextColor(invertedColor)
        // Use color here
//        }
    }

    override fun onPageScrollStateChanged(state: Int) {
        //com.synnapps.carouselview.CarouselView

        Log.e("onPageScrollChange", "onPageScrollChange")
    }

    fun getBitmapFromView(view: View): Bitmap? {

        val returnedBitmap: Bitmap =
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) bgDrawable.draw(canvas) else canvas.drawColor(Color.WHITE)
        view.draw(canvas)
        return returnedBitmap
    }

    //color
    fun getHex(intColor: Int): String? {
        return String.format("#%06X", 0xFFFFFF and intColor)
    }

    fun getRGBStringfromHex(hexColor: String?): String? {
        val color: Int = Color.parseColor(hexColor)
        val red: Int = Color.red(color)
        val green: Int = Color.green(color)
        val blue: Int = Color.blue(color)
        return "$red,$green,$blue"
    }

    fun getRGBStringtoRGBInt(splits: Array<String>): IntArray? {
        val list = IntArray(3)
        list[0] = splits[0].toInt()
        list[1] = splits[1].toInt()
        list[2] = splits[2].toInt()
        return list
    }

    fun getRGBColor(rgb: IntArray): Int {
        return Color.rgb(rgb[0], rgb[1], rgb[2])
    }

    fun invertedRGBfromRGB(rgb: IntArray): String? {
        val invertedRed = 255 - rgb[0]
        val invertedGreen = 255 - rgb[1]
        val invertedBlue = 255 - rgb[2]
        return "$invertedRed,$invertedGreen,$invertedBlue"
    }

    fun getColorFromRGBString(color: String): Int {
        val list: ArrayList<Int> = ArrayList()
        val splits: Array<String> =
            color.split(Pattern.quote(",").toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        list.add(splits[0].toInt())
        list.add(splits[1].toInt())
        list.add(splits[2].toInt())
        return Color.rgb(list[0], list[1], list[2])
    }

//    private fun subscribeonUpdatePrivatePhotoRequest() {
//        val userid = requireArguments().getString("userId")
//        lifecycleScope.launch(Dispatchers.IO) {
//
//            try {
//                apolloClientSubscription(requireActivity(), userToken!!).subscription(
//                    OnUpdatePrivateRequestSubscription()
//                ).toFlow()
//                    .retryWhen { cause, attempt ->
//                        Timber.d("reealltime retry $attempt ${cause.message}")
//                        Log.d(
//                            "nUpdatePrivatePhotoRequest",
//                            "realtime retry  $attempt ${cause.message}"
//                        )
//                        delay(attempt * 1000)
//                        true
//                    }.collect { newMessage ->
//                        if (newMessage.hasErrors()) {
//                            Log.e(
//                                "nUpdatePrivatePhotoRequest",
//                                "realtime response error = ${newMessage.errors?.get(0)?.message}"
//                            )
//                            Timber.d("reealltime response error = ${newMessage.errors?.get(0)?.message}")
//                        } else {
//                            Timber.d("reealltime onNewMessage ${newMessage.data?.onUpdatePrivateRequest?.requestedUser}")
//                            Log.d(
//                                "reealltime",
//                                "reealltime ${newMessage.data?.onUpdatePrivateRequest?.requestedUser}"
//                            )
//                            Log.e(
//                                "reealltime",
//                                "reealltime ${newMessage.data}"
//                            )
//                            Log.e(
//                                "nUpdatePrivatePhotoRequest",
//                                "realtime DeleteMessage Id  ${newMessage.data?.onUpdatePrivateRequest?.requestedUser}"
//                            )
//                            Log.e(
//                                "nUpdatePrivatePhotoRequest",
//                                "realtime  DeleteMessage Id  ${newMessage.data}"
//                            )
//                            lifecycleScope.launchWhenResumed {
//
//
//                                userId = getCurrentUserId()!!
//                                Timber.i("usertokenn 2 $userToken")
//                                viewModel?.getProfile(userId)
//                            }
//
//
//                        }
//                        //  viewModel.onNewMessage(newMessage = newMessage.data?.onNewMessage?.message)
//                    }
//                Timber.d("reealltime 2")
//            } catch (e2: Exception) {
//                e2.printStackTrace()
//                Log.d("nUpdatePrivatePhotoRequest", "realtime exception= ${e2.message}")
//                Timber.d("reealltime exception= ${e2.message}")
//            }
//        }
//    }

    fun getMainActivity() = activity as MainActivity

}
