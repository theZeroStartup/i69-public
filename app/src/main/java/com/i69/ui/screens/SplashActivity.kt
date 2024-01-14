package com.i69.ui.screens

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Base64
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.exception.ApolloException
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.gson.Gson
import com.i69.AttrTranslationQuery
import com.i69.R
import com.i69.applocalization.AppStringConstantViewModel
import com.i69.applocalization.LocalStringConstants
import com.i69.applocalization.getLoalizations
import com.i69.applocalization.getLoalizationsStringList
import com.i69.applocalization.updateLoalizationsConstString
import com.i69.databinding.ActivitySplashBinding
import com.i69.singleton.App
import com.i69.ui.base.BaseActivity
import com.i69.ui.screens.auth.AuthActivity
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.screens.main.search.SearchInterestedInFragment
import com.i69.ui.viewModels.UserViewModel
import com.i69.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import timber.log.Timber
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*




@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private lateinit var appUpdateManager: AppUpdateManager
    private val viewModessl: AppStringConstantViewModel by viewModels()
    private val viewModel: UserViewModel by viewModels()


    override fun getActivityBinding(inflater: LayoutInflater) =
        ActivitySplashBinding.inflate(inflater)

    fun printKeyHash(context: Activity): String? {
        val packageInfo: PackageInfo
        var key: String? = null
        try {
            //getting application package name, as defined in manifest
            val packageName = context.applicationContext.packageName

            //Retriving package info
            packageInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            Log.e("Package Name=", context.applicationContext.packageName)
            for (signature in packageInfo.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                key = String(Base64.encode(md.digest(), 0))

                // String key = new String(Base64.encodeBytes(md.digest()));
                Log.e("MyKeyHash=", key)
            }
        } catch (e1: PackageManager.NameNotFoundException) {
            Log.e("Name not found", e1.toString())
        } catch (e: NoSuchAlgorithmException) {
            Log.e("No such an algorithm", e.toString())
        } catch (e: Exception) {
            Log.e("Exception", e.toString())
        }
        return key
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = SharedPref(this)

        if (!sharedPref.getFirstTimeLanguage()) {


            var allLangs = fetchLanguages()
            var language = Locale.getDefault().getLanguage()

            Log.e("MyselectedUserlanguage", "$language")
//            var language ="af"
            var pref = PreferenceManager.getDefaultSharedPreferences(this@SplashActivity)
            for (i in 0 until allLangs.size) {
                if (allLangs[i].supportedLangCode.equals(language)) {
                    pref.edit()?.putString("language", language)
                        ?.apply()

                }
            }

            sharedPref.setFirtsTimeLanguage(true)


        }
        if (sharedPref.getLanguage()) {
            // Toast.makeText(this,"Language Code true",Toast.LENGTH_SHORT).show()
            val background = findViewById<ConstraintLayout>(R.id.ctSplash)
            if (sharedPref.getLanguageFromSettings()) {
                TempConstants.isFromSettings = true
                background.setBackgroundColor(ContextCompat.getColor(this, R.color.container_color))
            }
            TempConstants.LanguageChanged = true
            val intent = Intent(this, ProgressBarActivity::class.java)
            startActivity(intent)
        }

        Log.d("SplashActivity", "onCreate: $packageName")
    }


    override fun setupTheme(savedInstanceState: Bundle?) {
//        PreferenceManager.getDefaultSharedPreferences(this@SplashActivity).edit().clear().apply();
        if (intent.hasExtra("data")) {

            Log.e("data", "" + Gson().toJson(intent.extras!!.get("data").toString()))
            val intentt = Intent(this, MainActivity::class.java)

            val dataStr = intent.extras!!.get("data").toString()
            val datavalues = JSONObject(dataStr)

            if (datavalues.has("roomID")) {
                Log.e("room id", datavalues.getString("roomID"))
                intentt.putExtra("isChatNotification", "yes")
                intentt.putExtra("roomIDs", datavalues.getString("roomID"))
            } else {
                intentt.putExtra("isNotification", "yes")
                intentt.putExtra("notificationData", dataStr)
            }
            startActivity(intentt)
        } else {
           // appUpdateManager = AppUpdateManagerFactory.create(this)
          //  val appUpdateInfoTask = appUpdateManager.appUpdateInfo
            Log.e("splash_act", "158")

            navigate()

           /* appUpdateInfoTask
                .addOnSuccessListener { appUpdateInfo ->
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                    )
                        startUpdate(appUpdateInfo) else navigate()
                }
                .addOnFailureListener {
                    Log.e("splash_act", "167")
                    navigate()
                }*/

        }


        SearchInterestedInFragment.setShowAnim(true)
        binding.splashLogo.defaultAnimate(400, 500)
        binding.splashTitle.defaultAnimate(300, 700)


//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
//        val channelId = getString(R.string.default_notification_channel_id)
//        val channelName = getString(R.string.default_notification_channel_name)
//        val channelDescription = getString(R.string.default_notification_channel_desc)
//        val importance = NotificationManagerCompat.IMPORTANCE_HIGH
//        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
//
//        val channel = NotificationChannelCompat.Builder(channelId, importance).apply {
//            setName(channelName)
//            setDescription(channelDescription)
//            setSound(alarmSound, Notification.AUDIO_ATTRIBUTES_DEFAULT)
//        }
//        channel.setVibrationEnabled(true)
//        NotificationManagerCompat.from(this).createNotificationChannel(channel.build())


        printKeyHash(this)
//        getEncodedApiKey(LocalStringConstants.google_maps_key)
    }

    override fun setupClickListeners() {

    }

   /* private fun startUpdate(appUpdateInfo: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            AppUpdateType.FLEXIBLE,
            this,
            MY_REQUEST_CODE
        )
    }*/

    private fun navigate() {
        Log.e("splash_act", "navigate")
        val sharedPref = SharedPref(this)
        lifecycleScope.launch(Dispatchers.Main) {
            val userId = getCurrentUserId()
            if (userId != null && !sharedPref.getLanguage()) {
                updateLanguage(userId!!, getCurrentUserToken()!!)
            } else {
                // delay(1200)
                //  updateLanguage(getCurrentUserId()!!, getCurrentUserToken()!!)
                withContext(Dispatchers.Main) {
                    Log.e("splash_act", "goToAuthActivity")
                    if (userId == null) {
                        goToAuthActivity()
                    } else {
                        Log.e("splash_act", "goToMainActivity")
                        startActivity<MainActivity>()
                    }
                }
            }
        }
    }

    private fun goToAuthActivity() {
        Log.e("splash_act", "goToAuthActivity")
        val transactions = arrayOf<Pair<View, String>>(
            Pair(binding.splashLogo, "logoView"),
            Pair(binding.splashTitle, "logoTitle")
        )
        val options = ActivityOptions.makeSceneTransitionAnimation(this, *transactions)
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent, options.toBundle())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.e("request code", "" + requestCode)
        Log.e("resultCode code", "" + requestCode)
        Log.e("data", "" + Gson().toJson(data))


       /* if (requestCode == MY_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Timber.e("MY_APP", "Update flow failed! Result code: $resultCode")
                navigate()
            } else {
                navigate()
            }
        }*/
    }


    fun updateLanguageChanged() {
        Log.e("UpdatageTranslation", "UpdateLanguageTranslation")
        Log.e("splash_act", "updateLanguageChanged")
        lifecycleScope.launch() {

//                Log.e("u id", "-->" + getCurrentUserId())
//                Log.e("userToken", "-->" + getCurrentUserToken())
//                userId = getCurrentUserId()!!
            var userToken = App.userPreferences.userToken.first()
            Timber.i("usertokenn $userToken")

            Log.e("callProgressBarActivity", "callProgressBarActivity")
            var locaLizationString = getLoalizationsStringList()
            Log.e("splash_act_", "${"locaLizationString"} ${locaLizationString}")
            if (userToken.isNullOrEmpty()) {
                var consted =
                    getLoalizations(this@SplashActivity, isUpdate = true)
                viewModessl.data.postValue(consted)
            } else {
                val res = try {
                    apolloClient(this@SplashActivity, userToken!!).query(
                        AttrTranslationQuery(
                            locaLizationString
                        )
                    )
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponse ${e.message}")
                    Toast.makeText(this@SplashActivity, " ${e.message}", Toast.LENGTH_LONG)
                        .show()

//                hideProgressView()
                    return@launch
                }
                if (res.hasErrors()) {

                    Log.e("responsegetted", Gson().toJson(res.errors))
                } else {

                    res.data?.attrTranslation?.forEach {
                        if (it?.nameTranslated == "") {
                            Log.v("TransaltionNw", it?.name + " " + it?.nameTranslated)
                        }
                    }
                    Log.e("splash_act_", "${"res.data?.attrTranslation"} ${res.data?.attrTranslation}")
                    var consted =
                        getLoalizations(this@SplashActivity, res.data?.attrTranslation)
                    Log.e("splash_act_", "${"consted"} ${consted}")
//                  Log.e(":responsegetted", Gson().toJson( res.data))
                    val tarnslationJson = Gson().toJson(consted)
                    /* val veryLongString = tarnslationJson.toString()
                     val maxLogSize = 1000
                     for (i in 0..veryLongString.length / maxLogSize) {
                         val start = i * maxLogSize
                         var end = (i + 1) * maxLogSize
                         end = if (end > veryLongString.length) veryLongString.length else end
                         Log.v("TAG", veryLongString.substring(start, end))
                     }*/
                    Log.e("responsegetted", tarnslationJson)
//                    viewModessl.data.postValue(consted)
                    val sharedPref = SharedPref(this@SplashActivity)
                    sharedPref.setAttrTranslater(consted)
                    updateLoalizationsConstString(this@SplashActivity, consted)
                    Log.v("WalletTranslation", consted.wallet)
//                    AppStringConstant1.feed = consted.feed
//                    AppStringConstant1(this@SplashActivity).feed= consted.feed


//                    lifecycleScope.launch {
//                        updateLanguage(getCurrentUserId()!!, getCurrentUserToken()!!)
//                    }

                    //delay(1200)

                    lifecycleScope.launch(Dispatchers.Main) {
                        Log.e("splash_act", "342")

                        if (getCurrentUserId() == null) goToAuthActivity() else startActivity<MainActivity>()
                    }

                }

            }

        }

    }


    private fun updateLanguage(id: String, token: String) {
        Log.e("splash_act", "updateLanguage")
        val deviceLocale = Locale.getDefault().language
        Log.e("splash_act_", "${deviceLocale}")
        lifecycleScope.launch(Dispatchers.Main) {

            when (val response = viewModel.updateLanguage(
                languageCode = deviceLocale,
                userid = id,
                token = token
            ))
            {
                is Resource.Success -> {
                    Log.e("splash_act_", "${"LanguageUpdate Success"} ${response.message}")
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

//        lifecycleScope.launch(Dispatchers.Main) {
//            Log.d("languge123", "langauage")
//            if (getCurrentUserId() == null) goToAuthActivity() else startActivity<MainActivity>()
//        }

    }

}