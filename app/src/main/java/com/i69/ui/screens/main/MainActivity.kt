package com.i69.ui.screens.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.content.*
import android.location.Location
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.text.format.DateUtils
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.InputMethodManager
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.size
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.apollographql.apollo3.exception.ApolloException
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.*
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.ktx.messaging
import com.google.gson.Gson
import com.i69.*
import com.i69.applocalization.*
import com.i69.data.models.User
import com.i69.data.remote.repository.UserDetailsRepository
import com.i69.data.remote.repository.UserUpdateRepository
import com.i69.databinding.ActivityMainBinding
import com.i69.profile.db.dao.UserDao
import com.i69.singleton.App
import com.i69.ui.base.BaseActivity
//import com.i69.ui.screens.MY_REQUEST_CODE
import com.i69.ui.screens.SplashActivity
import com.i69.ui.screens.main.coins.PurchaseFragment
import com.i69.ui.screens.main.messenger.chat.MessengerNewChatFragment
import com.i69.ui.screens.main.moment.PlayUserStoryDialogFragment
import com.i69.ui.screens.main.moment.UserStoryDetailFragment
import com.i69.ui.screens.main.notification.NotificationDialogFragment
import com.i69.ui.screens.main.search.SearchInterestedInFragment
import com.i69.ui.screens.main.search.userProfile.SearchUserProfileFragment
import com.i69.ui.viewModels.UserMomentsModelView
import com.i69.ui.viewModels.UserViewModel
import com.i69.ui.views.MyBottomNavigation
import com.i69.utils.*
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.PaymentMethodsActivityStarter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.random.Random


@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(), BottomNavigationView.OnNavigationItemSelectedListener {

    private val mViewModel: UserViewModel by viewModels()


    lateinit var navController: NavController
    private var navController2: LiveData<NavController>? = null

    @Inject
    lateinit var userDetailsRepository: UserDetailsRepository

    @Inject
    lateinit var userUpdateRepository: UserUpdateRepository

    @Inject
    lateinit var userDao: UserDao
    private var mUser: User? = null
    private var userId: String? = null
    private var userToken: String? = null
    private var chatUserId: Int = 0
    private lateinit var job: Job
    private val viewModel: UserViewModel by viewModels()
    var bottomNav1: MyBottomNavigation? = null
    var userprofile: String = ""
    private val drawerSelectedItemIdKey = "DRAWER_SELECTED_ITEM_ID_KEY"
    private var drawerSelectedItemId = R.id.nav_search_graph
    val mViewModelUser: UserMomentsModelView by viewModels()
    lateinit var pref: SharedPreferences
    var totoalUnreadBadge = 0

    //    lateinit var paymentLauncher:  PaymentLauncher
    private var stripe: Stripe? = null

    private val viewModessl: AppStringConstantViewModel by viewModels()
    private var remoteMessage: RemoteMessage? = null

    var searchInterestedInFragment = SearchInterestedInFragment()

    var width = 0
    var size = 0

    public val permissionReqLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
        showLoader()
            if (hasLocationPermission(this, locPermissions)) {
                val locationService =
                    LocationServices.getFusedLocationProviderClient(this@MainActivity)
                locationService.lastLocation.addOnSuccessListener { location: Location? ->
                    val lat: Double? = location?.latitude
                    val lon: Double? = location?.longitude
                    hideProgressView()
//                toast("lat = $lat lng = $lon")
                    if (lat != null && lon != null) {
                        // Update Location
                        lifecycleScope.launch(Dispatchers.Main) {
                            var res = mViewModel.updateLocation(
                                userId = userId!!,
                                location = arrayOf(lat, lon),
                                token = userToken!!
                            )
                            //Log.e("aaaaaaaaz",""+res.data!!.errorMessage)
                            Log.e("aaaaaaaaz", "" + res.message)
                        }
                    }
                }.addOnFailureListener { hideProgressView() }
            }
            else hideProgressView()
        }

    override fun getActivityBinding(inflater: LayoutInflater) =
        ActivityMainBinding.inflate(inflater)

    fun updateLanguageTranslation() {

        lifecycleScope.launch {
            val sharedPref = SharedPref(this@MainActivity)
            var consted = sharedPref.getAttrTranslater()
            if (consted == null) {
                consted = getLoalizations(this@MainActivity, isUpdate = true)
            }

//        binding.lifecycleOwner = this@MainActivity
            binding.invalidateAll()
            AppStringConstant(this@MainActivity).feed = consted.feed
            Log.e("callTranslation", "callTranslation ==> ${consted.interested_in}")

            Log.e("AppStringConstant", "callTranslation ==> ${AppStringConstant1.interested_in}")
            viewModessl.data.postValue(consted)

            Log.e(
                "callTranslation",
                "callTranslation1 ==> ${AppStringConstant(this@MainActivity).interested_in}"
            )

            updateLanguageChanged()
        }
    }

    override fun setupTheme(savedInstanceState: Bundle?) {
        Log.e("splash_act_main","setupTheme")
        window?.decorView?.setOnApplyWindowInsetsListener { view, insets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets, view)
            MainActivity.getMainActivity()?.binding?.bottomNavigation?.isGone =
                insetsCompat.isVisible(
                    WindowInsetsCompat.Type.ime()
                )
            view.onApplyWindowInsets(insets)
        }
        updateLanguageTranslation()
//        getStripePayMentIntent()
        mainActivity = this
        pref = PreferenceManager.getDefaultSharedPreferences(mainActivity)
        val lang = Locale.getDefault().language
        Log.d("DefaultPickerLng", pref.getString("language", null) ?: "null")
        if (pref.getString("language", null) == null) {

            lifecycleScope.launch(Dispatchers.Main) {
                pref.edit()
                    ?.putString("language", lang)
                    ?.apply()
                async(Dispatchers.IO) {
//                            result?.list?.let { userRepository.insertDropDownData(it) }
                    val res = viewModel.updateLanguage(
                        languageCode = if (lang == "pt") "pt_pt" else lang,
                        userid = getCurrentUserId()!!,
                        token =  getCurrentUserToken()!!
                    )
                    //     Log.d("DefaultPicker","langauage")
                    //    Log.d("DefaultPicker","Language message ${res.message} data ${res.data} code ${res.code}")
                }.await()

            }
        }



        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setViewModel(mViewModel, binding)
        if (savedInstanceState == null) {
            setupBottomNav()
            //drawerSelectedItemId = getInt(drawerSelectedItemIdKey, drawerSelectedItemId)
        }
        savedInstanceState?.let {
            drawerSelectedItemId = it.getInt(drawerSelectedItemIdKey, drawerSelectedItemId)
        }
        setupNavigation()
        notificationOpened = true
//        showProgressView()
        //updateNavItem(R.drawable.ic_default_user)
        updateFirebaseToken(userUpdateRepository)

        lifecycleScope.launch(Dispatchers.Main) {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            Timber.d("UserId $userId!!")
            Timber.d("UserId1 $userToken!!")
            Log.d("MainActivity", "Usertoken $userToken!!")
            Log.d("MainActivity", "UserId $userId!!")
            getBroadcastMessageBadge()
            getWelcomeMessageBadge()
            updateChatBadge()
            loadUser()
            handleNotificationClick()
        }

        checkAppUpdate()
    }

    fun loadUser() {
        mViewModel.getCurrentUser(userId!!, token = userToken!!, true)
            .observe(this@MainActivity) { user ->
                Timber.d("User $user")
                user?.let {
                    try {
                        mUser = it
                        mUser!!.id = "$userId"
                        updateNavItem(
                            mUser!!.avatarPhotos!!.get(mUser!!.avatarIndex!!).url?.replace(
                                "${BuildConfig.BASE_URL}media/",
                                "${BuildConfig.BASE_URL}media/"
                            ).toString()
                        )
//                            mUser = it
//                            updateNavItem(
//                                mUser!!.avatarPhotos!!.get(mUser!!.avatarIndex!!).url.replace(
//                                    "${BuildConfig.BASE_URL}media/",
//                                    "${BuildConfig.BASE_URL}media/"
//                                )
//                            )
                    } catch (e: Exception) {
                        Timber.e("${e.message}")
                    }


                }
            }
    }

    fun handleNotificationClick() {
        if ((intent.hasExtra("isNotification") && intent.getStringExtra("isNotification") != null)) {
            Log.e("notii", "--> " + "11")
            binding.bottomNavigation.selectedItemId = R.id.nav_home_graph
            try {
                val dataValues = JSONObject(intent.getStringExtra("notificationData"))
                navigateByType(dataValues, dataValues.getInt("pk"))
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else if ((intent.hasExtra("isChatNotification") && intent.getStringExtra("isChatNotification") != null)) {
            if ((intent.hasExtra("roomIDs") && intent.getStringExtra("roomIDs") != null)) {
                val rID = intent.getStringExtra("roomIDs")
                pref.edit().putString("roomIDNotify", "true").putString("roomID", rID).apply()
                binding.bottomNavigation.selectedItemId = R.id.nav_chat_graph
            }
        }
    }

    private fun exitOnError() {
        lifecycleScope.launch(Dispatchers.Main) {
            App.userPreferences.clear()
            val intent = Intent(this@MainActivity, SplashActivity::class.java)
            startActivity(intent)
            finishAffinity()
            return@launch
        }
    }


    //Implemented to Close Keyboard on touch outside of Edittext Fields
    /*override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // all touch events close the keyboard before they are processed except EditText instances.
        // if focus is an EditText we need to check, if the touchevent was inside the focus editTexts
        val currentFocus = currentFocus
        if (currentFocus !is EditText || !isTouchInsideView(ev, currentFocus)) {
            hideKeyboard()
        }
        return super.dispatchTouchEvent(ev)
    }*/

    private fun hideKeyboard() {

        // TODO Auto-generated method stub
        val im = this
            .getApplicationContext().getSystemService(
                INPUT_METHOD_SERVICE
            ) as InputMethodManager
        im.hideSoftInputFromWindow(
            this.getWindow().getDecorView()
                .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    private fun isTouchInsideView(ev: MotionEvent, currentFocus: View): Boolean {
        val loc = IntArray(2)
        currentFocus.getLocationOnScreen(loc)
        return (ev.rawX > loc[0] && ev.rawY > loc[1] && ev.rawX < loc[0] + currentFocus.width
                && ev.rawY < loc[1] + currentFocus.height)
    }

    fun navigateByType(dataValues: JSONObject, pkId: Int) {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(this@MainActivity, userToken!!).query(
                    GetNotificationQuery(pkId)
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception all stories${e.message}")
                binding.root.snackbar(" ${e.message}")
                return@launchWhenResumed
            }
            Timber.d("apolloResponse GetNotificationQuery  ${res.hasErrors()}")
            Log.e("GetNotificationQuery", "-->" + Gson().toJson(res))
            if (res.hasErrors()) {
                if (res.errors!![0].nonStandardFields!!["code"].toString() == "InvalidOrExpiredToken") {
                    return@launchWhenResumed exitOnError()
                }
            }
            if (dataValues.has("notification_type")) {
                when (dataValues.getString("notification_type")) {
                    "LIKE", "CMNT", "MMSHARED" -> {
                        //Moment Liked
                        //Comment on Moment
                        //Moment shared
                        val momentid = dataValues.get("momentId").toString()
                        getMoments(momentid)
                    }

                    "STSHARED", "STLIKE", "STCMNT" -> {
                        //story shared
                        //Story Like
                        //Story Commented
                        val storyId = if (dataValues.has("storyID")) {
                            dataValues.get("storyID").toString()
                        } else {
                            dataValues.get("storyId").toString()
                        }
                        getStories(storyId)
                    }

                    "PROFILEVISIT" -> {
                        val followUserId = dataValues.getString("visited_user_id")
                        if (userId == followUserId) {
                            binding.bottomNavigation.selectedItemId = R.id.nav_user_profile_graph
                        } else {
                            openOtherProfileScreen(followUserId)
                        }
                    }

                    "USERFOLLOW" -> {
                        val followUserId = dataValues.getString("followerID")
                        if (userId == followUserId) {
                            binding.bottomNavigation.selectedItemId = R.id.nav_user_profile_graph
                        } else {
                            openOtherProfileScreen(followUserId)
                        }
                    }

                    "SNDMSG" -> {
                        //Gift received
                        binding.bottomNavigation.selectedItemId = R.id.nav_user_profile_graph
                        updateChatBadge()
                    }
                }
            } else if (dataValues.has("data")) {
                //handle new user joined notification
                val jsonObject = dataValues.getJSONObject("data")
                if (jsonObject.has("notification_type")) {
                    if (jsonObject.getString("notification_type") == "SM") {
                        if (dataValues.has("user_id")) {
                            val id = dataValues.getString("user_id")
                            if (userId == id) {
                                binding.bottomNavigation.selectedItemId =
                                    R.id.nav_user_profile_graph
                            } else {
                                openOtherProfileScreen(id)
                            }
                        }
                    }
                }
            }
        }
    }

    fun openOtherProfileScreen(followUserId: String) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
        if (currentFragment != null) {
            val bundle = Bundle()
            bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
            bundle.putString("userId", followUserId)
            currentFragment.findNavController().navigate(
                destinationId = R.id.action_global_otherUserProfileFragment,
                popUpFragId = null,
                animType = AnimationTypes.SLIDE_ANIM,
                inclusive = true,
                args = bundle
            )
        }
    }

    private fun getMoments(ids: String) {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(this@MainActivity, userToken!!).query(
                    GetAllUserParticularMomentsQuery(
                        width,
                        size,
                        ids
                    )
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception all moments ${e.message}")
                binding.root.snackbar(" ${e.message}")
                return@launchWhenResumed
            }
            Log.e("ddd5ddddd", "-->" + Gson().toJson(res))
            if (res.hasErrors()) {
                if (res.errors!![0].nonStandardFields!!["code"].toString() == "InvalidOrExpiredToken") {
                    // error("User doesn't exist")
                    return@launchWhenResumed exitOnError()
                }
            }

            val allmoments = res.data?.allUserMoments!!.edges
            allmoments.indices.forEach { i ->
                if (ids == allmoments[i]!!.node!!.pk.toString()) {
                    val navHostFragment =
                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val currentFragment =
                        navHostFragment.childFragmentManager.primaryNavigationFragment
                    if (currentFragment != null) {
                        val bundle = Bundle().apply {
                            putString("momentID", allmoments[i]?.node!!.pk!!.toString())
                            putString("filesUrl", allmoments[i]?.node!!.file!!)
                            putString("Likes", allmoments[i]?.node!!.like!!.toString())
                            putString("Comments", allmoments[i]?.node!!.comment!!.toString())
                            val gson = Gson()
                            putString(
                                "Desc",
                                gson.toJson(allmoments[i]?.node!!.momentDescriptionPaginated)
                            )
                            putString("fullnames", allmoments[i]?.node!!.user!!.fullName)
                            if (allmoments[i]!!.node!!.user!!.gender != null) {
                                putString("gender", allmoments[i]!!.node!!.user!!.gender!!.name)
                            } else {
                                putString("gender", null)
                            }
                            putString("momentuserID", allmoments[i]?.node!!.user!!.id.toString())
                        }
                        currentFragment.findNavController()
                            .navigate(R.id.momentsAddCommentFragment, bundle)
                    }
                    return@forEach
                }
            }
        }
    }

    private fun getStories(storyId: String) {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(this@MainActivity, userToken!!).query(
                    GetAllUserStoriesQuery(
                        100,
                        "",
                        storyId,
                        ""
                    )
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception all stories ${e.message}")
                binding.root.snackbar(" ${e.message}")
                return@launchWhenResumed
            }
            Timber.d("apolloResponse allUserStories stories ${res.hasErrors()}")
            Log.e("ddd", "-->" + Gson().toJson(res))
            if (res.hasErrors()) {
//                Log.e("ddddddww", "-->" + res.errors!!.get(0).nonStandardFields!!["code"])
                try {
                    if (res.errors?.get(0)?.nonStandardFields!!["code"]?.toString() == "InvalidOrExpiredToken") {
                        // error("User doesn't exist")
                        return@launchWhenResumed exitOnError()
                    }
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }
            val allUserStories = res.data?.allUserStories!!.edges
            allUserStories.indices.forEach { i ->
                if (storyId == allUserStories[i]!!.node!!.pk?.toString()) {
                    val userStory = allUserStories[i]
                    val formatter =
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                    Timber.d("filee ${userStory?.node!!.fileType} ${userStory.node.file}")
                    val url = "${BuildConfig.BASE_URL}media/${userStory.node.file}"
                    var userurl = ""
                    userurl =
                        if (userStory.node.user!!.avatar != null && userStory.node.user!!.avatar!!.url != null) {
                            userStory.node.user.avatar!!.url!!
                        } else {
                            ""
                        }
                    val username = userStory.node.user!!.fullName
                    val UserID = userId
                    val objectId = userStory.node.pk
                    var text = userStory.node.createdDate.toString()
                    text = text.replace("T", " ").substring(0, text.indexOf("."))
                    val momentTime = formatter.parse(text)
                    val times = DateUtils.getRelativeTimeSpanString(
                        momentTime.time,
                        Date().time,
                        DateUtils.MINUTE_IN_MILLIS
                    )
                    if (userStory.node.fileType.equals("video")) {
                        val dialog = PlayUserStoryDialogFragment(
                            object : UserStoryDetailFragment.DeleteCallback {
                                override fun deleteCallback(objectId: Int) {

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
                        dialog.arguments = b
                        dialog.show(supportFragmentManager, "${AppStringConstant1.story}")
                    } else {
                        val dialog = UserStoryDetailFragment(null)
                        val b = Bundle()
                        b.putString("Uid", UserID)
                        b.putString("url", url)
                        b.putString("userurl", userurl)
                        b.putString("username", username)
                        b.putString("times", times.toString())
                        b.putString("token", userToken)
                        b.putInt("objectID", objectId!!)
                        dialog.arguments = b
                        dialog.show(supportFragmentManager, "${AppStringConstant1.story}")
                    }
                    return@forEach
                }
            }
        }
    }

    fun updateLanguageChanged() {


        lifecycleScope.launch() {

//                Log.e("u id", "-->" + getCurrentUserId())
//                Log.e("userToken", "-->" + getCurrentUserToken())
//                userId = getCurrentUserId()!!
            var userToken = App.userPreferences.userToken.first()
            Timber.i("usertokenn $userToken")

            Log.e("callProgressBarActivity", "callProgressBarActivity")
            var locaLizationString = getLoalizationsStringList()

            if (userToken.isNullOrEmpty()) {
                var consted =
                    getLoalizations(this@MainActivity, isUpdate = true)
                viewModessl.data.postValue(consted)
            } else {
                val res = try {
                    apolloClient(this@MainActivity, userToken!!).query(
                        AttrTranslationQuery(
                            locaLizationString
                        )
                    )
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponse ${e.message}")
                    Toast.makeText(this@MainActivity, " ${e.message}", Toast.LENGTH_LONG)
                        .show()

//                hideProgressView()
                    return@launch
                }
                if (res.hasErrors()) {
                } else {


                    var consted =
                        getLoalizations(this@MainActivity, res.data?.attrTranslation)

//                  Log.e(":responsegetted", Gson().toJson( res.data))
                    Log.e("responsegetted", Gson().toJson(consted))
                    viewModessl.data.postValue(consted)
                    val sharedPref = SharedPref(this@MainActivity)
                    sharedPref.setAttrTranslater(consted)
                    updateLoalizationsConstString(this@MainActivity, consted)

//                    CoroutineScope(Dispatchers.Main).launch {
//                        delay(500)
//                        startActivity(Intent(this@ProgressBarActivity, MainActivity::class.java))
//                        finish()
//                    }
                }
            }


//            if (res?.hasErrors() == false) {
////                                views.snackbar("You bought ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} successfully!")
//                Toast.makeText(
//                    requireContext(),
//                    context?.resources?.getString(R.string.you_bought) + " ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} " + context?.resources?.getString(
//                        R.string.successfully
//                    ),
//                    Toast.LENGTH_LONG
//                ).show()
//
//                //fireGiftBuyNotificationforreceiver(gift.id,giftUserid!!)
//
//            }
//            if (res!!.hasErrors()) {
////                                views.snackbar(""+ res.errors!![0].message)
//                Toast.makeText(
//                    requireContext(),
//                    "" + res.errors!![0].message,
//                    Toast.LENGTH_LONG
//                ).show()
//
//            }
//            Timber.d("apolloResponse ${res.hasErrors()} ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName}")
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PaymentMethodsActivityStarter.REQUEST_CODE) {

            data?.let {
                val result = PaymentMethodsActivityStarter.Result.fromIntent(data)

                val paymentMethod = result?.paymentMethod
//                Log.e(TAG, "paymentMethodId : " + paymentMethod?.id)
//                paymentMethodId = paymentMethod?.id.toString()
//                paymentSession?.handlePaymentData(requestCode, resultCode, it)
            }
        } else {
            if (stripe != null) {
                stripe?.onPaymentResult(
                    requestCode,
                    data,
                    object : ApiResultCallback<PaymentIntentResult> {
                        override fun onSuccess(result: PaymentIntentResult) {                           
                            val paymentIntent = result.intent

                            when (paymentIntent.status) {
                                StripeIntent.Status.Succeeded -> {
                                    Log.e("TAG_Request_Result", "Payment Success")

                                    val navHostFragment =
                                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                                    val currentFragment =
                                        navHostFragment.childFragmentManager.primaryNavigationFragment
                                    if (currentFragment != null && currentFragment is PurchaseFragment) {
                                        var purchaseFragment =
                                            currentFragment as PurchaseFragment
                                        purchaseFragment.paymentIntentComplete(paymentIntent)
                                    }
                                }

                                StripeIntent.Status.RequiresPaymentMethod -> {
                                    hideProgressView()
//                                    binding.root.snackbar("Payment Failed " + paymentIntent.lastPaymentError?.message)
                                    paymentIntent.lastPaymentError?.message?.let {
                                        binding.root.snackbar(
                                            it
                                        )
                                    }
                                    Log.e(
                                        "TAG_Request_Result",
                                        "Payment Failed " + paymentIntent.lastPaymentError?.message
                                    )
                                }

                                else -> {
                                    hideProgressView()
//                                    binding.root.snackbar("Payment status unknown " + paymentIntent.status)
                                    showDialog("${paymentIntent.status}")

//                                    binding.root.snackbar("${paymentIntent.status}")
                                    Log.e(
                                        "TAG_Request_Result",
                                        "Payment status unknown " + paymentIntent.status
                                    )
                                }
                            }
                        }

                        override fun onError(e: Exception) {
                            hideProgressView()
//                            binding.root.snackbar("Payment Error " + e.localizedMessage)
                            showDialog("${e.localizedMessage}")
//                            binding.root.snackbar(e.localizedMessage)
//                            Log.e(
//                                "TAG_Request_Result",
//                                "Payment status unknown " + e.message
//                            )
//
                            Log.e("TAG_Request_Result", "Payment Error " + e.localizedMessage)
                        }
                    })

            }
        }
    }


    //    private fun getStripePayMentIntent() {
//        val paymentConfiguration = PaymentConfiguration.getInstance(this@MainActivity)
////
//    paymentLauncher =     PaymentLauncher.Companion.create(
//            this@MainActivity,
//            paymentConfiguration.publishableKey,
//            paymentConfiguration.stripeAccountId,
////            PaymentLauncher.PaymentResultCallback {
////                purchaseFragment.onPaymentResult(it)
////            }
//            ::onPaymentResult
//        )
//
//    }
//
//
    fun setStripePayMentIntent(

        striped: Stripe
    ) {

        stripe = striped

//        purchaseFragment.setPaymentLauncherForStripe(paymentLauncher)
    }

    private fun loadUserMomentsData() {
        userToken?.let {
            mViewModelUser.getAllMoments(this@MainActivity, it, 100, 100, 10, "") { error ->
                if (error == null) {

                } else {

                }

            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(drawerSelectedItemIdKey, drawerSelectedItemId)
        super.onSaveInstanceState(outState)
    }

    fun getmsgsubscriptionlistner() {
        job = lifecycleScope.launch {
            viewModel.newMessageFlow.collect { message ->
                message?.let { newMessage ->
                    Log.e("fff", "fffff")
                    if (userId != message.userId.id) {
                        Log.e("fff", "fffff222")
                        if (mContextTemp != null) {
                            Log.e("fff", "fffff333")
                            sendNotification(message)
                        }
                        updateChatBadge()
                    }

                }
            }
        }
    }

    fun updateFirebaseToken(userUpdateRepository: UserUpdateRepository) {

        Firebase.messaging.token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Timber.d("App", "Fetching FCM registration token failed")
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("FirebaseToken", "" + token)
            Timber.d("FirebaseToken", "" + token)
            token?.let {
                GlobalScope.launch {
                    val userId = App.userPreferences.userId.first()
                    val userToken = App.userPreferences.userToken.first()

                    if (userId != null && userToken != null) {
                        var res = userUpdateRepository.updateFirebasrToken(userId, token, userToken)
                        getmsgsubscriptionlistner()

                        Timber.d("TOKEN")
                    }
                }
            }

//Toast.makeText(mInstance, token, Toast.LENGTH_SHORT).show()
        })
    }

    private fun sendNotification(message: ChatRoomSubscription.Message) {
        notificationOpened = false
        val intent = Intent(App.getAppContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK
        val id2 = message.roomId.id

        intent.putExtra("isChatNotification", "yes")
        intent.putExtra("roomIDs", id2)


        var pendingIntent: PendingIntent? = null
        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val soundUri: Uri

        soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        //  soundUri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.iphone_ringtone);

        Log.e("iconn andr", "" + BuildConfig.BASE_URL + message.userId.avatarPhotos?.get(0)!!.url)
        //   val url = URL(BuildConfig.BASE_URL+ message.userId.avatarPhotos?.get(0)!!.url)
        //   val image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val mBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, getString(R.string.app_name))
                .setSmallIcon(R.drawable.icon_buy_chat_coins)
                .setColor(resources.getColor(R.color.colorPrimary))
//                .setContentTitle(message.roomId.name)
                .setSound(soundUri)
                .setAutoCancel(true)
                //  .setLargeIcon(image)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setPriority(PRIORITY_HIGH)

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.app_name)
            val description = getString(R.string.app_name)
            val importance = IMPORTANCE_HIGH
            val channel = NotificationChannel(getString(R.string.app_name), name, importance)
            channel.description = description
            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            channel.enableLights(true)
            channel.enableVibration(true)
            channel.setSound(soundUri, attributes)

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationManager = NotificationManagerCompat.from(this)

        val myContentView = RemoteViews(packageName, R.layout.layout_notification_content_expanded)
        val myExpandedContentView =
            RemoteViews(packageName, R.layout.layout_notification_content_expanded)
        myContentView.setTextViewText(R.id.content_title, message.userId.fullName)
        myExpandedContentView.setTextViewText(R.id.content_title, message.userId.fullName)
        if (message.content.contains("media/chat_files")) {
            val ext = message.content.findFileExtension()
            val stringResId =
                if (ext.isImageFile()) {
                    R.string.photo
                } else if (ext.isVideoFile()) {
                    R.string.video
                } else {
                    R.string.file
                }
            val icon =
                if (ext.isImageFile()) {
                    R.drawable.ic_photo
                } else if (ext.isVideoFile()) {
                    R.drawable.ic_video
                } else {
                    R.drawable.ic_baseline_attach_file_24
                }
            myContentView.setTextViewText(R.id.content_message, getString(stringResId))
            myContentView.setImageViewResource(R.id.myimage, icon)
            myContentView.setViewVisibility(R.id.myimage, VISIBLE)

            myExpandedContentView.setTextViewText(R.id.content_message, getString(stringResId))
            myExpandedContentView.setImageViewResource(R.id.myimage, icon)
            myExpandedContentView.setViewVisibility(R.id.myimage, VISIBLE)

//            mBuilder.setContentText(message.userId.fullName+" : "+message.content)
        } else {
            myContentView.setTextViewText(R.id.content_message, message.content)
            myContentView.setViewVisibility(R.id.myimage, GONE)

            myExpandedContentView.setTextViewText(R.id.content_message, message.content)
            myExpandedContentView.setViewVisibility(R.id.myimage, GONE)
//            mBuilder.setContentText(message.userId.fullName+" : "+message.content)
        }

        if (!message.userId.avatarPhotos.isNullOrEmpty()) {
            if (message.userId.avatarPhotos[0] != null && message.userId.avatarPhotos[0]!!.url != null) {
                Log.e("rrr", "111111")
                loadImage(
                    this,
                    BuildConfig.BASE_URL + message.userId.avatarPhotos.get(0)!!.url,
                    { bitmap ->
//                    myContentView.setImageViewBitmap(R.id.iv_profile, bitmap)
                        Log.e("rrrccc", "111111")
                        myContentView.setImageViewBitmap(R.id.iv_profile, bitmap)
//                    mBuilder.setLargeIcon(bitmap)
                        mBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
//                    mBuilder.setContentTitle(message.userId.fullName)
                        mBuilder.setCustomContentView(myContentView)
                        //mBuilder.setCustomBigContentView(myExpandedContentView)
                        // notificationId is a unique int for each notification that you must define
                        notificationManager.notify(Random.nextInt(), mBuilder.build())
                    },
                    { drawable ->
                        if (drawable != null) {
                            Log.e("rrrccc", "22222")
                            //This Bitmap conversion code is in working state
//                        val bitmap = AppCompatResources.getDrawable(applicationContext, R.drawable.ic_default_user)?.toBitmap()
//                        mBuilder.setLargeIcon(bitmap)
                            myContentView.setImageViewResource(
                                R.id.iv_profile,
                                R.drawable.ic_default_user
                            )
                            myExpandedContentView.setImageViewResource(
                                R.id.iv_profile,
                                R.drawable.ic_default_user
                            )
                        }
                        mBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
//                    mBuilder.setContentTitle(message.userId.fullName)
                        mBuilder.setCustomContentView(myContentView)
                        // mBuilder.setCustomBigContentView(myExpandedContentView)
                        // notificationId is a unique int for each notification that you must define
                        notificationManager.notify(Random.nextInt(), mBuilder.build())
                    })
            } else {
                Log.e("rrr", "22222")
                myContentView.setImageViewResource(R.id.iv_profile, R.drawable.login_logo)
                myExpandedContentView.setImageViewResource(R.id.iv_profile, R.drawable.login_logo)
                mBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                myExpandedContentView.setViewVisibility(R.id.iv_profile, GONE)
                myContentView.setViewVisibility(R.id.iv_profile, GONE)
                //This Bitmap conversion code is in working state
//                val bitmap = AppCompatResources.getDrawable(applicationContext, R.drawable.ic_default_user)?.toBitmap()
//                mBuilder.setLargeIcon(bitmap)
                mBuilder.setCustomContentView(myContentView)
                //  mBuilder.setCustomBigContentView(myExpandedContentView)
                // notificationId is a unique int for each notification that you must define
                notificationManager.notify(Random.nextInt(), mBuilder.build())
            }
        } else {
            Log.e("rrr", "3333")
            myContentView.setImageViewResource(R.id.iv_profile, R.drawable.ic_default_user)
            myExpandedContentView.setImageViewResource(R.id.iv_profile, R.drawable.ic_default_user)
            mBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())

            //This Bitmap conversion code is in working state
//                val bitmap = AppCompatResources.getDrawable(applicationContext, R.drawable.ic_default_user)?.toBitmap()
//                mBuilder.setLargeIcon(bitmap)
            mBuilder.setCustomContentView(myContentView)
            //mBuilder.setCustomBigContentView(myExpandedContentView)
            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(Random.nextInt(), mBuilder.build())
        }
        /*if(WEB_URL.matcher(message.content).matches()){
            mBuilder.setContentText(message.userId.fullName+" : ")
            loadImage(this, message.content, { bitmap ->
                mBuilder.setLargeIcon(bitmap)
                mBuilder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
                // notificationId is a unique int for each notification that you must define
                notificationManager.notify(Random.nextInt(), mBuilder.build())
            }, { drawable ->
                if(drawable != null){
                    mBuilder.setSmallIcon(R.drawable.ic_default_user)
                }
                // notificationId is a unique int for each notification that you must define
                notificationManager.notify(Random.nextInt(), mBuilder.build())
            })
        }else{
            mBuilder.setContentText(message.userId.fullName+" : "+message.content)
        }*/
    }

    private fun sendNotification(title: String, message: String) {
        Log.e("fdfdf", "fd")
        notificationOpened = false
        val intent = Intent(App.getAppContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK
        val id2 = "fdsfsd"

        intent.putExtra("isChatNotification", "yes")
        intent.putExtra("roomIDs", id2)


        var pendingIntent: PendingIntent? = null
        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val soundUri: Uri

        soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

//        soundUri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.iphone_ringtone);

        val mBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_launcher)
//                .setContentTitle(message.roomId.name)
                .setSound(soundUri)

                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setPriority(PRIORITY_HIGH)

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.app_name)
            val description = getString(R.string.app_name)
            val importance = IMPORTANCE_HIGH
            val channel = NotificationChannel(getString(R.string.app_name), name, importance)
            channel.description = description
            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            channel.enableLights(true)
            channel.enableVibration(true)
            channel.setSound(soundUri, attributes)

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationManager = NotificationManagerCompat.from(this)

        if (message.contains("media/chat_files")) {
            val ext = message.findFileExtension()
            val stringResId =
                if (ext.isImageFile()) {
                    R.string.photo
                } else if (ext.isVideoFile()) {
                    R.string.video
                } else {
                    R.string.file
                }
            val icon =
                if (ext.isImageFile()) {
                    R.drawable.ic_photo
                } else if (ext.isVideoFile()) {
                    R.drawable.ic_video
                } else {
                    R.drawable.ic_baseline_attach_file_24
                }
            val myContentView =
                RemoteViews(packageName, R.layout.layout_notification_content_expanded)
            myContentView.setImageViewResource(R.id.myimage, icon)
            myContentView.setTextViewText(R.id.content_title, title)
            myContentView.setTextViewText(R.id.content_message, getString(stringResId))
//            mBuilder.setContentText(message.userId.fullName+" : "+message.content)
            mBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            mBuilder.setCustomContentView(myContentView)
            mBuilder.setCustomBigContentView(myContentView)
        } else {
            mBuilder.setContentText("UserName : " + message)
        }
        notificationManager.notify(Random.nextInt(), mBuilder.build())
    }

    fun getWelcomeMessageBadge() {
        lifecycleScope.launchWhenResumed {
            val resFirstMessage = try {
                apolloClient(this@MainActivity, userToken!!).query(GetFirstMessageQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception getFirstMessage${e.message}")
//                binding.root.snackbar("Exception getFirstMessage ${e.message}")
                binding.root.snackbar(" ${e.message}")
                //hideProgressView()
                return@launchWhenResumed
            }

            if (!resFirstMessage.hasErrors()) {
                if (resFirstMessage.data?.firstmessage != null) {


                    //addBadge(0)
                    if (totoalUnreadBadge == 0) {
                        totoalUnreadBadge = resFirstMessage.data?.firstmessage?.unread!!.toInt()
                    } else {
                        totoalUnreadBadge =
                            totoalUnreadBadge + resFirstMessage.data?.firstmessage?.unread!!.toInt()
                    }
                    addUnReadBadge()

                }

            }
        }
    }

    fun getBroadcastMessageBadge() {
        lifecycleScope.launchWhenResumed {
            val resBroadcast = try {
                apolloClient(this@MainActivity, userToken!!).query(GetBroadcastMessageQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception getBroadcastMessage${e.message}")
                binding.root.snackbar(" ${e.message}")
                //hideProgressView()
                return@launchWhenResumed
            }

            val allRoom = resBroadcast.data?.broadcast
//            val allRoom = res.data?.rooms?.edges


            if (allRoom != null) {
                //addBadge(0)
                if (totoalUnreadBadge == 0 && allRoom?.unread!!.toInt() > 0) {
                    totoalUnreadBadge = 1//allRoom?.unread!!.toInt()
                } else {
                    if (totoalUnreadBadge > 0 && allRoom?.unread!!.toInt() > 0) {
                        totoalUnreadBadge + 1//data!!.node!!.unread!!.toInt()
                    } else
                        totoalUnreadBadge
//                    totoalUnreadBadge = totoalUnreadBadge + 1 //allRoom?.unread!!.toInt()
                }
                addUnReadBadge()

            }

        }
    }

    fun updateChatBadge() {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(this@MainActivity, userToken!!).query(GetAllRoomsQuery(20)).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception all room API${e.message}")
                binding.root.snackbar(" ${e.message}")
                return@launchWhenResumed
            }

            val allRoom = res.data?.rooms?.edges
            Log.e("TAG", "updateChatBadge: " + res.data?.rooms?.toString() )



            if (allRoom.isNullOrEmpty()) {
                //addBadge(0)
//                binding.bottomNavigation.addBadge(0)
                return@launchWhenResumed
            }

            totoalUnreadBadge = 0
            allRoom.indices.forEach { i ->
                val data = allRoom[i]
                if (totoalUnreadBadge == 0 && data!!.node!!.unread!!.toInt() > 0) {
                    totoalUnreadBadge = 1//data!!.node!!.unread!!.toInt()
                } else {
                    if ( data!!.node!!.unread!!.toInt() > 0) {
                        totoalUnreadBadge + 1//data!!.node!!.unread!!.toInt()
                    }
//                    if (totoalUnreadBadge > 0 && data!!.node!!.unread!!.toInt() > 0) {
//                        totoalUnreadBadge + 1//data!!.node!!.unread!!.toInt()
//                    } else
//                        totoalUnreadBadge
                    //totoalUnreadBadge = totoalUnreadBadge + 1//data!!.node!!.unread!!.toInt()
                }
            }
            addUnReadBadge()
        }
    }

    fun addUnReadBadge() {
        try {
            binding.bottomNavigation.addBadge(totoalUnreadBadge)
            //binding.navView.updateMessagesCount(totoalunread)
        } catch (e: Exception) {
            Log.e("exceptionInAddBadge", "${e.message}")
            e.printStackTrace()
        }
    }

    override fun setupClickListeners() {
        registerReceiver(notiBroadcastReceiver, IntentFilter(ACTION_NEW_NOTIFICATION),
            Context.RECEIVER_NOT_EXPORTED
        )
        binding.clNotification.setOnClickListener {
            remoteMessage?.let { messageBody ->
                try {
                    val obj = JSONObject(messageBody.data.toString())
                    if (obj.length() != 0) {
                        val dataValues =
                            JSONObject(messageBody.data.toString()).getJSONObject("data")
                        if (dataValues.length() != 0) {
                            if (dataValues.has("roomID")) {
                                val rID = dataValues.getString("roomID")
                                pref.edit().putString("roomIDNotify", "true")
                                    .putString("roomID", rID).apply()
                                binding.bottomNavigation.selectedItemId = R.id.nav_chat_graph
                            } else {
                                navigateByType(dataValues, dataValues.getInt("pk"))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            binding.clNotification.visibility = GONE
            handler.removeCallbacks(runnable)
        }

        binding.ivDismissNotification.setOnClickListener { binding.clNotification.visibility = GONE }
    }

    private fun updateLanguage(id: String, token: String) {
        val deviceLocale = Locale.getDefault().language
        lifecycleScope.launch(Dispatchers.Main) {

            when (val response = viewModel.updateLanguage(
                languageCode = deviceLocale,
                userid = id,
                token = token
            )) {
                is Resource.Success -> {
                    updateLanguageChanged()
                    Timber.e("${"LanguageUpdate Success"} ${response.message}")
                }

                is Resource.Error -> {
                    Timber.e("${"LanguageUpdate Failed"} ${response.message}")
                    binding.root.snackbar("${"LanguageUpdate Failed"} ${response.message}")
                }

                else -> {

                }
            }
        }

        val sharedPref = SharedPref(applicationContext)
        sharedPref.setLanguage(true)
        sharedPref.setLanguageFromSettings(true)

    }


    override fun onDestroy() {
        super.onDestroy()
        if (this::job.isInitialized) {
            job.cancel()
        }
        handler.removeCallbacks(runnable)
        unregisterReceiver(notiBroadcastReceiver)
        //unregisterQbChatListeners()
    }

    fun refreshActivity() {
        finish()
        startActivity(intent)
    }

    private fun setupNavigation() {
        Log.e("splash_act_main","setupNavigation")
        updateLocation()
        binding.mainNavView.getHeaderView(0).findViewById<View>(R.id.btnHeaderClose)
            .setOnClickListener {
                //disableNavigationDrawer()
                if (binding.drawerLayout.isOpen) {
                    binding.drawerLayout.close()
                } else {
                    navController.popBackStack()
                }
            }
        /*  KeyboardUtils.addKeyboardToggleListener(this) { isVisible ->
             // binding.navView.visibility = if (isVisible) GONE else VISIBLE
          }*/
        binding.mainNavView.itemIconTintList = null
        binding.mainNavView.setNavigationItemSelectedListener {
            Handler(Looper.getMainLooper()).postDelayed({
                when (it.itemId) {
                    R.id.nav_search_graph -> openSearchScreen()
                    R.id.nav_coinpurchase_graph -> openCoinScreen()
                    R.id.nav_coinplan_graph -> openPlanScreen()
                    R.id.nav_item_contact -> openContactusScreen()
//                    R.id.nav_item_contact -> this.startEmailIntent(
//                        com.i69app.data.config.Constants.ADMIN_EMAIL,
//                        "",
//                        ""
//                    )
                    R.id.nav_privacy_graph -> {
                        openPrivacyScreen()
                      /*  val intent = Intent(this, PrivacyOrTermsConditionsActivity::class.java)
                         intent.putExtra("type", "privacy")
                         startActivity(intent)*/
                    }

                    R.id.nav_setting_graph -> openSettingsScreen()
                }
            }, 200)

            binding.drawerLayout.closeNavigationDrawer()
            return@setNavigationItemSelectedListener true
        }
    }

    private fun openContactusScreen() {
//        val intent = Intent(applicationContext, WebGoogleMapActivity::class.java)
//        intent.putExtra("url","https://chatadmin-mod.click/#/contactUs")//+"&"+"daddr="+latitude.toString()+","+longitude.toString())
////        intent.setPackage("com.google.android.apps.maps")
//        startActivity(intent)
        Log.e("splash_act_main","openContactusScreen")
        pref.edit()?.putString("typeview", "privacy")?.apply()
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
        currentFragment?.findNavController()?.navigate(R.id.actionGoToContactFragment)
    }

    private fun observeNotification() {
        if ((intent.hasExtra("isNotification") && intent.getStringExtra("isNotification") != null)) {
            Log.e("notii", "--> " + "11")
            //val bundle = Bundle()
            //bundle.putString("ShowNotification", "true")
            //navController.navigate(R.id.action_user_moments_fragment, bundle)
            //newActionHome
            pref.edit().putString("ShowNotification", "true").apply()
            binding.bottomNavigation.selectedItemId = R.id.nav_home_graph
            /*val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
            if (currentFragment != null) {
                currentFragment.findNavController().navigate(R.id.newActionHome,bundle)
            }*/
        } else if ((intent.hasExtra("isChatNotification") && intent.getStringExtra("isChatNotification") != null)) {
            Log.e("notii", "--> " + "22")
            Log.e("notii", "--> " + "22-->" + intent.getStringExtra("roomIDs"))

            if ((intent.hasExtra("roomIDs") && intent.getStringExtra("roomIDs") != null)) {
                val rID = intent.getStringExtra("roomIDs")

                /* val bundle = Bundle()
                 bundle.putString("roomIDNotify", rID)
                 //navController.navigate(R.id.messengerListFragment, bundle)
                 val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                 val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
                 if (currentFragment != null) {
                     currentFragment.findNavController().navigate(R.id.newAction,bundle)
                 }*/
                pref.edit().putString("roomIDNotify", "true").putString("roomID", rID).apply()
                binding.bottomNavigation.selectedItemId = R.id.nav_chat_graph
            }
            //binding.root.snackbar("Chat Message Notification cliked.")
        } else if (intent.hasExtra(ARGS_SCREEN) && intent.getStringExtra(ARGS_SCREEN) != null) {
            Log.e("notii", "--> " + "33")
            if (intent.hasExtra(ARGS_SENDER_ID) && intent.getStringExtra(ARGS_SENDER_ID) != null) {
                val senderId = intent.getStringExtra(ARGS_SENDER_ID)
                onNotificationClick(senderId!!)

            } else {
                openMessagesScreen()
            }
        }
    }
    private lateinit var appUpdateManager: AppUpdateManager
    private  val MY_REQUEST_CODE = 101

    private fun checkAppUpdate()
    {
        appUpdateManager = AppUpdateManagerFactory.create(this)
          val appUpdateInfoTask = appUpdateManager.appUpdateInfo
       // Log.e("splash_act", "158")


         appUpdateInfoTask
             .addOnSuccessListener { appUpdateInfo ->
                 if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                     && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                 )
                     startUpdate(appUpdateInfo)
             }
             .addOnFailureListener {
                 Log.e("splash_act", "167")
             }

    }

    private fun startUpdate(appUpdateInfo: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            AppUpdateType.FLEXIBLE,
            this,
            MY_REQUEST_CODE
        )
    }

    private fun onNotificationClick(senderId: String) {
//        val msgPreviewModel: MessagePreviewModel? = QbDialogHolder.getChatDialogById(senderId)
//        msgPreviewModel?.let {
//            viewModel?.setSelectedMessagePreview(it)
//            navController.navigate(R.id.globalUserToChatAction)
//        }
    }

    fun drawerSwitchState() {
        binding.drawerLayout.drawerSwitchState()
    }

    fun enableNavigationDrawer() {
        binding.drawerLayout.enableNavigationDrawer()
    }

    fun disableNavigationDrawer() {
        binding.drawerLayout.disableNavigationDrawer()
    }

    fun reloadNavigationMenu() {
        binding.mainNavView.menu.clear();
        binding.mainNavView.inflateMenu(R.menu.activity_main_drawer);
    }

    fun setDrawerItemCheckedUnchecked(id: Int?) {
        //binding.mainNavView.setCheckedItem(id!!)
        // binding.mainNavView.menu.getItem(0).isChecked = false;

        if (id != null)
            binding.mainNavView.setCheckedItem(id)
        else {
            val size = binding.mainNavView.menu.size
            for (i in 0 until size)
                binding.mainNavView.menu.getItem(i).isChecked = false
        }
    }

    private fun updateNavItem(userAvatar: Any?) {
        userprofile = userAvatar.toString()
       //  binding.bottomNavigation.selectedItemId = R.id.nav_search_graph
        binding.bottomNavigation.loadImage(
            userprofile, R.id.nav_user_profile_graph, R.drawable.ic_default_user
        )

        /* binding.navView.setItems(
             arrayListOf(
                 Pair(R.drawable.ic_search_inactive, R.drawable.ic_search_active),
                 Pair(R.drawable.ic_home_inactive, R.drawable.ic_home_active),
                 Pair(R.drawable.ic_add_btn, R.drawable.icon_add_black_button),
                 Pair(R.drawable.ic_chat_inactive, R.drawable.ic_chat_active),
                 Pair(userAvatar, userAvatar)
             )
         )*/
    }

    private fun updateLocation() {
        /*Permissions.check(this, Manifest.permission.ACCESS_FINE_LOCATION, null, object : PermissionHandler() {
            override fun onGranted() {
                val locationService = LocationServices.getFusedLocationProviderClient(this@MainActivity)
                locationService.lastLocation.addOnSuccessListener { location: Location? ->
                    val lat: Double? = location?.latitude
                    val lon: Double? = location?.longitude
                    toast("lat = $lat lng = $lon")
                    if (lat != null && lon != null) {
                        // Update Location
                        lifecycleScope.launch(Dispatchers.Main) {
                            mViewModel.updateLocation(userId = userId!!, location = arrayOf(lat, lon), token = userToken!!)
                        }
                    }
                }
            }
        })*/
        if (hasLocationPermission(applicationContext, locPermissions)) {
            val locationService = LocationServices.getFusedLocationProviderClient(this@MainActivity)
            locationService.lastLocation.addOnSuccessListener { location: Location? ->
                val lat: Double? = location?.latitude
                val lon: Double? = location?.longitude
//                toast("lat = $lat lng = $lon")
                if (lat != null && lon != null) {
                    // Update Location
                    lifecycleScope.launch(Dispatchers.Main) {
                        var res = mViewModel.updateLocation(
                            userId = userId!!,
                            location = arrayOf(lat, lon),
                            token = userToken!!
                        )

                        if (res.message.equals("User doesn't exist")) {
                            //error("User doesn't exist")
                            lifecycleScope.launch(Dispatchers.Main) {
                                userPreferences.clear()
                                //App.userPreferences.saveUserIdToken("","","")
                                val intent = Intent(this@MainActivity, SplashActivity::class.java)
                                startActivity(intent)
                                finishAffinity()
                            }
                        }
                        //  Log.e("aaaaaaaa",""+res.data!!.errorMessage)
                        Log.e("aaaaaaaa", "" + res.message)

                    }
                }
            }
        } else {
            permissionReqLauncher.launch(locPermissions)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    var mContextTemp: Context? = null
    override fun onPause() {
        super.onPause()
        mContextTemp = null
        isAppInFront = false

    }

    override fun onResume() {
        super.onResume()
        isAppInFront = true
        mContext = this@MainActivity
        mContextTemp = this@MainActivity
        if (!notificationOpened) {
            notificationOpened = true
            observeNotification()
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction("com.my.app.onMessageReceived")
        intentFilter.addAction("gift_Received_1")
        intentFilter.addAction("notification_received")
        registerReceiver(broadCastReceiver, intentFilter,
            Context.RECEIVER_NOT_EXPORTED)

//        val intentFilter1 = IntentFilter()
//        intentFilter1.addAction("notification_received")
//        registerReceiver(broadCastReceiver1, intentFilter1)
//        setDrawerItemCheckedUnchecked(null)
    }

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            if(intent!!.action!!.equals("gift_Received_1")) {
                updateChatBadge()
            }
            if(intent!!.action!!.equals("notification_received")) {
                if(dialog != null){
                    val notificationId = intent.getStringExtra("notification_id")
                    if(dialog != null && dialog!!.isVisible && notificationId != null)
                        dialog!!.addnotification(notificationId)
                }
            }
        }
    }
//private val broadCastReceiver1 = object : BroadcastReceiver() {
//        override fun onReceive(contxt: Context?, intent: Intent?) {
//            if(dialog != null){
//                dialog!!.addnotification(intent)
//            }
//        }
//    }

    private fun goToMainActions(position: Int) {
        when (position) {
            0 -> openSearchScreen()
            1 -> openUserMoments()
            2 -> openNewUserMoment()
            3 -> openMessagesScreen()
            4 -> openProfileScreen()
        }
    }

     fun openSearchScreen() {
         Log.e("splash_act_main","openSearchScreen")
        //navController.navigate(R.navigation.nav_search_graph)
        binding.bottomNavigation.selectedItemId = R.id.nav_search_graph
        //navController.setGraph(R.id.nav_chat_graph)
        //navController.navigate(R.id.action_global_search_interested_in)
        //navController.popBackStack(R.id.action_global_search_interested_in,true)
    }

    fun openUserMoments() {
        //val bundle = Bundle()
        //bundle.putString("ShowNotification", "false")
        //navController.navigate(R.id.action_user_moments_fragment, bundle)
        binding.bottomNavigation.selectedItemId = R.id.nav_home_graph
    }

    private fun openNewUserMoment() {
        //navController.navigate(R.id.action_new_user_moment_fragment)
    }

    private fun openMessagesScreen() {
        //navController.navigate(R.id.messengerListAction)
        binding.bottomNavigation.selectedItemId = R.id.nav_chat_graph
    }

    fun openProfileScreen() {
        //navController.navigate(R.id.action_global_user_profile)
        binding.bottomNavigation.selectedItemId = R.id.nav_user_profile_graph
    }

    private fun openPrivacyScreen() {
        Log.e("splash_act_main","openPrivacyScreen")
        pref.edit()?.putString("typeview", "privacy")?.apply()
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
        if (currentFragment != null) {
            currentFragment.findNavController().navigate(R.id.actionGoToPrivacyFragment)
        }
    }

    fun openPlanScreen() {
        Log.e("splash_act_main","openPlanScreen")
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
        if (currentFragment != null) {
            currentFragment.findNavController().navigate(R.id.action_global_plan)
        }

    }

    fun openCoinScreen() {
        //navController.navigate(R.id.nav_coinpurchase_graph)
        /*val graph = navController.navInflater.inflate(R.navigation.nav_coinpurchase_graph)
         graph.startDestination = R.id.purchaseFragment
         navController.graph = graph*/
        Log.e("splash_act_main","openCoinScreen")
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
        if (currentFragment != null) {
            currentFragment.findNavController().navigate(R.id.action_global_purchase)
        }
    }

    fun openSettingsScreen() {
        //binding.bottomNavigation.selectedItemId = R.id.nav_setting_graph
        //navController.navigate(R.id.actionGoToSettingsFragment)
        //navController = navHostFragment.navController
        //navController.setGraph(R.navigation.nav_setting_graph)
        Log.e("splash_act_main","openSettingsScreen")
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
        if (currentFragment != null) {
            //   toast("currentFragment not null")
            currentFragment.findNavController().navigate(R.id.action_global_setting)
        } else {
            //   toast("currentFragment null")
        }
    }


    // Chat Section (Companion Object)
    companion object {
        var mContext: Context? = null
        const val CHAT_TAG = "SH_CHAT"
        const val ARGS_SCREEN = "screen"
        const val ARGS_MESSAGE_SCREEN = "message_screen"
        const val ARGS_SENDER_ID = "sender_id"
        const val ARGS_CHANNEL_ID = "5f2f7e32-cf68-4a8e-b27b-41b692aab5b1"

        var notificationOpened = false
        private var viewModel: UserViewModel? = null
        private var binding: ActivityMainBinding? = null

        var mainActivity: MainActivity? = null

        var isAppInFront = false
        const val ACTION_NEW_NOTIFICATION = BuildConfig.APPLICATION_ID + ".new_notification"
        const val KEY_REMOTE_MSG = "remoteMessage"

        @JvmName("getMainActivity1")
        fun getMainActivity(): MainActivity? {
            return mainActivity
        }


        fun setViewModel(updatedViewModel: UserViewModel, updatedBinding: ActivityMainBinding) {
            viewModel = updatedViewModel
            binding = updatedBinding
        }

        /*private fun updateUnseenMessages(previewMsgList: ArrayList<MessagePreviewModel>?) {
            if (!previewMsgList.isNullOrEmpty()) {
                var unseenMsgCount = 0
                previewMsgList.forEach { msgPreview ->
                    if (msgPreview.chatDialog.unreadMessageCount > 0) unseenMsgCount =
                        unseenMsgCount.plus(1)
                }

                //binding!!.navView.updateMessagesCount(unseenMsgCount)
                binding?.bottomNavigation?.addBadge(unseenMsgCount)
                viewModel!!.updateAdapterFlow()

            }
        }*/


    }

    private val locPermissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        setupBottomNav()
        //setupNavigation()
    }


    fun addBadge(number: Int) {
        val badge = bottomNav1?.getOrCreateBadge(R.id.nav_chat_graph)
        if (number > 0) {
            if (badge != null) {
                badge.isVisible = true
                badge.number = number
//                 badge.badgeTextColor = R.color.white
                badge.backgroundColor = ContextCompat.getColor(this, R.color.colorPrimary)
            }
        } else {
            if (badge != null) {
                badge.isVisible = false
                badge.clearNumber()
            }
        }

    }

    private fun removeBadge() {
        bottomNav1?.removeBadge(R.id.nav_chat_graph)
    }

    @SuppressLint("RestrictedApi")
    private fun setupBottomNav() {


        val graphIds = listOf(
            R.navigation.nav_search_graph,
            R.navigation.nav_home_graph,
            R.navigation.nav_add_new_moment_graph,
            R.navigation.nav_chat_graph,
            R.navigation.nav_user_profile_graph
        )
        val bottomNav = findViewById<MyBottomNavigation>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener(this)
        bottomNav1 = bottomNav


        /*val controller = bottomNav.setupWithNavController(
            graphIds,
            supportFragmentManager,
            R.id.nav_host_fragment,
            intent
        )*/
        binding.bottomNavigation.itemIconTintList = null

        val controller = bottomNav.setupWithNavController(
            fragmentManager = supportFragmentManager,
            navGraphIds = graphIds,
            backButtonBehaviour = BackButtonBehaviour.POP_HOST_FRAGMENT,
            containerId = R.id.nav_host_fragment,
            firstItemId = R.id.nav_search_graph, // Must be the same as bottomNavSelectedItemId
            intent = this@MainActivity.intent
        )
        controller.observe(this) {

            setupActionBarWithNavController(it)
        }
        navController2 = controller

        val navigationItemView = bottomNav.getChildAt(0) as BottomNavigationMenuView
        val navigationItemView2 = navigationItemView.getChildAt(4) as BottomNavigationItemView
        val displayMetrics = resources.displayMetrics

        navigationItemView2.setIconSize(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 50f,
                displayMetrics
            ).toInt()
        )




        if (TempConstants.LanguageChanged) {
            if (TempConstants.isFromSettings) {
                openUserMoments()
            } else {
                openSettingsScreen()
            }

        }


    }

    override fun onBackPressed() {
        try {
            exitConfirmation();
        } catch (e: Exception) {
            Timber.e(e.message)
        }
    }

   /* private fun exitConfirmation() {


        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.app_name))
//        builder.setMessage("Are you sure you want to block $otherFirstName ?")
//        builder.setMessage("${resources.getString(R.string.are_you_sure_you_want_to_exit_I69)}")

        var title = "${AppStringConstant(this@MainActivity).are_you_sure_you_want_to_exit_I69}"

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
        if(currentFragment!=null && currentFragment is MessengerNewChatFragment) {
            title = "${AppStringConstant(this@MainActivity).do_you_want_to_leave_this_page}"
        }


        builder.setMessage(title)
            .setCancelable(false)
            .setPositiveButton(AppStringConstant(this@MainActivity).yes) { dialog, which ->
                super.onBackPressed()
            }
            .setNegativeButton(AppStringConstant(this@MainActivity).no) { dialog, which ->
                dialog.dismiss();
            }
//            .setPositiveButton(android.R.string.yes) { dialog, which ->
//                super.onBackPressed()
//            }
//            .setNegativeButton(android.R.string.no) { dialog, which ->
//                dialog.dismiss();
//            }
        val alert = builder.create()
        alert.show()
        alert.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(resources.getColor(R.color.black));
        alert.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(resources.getColor(R.color.black));

//        val builder = AlertDialog.Builder(this, `R.style.AlertDialogTheme`)
//        builder.setTitle("I69")
//        builder.setMessage("Are you sure you want to exit I69")
//        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
//            super.onBackPressed()
//        }
//        builder.setNegativeButton(android.R.string.no) { dialog, which ->
//            dialog.dismiss();
//        }
//        val a: AlertDialog = builder.create()
//        a.show()
//        val buttonbackground: Button = a.getButton(DialogInterface.BUTTON_NEGATIVE)
//        buttonbackground.setTextColor(resources.getColor(R.color.black))
//
//        val buttonbackground1: Button = a.getButton(DialogInterface.BUTTON_POSITIVE)
//        buttonbackground1.setTextColor(resources.getColor(R.color.black))
    }*/

    private fun exitConfirmation() {

        val dialogLayout = layoutInflater.inflate(R.layout.dialog_delete, null)
        val headerTitle = dialogLayout.findViewById<TextView>(R.id.header_title)
        val noButton = dialogLayout.findViewById<TextView>(R.id.no_button)
        val yesButton = dialogLayout.findViewById<TextView>(R.id.yes_button)

        var title = "${AppStringConstant(this@MainActivity).are_you_sure_you_want_to_exit_I69}"

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
        if(currentFragment!=null && currentFragment is MessengerNewChatFragment) {
            title = "${AppStringConstant(this@MainActivity).do_you_want_to_leave_this_page}"
        }

        headerTitle.text = title
        noButton.text = "${AppStringConstant(this@MainActivity).no}"
        yesButton.text = "${AppStringConstant(this@MainActivity).yes}"

        val builder = AlertDialog.Builder(getMainActivity(),R.style.DeleteDialogTheme)
        builder.setView(dialogLayout)
        builder.setCancelable(false)
        val dialog = builder.create()

        noButton.setOnClickListener {
            dialog.dismiss();
        }

        yesButton.setOnClickListener {
            dialog.dismiss();
            super.onBackPressed()
        }

        dialog.show()
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController2?.value?.navigateUp()!! || super.onSupportNavigateUp()
    }

    val notiBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (it.action == ACTION_NEW_NOTIFICATION) {
                    remoteMessage = it.getParcelableExtra(KEY_REMOTE_MSG) as RemoteMessage?
                    remoteMessage?.let { messageBody ->
                        val textTitle: String = messageBody.notification!!.title!!
                        val url = if (JSONObject(messageBody.data.toString()).getJSONObject("data")
                                .has("title")
                        ) {
                            if (JSONObject(messageBody.data.toString()).getJSONObject("data")
                                    .getString("title").equals("Received Gift")
                            ) {
                                BuildConfig.BASE_URL + JSONObject(messageBody.data.toString()).getJSONObject(
                                    "data"
                                ).getString("giftUrl")
                            } else if (JSONObject(messageBody.data.toString()).getJSONObject("data")
                                    .getString("title").equals("Sent message")
                            ) {
                                BuildConfig.BASE_URL + JSONObject(messageBody.data.toString()).getJSONObject(
                                    "data"
                                ).getString("user_avatar")
                            } else {
                                null
                            }
                        } else {
                            if (messageBody.notification!!.icon != null) {
                                messageBody.notification!!.icon
                            } else {
                                null
                            }
                        }
                        loadImageAndPostNotification(messageBody, url, textTitle)
                    }
                }
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        binding.clNotification.visibility = GONE
    }

    private fun loadImageAndPostNotification(
        messageBody: RemoteMessage,
        url: String?,
        textTitle: String
    ) {
        if (!url.isNullOrEmpty()) {
            binding.ivImage.visibility = VISIBLE
            Glide.with(this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .optionalCircleCrop()
                .placeholder(R.drawable.ic_default_user)
                .into(binding.ivImage)
        } else {
            binding.ivImage.visibility = GONE
        }
        if (isCurrentLanguageFrench()) {
            binding.tvTitle.text =
                JSONObject(messageBody.data.toString()).getJSONObject("data").getString("title_fr")
            binding.tvBody.text =
                JSONObject(messageBody.data.toString()).getJSONObject("data").getString("body_fr")
            binding.clNotification.visibility = View.VISIBLE
            handler.postDelayed(runnable, 7 * 1000)
        } else {
            binding.tvTitle.text = textTitle
            binding.tvBody.text = messageBody.notification!!.body
            binding.clNotification.visibility = View.VISIBLE
            handler.postDelayed(runnable, 7 * 1000)
        }

    }

    fun showLoader() {
        showProgressView()
    }

    var filePath: File? = null
    var description: String? = null
    var checked: Boolean = false
    var isShare: Boolean = false
    var isShareLater: Boolean = false
    var publishAt: String = ""

    fun isValidTime(
        year: Int,
        monthOfYear: Int,
        dayOfMonth: Int,
        hourOfDay: Int,
        minute: Int
    ): Boolean {
        val now = Calendar.getInstance().time
        val selectedTime = Calendar.getInstance()
        selectedTime.set(year, monthOfYear, dayOfMonth)
        selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
        selectedTime.set(Calendar.MINUTE, minute)

        return now.before(selectedTime.time)
    }

    fun openUserAllMoments(file: File, description: String, checked: Boolean) {

        filePath = file
        this.description = description
        this.checked = checked
        isShare = true
        binding.bottomNavigation.selectedItemId = R.id.nav_home_graph


//        val navHostFragment =
//            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
//        if (currentFragment is UserMomentsFragment) {
//            val cFragment = currentFragment as UserMomentsFragment
//            cFragment.momentSharing(mFilePath, description, checked)
//        }
    }

    fun openUserAllMoments(file: File, description: String, checked: Boolean, publishAt: String) {
        filePath = file
        this.description = description
        this.checked = checked
        isShare = false
        isShareLater = true
        this.publishAt = publishAt
        binding.bottomNavigation.selectedItemId = R.id.nav_home_graph


//        val navHostFragment =
//            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
//        if (currentFragment is UserMomentsFragment) {
//            val cFragment = currentFragment as UserMomentsFragment
//            cFragment.momentSharing(mFilePath, description, checked)
//        }
    }


    var dialog: NotificationDialogFragment? = null

    fun notificationDialog(
        dialog: NotificationDialogFragment,
        childFragmentManager: FragmentManager,
        s: String
    ) {
        this.dialog = dialog
        this.dialog!!.show(childFragmentManager, s)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.d("OnBottomNavPressed", "onNavigationItemSelected: ${item.itemId}")
        if (item.itemId == R.id.nav_user_profile_graph) {
            openProfileScreen()
        }
        return false
    }

    fun isUserAllowedToScheduleStory() = mUser?.canScheduleStory == true

    fun isUserAllowedToScheduleMoment() = mUser?.canScheduleMoment == true

    fun isUserHasSubscription(): Boolean {
        return mUser?.userSubscription?.isActive == true && mUser?.userSubscription?.isCancelled == false
    }

    fun getRequiredCoins(coinsNeededFor: String, coinsNeeded: (Int) -> Unit) {
        viewModel.getCoinSettingsByRegion(userToken.toString(), coinsNeededFor).observe(this) {
            Log.d("getRequiredCoins", "getRequiredCoins: ${it.coinsNeeded}")
            coinsNeeded.invoke(it.coinsNeeded)
        }
    }


}
