package com.i69.firebasenotification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.screens.main.MainActivity.Companion.getMainActivity
import com.i69.ui.screens.main.messenger.chat.MessengerNewChatFragment
import com.i69.ui.screens.main.messenger.list.MessengerListFragment
import com.i69.utils.LogUtil
import org.json.JSONObject


class NotificationBroadcast(var activity: Fragment?) : BroadcastReceiver() {
    var NOTIFY_ID = 0 // ID of notification

    override fun onReceive(context: Context?, intent: Intent) {
        try {
            if (intent.extras!!.getString("data") != null) {
                if (TextUtils.isEmpty(intent.extras!!.getString("data"))) {
                    Log.d(
                        "onMessageReceived", "onReceive: Data Recived " + JSONObject(
                            intent.extras!!.getString("data")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.d("onMessageReceived", "onReceive: Data Recived " + intent.extras!!.getString("type"))
        getAction(intent)
    }

    private fun getAction(intent: Intent) {
        if (intent.extras != null) {
            //getMainActivity()?.binding?.bottomNavigation?.addBadge(1);
            //  Constants.printResponse("Daaaaa", "Datasss", intent.getExtras())
            LogUtil.debug("NotificationData : : : ${intent.extras}")
            if ((intent.hasExtra("isChatNotification") && intent.getStringExtra("isChatNotification") != null)
                && intent.getStringExtra("isChatNotification") == "yes"
            ) {
                LogUtil.debug("Here 1")
                if ((intent.hasExtra("roomIDs") && intent.getStringExtra("roomIDs") != null)) {
                    LogUtil.debug("Here 2")
                    Handler(Looper.getMainLooper()).postDelayed({
                        kotlin.run {
                            LogUtil.debug("Here 3")
                            val rID = intent.getStringExtra("roomIDs")
                            val bundle = Bundle()
                            bundle.putString("roomIDNotify", rID)
                            LogUtil.debug("Activity : : : $activity")
                            when (activity) {
                                is MessengerListFragment -> {
                                    LogUtil.debug("Here 4")
                                    soundPlay(activity as MessengerListFragment)
                                    clearNotification(activity as MessengerListFragment)
                                    (activity as MessengerListFragment).onNewMessage(
                                        intent.getStringExtra(
                                            "roomIDs"
                                        ).toString()
                                    )
                                }
                                is MessengerNewChatFragment -> {
                                    LogUtil.debug("Here 5")
                                    soundPlay(activity as MessengerNewChatFragment)
                                    clearNotification(activity as MessengerNewChatFragment)
                                    //(activity as MessengerNewChatFragment).setupData(false)
                                }
                                is BaseFragment<*> -> {
                                    LogUtil.debug("Here 6")
                                    soundPlay(activity as BaseFragment<*>)
                                    //getMainActivity()?.updateChatBadge()
                                    getMainActivity()?.pref?.edit()?.putString("newChat", "true")
                                        ?.putString("roomIDS", rID)?.apply()
                                    //clearNotification(activity as BaseFragment<*>)
                                    //(activity as BaseFragment<*>).setupData(false)
                                }
                            }
//                            MainActivity.getMainActivity()!!.navController.navigate(R.id.messengerListFragment, bundle)
                        }
                    }, 500)
                }
            } else if ((intent.hasExtra("isNotification") && intent.getStringExtra("isNotification") != null)) {
                LogUtil.debug("Here isNotification")
                Handler(Looper.getMainLooper()).postDelayed({
                    kotlin.run {
                        val bundle = Bundle()
                        bundle.putString("ShowNotification", "true")
                        // MainActivity.getMainActivity()!!.navController.navigate(R.id.action_user_moments_fragment, bundle)
                    }
                }, 500)
            } else if (intent.hasExtra(MainActivity.ARGS_SCREEN) && intent.getStringExtra(
                    MainActivity.ARGS_SCREEN
                ) != null
            ) {
                if (intent.hasExtra(MainActivity.ARGS_SENDER_ID) && intent.getStringExtra(
                        MainActivity.ARGS_SENDER_ID
                    ) != null
                ) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        kotlin.run {
                            val senderId = intent.getStringExtra(MainActivity.ARGS_SENDER_ID)
                            onNotificationClick(senderId!!)
                        }
                    }, 500);
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({
                        kotlin.run {
                            if (activity is MessengerListFragment) {
                                soundPlay(activity as MessengerListFragment)
                                clearNotification(activity as MessengerListFragment)
                                //(activity as MessengerListFragment).updateList(false)
                            } else if (activity is MessengerNewChatFragment) {
                                soundPlay(activity as MessengerNewChatFragment)
                                clearNotification(activity as MessengerNewChatFragment)
                                //(activity as MessengerNewChatFragment).setupData(false)
                            }
//                            MainActivity.getMainActivity()!!.navController.navigate(R.id.messengerListAction)
                        }
                    }, 500)
                }
            }
        }
    }

    private fun soundPlay(fragment: Fragment) {
        try {
            if (fragment.isAdded) {
                val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(fragment.requireActivity(), notification)
                r.play()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun onNotificationClick(senderId: String) {
//        val msgPreviewModel: MessagePreviewModel? = QbDialogHolder.getChatDialogById(senderId)
//        msgPreviewModel?.let {
//            viewModel?.setSelectedMessagePreview(it)
//            navController.navigate(R.id.globalUserToChatAction)
//        }
    }

    private fun clearNotification(fragment: Fragment) {
        if (fragment.isAdded) {
            val notificationManager = fragment.requireActivity()
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.cancel(NOTIFY_ID)
            notificationManager.cancelAll()
        }
    }
}
