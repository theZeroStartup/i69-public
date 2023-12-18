package com.i69.ui.screens.main.coins

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.webkit.*
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.exception.ApolloException
import com.google.gson.Gson
import com.i69.PaypalCapturePaymentMutation
import com.i69.applocalization.AppStringConstant1
import com.i69.databinding.ActivityWebLoginBinding
import com.i69.ui.base.BaseActivity
import com.i69.utils.apolloClient
import kotlinx.coroutines.launch

class WebPaymentActivity : BaseActivity<ActivityWebLoginBinding>() {

    companion object {
        const val ARGS_ACCESS_TOKEN = "access_token"
        const val ARGS_ACCESS_VERIFIER = "access_verifier"
        var IS_Done = false
        var paypalCapturePayment = ""
    }

    override fun getActivityBinding(inflater: LayoutInflater) =
        ActivityWebLoginBinding.inflate(inflater)

    override fun setupTheme(savedInstanceState: Bundle?) {
        loadingDialog.show()

        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
        }
        WebStorage.getInstance().deleteAllData()

        with(binding.webView) {
            clearHistory()
            clearFormData()
            clearMatches()
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            webViewClient = mWebViewClient
            webChromeClient = mWebChromeClient
            loadUrl(intent.getStringExtra("url").toString())
        }
    }

    override fun setupClickListeners() {

    }

    private val mWebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            Log.d("RequestUrl", request?.url.toString())
//            if(request?.url.toString().contains("returnUrl")){
            paypalCapturePayment(intent.getStringExtra("id").toString())
//            }
//            if (request?.url.toString().startsWith(Constants.TWITTER_CALLBACK_TEMP_URL)) {
//                val decodedUrl = URLDecoder.decode(request?.url.toString(), "UTF-8")
//                Timber.d("Decode Url $decodedUrl")
//
//                if (decodedUrl.contains("oauth_token=")) {
//                    val uri = Uri.parse(decodedUrl)
//                    val accessToken = uri.getQueryParameter("oauth_token")
//                    val accessVerifier = uri.getQueryParameter("oauth_verifier")
//                    Timber.d("Access Token: $accessToken")
//
//                    val intent = Intent()
//                    intent.putExtra(ARGS_ACCESS_TOKEN, accessToken)
//                    intent.putExtra(ARGS_ACCESS_VERIFIER, accessVerifier)
//                    setResult(RESULT_OK, intent)
//                    finish()
//                }
//                return true
//            }
            return false
        }
    }

    private val mWebChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            if (newProgress == 100) loadingDialog.dismiss()
        }
    }

    private fun paypalCapturePayment(orderId: String) {

//        Log.e("craetedOrderId", paypalCreateOrderId)
        Log.e("FromcraetedOrderId", orderId)
        lifecycleScope.launch {
            val userToken = getCurrentUserToken()
            try {
                val response = apolloClient(applicationContext, userToken!!).mutation(
                    PaypalCapturePaymentMutation(orderId)
                ).execute()

                if (response.hasErrors()) {

                    var message = response.errors?.get(0)?.message
                        ?: AppStringConstant1.something_went_wrong_please_try_again_later
//                        ?: "Something went wrong, Please try after sometime"

                    Log.e("MyPaymentIdWrong", Gson().toJson(response.errors))
//                    binding.root.snackbar(message)
                } else {

                    Log.e(
                        "CapturePaypalOrderId",
                        Gson().toJson(response.data?.paypalCapturePayment)
                    )
                    IS_Done = true
                    paypalCapturePayment = response.data?.paypalCapturePayment?.id!!
//                    val purchaseFragment = PurchaseFragment()
//                    purchaseFragment.onPaymentSuccess("in-app-purchase", response.data?.paypalCapturePayment?.id!!)
                    finish()
//                    onPaymentSuccess("in-app-purchase", response.data?.paypalCapturePayment?.id!!)
                }
            } catch (e: ApolloException) {
                Log.d("PurchaseFragment", "Operators Exception ${e.message}")

            } catch (e: Exception) {
                Log.d("PurchaseFragment", "Operators Exception ${e.message}")

            }
        }


    }


}