package com.i69.singleton

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ProcessLifecycleOwner
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.ktx.BuildConfig
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.gson.Gson
import com.i69.data.config.Constants.PAYPAL_CLIENT_ID
import com.i69.data.preferences.UserPreferences
import com.i69.data.remote.repository.UserUpdateRepository
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.screens.main.coins.PurchaseFragment
import com.onesignal.OneSignal
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.config.Environment
import com.paypal.checkout.config.SettingsConfig
import com.paypal.checkout.createorder.CurrencyCode
import com.paypal.checkout.createorder.UserAction
import com.stripe.android.PaymentConfiguration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@HiltAndroidApp
class App : Application() {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_data_store")

    companion object {
        lateinit var userPreferences: UserPreferences
        private lateinit var mInstance: App

        fun getAppContext(): Context = mInstance.applicationContext

        fun getOneSignalPlayerId() = OneSignal.getDeviceState()?.userId

        fun updateOneSignal(userUpdateRepository: UserUpdateRepository) {
            val playerId = getOneSignalPlayerId()
            Timber.e("Player Id $playerId")

            playerId?.let {
                GlobalScope.launch {
                    val userId = userPreferences.userId.first()
                    val userToken = userPreferences.userToken.first()

                    if (userId != null && userToken != null) {
                        var res = userUpdateRepository.updateOneSignalPlayerId(
                            userId,
                            playerId,
                            userToken
                        )
                        //Log.e("bbbbc",""+res.data!!.errorMessage)


                        Log.e("bbbbbc", "" + res.message)
                    }
                }
            }
        }


        fun updateFirebaseToken(userUpdateRepository: UserUpdateRepository) {
            Log.e("rrrrr", "111111")
            Firebase.messaging.getToken().addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("rrrrr", "222222")
                    Timber.d("App", "Fetching FCM registration token failed")
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result
                Log.e("rrrrr", "33333333")
                Log.e("sendTokenToServerLog", "($token)")
                Timber.d("FirebaseToken", "" + token)
                token?.let {
                    GlobalScope.launch {
                        Log.e("rrrrr", "44444444")
                        val userId = userPreferences.userId.first()
                        val userToken = userPreferences.userToken.first()
                        Log.e("rrrrr", "5555555")
                        if (userId != null && userToken != null) {
                            Log.e("rrrrr", "6666666")
                            var res =
                                userUpdateRepository.updateFirebasrToken(userId, token, userToken)
                            Log.e("rrrrr", "777777777")
                            // Log.e("bbbb",""+res.data!!.errorMessage)
                            Log.e("bbbbb", "" + Gson().toJson(res))
                            Timber.d("TOKEN")
                        }
                    }
                }

//            Toast.makeText(mInstance, token, Toast.LENGTH_SHORT).show()
            })
        }


        fun initStripe(stripePublickey: String, purchaseFragment: PurchaseFragment) {
            Log.e("StripePublicKeySet",stripePublickey )
            PaymentConfiguration.init(mInstance.applicationContext, stripePublickey)


        }


//         [START log_reg_token]

//         [END log_reg_token]
    }

    @Inject
    lateinit var userUpdateRepository: UserUpdateRepository


    override fun onCreate() {
        super.onCreate()

        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)

        mInstance = this
        userPreferences = UserPreferences(this.applicationContext.dataStore)
        FacebookSdk.fullyInitialize()

        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) =
                    super.log(priority, "ayan_$tag", message, t)

                override fun createStackElementTag(element: StackTraceElement): String =
                    String.format(
                        "(%s, Line: %s, Method: %s)",
                        super.createStackElementTag(element),
                        element.lineNumber,
                        element.methodName
                    )
            })
        }
        Log.e("pppp", "ppp")
        initOneSignal()
        initFirebase()
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            AppLifecycleListener(
                userPreferences,
                userUpdateRepository
            )
        )
        initPayPal()
//

    }

    private fun initFirebase() {
        updateFirebaseToken(userUpdateRepository)

    }

    private fun initOneSignal() {


        OneSignal.initWithContext(this)
        OneSignal.setAppId(com.i69.data.config.Constants.ONESIGNAL_APP_ID)
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE)
        updateOneSignal(userUpdateRepository)

        OneSignal.setNotificationOpenedHandler { result ->


            Log.e("tgtgt", "-->" + Gson().toJson(result))
            Timber.tag(MainActivity.CHAT_TAG).w("Result: $result")
            val notification = result.notification


            if (notification.title != null) {


                if (notification.title.equals("Moment Liked") || notification.title.equals("Great News!!")) {

                    MainActivity.notificationOpened = false
                    val intent = Intent(getAppContext(), MainActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra("LikeComment", "yes")

                    startActivity(intent)
                } else {
                    val senderId =
                        notification.additionalData[MainActivity.ARGS_SENDER_ID].toString()

                    if (notification.launchURL != null) {
                        MainActivity.notificationOpened = false
                        val intent = Intent(getAppContext(), MainActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK
                        intent.putExtra(MainActivity.ARGS_SCREEN, notification.launchURL)
                        intent.putExtra(MainActivity.ARGS_SENDER_ID, senderId)
                        startActivity(intent)
                    }
                }
            } else {
                val senderId = notification.additionalData[MainActivity.ARGS_SENDER_ID].toString()

                if (notification.launchURL != null) {
                    MainActivity.notificationOpened = false
                    val intent = Intent(getAppContext(), MainActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra(MainActivity.ARGS_SCREEN, notification.launchURL)
                    intent.putExtra(MainActivity.ARGS_SENDER_ID, senderId)
                    startActivity(intent)
                }
            }
        }
    }


    private fun initPayPal() {
        val config = CheckoutConfig(
            application = this,
            clientId = PAYPAL_CLIENT_ID,
            environment = Environment.LIVE,
//            environment = Environment.SANDBOX,

            currencyCode = CurrencyCode.USD,
            userAction = UserAction.PAY_NOW,
            returnUrl = "${com.i69.BuildConfig.APPLICATION_ID}://paypalpay",
            settingsConfig = SettingsConfig(
                loggingEnabled = true
            )
        )
        PayPalCheckout.setConfig(config)
    }

}