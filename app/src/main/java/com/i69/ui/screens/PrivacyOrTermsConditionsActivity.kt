package com.i69.ui.screens

import android.content.DialogInterface
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.Window.FEATURE_NO_TITLE
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.i69.ui.screens.main.MainActivity


class PrivacyOrTermsConditionsActivity : AppCompatActivity() {

    private lateinit var mWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(FEATURE_NO_TITLE)
        mWebView = WebView(this)
        mWebView.loadUrl(getUrl())
      var privacyUrl =  com.i69.data.config.Constants.URL_PRIVACY_POLICY//.plus(MainActivity.getMainActivity()?.pref?.getString("language", "en")).plus("/policy")
      Log.e("myPrivacyUrldsvbv",privacyUrl)
        mWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }
        }

        this.setContentView(mWebView)
    }



    private fun getUrl() =
        if (intent.hasExtra("type") && intent.getStringExtra("type") == "privacy") com.i69.data.config.Constants.URL_PRIVACY_POLICY else com.i69.data.config.Constants.URL_TERMS_AND_CONDITION

}