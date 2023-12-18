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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.gson.Gson
import com.i69.R
import com.i69.databinding.ActivitySplashDemoBinding
import com.i69.ui.base.BaseActivity
import com.i69.ui.screens.auth.AuthActivity
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.screens.main.search.SearchInterestedInFragment
import com.i69.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

class SplashDemoActivity : BaseActivity<ActivitySplashDemoBinding>() {

    private lateinit var appUpdateManager: AppUpdateManager

    override fun getActivityBinding(inflater: LayoutInflater) =
        ActivitySplashDemoBinding.inflate(inflater)

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
//            var language ="af"
            var pref = PreferenceManager.getDefaultSharedPreferences(this@SplashDemoActivity)
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

        binding.splashLogo.setOnClickListener {

            noActiveSubScription();
//            val dialog = DialogFragment(
//
//                )
//            dialog.show(
//               fragmentManager,
//                "${resources.getString(R.string.filter)}"
//            )

        }
    }


    override fun setupTheme(savedInstanceState: Bundle?) {
//        PreferenceManager.getDefaultSharedPreferences(this@SplashActivity).edit().clear().apply();


        if (intent.hasExtra("data")) {
            Log.e("data", "" + Gson().toJson(intent.extras!!.get("data").toString()))
            Log.e(
                "datatype",
                "" + JSONObject(
                    intent.extras!!.get("data").toString()
                ).getString("notification_type")
            )
            val intentt = Intent(this, MainActivity::class.java)

            var datavalues = JSONObject(intent.extras!!.get("data").toString())
            if (datavalues.has("title")) {
                var title = JSONObject(intent.extras!!.get("data").toString()).getString("title")

                if (title.equals("Moment Liked") || title.equals("Comment in moment") ||
                    title.equals("Story liked") || title.equals("Story Commented") ||
                    title.equals("Gift received")
                ) {
                    intentt.putExtra("isNotification", "yes")
                }
            } else {
                //Log.e("room id",JSONObject(messageBody.data.toString()).getString("roomID"));
                Log.e(
                    "room id",
                    JSONObject(intent.extras!!.get("data").toString()).getString("roomID")
                )

                intentt.putExtra("isChatNotification", "yes")
                intentt.putExtra(
                    "roomIDs",
                    JSONObject(intent.extras!!.get("data").toString()).getString("roomID")
                )
            }
            startActivity(intentt)
        } else {
            appUpdateManager = AppUpdateManagerFactory.create(this)
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask
                .addOnSuccessListener { appUpdateInfo ->
//                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
//                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
//                    )
////                        startUpdate(appUpdateInfo) else navigate()
////                }
//                .addOnFailureListener {
//                    navigate()
                }
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
    }

    override fun setupClickListeners() {

    }

//    private fun startUpdate(appUpdateInfo: AppUpdateInfo) {
//        appUpdateManager.startUpdateFlowForResult(
//            appUpdateInfo,
//            AppUpdateType.FLEXIBLE,
//            this,
////            MY_REQUEST_CODE
//        )
//    }

    private fun navigate() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1200)


            withContext(Dispatchers.Main) {
                if (getCurrentUserId() == null) goToAuthActivity() else startActivity<MainActivity>()
            }
        }
    }

    private fun goToAuthActivity() {
        val transactions = arrayOf<Pair<View, String>>(
            Pair(binding.splashLogo, "logoView"),
            Pair(binding.splashTitle, "logoTitle")
        )
        val options = ActivityOptions.makeSceneTransitionAnimation(this, *transactions)
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent, options.toBundle())
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        Log.e("request code", "" + requestCode)
//        Log.e("resultCode code", "" + requestCode)
//        Log.e("data", "" + Gson().toJson(data))
//
//
//        if (requestCode == MY_REQUEST_CODE) {
//            if (resultCode != RESULT_OK) {
//                Timber.e("MY_APP", "Update flow failed! Result code: $resultCode")
//                navigate()
//            } else {
//                navigate()
//            }
//        }
//    }


      private fun noActiveSubScription() {
        val bottomsheetDialog = BottomSheetDialog(this)
//        val customView = layoutInflater.inflate(R.layout.dialog_entermobilenumber, null, false)
        val customView =
            layoutInflater.inflate(com.i69.R.layout.dialog_subscription_fragment, null, false)

//        val title = customView.findViewById<TextView>(com.i69app.R.id.description)
//        val iv_gpay = customView.findViewById<ImageView>(com.i69app.R.id.iv_gpay)
//        val tvUserBalance =
//            customView.findViewById<MaterialTextView>(com.i69app.R.id.tv_user_balance)
//
//        val upgrade_button =
//            customView.findViewById<MaterialTextView>(com.i69app.R.id.upgrade_button)
//
//        val cdUserBalance = customView.findViewById<ConstraintLayout>(com.i69app.R.id.cd_user_balance)
//
//        val cdUpgradeBalance =
//            customView.findViewById<LinearLayout>(com.i69app.R.id.cd_upgrade_balance)
//
//        val imageCross =
//            customView.findViewById<ImageView>(com.i69app.R.id.iv_cross)
//
//        val typeface_regular =
//            Typeface.createFromAsset(activity?.assets, "fonts/poppins_semibold.ttf")
//        val typeface_light = Typeface.createFromAsset(activity?.assets, "fonts/poppins_light.ttf")

//        tvUserBalance.typeface = typeface_light;
//        upgrade_button.typeface = typeface_regular;

//        cdUpgradeBalance.visibility= View.GONE
//        cdUserBalance.visibility= View.GONE

//        ball

//        iv_gpay.setImageResource(com.i69app.R.drawable.subscription)
//        title.visibility= View.GONE
//        tvUserBalance.text = resources.getString(com.i69app.R.string.no_active_subscription)
//        upgrade_button.text = resources.getString(com.i69app.R.string.buy_subscription)
//        titleSubscription.visibility= View.VISIBLE
//        tvUserBalance.text =
//            requireContext().resources.getString(com.i69app.R.string.text_user_balance)
//                .plus(" $coins")

//        tvUserBalanceCoins.text = " $coins".plus("\nCoin")


//        cdUpgradeBalance.setOnClickListener {
//            bottomsheetDialog.dismiss()
//            navigatePlanPurchase()
//        }
//
//        imageCross.setOnClickListener {
//            bottomsheetDialog.dismiss()
//        }

//        cdUserBalance.setOnClickListener {
//
//        }
        bottomsheetDialog.setContentView(customView)
        bottomsheetDialog.show()



    }

}