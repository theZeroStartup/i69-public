package com.i69.ui.screens.main.privacy

import android.annotation.SuppressLint
import android.net.http.SslError
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.i69.R
import com.i69.databinding.FragmentPrivacyBinding
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.screens.main.MainActivity.Companion.getMainActivity
import com.i69.ui.screens.main.notification.NotificationDialogFragment
import kotlinx.coroutines.launch
import timber.log.Timber


class PrivacyFragment:BaseFragment<FragmentPrivacyBinding>() {
    private lateinit var mWebView: WebView
    private var userToken: String? = null
    private var userId: String? = null
    private var url: String? = null

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentPrivacyBinding.inflate(inflater, container, false)


    private class HelloWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun setupTheme() {
        //requireActivity().window.requestFeature(Window.FEATURE_NO_TITLE)
        lifecycleScope.launch {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            Timber.i("usertokenn $userToken")
        }
        if (getMainActivity()?.pref?.getString("typeview","privacy").equals("privacy")){
            var privacyUrl =  com.i69.data.config.Constants.URL_PRIVACY_POLICY//.plus(MainActivity.getMainActivity()?.pref?.getString("language", "en")).plus("/policy")
            Log.e("myPrivacyUrl",privacyUrl)
//            url=com.i69app.data.config.Constants.URL_PRIVACY_POLICY
            url = privacyUrl
        }else{
            url=com.i69.data.config.Constants.URL_TERMS_AND_CONDITION
        }
       /* binding.privacyWebView.loadUrl(url!!)
        binding.privacyWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }
        }*/

        val webView: WebView = binding.privacyWebView

        webView.settings.javaScriptEnabled = true

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.domStorageEnabled = true

        webView.webViewClient = WebViewController()
        webView.loadUrl(url!!)

    }

    override fun setupClickListeners() {
        binding.toolbarHamburger.setOnClickListener {
            getMainActivity()?.drawerSwitchState()
        }
        binding.toolbarLogo.setOnClickListener {
            findNavController().popBackStack()
        }


        binding.bell.setOnClickListener {
            val dialog = NotificationDialogFragment(
                userToken,
                binding.counter,
                userId,
                binding.bell
            )
//            dialog.show(childFragmentManager, "${ requireActivity().resources.getString(R.string.notificatins) }")
            getMainActivity()?.notificationDialog(dialog,childFragmentManager,"${ requireActivity().resources.getString(R.string.notificatins) }")
        }
    }


    class WebViewController : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)
            return true
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            /*val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this@WebViewController)
            builder.setMessage("Error")
            builder.setPositiveButton("Continue",
                DialogInterface.OnClickListener { dialog, which -> handler.proceed() })
            builder.setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialog, which -> handler.cancel() })
            val dialog: AlertDialog = builder.create()
            dialog.show()*/
        }
    }


}