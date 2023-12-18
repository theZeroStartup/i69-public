package com.i69.ui.screens.main.messenger.chat

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.NotificationManager
import android.content.*
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
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
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.i69.*
import com.i69.R
import com.i69.applocalization.AppStringConstant
import com.i69.applocalization.AppStringConstant1
import com.i69.applocalization.AppStringConstantViewModel
import com.i69.data.config.Constants
import com.i69.data.models.ModelGifts
import com.i69.data.models.User
import com.i69.data.remote.responses.ResponseBody
import com.i69.databinding.AlertFullImageBinding
import com.i69.databinding.FragmentNewMessengerChatBinding
import com.i69.di.modules.AppModule
import com.i69.gifts.FragmentRealGifts
import com.i69.gifts.FragmentVirtualGifts
import com.i69.type.MessageMessageType
import com.i69.ui.adapters.NewChatMessagesAdapter
import com.i69.ui.adapters.UserItemsAdapter
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.ImagePickerActivity
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.screens.main.search.userProfile.SearchUserProfileFragment
import com.i69.ui.viewModels.UserViewModel
import com.i69.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import timber.log.Timber
import java.io.File


public class MessengerNewChatFragment : BaseFragment<FragmentNewMessengerChatBinding>(),
    NewChatMessagesAdapter.ChatMessageListener {

    private lateinit var adapter: NewChatMessagesAdapter
    private var edges: ArrayList<GetChatMessagesByRoomIdQuery.Edge?> = ArrayList()
    private lateinit var ShowBottomSheetBehavior : BottomSheetBehavior<ConstraintLayout>
    var endCursor: String = ""
    var hasNextPage: Boolean = false
    var isUpdatesChatView: Boolean = false

    private lateinit var deferred: Deferred<Unit>
    var exoPlayer: SimpleExoPlayer? = null
    private var timer1: CountDownTimerExt? = null
    private var userId: String? = null
    private var userToken: String? = null
    private var currentUser: User? = null
    private val viewModel: UserViewModel by activityViewModels()
    var giftUserid: String? = null
    var otherFirstName: String? = null
    var otherUserId: String? = null
    var ChatType: String? = null
    var fragVirtualGifts: FragmentVirtualGifts? = null
    var fragRealGifts: FragmentRealGifts? = null
    var otherUserProfile: String = ""
    private lateinit var GiftbottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    var isProgressShow: Boolean = true
    var isMessageSending: Boolean = false
    private val viewStringConstModel: AppStringConstantViewModel by activityViewModels()

    private val photosLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            val data = activityResult.data
            if (activityResult.resultCode == Activity.RESULT_OK) {
                val result = data?.getStringExtra("result")
                Log.d("GalleryImageLauncher", "PhotosLauncher result $result")
                Timber.d("Result $result")
                Log.e("ddd", "" + result)
                if (result != null) {
                    var mediaType: String = "video"
                    if (File(result).getMimeType().toString().startsWith("image")) {
                        mediaType = "image"
                    } else {
                        mediaType = "video"
                    }
                    Log.d("MessengerNewChatFragment", "UserToken $userToken")
                    UploadUtility(this@MessengerNewChatFragment).uploadFile(
                        result,
                        authorization = userToken, upload_type = mediaType
                    ) { url ->
                        Log.d("MessengerNewChatFragment", "ReponseUrl $url")
                        Timber.d("responseurll $url")
                        if (url.equals("url")) {

                            binding.root.snackbar(
                                AppStringConstant1.no_enough_coins,
                                Snackbar.LENGTH_INDEFINITE,
                                callback = {

                                    findNavController().navigate(
                                        destinationId = R.id.actionGoToPurchaseFragment,
                                        popUpFragId = null,
                                        animType = AnimationTypes.SLIDE_ANIM,
                                        inclusive = true,

                                        )
                                })
                        } else {
                            Log.d("MessengerNewChatFragment", "ReponseUrl $url")
                            var input = url
                            if (url?.startsWith("/media/chat_files/") == true) {
                                input = "${BuildConfig.BASE_URL}$url"
                            }
                            sendMessageToServer(input)
                        }
                    }
                }
            }
        }

    val galleryImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->

            val data = activityResult.data
            if (activityResult.resultCode == Activity.RESULT_OK) {

                val result = data?.data?.path
                val openInputStream =
                    requireActivity().contentResolver?.openInputStream(data?.data!!)
                val type = if (result?.contains("video") == true) ".mp4" else ".jpg"
                val outputFile =
                    requireContext().filesDir.resolve("${System.currentTimeMillis()}$type")
                openInputStream?.copyTo(outputFile.outputStream())

                Timber.d("Result $result")
                Log.e("ddd", "" + result)
                Log.d("GalleryImageLauncher", "galleryImageLauncher result $result")
                if (result != null) {
                    var mediaType: String = "video"
                    val file = File(outputFile.toURI())
                    if (file.getMimeType().toString().startsWith("image")) {
                        mediaType = "image"
                    } else {
                        mediaType = "video"
                    }

                    Log.d("GalleryImageLauncher", "File ${file.getMimeType()}")

                    Log.d("GalleryImageLauncher", "Upload Called")
                    UploadUtility(this@MessengerNewChatFragment).uploadFile2(
                        file,
                        authorization = userToken, upload_type = mediaType
                    ) { url ->
                        Timber.d("responseurll $url")
                        Log.d("GalleryImageLauncher", "responseurll $url")
                        if (url.equals("url")) {

                            binding.root.snackbar(
                                AppStringConstant1.no_enough_coins,
                                Snackbar.LENGTH_INDEFINITE,
                                callback = {

                                    findNavController().navigate(
                                        destinationId = R.id.actionGoToPurchaseFragment,
                                        popUpFragId = null,
                                        animType = AnimationTypes.SLIDE_ANIM,
                                        inclusive = true,
                                    )
                                })
                        } else {
                            Log.d("GalleryImageLauncher", "responseurll input $url")
                            var input = url
                            if (url?.startsWith("/media/chat_files/") == true) {
                                input = "${BuildConfig.BASE_URL}$url"
                            }
                            sendMessageToServer(input)
                        }
                    }
                }
            }
        }

    private val permissionReqLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            run {
                val granted = permission.entries.all {
                    it.value == true
                }
                if (granted) {
                    val locationService =
                        LocationServices.getFusedLocationProviderClient(requireContext())
                    locationService.lastLocation.addOnSuccessListener { location: Location? ->
                        val lat: Double? = location?.latitude

                        val lon: Double? = location?.longitude
//                toast("lat = $lat lng = $lon")

                        // Update Location
                        if (lat != null && lon != null) {
                            // Share Location
                            lifecycleScope.launch(Dispatchers.Main) {
                                val roomId = arguments?.getInt("chatId")
                                //viewModel.shareLocation("$lat,$lon", roomId!!, userId!!, userToken!!)
                                viewModel.shareLocation(
                                    "$lat,$lon",
                                    roomId!!,
                                    "bb2bb0d9-9c84-44ce-889c-7707c1cd7387",
                                    userToken!!
                                )
                                    .let { data -> showErrorInLocation(data) }

                            }
                        }

                    }
                }
            }
        }


    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNewMessengerChatBinding =
        FragmentNewMessengerChatBinding.inflate(inflater, container, false).apply {
            stringConstant = AppStringConstant(requireContext())
        }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        /*  activity?.window?.decorView?.setOnApplyWindowInsetsListener { view, insets ->
              val insetsCompat = toWindowInsetsCompat(insets, view)
              binding.rvChatMessages.layoutManager?.scrollToPosition(0)
              getMainActivity()?.binding?.bottomNavigation?.isGone = insetsCompat.isVisible(ime())
              view.onApplyWindowInsets(insets)
          }*/
        //broadcast = NotificationBroadcast(this);
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun setupTheme() {
        viewStringConstModel.data.observe(this@MessengerNewChatFragment) { data ->

            binding.stringConstant = data

        }
        viewStringConstModel.data?.also {
            binding.stringConstant = it.value
//            Log.e("MydataBasesss", it.value!!.messages)
        }
        lifecycleScope.launch {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            Timber.i("usertokenn $userToken")
//        }
            /* getTypeActivity<MainActivity>()?.setSupportActionBar(binding.toolbar)
             val supportActionBar = getTypeActivity<MainActivity>()?.supportActionBar
             supportActionBar?.setDisplayHomeAsUpEnabled(false)
             supportActionBar?.setDisplayShowHomeEnabled(false)
             supportActionBar?.title = ""*/
            //currentUser = null
            Log.e("callPersonalChat", "callPersonalChat")
//            initObservers()
            setupData(true)
            initObservers()
            setHasOptionsMenu(true)
            initInputListener()
//        initObservers()
            subscribeonUpdatePrivatePhotoRequest()
//        setupData(true)
        }
        /*        lifecycleScope.launch {
            viewModel.newMessageFlow.collect { message ->
                val avatarPhotos =
                    edges?.find { it?.node?.userId?.id == message?.userId?.id }?.node?.userId?.avatarPhotos
                message?.let { message ->
                    edges?.add(
                        0, GetChatMessagesByRoomIdQuery.Edge(
                            GetChatMessagesByRoomIdQuery.Node(
                                id = message.id,
                                content = "",
                            //    appLanguageCode =message.appLanguageCode,
                                roomId = GetChatMessagesByRoomIdQuery.RoomId(
                                    id = message.roomId.id,
                                    name = message.roomId.name
                                ),
                                timestamp = message.timestamp,
                                                                userId = GetChatMessagesByRoomIdQuery.UserId(
                                    id = message.userId.id,
                                    username = message.userId.username,
                                    avatarIndex = message.userId.avatarIndex,
                                    avatarPhotos = avatarPhotos
                                ),
                            )
//                            GetChatMessagesByRoomIdQuery.Node(
//                                id = message.id,
//
//
//                                roomId = GetChatMessagesByRoomIdQuery.RoomId(
//                                    id = message.roomId.id,
//                                    name = message.roomId.name
//                                ),
//                                appLanguageCode = message.appLanguageCode,
//                                timestamp = message.timestamp,
//                                userId = GetChatMessagesByRoomIdQuery.UserId(
//                                    id = message.userId.id,
//                                    username = message.userId.username,
//                                    avatarIndex = message.userId.avatarIndex,
//                                    avatarPhotos = avatarPhotos
//                                ),
//                            )
                        )
                    )
                    //notifyOnlyNew(edges = edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)
                    val chatId = arguments?.getInt("chatId", 0)!!
                    if (message.roomId.id.toInt()==chatId)
                        notifyAdapter(edges2 = edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)
                }
            }
        }*/
    }

    private fun initObservers() {
        viewModel.currentUserLiveData.observe(viewLifecycleOwner) { user ->
            Timber.d("User $user")
            user?.let {
                currentUser = it
                val coinsTextResource =
                    if (currentUser!!.purchaseCoins == 0) {
                        binding.coinsCounter.text = currentUser!!.giftCoins.toString()
                        if (currentUser!!.giftCoins == 0 || currentUser!!.giftCoins == 1) {
//                            R.string.coin_left
                            AppStringConstant1.coins
//                            R.string.coins
                        } else {
                            AppStringConstant1.coins
//                            R.string.coins
//                            R.string.coins_left
                        }
                    } else {
                        binding.coinsCounter.text =
                            currentUser!!.purchaseCoins.toString()
                        if (currentUser!!.purchaseCoins == 0 || currentUser!!.purchaseCoins == 1) {
//                            R.string.coin_left

                            AppStringConstant1.coins
//                            R.string.coins
                        } else {
//                            R.string.coins_left
                            AppStringConstant1.coins
//                            R.string.coins
                        }
                    }
                binding.coinsLeftTv.text = coinsTextResource
//                binding.coinsLeftTv.text = getString(coinsTextResource)
            }
        }
    }

    override fun setupClickListeners() {

        ShowBottomSheetBehavior =
            BottomSheetBehavior.from<ConstraintLayout>(binding.showImageVideoBottomSheet)
        ShowBottomSheetBehavior.addBottomSheetCallback(object :
//        )
//        GiftbottomSheetBehavior.setBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        Timber.d("Slide Up")
                        releasePlayer()

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

        binding.imgClose1.setOnClickListener {
            ShowBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            releasePlayer()
        }


        binding.videoDisable.setOnClickListener(View.OnClickListener {
            if (exoPlayer != null) {
                if (exoPlayer!!.isPlaying) {
                    binding.imgPlay.visibility = View.VISIBLE
                    binding.imgPlay.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_play_circle_outline_24))
                    exoPlayer!!.pause()
                    timer1!!.pause()
                } else {
                    binding.imgPlay.visibility = View.GONE
                    binding.imgPlay.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_pause_circle_outline))
                    exoPlayer!!.play()
                    timer1!!.start()
                }
            }
        })
    }

    private fun getCurrentUserDetails() {
        viewModel.getCurrentUserUpdate(userId!!, token = userToken!!, true)
    }

    fun setupData(isProgressShow: Boolean) {
        this.isProgressShow = isProgressShow
        ChatType = arguments?.getString("ChatType")
        if (ChatType.equals("001")) {
            getFirstMessages()
            binding.inputLayout.visibility = View.GONE
            binding.userName.text = requireArguments().getString("otherUserName")
            otherFirstName = requireArguments().getString("otherUserName")
            binding.userProfileImg.loadCircleImage(R.drawable.ic_chat_item_logo_new)
            binding.actionReportMes.visibility = View.GONE
        } else if (ChatType.equals("000")) {
            getBrodcastMessages()
            binding.inputLayout.visibility = View.GONE
            binding.userName.text = requireArguments().getString("otherUserName")
            otherFirstName = requireArguments().getString("otherUserName")
            binding.actionReportMes.visibility = View.GONE
            binding.userProfileImg.loadCircleImage(R.drawable.ic_chat_item_logo_new)
        } else /*if(ChatType.equals("Normal"))*/ {
            //  toast("chatType : $ChatType")
            endCursor = ""
            updateCoinView("")
            initInputListener()
            binding.inputLayout.visibility = View.VISIBLE
            giftUserid = arguments?.getString("otherUserId")
            otherUserId = arguments?.getString("otherUserId")
            otherFirstName = requireArguments().getString("otherUserName")
            binding.userName.text = requireArguments().getString("otherUserName")
            binding.sendgiftto.text =

                AppStringConstant1.send_git_to + " " + requireArguments().getString(
                    "otherUserName"
                )
//                context?.resources?.getString(R.string.send_git_to) + " " + requireArguments().getString(
//                    "otherUserName"
//                )


            val url = requireArguments().getString("otherUserPhoto")
            binding.userProfileImg.loadCircleImage(url!!)
            otherUserProfile = url
            //binding.userProfileImgContainer.setOnClickListener { navigateToOtherUserProfile() }
            isOtherUserOnline()
            binding.userProfileImg.setOnClickListener {
                gotoChatUserProfile()
            }
            binding.userName.setOnClickListener {
                gotoChatUserProfile()
            }
            val gender = requireArguments().getInt("otherUserGender", 0)
            binding.input.updateGiftIcon(gender)
            updateGiftIcon(gender)
        }
        binding.closeBtn.setOnClickListener {
            moveUp()
        }
        binding.actionReportMes.setOnClickListener {
            openMenuItem()
        }

        binding.llCoinLeft.setOnClickListener {

            findNavController().navigate(
                destinationId = R.id.actionGoToPurchaseFragment,
                popUpFragId = null,
                animType = AnimationTypes.SLIDE_ANIM,
                inclusive = true,
            )
        }

//        binding.imgBuyCoin.setOnClickListener {
//
//            findNavController().navigate(
//                destinationId = R.id.actionGoToPurchaseFragment,
//                popUpFragId = null,
//                animType = AnimationTypes.SLIDE_ANIM,
//                inclusive = true,
//            )
//        }
    }

    fun updateGiftIcon(gender: Int) {
        when (gender) {
            0 -> binding.ivGiftIcon.setImageDrawable(
                ResourcesCompat.getDrawable(
                    requireContext().resources,
                    R.drawable.yellow_gift_male,
                    null
                )
            )
            1 -> binding.ivGiftIcon.setImageDrawable(
                ResourcesCompat.getDrawable(
                    requireContext().resources,
                    R.drawable.pink_gift_noavb,
                    null
                )
            )
            else -> binding.ivGiftIcon.setImageDrawable(
                ResourcesCompat.getDrawable(
                    requireContext().resources,
                    R.drawable.purple_gift_nosay,
                    null
                )
            )
        }
    }

    private fun openMenuItem() {
        val popup = PopupMenu(
            requireContext(),
            binding.actionReportMes,
            10,
            R.attr.popupMenuStyle,
            R.style.PopupMenu2
        )
        popup.menuInflater.inflate(R.menu.search_profile_options, popup.menu);
        //adding click listener
        popup.setOnMenuItemClickListener { item: MenuItem? ->
            when (item!!.itemId) {
                R.id.nav_item_report -> {
                    reportDialog()
                }
                R.id.nav_item_block -> {
                    blockUserAlert()
                }
            }
            true
        }
        popup.show()
    }

    private fun blockUserAlert() {
//        val builder = AlertDialog.Builder(requireContext())
////        builder.setMessage("Are you sure you want to block $otherFirstName ?")
////        builder.setMessage("${requireActivity().resources.getString(R.string.are_you_sure_you_want_to_block)} $otherFirstName ?")
//        builder.setMessage("${AppStringConstant1.are_you_sure_you_want_to_block} $otherFirstName ?")
//            .setCancelable(false)
////            .setPositiveButton(requireActivity().resources.getString(R.string.yes)) { dialog, id ->
//            .setPositiveButton(AppStringConstant1.yes) { dialog, id ->
//                blockAccount()
//            }
////            .setNegativeButton(requireActivity().resources.getString(R.string.no)) { dialog, id ->
//            .setNegativeButton(AppStringConstant1.no) { dialog, id ->
//                dialog.dismiss()
//            }
//        val alert = builder.create()
//        alert.show()
//        alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLUE);
//        alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLUE);

        val dialogLayout = layoutInflater.inflate(R.layout.dialog_delete, null)
        val headerTitle = dialogLayout.findViewById<TextView>(R.id.header_title)
        val noButton = dialogLayout.findViewById<TextView>(R.id.no_button)
        val yesButton = dialogLayout.findViewById<TextView>(R.id.yes_button)

        headerTitle.text = "${AppStringConstant1.are_you_sure_you_want_to_block} $otherFirstName ?"
        noButton.text = "${AppStringConstant(requireActivity()).no}"
        yesButton.text = "${AppStringConstant(requireActivity()).yes}"

        val builder = AlertDialog.Builder(activity,R.style.DeleteDialogTheme)
        builder.setView(dialogLayout)
        builder.setCancelable(false)
        val dialog = builder.create()

        noButton.setOnClickListener {
            dialog.dismiss();
        }

        yesButton.setOnClickListener {
            dialog.dismiss();
            blockAccount()
        }

        dialog.show()
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
            reportAccount(otherUserId, message)
            dialog.dismiss()
        }

        cancleButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun blockAccount() {
        lifecycleScope.launch(Dispatchers.Main) {
            when (val response = viewModel.blockUser(userId, otherUserId, token = userToken)) {
                is Resource.Success -> {
                    viewModel.getCurrentUser(userId!!, userToken!!, true)
                    hideProgressView()
//                    binding.root.snackbar("${otherFirstName} ${getString(R.string.blocked)}")
                    binding.root.snackbar("${otherFirstName} ${AppStringConstant1.blocked}")

                    (activity as MainActivity)?.pref?.edit()?.putString("chatListRefresh", "true")
                        ?.putString("readCount", "false")?.apply()
                    findNavController().popBackStack()
                }
                is Resource.Error -> {
                    hideProgressView()
                    Timber.e("${getString(R.string.something_went_wrong)} ${response.message}")
                    binding.root.snackbar("${ AppStringConstant1.something_went_wrong} ${response.message}")
//                    binding.root.snackbar("${getString(R.string.something_went_wrong)} ${response.message}")
                }

                else -> {

                }
            }
        }
    }

    private fun reportAccount(otherUserId: String?, reasonMsg: String?) {
        lifecycleScope.launch(Dispatchers.Main) {
            reportUserAccount(
                token = userToken,
                currentUserId = userId,
                otherUserId = otherUserId,
                reasonMsg = reasonMsg,
                mViewModel = viewModel
            ) { message ->
                hideProgressView()
                binding.root.snackbar(message)
            }
        }
    }

    private fun setupOtherUserData() {
        giftUserid = arguments?.getString("otherUserId")
        binding.userName.text = requireArguments().getString("otherUserName")
        binding.sendgiftto.text =
            AppStringConstant1.send_git_to +
                    " " + requireArguments().getString(
                "otherUserName"
            )
        //            context?.resources?.getString(R.string.send_git_to)
        val url = requireArguments().getString("otherUserPhoto")
        binding.userProfileImg.loadCircleImage(url!!)
        //binding.userProfileImgContainer.setOnClickListener { navigateToOtherUserProfile() }
        binding.closeBtn.setOnClickListener {
            moveUp()
        }
        isOtherUserOnline()
        binding.userProfileImg.setOnClickListener {
            gotoChatUserProfile()
        }
        binding.userName.setOnClickListener {
            gotoChatUserProfile()
        }
        val gender = requireArguments().getInt("otherUserGender", 0)
        binding.input.updateGiftIcon(gender)
    }

    private fun navigateToOtherUserProfile() {

    }

    fun gotoChatUserProfile() {
        val bundle = Bundle()
        bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
        bundle.putString("userId", giftUserid)

        findNavController().navigate(
            destinationId = R.id.action_global_otherUserProfileFragment,
            popUpFragId = null,
            animType = AnimationTypes.SLIDE_ANIM,
            inclusive = true,
            args = bundle
        )
    }

    private fun initInputListener() {
        Timber.d("check input string ${arguments?.getInt("chatId")} ${arguments?.getString("otherUserId")}")
        //sendMessageToServer("hardcode text message")
        binding.input.setInputListener { input ->

            if (!isMessageSending)
                sendMessageToServer(input.toString())
            binding.input.inputEditText?.hideKeyboard()
            binding.input.inputEditText?.setText("")
            Timber.d(
                "check input string $input ${arguments?.getInt("chatId")} ${
                    arguments?.getString(
                        "otherUserId"
                    )
                }"
            )
            return@setInputListener false
        }

//        binding.rvChatMessages.setOnTouchListener { v, event ->
//            if (binding.includeAttached.clAttachments.visibility == View.VISIBLE) {
//                binding.includeAttached.clAttachments.visibility = View.GONE
//            }
//            return@setOnTouchListener true
//        }

//        binding.root.getViewTreeObserver().addOnWindowFocusChangeListener(
//            OnWindowFocusChangeListener { hasFocus ->
//                // Remove observer when no longer needed
//                // contentView.getViewTreeObserver().removeOnWindowFocusChangeListener(this);
//
//                if (binding.includeAttached.clAttachments.visibility == View.VISIBLE) {
//                    binding.includeAttached.clAttachments.visibility = View.GONE
//                }
//
//
//            })

//        binding.clMessageParent.setOnTouchListener { v, event ->
//            if (binding.includeAttached.clAttachments.visibility == View.VISIBLE) {
//                binding.includeAttached.clAttachments.visibility = View.GONE
//            }
//            return@setOnTouchListener true
//        }

        binding.input.inputEditText.setOnFocusChangeListener { v, hasFocus ->
            if (binding.includeAttached.clAttachments.visibility == View.VISIBLE) {
                binding.includeAttached.clAttachments.visibility = View.GONE
            }
        }


        binding.input.inputEditText.setOnFocusChangeListener { v, hasFocus ->
            if (binding.includeAttached.clAttachments.visibility == View.VISIBLE) {
                binding.includeAttached.clAttachments.visibility = View.GONE
            }
        }

        binding.input.setAttachmentsListener {

            if (binding.includeAttached.clAttachments.visibility == View.VISIBLE) {
                binding.includeAttached.clAttachments.visibility = View.GONE
            } else {
                binding.includeAttached.clAttachments.visibility = View.VISIBLE
                binding.includeAttached.llCamera.setOnClickListener {
                    val intent = Intent(requireActivity(), ImagePickerActivity::class.java)
                    intent.putExtra("video_duration_limit",60)
                    intent.putExtra("withCrop", false)
                    photosLauncher.launch(intent)
                    binding.includeAttached.clAttachments.visibility = View.GONE
                }

                binding.includeAttached.llGallery.setOnClickListener {
                    val intent = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.INTERNAL_CONTENT_URI
                    )
                    //  intent.setType("image/* video/*")
                    galleryImageLauncher.launch(
                        intent
                    )
                    binding.includeAttached.clAttachments.visibility = View.GONE
                }

                binding.includeAttached.llLocation.setOnClickListener {
                    shareLocation()
                    Log.e("Location", "Location")
                    binding.includeAttached.clAttachments.visibility = View.GONE
                }
            }


//               val intent = Intent(requireActivity(), ImagePickerActivity::class.java)
//            intent.putExtra("video_duration_limit", 180)
//            photosLauncher.launch(intent)

//            val dialog = Dialog(requireContext())
//            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//            dialog.setCancelable(true)
//            dialog.setContentView(R.layout.dialog_image_option)
//            dialog.findViewById<TextView>(R.id.header_title).text =
//                getString(R.string.select_image_file)
//            dialog.findViewById<LinearLayoutCompat>(R.id.ll_camera).setOnClickListener {
//                val intent = Intent(requireActivity(), ImagePickerActivity::class.java)
//                intent.putExtra("withCrop", false)
//                photosLauncher.launch(intent)
//                dialog.dismiss()
//            }
//            dialog.findViewById<LinearLayoutCompat>(R.id.ll_gallery).setOnClickListener {
//                val intent = Intent(
//                    Intent.ACTION_PICK,
//                    MediaStore.Images.Media.INTERNAL_CONTENT_URI
//                )
//                //  intent.setType("image/* video/*")
//                galleryImageLauncher.launch(
//                    intent
//                )
//                dialog.dismiss()
//            }
//            val llLocation = dialog.findViewById<LinearLayoutCompat>(R.id.ll_location)
//            llLocation.visibility = View.VISIBLE
//            llLocation.setOnClickListener {
//                dialog.dismiss()
//                shareLocation()
//            }
//            dialog.show()
        }


        binding.input.setGiftButtonListener {

            if (binding.includeAttached.clAttachments.visibility == View.VISIBLE) {
                binding.includeAttached.clAttachments.visibility = View.GONE
            }
            if (GiftbottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            buttonSlideUp.text = "Slide Down";
            } else {
                GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//            buttonSlideUp.text = "Slide Up"
            }
//            if (arguments?.getString("otherUserId")?.isEmpty() == true) return@setGiftButtonListener
//
//            val bundle = Bundle()
//            bundle.putString("userId", arguments?.getString-("otherUserId"))
//            Handler(Looper.getMainLooper()).postDelayed({
//                findNavController().navigate(destinationId = R.id.action_to_userGiftsFragment,
//                    popUpFragId = null,
//                    animType = AnimationTypes.SLIDE_ANIM,
//                    inclusive = false,
//                    args = bundle)
//            }, 200)
        }

        GiftbottomSheetBehavior =
            BottomSheetBehavior.from(binding.giftbottomSheet)
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
            isProgressShow = true
            val items: MutableList<ModelGifts.Data.AllRealGift> = mutableListOf()
            fragVirtualGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }
            fragRealGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }
            lifecycleScope.launchWhenCreated {
                if (items.size > 0) {
                    if (isProgressShow) {
                        showProgressView()
                    }
                    items.forEach { gift ->
                        Log.e("gift.id", "-->" + gift.id)
                        Log.e("giftUserid", "--> $giftUserid")
                        Log.e("currentUserId", "--> ${getCurrentUserId()}")
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
                            Timber.d("apolloResponse ${e.message}")
                            Toast.makeText(
                                requireContext(),
                                "Exception ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
//                                views.snackbar("Exception ${e.message}")
                            //hideProgressView()
                            //return@launchWhenResumed
                        }
                        Log.e("res", "-->" + Gson().toJson(res))
                        if (res?.hasErrors() == false) {
//                            getCurrentUserDetails()
//                  updateCoinView("")
//                            isMessageSending = false
//                                views.snackbar("You bought ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} successfully!")
                            Toast.makeText(
                                requireContext(),
                                AppStringConstant1.you_bought
//                                requireContext().resources?.getString(R.string.you_bought)
                                        + " ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} successfully!",
                                Toast.LENGTH_LONG
                            ).show()
//                            endCursor = ""
//                            hasNextPage = false
//                            updateCoinView("")
                            //     fireGiftBuyNotificationforreceiver(gift.id, giftUserid)
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
                    getCurrentUserDetails()
                    hideProgressView()
                    GiftbottomSheetBehavior.state=BottomSheetBehavior.STATE_HIDDEN

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
    }

    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = UserItemsAdapter(childFragmentManager)
        fragRealGifts = FragmentRealGifts()
        fragVirtualGifts = FragmentVirtualGifts()

        adapter.addFragItem(fragRealGifts!!, AppStringConstant1.real_gifts)
        adapter.addFragItem(fragVirtualGifts!!, AppStringConstant1.virtual_gifts)
//        adapter.addFragItem(fragRealGifts!!, getString(R.string.real_gifts))
//        adapter.addFragItem(fragVirtualGifts!!, getString(R.string.virtual_gifts))
        viewPager.adapter = adapter
    }

    fun fireGiftBuyNotificationforreceiver(gid: String, userid: String?) {
        lifecycleScope.launchWhenResumed {
            val queryName = "sendNotification"
            val query = StringBuilder()
                .append("mutation {")
                .append("$queryName (")
                .append("userId: \"${userid}\", ")
                .append("notificationSetting: \"GIFT\", ")
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


    //This method would check that the recyclerview scroll has reached the bottom or not
    private fun isLastItemDisplaying(recyclerView: RecyclerView): Boolean {
        if (recyclerView.adapter!!.itemCount != 0) {
            val lastVisibleItemPosition =
                (recyclerView.layoutManager as LinearLayoutManager?)!!.findLastCompletelyVisibleItemPosition()
            if (lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.adapter!!
                    .itemCount - 1
            ) return true
        }
        return false
    }

    private fun notifyOnlyNew(edges: ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?) {
        adapter.updateList(edges)
    }

    private fun notifyAdapter(
        edges2: ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?,
        isUpdateNewMessage: Boolean = false
    ) {
        Log.d("MainActivitySubscription", "Adapter created")
        if (binding.rvChatMessages.adapter == null) {
            adapter = NewChatMessagesAdapter(
                requireActivity(),
                userId,
                this@MessengerNewChatFragment,
                //edges
            )
            adapter.otherUserAvtar = otherUserProfile
            // val lastSeenMessageIdOld = adapter.lastSeenMessageId
            /*           adapter.lastSeenMessageId = lastSeenMessageId
                       adapter.otherUserAvtar = otherUserProfile*/
            /*     if (lastSeenMessageIdOld.isNotEmpty()){
                     val position = adapter.lastSeenPosition
                     adapter.notifyItemChanged(position)
                 }*/
            (binding.rvChatMessages.layoutManager as LinearLayoutManager).apply {
                reverseLayout = true
                stackFromEnd = true

                binding.rvChatMessages.layoutManager = this
            }
            binding.rvChatMessages.setHasFixedSize(true)
//            binding.rvChatMessages.setItemViewCacheSize(20)
            binding.rvChatMessages.setDrawingCacheEnabled(true)

            binding.rvChatMessages.adapter = adapter
            adapter.updateList(edges2)

            if (adapter.itemCount > 0) {
//            binding.rvChatMessages.postDelayed({
                binding.rvChatMessages.layoutManager?.scrollToPosition(0)
//            }, 100)

//            binding.rvChatMessages.layoutManager?.scrollToPosition(0)
            }

            adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    Log.d("ativitySubscription", "ObserverInside scrolled to zero")
                    if (isUpdatesChatView) {
                        Log.e("updateDataaa", "updateDataaaposition zero")
                        binding.rvChatMessages.layoutManager?.scrollToPosition(0)
                    } else {
                        isUpdatesChatView = true
                    }
                    // adapter.unregisterAdapterDataObserver(this)
                }
            })


            binding.rvChatMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
//                    Log.e("myscrollCalled", "myscrollCalled")
                    if (binding.includeAttached.clAttachments.visibility == View.VISIBLE) {
                        binding.includeAttached.clAttachments.visibility = View.GONE
                    }
                    if (isLastItemDisplaying(recyclerView)) {
                        if (!ChatType.equals("001") && !ChatType.equals("000")) {
                            Log.e("myscrollCalled", "$ChatType")
                            //Calling the method getdata again

                            if (hasNextPage) {
                                getChatMessages()
                            }
                        }
                    }
                }
            })

            Log.d("MessengerNewChatFragment", "otheruserAvtar $otherUserId")
            Log.d("MessengerNewChatFragment", "edges2 $edges2")

            if (!ChatType.equals("001")
                && !ChatType.equals("000")
            ) {
                subscribeOnDeleteMessage()
                subscribeOnNewMessage()
                subscribeOnLastSeenMessage()
                arguments?.getInt("chatId")?.let { getLastSeenMessageId(it) }
            }

        } else {

            Log.e("updateChatMessage", "updateChatMessage else ")
            isUpdatesChatView = isUpdateNewMessage
            if (edges2?.isNotEmpty() == true) {
                val its = edges2.first()

                Log.e("MyLastMessagePrint", its?.node?.content.toString())
//            binding.rvChatMessages.adapter = adapter
                adapter.updateList(edges2, isUpdate = isUpdateNewMessage)

                val recyclerViewState = binding.rvChatMessages.layoutManager?.onSaveInstanceState()
                adapter.notifyDataSetChanged()
                binding.rvChatMessages.layoutManager?.onRestoreInstanceState(recyclerViewState)
            }

        }

        if (adapter.itemCount > 0) {
            Handler().postDelayed(Runnable {
                binding.rvChatMessages.layoutManager?.scrollToPosition(0)
            },500)
            //  Log.d("MainActivitySubscription","ScrollToPosition itemCount > 0")
        }

//        hideProgressView()
    }

    private fun isOtherUserOnline() {
        if (isProgressShow) {
            //howProgressView()
        }
        lifecycleScope.launch {
            try {
                val id = requireArguments().getString("otherUserId")
                val res =
                    apolloClient(requireContext(), userToken!!).query(IsOnlineQuery(id!!)).execute()
                if (!res.hasErrors()) {
                    binding.otherUserOnlineStatus.visibility =
                        if (res.data?.isOnline?.isOnline == true) View.VISIBLE else View.GONE
                    Timber.d("apolloResponse isOnline ${res.data?.isOnline?.isOnline}")
                }
            } catch (e: ApolloException) {
                Timber.d("apolloResponse isOnline ${e.message}")
            }
            //hideProgressView()
        }
    }

    private fun getFirstMessages() {
        //edges = mutableListOf()
        if (isProgressShow) {
            //showProgressView()
        }
        Log.e("calllGetFirstMessageListQuery", "GetFirstMessageListQuery");
        lifecycleScope.launchWhenCreated {
            try {

                val res = apolloClient(requireContext(), userToken!!).query(
                    GetFirstMessageListQuery()
                ).execute()
                val datas = res.data!!.firstmessageMsgs!!.edges

                datas.forEach { Edge ->

                    val msg = GetChatMessagesByRoomIdQuery.Edge(
                        GetChatMessagesByRoomIdQuery.Node(
                            id = Edge!!.node!!.byUserId.id!!,
                            content = Edge.node!!.content,
                            //appLanguageCode =Edge.node.byUserId.userLanguageCode.toString(),
                            roomId = GetChatMessagesByRoomIdQuery.RoomId(
                                id = "",
                                name = ""
                            ),
                            timestamp = Edge.node.timestamp,
                            userId = GetChatMessagesByRoomIdQuery.UserId(
                                id = Edge.node.byUserId.id,
                                username = Edge.node.byUserId.username,
                                avatarIndex = Edge.node.byUserId.avatarIndex,
                                null
                            ),

                            messageType = MessageMessageType.C,
                            privatePhotoRequestId = 0,
                            requestStatus = ""
//                            messageType = Edge.node.messageType,
//                            privatePhotoRequestId = Edge.node.privatePhotoRequestId,
//                            requestStatus = Edge.node.requestStatus
                        )
                    )

                    //edges.add(msg)
                    edges.addAll(checkForImageVideoInMessage(msg))
                }

                /*edges?.forEach {
                    Timber.d("apolloResponse getChatMessages ${it?.node?.text}")
                }*/
                if (!res.hasErrors()) {
                    Log.e("getMyCurrentTime", "getMyCurrentTime")
                    Timber.d("apolloResponse success ${edges.size}")
                    notifyAdapter(edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)
                } else {
                    Timber.d("apolloResponse error ${res.errors?.get(0)?.message}")
                }
            } catch (e: ApolloException) {
                Log.e("ExceptionInGetFirstMessages", "${e.message}")
                Timber.d("apolloResponse ${e.message}")
                //binding.root.snackbar("Exception all moments $GetAllMomentsQuery")
                return@launchWhenCreated
            }
            //hideProgressView()
        }
    }

    private fun getBrodcastMessages() {
        // edges = mutableListOf()
        if (isProgressShow) {
            //showProgressView()
        }
        lifecycleScope.launch {
            try {
                val res = apolloClient(requireContext(), userToken!!).query(
                    GetBroadcastMessageListQuery()
                ).execute()

                val datas = res.data!!.broadcastMsgs!!.edges
                Log.e("MessengerNewChatFragment", "res $res")

                datas.forEach { Edge ->

                    val msg = GetChatMessagesByRoomIdQuery.Edge(
                        GetChatMessagesByRoomIdQuery.Node(
                            id = Edge!!.node!!.byUserId.id!!,
                            content = Edge.node!!.content,
                            roomId = GetChatMessagesByRoomIdQuery.RoomId(
                                id = "",
                                name = ""
                            ),
                            //      appLanguageCode =Edge.node.byUserId.userLanguageCode.toString(),
                            timestamp = Edge.node.timestamp,
                            userId = GetChatMessagesByRoomIdQuery.UserId(
                                id = Edge.node.byUserId.id,
                                username = Edge.node.byUserId.username,
                                avatarIndex = Edge.node.byUserId.avatarIndex,
                                null
                            ),
                            messageType = MessageMessageType.C,
                            privatePhotoRequestId = 0,
                            requestStatus = ""
                        )
                    )
                    edges.addAll(checkForImageVideoInMessage(msg))
                    //edges!!.add(msg)
                }
                /*edges?.forEach {
                    Timber.d("apolloResponse getChatMessages ${it?.node?.text}")
                }*/

                if (!res.hasErrors()) {
                    Timber.d("apolloResponse success ${edges?.size}")
                    notifyAdapter(edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)
                } else {
                    Timber.d("apolloResponse error ${res.errors?.get(0)?.message}")
                }
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                //binding.root.snackbar("Exception all moments $GetAllMomentsQuery")
                return@launch
            }
            //hideProgressView()
        }
    }

    override fun onChatMessageDelete(message: GetChatMessagesByRoomIdQuery.Edge?) {

        lifecycleScope.launch {

            try {

                val response = apolloClient(requireContext(), userToken!!).mutation(
                    DeleteUserMessagesMutation(message?.node?.id.toString())
                ).execute()

                if (response.hasErrors()) {

                    val errors = response.errors?.get(0)?.message
                    Toast.makeText(requireContext(), errors, Toast.LENGTH_LONG).show()

                } else {

//                      getChatMessages()

                }

            } catch (e: ApolloException) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }

        }

    }

    override fun onPrivatePhotoAccessResult(decision: String, requestId: Int) {

        lifecycleScope.launch {

            try {

                val response = apolloClient(requireContext(), userToken!!).mutation(
                    PrivatePhotosDecisionMutation(decision, requestId)
                ).execute()

                if (response.hasErrors()) {

                    val errors = response.errors?.get(0)?.message
                    Toast.makeText(requireContext(), errors, Toast.LENGTH_LONG).show()

                } else {

                    if (decision == "A") {

//                        binding.root.snackbar(getString(R.string.you_accepted_the_request))
                        binding.root.snackbar(AppStringConstant1.you_accepted_the_request)
                    } else {
//                        binding.root.snackbar(getString(R.string.you_reject_the_request))
                        binding.root.snackbar(AppStringConstant1.you_reject_the_request)
                    }

                    updateCoinView("")

                }

            } catch (e: ApolloException) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun updateLastSeen(roomId: Int) {

        lifecycleScope.launch {

            try {

                val response = apolloClient(requireContext(), userToken!!).mutation(
                    GetLastSeenMessageMutation(roomId)
                ).execute()

                if (response.hasErrors()) {
                    Log.e("updateLastseenErro", "${response.errors} ")
//                    if (!ChatType.equals("001") && !ChatType.equals("000")) {
                    val errors = response.errors?.get(0)?.message
                    Toast.makeText(requireContext(), errors, Toast.LENGTH_LONG).show()
//                    }

                } else {
                    Log.d("MainActivitySubscription", "updateLastSeen ")
                }

            } catch (e: ApolloException) {
//                Toast.makeText(requireContext(), "UpdateLastSeen " + e.message, Toast.LENGTH_LONG)
//                    .show()
            } catch (e: Exception) {
//                Toast.makeText(requireContext(), "UpdateLastSeen " + e.message, Toast.LENGTH_LONG)
//                    .show()
            }

        }

    }

    private fun getLastSeenMessageId(roomId: Int) {
        lifecycleScope.launch {
            try {
                val response = apolloClient(requireContext(), userToken!!).query(
                    GetLastSeenMessageUserQuery(roomId)
                ).execute()
                if (response.hasErrors()) {
                    Log.e("myVvvded", "${response.errors}")
                    val errors = response.errors?.get(0)?.message
                    Toast.makeText(requireContext(), errors, Toast.LENGTH_LONG).show()
                } else {
                    adapter.lastSeenMessageId = response.data?.lastSeenMessageUser?.id.toString()
                    val edge =
                        edges.filter { it?.node?.id == response.data?.lastSeenMessageUser?.id.toString() }
                    CoroutineScope(Dispatchers.Main).launch {
                        if (edge.isNotEmpty()) {
                            adapter.notifyItemChanged(edges.indexOf(edge[0]))
                        }
                    }
                    Log.d("MainActivitySubscription", "updateLastSeen $edge")
                }

            } catch (e: ApolloException) {
                Toast.makeText(requireContext(), "UpdateLastSeen " + e.message, Toast.LENGTH_LONG)
                    .show()
            } catch (e: Exception) {
                //Toast.makeText(requireContext(), "UpdateLastSeen " + e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkForImageVideoInMessage(item: GetChatMessagesByRoomIdQuery.Edge?) : ArrayList<GetChatMessagesByRoomIdQuery.Edge?> {
        if (item?.node?.messageType != MessageMessageType.G) {
            val content = item?.node?.content
            if (content?.contains("media/chat_files") == true) {
                if (content.contains(" ")) {
                    val link = content.substring(0, content.indexOf(" "))
                    val message = content.substring(content.indexOf(" ") + 1)
                    val newNode = item.node.copy(content = message)
                    val textItem = item.copy(node = newNode)
                    val newNodeMedia = item.node.copy(content = link)
                    val linkItem = item.copy(node = newNodeMedia)
                    return arrayListOf(linkItem, textItem)
                }
            }
        }
        return arrayListOf(item)
    }

    private fun getChatMessages(updateMessage: Boolean = false) {
//        getCurrentUserDetails()
        // if (isProgressShow) {
        //showProgressView()
        //}
        Log.d("MessengerNewChatFragment", "calling")
        Timber.d("GetChatMessages calling")
        val activity: Activity? = activity
        if (activity != null) {
            lifecycleScope.launchWhenStarted {
                try {
                    val roomId = arguments?.getInt("chatId")
                    Timber.d("apolloResponse roomId $roomId")
                    Log.d("MessengerNewChatFragment", "apolloResponse roomId $roomId")
                    /*
                                        val queryLastSeenMessageRes = apolloClient(requireContext(),userToken!!).mutation(
                                            GetLastSeenMessageMutation(
                                                roomId!!
                                            )
                                        ) .execute()

                                        if (queryLastSeenMessageRes.hasErrors()){
                                            Toast.makeText(requireContext(),"Last seen message exception ${queryLastSeenMessageRes.errors?.get(0)?.message}",Toast.LENGTH_LONG).show()
                                        }else{
                                            lastSeenMessageId = queryLastSeenMessageRes.data?.lastseenMessages?.messageId.toString()

                                            Log.d("MessengerNewChatFragment","Lastseen $lastSeenMessageId")
                                        }*/

//room message query removed last param

//                    query GetChatMessagesByRoomId ($roomID: ID!, $last: Int!) {
//                        messages(id: $roomID, last: $last) {
//                        edges {
//                            node {
//                                id
//                                content
//                                timestamp
//                                messageType
//                                privatePhotoRequestId
//                                requestStatus
//                                roomId{
//                                    id
//                                    name
//                                }
//                                userId {
//                                    id
//                                    username
//                                    avatarIndex
//                                    avatarPhotos {
//                                        url
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    }

//                    val query = GetChatMessagesByRoomIdQuery(
//                        roomId.toString(),
//                        99
//                    )

                    val query = GetChatMessagesByRoomIdQuery(
                        roomId.toString(), 10, endCursor
                    )
//
                    val res = apolloClient(requireActivity(), userToken!!).query(
                        query
                    ).execute()
                    Log.d("MessengerNewChatFragment", "res $res")
                    /*edges?.forEach {
                       Timber.d("apolloResponse getChatMessages ${it?.node?.text}")
                       }*/

                    if (!res.hasErrors()) {
                        edges.clear()
                        res.data?.messages?.edges?.forEach {edge ->
                            edges.addAll(checkForImageVideoInMessage(edge))
                        }
                        //edges = res.data?.messages?.edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>

                        Timber.d("apolloResponse success ${edges.size}")
                        Log.e("getMyCurrentTime1", "${edges.size}")
                        Log.e("getMyCurrentTime2", "${res.data?.messages!!.pageInfo}")
                        endCursor = res.data?.messages?.pageInfo?.endCursor ?: ""
                        hasNextPage = res.data?.messages!!.pageInfo.hasNextPage
                        isUpdatesChatView = false
                        Log.e("getMyCurrentTime", "getMyCurrentTime")
                        notifyAdapter(edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?, updateMessage)
                    } else {
                        Log.d(
                            "MessengerNewChatFragment",
                            "apolloResponse error  ${res.errors?.get(0)?.message}"
                        )
                        Timber.d("apolloResponse error ${res.errors?.get(0)?.message}")
                    }


                } catch (e: ApolloException) {
                    Log.d("MessengerNewChatFragment", "apolloResponse ${e.message}")
                    Timber.d("apolloResponse ${e.message}")
                    //binding.root.snackbar("Exception all moments $GetAllMomentsQuery")
                    return@launchWhenStarted
                }
                //hideProgressView()
            }
        }
    }

    private fun sendMessageToServer(input: String?) {
        isProgressShow = true
        isMessageSending = true
        Timber.d("apolloResponse 1 $input")
        lifecycleScope.launch {
            send(input!!)
        }
        Timber.d("apolloResponse 8")
    }

    suspend fun send(input: String) {
        val chatId = arguments?.getInt("chatId", 0)!!
        Timber.d("apolloResponse 3 c $chatId and crrent id $userId")
        var res: ApolloResponse<SendChatMessageMutation.Data>? = null
        try {
            val requ = SendChatMessageMutation(
                input,
                chatId
                //  getMainActivity()?.pref?.getString("language","en").toString()
            )
            Log.e("dcfvdffd",requ.toString())
            res = apolloClient(requireActivity(), userToken!!).mutation(
                requ
            ).execute()
            Timber.d("apolloResponse 1111 c ${res.hasErrors()} ${res.data?.sendMessage?.message}")
        } catch (e: ApolloException) {
            isMessageSending = false
            Timber.d("apolloResponse ${e.message}")
        } catch (ex: Exception) {
            isMessageSending = false
            Timber.d("General exception ${ex.message}")
        }
        //hideProgressView()
        if (res != null) {
            Log.e("ddd", Gson().toJson(res))
            if (!res.hasErrors()) {
                getCurrentUserDetails()
//                  updateCoinView("")
                isMessageSending = false
                Timber.d("apolloResponse ${res.hasErrors()} ${res.data?.sendMessage?.message}")
            } else {
                if (res.errors!![0].message.contains("Not enough coins")) {
                    binding.root.snackbar(
                        res.errors!![0].message,
                        Snackbar.LENGTH_INDEFINITE,
                        callback = {
                            findNavController().navigate(
                                destinationId = R.id.actionGoToPurchaseFragment,
                                popUpFragId = null,
                                animType = AnimationTypes.SLIDE_ANIM,
                                inclusive = true,
                            )
                        })
                } else {
                    Toast.makeText(
                        requireContext(),
                        "" + res.errors!![0].message,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
        binding.input.inputEditText.text = null
        isMessageSending = false
    }

    //flow.collect {
    //Timber.d("reealltime ${it.data?.chatSubscription?.message?.text}")
    //val adapter = binding.rvChatMessages.adapter as NewChatMessagesAdapter
    //adapter.addMessage(it.data?.chatSubscription?.message)
    //}

    private suspend fun cancelChatRoom() {
        Timber.d("reealltime detached 3")
        deferred.cancel()
        Timber.d("reealltime detached 4")
        try {
            val result = deferred.await()
            Timber.d("reealltime detached 5")
        } catch (e: CancellationException) {
            Timber.d("reealltime cancel room exception ${e.message}")
            // handle CancellationException if need
        } finally {
            // make some cleanup if need
            Timber.d("reealltime detached 6")
        }
    }

    override fun onChatMessageClick(position: Int, message: GetChatMessagesByRoomIdQuery.Edge?) {

        val url = message?.node?.content
        if (url?.contains("media/chat_files") == true) {
            var fullUrl = url
            var message = ""

            if (fullUrl.contains(" ")) {
                val link = fullUrl.substring(0, fullUrl.indexOf(" "))
                val giftMessage = fullUrl.substring(fullUrl.indexOf(" ") + 1)
                fullUrl = link
                message = giftMessage
            } else {
                if (url.startsWith("/media/chat_files/")) {
                    fullUrl = "${BuildConfig.BASE_URL}$url"
                }
            }

            val uri = Uri.parse(fullUrl)
            val lastSegment = uri.lastPathSegment
            val ext = lastSegment?.substring(lastSegment.lastIndexOf(".") + 1)
            Timber.d("clickk $lastSegment $ext")
            if (ext.contentEquals("jpg") || ext.contentEquals("png") || ext.contentEquals("jpeg")) {
//                val w = resources.displayMetrics.widthPixels
//                val h = resources.displayMetrics.heightPixels
//                showImageDialog(w, h, fullUrl)
                binding.imgUserStory1.visibility = View.VISIBLE
                binding.imgPlay.visibility = View.GONE
                binding.videoLayout.visibility = View.GONE

                if (ShowBottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                    ShowBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                    //setting user profile image and user name
                    binding.imgUserStory1.loadImage(fullUrl)

                } else {
                    ShowBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }

//                binding.lyDisplayChatImages.visibility = View.VISIBLE
//                binding.imgUserStory1.loadImage(fullUrl)
//
//                binding.imgClose.setOnClickListener {
//                    binding.lyDisplayChatImages.visibility = View.GONE
//                }


//                val dialog = PicViewerFragment()
//                val b = Bundle()
//
//                b.putString("url", fullUrl)
//                b.putString("mediatype", "image")
//                b.putString("otherUserName", requireArguments().getString("otherUserName"))
//                b.putString("otherUserPhotoUrl", requireArguments().getString("otherUserPhoto"))
//
//                dialog.arguments = b
//                dialog.show(childFragmentManager, "ChatpicViewer")



            } else {
//                val dialog = PicViewerFragment()
//                val b = Bundle()
//                b.putString("url", fullUrl)
//                b.putString("mediatype", "video")
//                b.putString("otherUserName", requireArguments().getString("otherUserName"))
//                b.putString("otherUserPhotoUrl", requireArguments().getString("otherUserPhoto"))
//
//                dialog.arguments = b
//                dialog.show(childFragmentManager, "ChatvideoViewer")

                binding.progressBar1.setOnTouchListener(View.OnTouchListener { v, event -> true })

                binding.videoview.visibility = View.VISIBLE
                binding.videoLayout.visibility = View.VISIBLE
                binding.progressBar1.visibility = View.VISIBLE
                binding.imgPlay.visibility = View.GONE
                binding.imgPlay.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_pause_circle_outline))

                val uri = Uri.parse(url)

                if (ShowBottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                    ShowBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                    //loading video player
                    isFirst = false
                    initExoPlayer(uri)

                } else {
                    ShowBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }



//                val downloadedFile = File(
//                    requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
//                    lastSegment!!
//                )
//                if (downloadedFile.exists()) {
//                    downloadedFile.openFile(requireActivity())
//                } else {
//                    DownloadUtil(this@MessengerNewChatFragment).downloadFile(
//                        fullUrl,
//                        lastSegment
//                    ) { downloadedFile ->
//                        Timber.d("downnfile ${downloadedFile?.exists()} ${downloadedFile?.absolutePath}")
//                        if (downloadedFile?.exists() == true) {
//                            downloadedFile.openFile(requireActivity())
//                        }
//                    }
//                }
            }
        }
    }

    override fun onChatUserAvtarClick() {
        gotoChatUserProfile()
    }

    private fun showImageDialog(width: Int, height: Int, imageUrl: String?) {
        val dialog = Dialog(requireContext())
        val dialogBinding = AlertFullImageBinding.inflate(layoutInflater, null, false)
        dialogBinding.fullImg.loadImage(imageUrl!!, {
            dialogBinding.alertTitle.setViewGone()
        }, {
            dialogBinding.alertTitle.text = it?.message
        })
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setLayout(width, height)
        dialog.show()
        dialogBinding.root.setOnClickListener {
            dialog.cancel()
        }
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction("gift_Received")
//        intentFilter.addAction("sent_message")
        activity?.registerReceiver(broadCastReceiver, intentFilter)
        Handler(Looper.getMainLooper()).postDelayed({
            kotlin.run {

                if (MainActivity.getMainActivity() != null) {
                    val notificationManager = MainActivity.getMainActivity()!!
                        .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancelAll()
                }
//                if(requireActivity()!=null) {
//                    val notificationManager = requireActivity()
//                        .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//                    notificationManager.cancelAll()
//                }
            }
        }, 1000)
        //   subscribeOnNewMessage()
        /*LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            broadcast!!, IntentFilter(Constants.INTENTACTION)
        );*/
    }

    private fun subscribeOnNewMessage() {
        val roomId = arguments?.getInt("chatId")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                apolloClientSubscription(requireActivity(), userToken!!).subscription(
                    OnNewMessageSubscription(roomId!!, userToken!!, "")
                ).toFlow().catch {
                    it.printStackTrace()
                    Timber.d("reealltime exception= ${it.message}")
                }
                    .retryWhen { cause, attempt ->
                        Timber.d("reealltime retry $attempt ${cause.message}")
                        Log.d(
                            "MainActivitySubscription",
                            "realtime retry  $attempt ${cause.message}"
                        )
                        delay(attempt * 1000)
                        true
                    }.collect { newMessage ->
                        if (newMessage.hasErrors()) {
                            Log.d(
                                "MainActivitySubscription",
                                "realtime response error = ${newMessage.errors?.get(0)?.message}"
                            )
                            Timber.d("reealltime response error = ${newMessage.errors?.get(0)?.message}")
                        } else {
                            Timber.d("reealltime onNewMessage ${newMessage.data?.onNewMessage?.message?.timestamp}")
                            Log.d(
                                "reealltime",
                                "reealltime ${newMessage.data?.onNewMessage?.message?.content}"
                            )
                            Log.e(
                                "Subscriptionreealltime",
                                "reealltime ${newMessage.data}"
                            )
                            Log.d(
                                "MainActivitySubscription",
                                "realtime messageData content ${newMessage.data?.onNewMessage?.message?.content}"
                            )
                            Log.d(
                                "MainActivitySubscription",
                                "realtime messageData ${newMessage.data}"
                            )

                            Log.d("TAG", "sendRESPOSE2: ")
//                            if (newMessage.data?.onNewMessage?.message != null) {
                            if (newMessage.data?.onNewMessage?.message != null) {
                                val avatarPhotos =
                                    edges.find { it?.node?.userId?.id == newMessage.data?.onNewMessage?.message?.userId?.id }?.node?.userId?.avatarPhotos
                                newMessage.data?.onNewMessage?.message?.let { message ->
                                    val edge = GetChatMessagesByRoomIdQuery.Edge(
                                        GetChatMessagesByRoomIdQuery.Node(
                                            id = message.id,
                                            content = message.content,
                                            //    appLanguageCode =message.appLanguageCode,
                                            roomId = GetChatMessagesByRoomIdQuery.RoomId(
                                                id = message.roomId.id,
                                                name = message.roomId.name
                                            ),
                                            timestamp = message.timestamp,
                                            userId = GetChatMessagesByRoomIdQuery.UserId(
                                                id = message.userId.id,
                                                username = message.userId.username,
                                                avatarIndex = message.userId.avatarIndex,
                                                avatarPhotos = avatarPhotos
                                            ),
                                            messageType = message.messageType,
                                            privatePhotoRequestId = message.privatePhotoRequestId,
                                            requestStatus = message.requestStatus
                                        )
                                        //                            GetChatMessagesByRoomIdQuery.Node(
                                        //                                id = message.id,
                                        //
                                        //
                                        //                                roomId = GetChatMessagesByRoomIdQuery.RoomId(
                                        //                                    id = message.roomId.id,
                                        //                                    name = message.roomId.name
                                        //                                ),
                                        //                                appLanguageCode = message.appLanguageCode,
                                        //                                timestamp = message.timestamp,
                                        //                                userId = GetChatMessagesByRoomIdQuery.UserId(
                                        //                                    id = message.userId.id,
                                        //                                    username = message.userId.username,
                                        //                                    avatarIndex = message.userId.avatarIndex,
                                        //                                    avatarPhotos = avatarPhotos
                                        //                                ),
                                        //                            )
                                    )

                                    //edges.add(0,edge)
                                    val listToAdd = checkForImageVideoInMessage(edge)
                                    edges.addAll(0,listToAdd)
                                    //notifyOnlyNew(edges = edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)
                                    /*              val chatId = arguments?.getInt("chatId", 0)!!
                                                  if (message.roomId.id.toInt()==chatId)
                                                      notifyAdapter(edges2 = edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)*/
                                    CoroutineScope(Dispatchers.Main).launch {
                                        /*adapter.updateList(
                                            edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?,
                                            true
                                        )*/
                                        //adapter.addItmToList(edges[0])
                                        adapter.addList(listToAdd)
                                        if (adapter.itemCount > 0) {
                                            Handler().postDelayed(Runnable {
                                                binding.rvChatMessages.layoutManager?.scrollToPosition(0)
                                            },100)
                                            //  Log.d("MainActivitySubscription","ScrollToPosition itemCount > 0")
                                        }

                                    }
                                    updateLastSeen(roomId)
                                }
                                //  viewModel.onNewMessage(newMessage = newMessage.data?.onNewMessage?.message)
                            } else {

                                lifecycleScope.launchWhenResumed {
                                    endCursor = ""
                                    hasNextPage = false

                                    updateCoinView("")
                                }
                            }
                        }
                    }
                Timber.d("reealltime 2")
            } catch (e2: Exception) {
                e2!!.message?.let { Log.e("exceptionFoundInScroll==>", it) }
                e2.printStackTrace()
                Log.d("MainActivitySubscription", "realtime exception= ${e2.message}")
                Timber.d("reealltime exception= ${e2.message}")
            }
        }
    }

    private fun subscribeOnDeleteMessage() {
        val roomId = arguments?.getInt("chatId")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                apolloClientSubscription(requireActivity(), userToken!!).subscription(
                    OnDeleteMessageSubscription(roomId!!, userToken!!, "")
                ).toFlow().catch {
                    it.printStackTrace()
                    Timber.d("reealltime exception= ${it.message}")
                }
                    .retryWhen { cause, attempt ->
                        Timber.d("reealltime retry $attempt ${cause.message}")
                        Log.d(
                            "MainActivitySubscription",
                            "realtime retry  $attempt ${cause.message}"
                        )
                        delay(attempt * 1000)
                        true
                    }.collect { newMessage ->
                        if (newMessage.hasErrors()) {
                            Log.d(
                                "MainActivitySubscription",
                                "realtime response error = ${newMessage.errors?.get(0)?.message}"
                            )
                            Timber.d("reealltime response error = ${newMessage.errors?.get(0)?.message}")
                        } else {
                            Timber.d("reealltime onNewMessage ${newMessage.data?.onDeleteMessage?.id}")
                            Log.d(
                                "reealltime",
                                "reealltime ${newMessage.data?.onDeleteMessage?.id}"
                            )
                            Log.e(
                                "reealltime",
                                "reealltime ${newMessage.data}"
                            )
                            Log.d(
                                "MainActivitySubscription",
                                "realtime DeleteMessage Id  ${newMessage.data?.onDeleteMessage?.id}"
                            )
                            Log.d(
                                "MainActivitySubscription",
                                "realtime  DeleteMessage Id  ${newMessage.data}"
                            )
                            val edge =
                                edges.find { it?.node?.id?.toInt() == newMessage.data?.onDeleteMessage?.id }
                            edges.remove(edge)
                            Log.e("notifyOnlyNewEdgeSize", "${edges.size}")
                            //notifyOnlyNew(edges = edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)
                            /*              val chatId = arguments?.getInt("chatId", 0)!!
                                          if (message.roomId.id.toInt()==chatId)
                                              notifyAdapter(edges2 = edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)*/



                            CoroutineScope(Dispatchers.Main).launch {
                                adapter.updateList(
                                    edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?,
                                    false,
                                    true
                                )
                                if (adapter.itemCount > 0) {
                                    //  Log.d("MainActivitySubscription","ScrollToPosition itemCount > 0")
                                    binding.rvChatMessages.layoutManager?.scrollToPosition(0)
                                }
//                                adapter.notifyItemChanged(edges.indexOf(edge))
                            }

                        }
                        //  viewModel.onNewMessage(newMessage = newMessage.data?.onNewMessage?.message)
                    }
                Timber.d("reealltime 2")
            } catch (e2: Exception) {
                e2.printStackTrace()
                Log.d("MainActivitySubscription", "realtime exception= ${e2.message}")
                Timber.d("reealltime exception= ${e2.message}")
            }
        }
    }

    private fun subscribeOnLastSeenMessage() {
        val roomId = arguments?.getInt("chatId")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                apolloClientSubscription(requireActivity(), userToken!!).subscription(
                    LastSeenMessageByReceiverSubscription(userToken!!, roomId!!)
                ).toFlow().catch {
                    it.printStackTrace()
                    Timber.d("reealltime exception= ${it.message}")
                }
                    .retryWhen { cause, attempt ->
                        Timber.d("reealltime retry $attempt ${cause.message}")
                        Log.d(
                            "MainActivitySubscription",
                            "realtime retry  $attempt ${cause.message}"
                        )
                        delay(attempt * 1000)
                        true
                    }.collect { newMessage ->
                        if (newMessage.hasErrors()) {

                            Log.d(
                                "MainActivitySubscription",
                                "Lastseen realtime response error = ${newMessage.errors?.get(0)?.message}"
                            )

                        } else {
                            Log.e("realetimeLastSeenException", "realetimeLastSeenException")
                            Log.d(
                                "MainActivitySubscription",
                                "realtime Last seen Id  ${newMessage.data?.onSeenLastMessageByReceiver?.message}"
                            )
                            Log.d(
                                "MainActivitySubscription",
                                "realtime  Last seen Id  ${newMessage.data}"
                            )
                            val edge =
                                edges.find { it?.node?.id == newMessage.data?.onSeenLastMessageByReceiver?.message?.id }
                            adapter.otherUserAvtar = otherUserProfile
                            var previousPosition = 0
                            val previousLastSeenMessage =
                                edges.filter { it?.node?.id == adapter.lastSeenMessageId }
                            if (previousLastSeenMessage.isNotEmpty()) {
                                previousPosition = edges.indexOf(previousLastSeenMessage[0])
                            }
                            if (edge != null) {
                                adapter.lastSeenMessageId = edge.node?.id.toString()
                                CoroutineScope(Dispatchers.Main).launch {
                                    adapter.notifyItemChanged(previousPosition)
                                    adapter.notifyItemChanged(edges.indexOf(edge))
                                }
                            } else {
                                adapter.lastSeenMessageId =
                                    newMessage.data?.onSeenLastMessageByReceiver?.message?.id.toString()
                                CoroutineScope(Dispatchers.Main).launch {
                                    adapter.notifyItemChanged(previousPosition)
                                }
                            }
                            //notifyOnlyNew(edges = edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)
                            /*              val chatId = arguments?.getInt("chatId", 0)!!
                                          if (message.roomId.id.toInt()==chatId)
                                              notifyAdapter(edges2 = edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)*/
                            /*     CoroutineScope(Dispatchers.Main).launch {
                                     adapter.updateList(edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)
                                     if (adapter.itemCount > 0) {
                                         //  Log.d("MainActivitySubscription","ScrollToPosition itemCount > 0")
                                         binding.rvChatMessages.layoutManager?.scrollToPosition(0)
                                     }
                                 }*/
                        }
                        //  viewModel.onNewMessage(newMessage = newMessage.data?.onNewMessage?.message)
                    }
                Timber.d("reealltime 2")
            } catch (e2: Exception) {
                e2.printStackTrace()
                Log.d("MainActivitySubscription", "lastseen realtime exception= ${e2.message}")
                Timber.d("reealltime exception= ${e2.message}")
            }
        }
    }

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            if (intent!!.action.equals("gift_Received")) {
                val extras = intent.extras
                val state = extras!!.getString("extra")
                //  updateCoinView(state.toString())
            }
//            if (intent.action.equals("sent_message")){
//                Log.e("Sent_message", "onReceive: " )
//                getChatMessages()
//            }
        }
    }

    private fun updateCoinView(toString: String) {
//        getCurrentUserDetails()
//        showProgressView()
        getChatMessages(true)
        getCurrentUserDetails()
    }

    override fun onPause() {
        super.onPause()
        // Constants.hideKeyboard(requireActivity())
        // LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(broadcast!!);
    }

    private val locPermissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun showErrorInLocation(jsonObject: Resource<ResponseBody<JsonObject>>) {
        Log.e("myShareLocation1", Gson().toJson(jsonObject))
//        if (jsonObject.message != null && jsonObject.message!!.contains("Not enough coins")) {

        if (jsonObject.message != null && jsonObject.message!!.contains("Not enough coins..")) {

            binding.root.snackbar(
                AppStringConstant1.no_enough_coins,
//                getString(R.string.no_enough_coins),
                Snackbar.LENGTH_INDEFINITE,
                callback = {

                    findNavController().navigate(
                        destinationId = R.id.actionGoToPurchaseFragment,
                        popUpFragId = null,
                        animType = AnimationTypes.SLIDE_ANIM,
                        inclusive = true,

                        )
                })

        }else{
            getCurrentUserDetails()
        }

//        val dialog = Dialog(requireContext())
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        dialog.setCancelable(true)
//        dialog.setContentView(R.layout.dialog_message_confirm)
//        dialog.findViewById<TextView>(R.id.title).text = activity!!.resources.getString(R.string.buy_coins)
//        dialog.findViewById<TextView>(R.id.description).text = jsonObject!!.message!!
//        dialog.findViewById<LinearLayoutCompat>(R.id.cd_submit).setOnClickListener {
//            MainActivity.getMainActivity()!!.openCoinScreen()
//            dialog.dismiss()
//        }
//        dialog.findViewById<LinearLayoutCompat>(R.id.cd_cancel).setOnClickListener {
//
//            dialog.dismiss()
//        }


    }

    private fun shareCurrentLocation() {
        val locationService = LocationServices.getFusedLocationProviderClient(requireContext())
        locationService.lastLocation.addOnSuccessListener { location: Location? ->
            var lat: Double? = location?.latitude
            var lon: Double? = location?.longitude
//                toast("lat = $lat lng = $lon")

            // Update Location
            if (lat != null && lon != null) {
                // Share Location
                lifecycleScope.launch(Dispatchers.Main) {
                    val roomId = arguments?.getInt("chatId")
                    //viewModel.shareLocation("$lat,$lon", roomId!!, userId!!, userToken!!)
                    viewModel.shareLocation(
                        "$lat,$lon",
                        roomId!!,
                        "bb2bb0d9-9c84-44ce-889c-7707c1cd7387",
                        userToken!!
                    )
                        .let { data -> showErrorInLocation(data) }

                }
            } else {

                val priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                val cancellationTokenSource = CancellationTokenSource()


                locationService.getCurrentLocation(priority, cancellationTokenSource.token)
                    .addOnSuccessListener { location ->

                        if (location == null)
                            binding.root.snackbar(
                                AppStringConstant1.location_enable_message,
//                                getString(R.string.location_enable_message),
                                Snackbar.LENGTH_INDEFINITE,
                                callback = {
                                    shareLocation()
//                        context?.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                })
                        else {
                            lat = location.latitude
                            lon = location.longitude
                            lifecycleScope.launch(Dispatchers.Main) {
                                val roomId = arguments?.getInt("chatId")
                                //viewModel.shareLocation("$lat,$lon", roomId!!, userId!!, userToken!!)
                                viewModel.shareLocation(
                                    "$lat,$lon",
                                    roomId!!,
                                    "bb2bb0d9-9c84-44ce-889c-7707c1cd7387",
                                    userToken!!
                                )
                                    .let { data -> showErrorInLocation(data) }

                            }

                        }

                        Log.d("Location", "location is found: $location")
                    }
                    .addOnFailureListener { exception ->
                        Log.d("Location", "Oops location failed with exception: $exception")
                    }
//                binding.root.snackbar(
//                    getString(R.string.location_enable_message),
//                    Snackbar.LENGTH_INDEFINITE,
//                    callback = {
//                        shareLocation()
////                        context?.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
//                    })
                Log.e("locationNotFound", "locationNotFound")
            }

        }
    }

    private fun shareLocation() {
        if (hasPermissions(requireContext(), locPermissions)) {

            if (isLocationEnabled()) {
                shareCurrentLocation()
            } else {
                Log.e("gpsNotAllowes", "gpsNotAllowes")
                enableLocation()
            }

        } else {
            permissionReqLauncher.launch(locPermissions)
//            requestPermissions(locPermissions, 101)
            //TODO Add alert message for enable permission
        }
    }

    private fun enableLocation() {
        val locationRequest = LocationRequest.create()
        locationRequest.apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 30 * 1000.toLong()
            fastestInterval = 5 * 1000.toLong()
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result =
            LocationServices.getSettingsClient(requireContext())
                .checkLocationSettings(builder.build())
        result.addOnCompleteListener {
            try {
                val response: LocationSettingsResponse = it.getResult(ApiException::class.java)
                println("location>>>>>>> ${response.locationSettingsStates.isGpsPresent}")
                if (response.locationSettingsStates.isGpsPresent) {
                    shareLocation()
                }
                //do something
            } catch (e: ApiException) {
                when (e.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val intentSenderRequest =
                            e.status.resolution?.let { it1 ->
                                IntentSenderRequest.Builder(it1).build()
                            }
                        launcher.launch(intentSenderRequest)
                    }
                    catch (e: IntentSender.SendIntentException) {
                    }
                }
            }
        }.addOnCanceledListener {

        }
    }

    private var launcher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("okayDialog", "OK")
//                shareCurrentLocation()
                shareLocation()
            } else {
                Log.d("CancelDialog", "CANCEL")
                binding.root.snackbar(
                    AppStringConstant1.location_enable_message,
//                    getString(R.string.location_enable_message),
                    Snackbar.LENGTH_INDEFINITE,
                    callback = {

//                        context?.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    })
//            requireContext().toast("Please Accept Location enable for use this App.")
            }
        }


    private fun isLocationEnabled(): Boolean {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        return locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }
//    private fun shareLocation() {
//        var isPermissionGranted = hasPermissions(requireContext(), locPermissions)
//        if (isPermissionGranted) {
//            val locationService = LocationServices.getFusedLocationProviderClient(requireContext())
////          var  mLocationRequest = LocationRequest.create()
////            mLocationRequest!!.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
////            mLocationRequest!!.setInterval(LOCATION_REQUEST_INTERVAL).fastestInterval =
////                LOCATION_REQUEST_INTERVAL
////            locationService.getCurrentLocation()
//            val lm = requireContext()
//                .getSystemService(Context.LOCATION_SERVICE) as LocationManager
//
//            var gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
//            var network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
//
//            if(gps_enabled){
//
//            locationService.lastLocation.addOnSuccessListener { location: Location? ->
//                var lat: Double? = location?.latitude
//                var lon: Double? = location?.longitude
//                if (lat != null && lon != null) {
//                    // Share Location
//                    lifecycleScope.launch(Dispatchers.Main) {
//                        val roomId = arguments?.getInt("chatId")
//                        //viewModel.shareLocation("$lat,$lon", roomId!!, userId!!, userToken!!)
//                        viewModel.shareLocation(
//                            "$lat,$lon",
//                            roomId!!,
//                            "bb2bb0d9-9c84-44ce-889c-7707c1cd7387",
//                            userToken!!
//                        )
//                            .let { data -> showErrorInLocation(data) }
//
//                    }
//                }
//
//            }
//
//            }else{
//                binding.root.snackbar(
//                    getString(R.string.location_enable_message),
//                    Snackbar.LENGTH_INDEFINITE,
//                    callback = {
//
//                        context?.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
//                    })
//
//            }
//
//        } else {
//            requestPermissions(locPermissions, 101)
//            //TODO Add alert message for enable permission
//        }
//    }


    private fun subscribeonUpdatePrivatePhotoRequest() {

        lifecycleScope.launch(Dispatchers.IO) {

            try {
                apolloClientSubscription(requireActivity(), userToken!!).subscription(
                    OnUpdatePrivateRequestSubscription()
                ).toFlow().catch {
                    it.printStackTrace()
                    Timber.d("reealltime exception= ${it.message}")
                }
                    .retryWhen { cause, attempt ->
                        Timber.d("reealltime retry $attempt ${cause.message}")
                        Log.d(
                            "nUpdatePrivatePhotoRequest",
                            "realtime retry  $attempt ${cause.message}"
                        )
                        delay(attempt * 1000)
                        true
                    }.collect { newMessage ->
                        if (newMessage.hasErrors()) {
                            Log.e(
                                "nUpdatePrivatePhotoRequest",
                                "realtime response error = ${newMessage.errors?.get(0)?.message}"
                            )
                            Timber.d("reealltime response error = ${newMessage.errors?.get(0)?.message}")
                        } else {
                            Timber.d("reealltime onNewMessage ${newMessage.data?.onUpdatePrivateRequest?.requestedUser}")
                            Log.d(
                                "reealltime",
                                "reealltime ${newMessage.data?.onUpdatePrivateRequest?.requestedUser}"
                            )
                            Log.e(
                                "reealltime",
                                "reealltime ${newMessage.data}"
                            )
                            Log.e(
                                "nUpdatePrivatePhotoRequest",
                                "realtime DeleteMessage Id  ${newMessage.data?.onUpdatePrivateRequest?.requestedUser}"
                            )
                            Log.e(
                                "nUpdatePrivatePhotoRequest",
                                "realtime  DeleteMessage Id  ${newMessage.data}"
                            )

                            lifecycleScope.launchWhenResumed {

                                updateCoinView("")
                            }


                        }
                        //  viewModel.onNewMessage(newMessage = newMessage.data?.onNewMessage?.message)
                    }
                Timber.d("reealltime 2")
            } catch (e2: Exception) {
                e2.printStackTrace()
                Log.d("nUpdatePrivatePhotoRequest", "realtime exception= ${e2.message}")
                Timber.d("reealltime exception= ${e2.message}")
            }
        }
    }

    var isFirst: Boolean = false

    fun initExoPlayer(videouri: Uri?) {

        // showProgressView()
        binding.progressBar.visibility=View.VISIBLE

        exoPlayer = SimpleExoPlayer.Builder(requireActivity()).build()
        binding.videoview.player = exoPlayer
        val mediaItem = MediaItem.fromUri(videouri!!)
        exoPlayer!!.setMediaItem(mediaItem)
        exoPlayer!!.prepare()
        exoPlayer!!.play()

        val durationSet = false

        exoPlayer!!.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_READY && !durationSet) {

                    binding.progressBar.visibility = View.GONE

                    if (!isFirst) {
                        isFirst = true
                        val realDurationMillis: Long = exoPlayer!!.getDuration()
                        binding.progressBar1.valueFrom = 0.0f
                        binding.progressBar1.valueTo = realDurationMillis.toFloat()

                        val millisInFuture = realDurationMillis
                        timer1 = object : CountDownTimerExt(millisInFuture, 100) {
                            override fun onTimerTick(millisUntilFinished: Long) {

                                val diffTime: Long = millisInFuture - millisUntilFinished
                                val elepsedTime: Long = 0 + diffTime

                                Log.d("MainActivity!!", "onTimerTick $millisUntilFinished")
                                onTickProgressUpdate(elepsedTime)
                            }

                            override fun onTimerFinish() {
                                Log.d("MainActivity!", "onTimerFinish")
                                ShowBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                            }
                        }
                        timer1!!.start()
                    } else {
                        if (exoPlayer!!.isPlaying) {
                            timer1!!.pause()
                        } else {
                            timer1!!.start()
                        }
                    }
                }
            }

        })
    }


    private fun onTickProgressUpdate(long: Long) {
//        countUp += 100
//        val progress = countUp
//        Timber.d("prggress $progress")
        binding.progressBar1.setValues(long.toFloat())
    }

    private fun releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer!!.stop();
            exoPlayer!!.release();
            /*if (videoview!!.getPlayer() != null) {
                videoview!!.getPlayer()!!.stop();
                videoview!!.getPlayer()!!.release();
            }*/
        }
        if (timer1 != null) {
            timer1!!.pause()
        }
    }
}
