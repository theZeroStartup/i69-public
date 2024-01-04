package com.i69.ui.screens.main.contact

import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.i69.BuildConfig
import com.i69.R
import com.i69.databinding.FragmentContactusBinding
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.main.MainActivity.Companion.getMainActivity
import com.i69.ui.screens.main.notification.NotificationDialogFragment
import com.i69.ui.viewModels.AuthViewModel
import com.i69.utils.snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import timber.log.Timber


class ContactUsFragment:BaseFragment<FragmentContactusBinding>() {
    //    private lateinit var mWebView: WebView
    private val viewModel: AuthViewModel by activityViewModels()
    private var userToken: String? = null
    private var userId: String? = null
//    private var url: String? = null

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentContactusBinding.inflate(inflater, container, false)

    override fun setupTheme() {
        //requireActivity().window.requestFeature(Window.FEATURE_NO_TITLE)
//        loadingDialog.show()
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        lifecycleScope.launch {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            Timber.i("usertokenn $userToken")
        }
//        CookieManager.getInstance().removeAllCookies {
//            CookieManager.getInstance().flush()
//        }
//        WebStorage.getInstance().deleteAllData()
//        url = "https://chatadmin-mod.click/#/contactUs"
////        if (getMainActivity()?.pref?.getString("typeview","privacy").equals("privacy")){
////            var privacyUrl =  com.i69app.data.config.Constants.URL_PRIVACY_POLICY.plus(MainActivity.getMainActivity()?.pref?.getString("language", "en")).plus("/contactUs")
////            Log.e("myPrivacyUrl",privacyUrl)
//////            url=com.i69app.data.config.Constants.URL_PRIVACY_POLICY
////            url = privacyUrl
////        }else{
////            url=com.i69app.data.config.Constants.URL_TERMS_AND_CONDITION
////        }
//        //mWebView = WebView(requireContext())
//        binding.privacyWebView.settings.javaScriptEnabled = true
//        binding.privacyWebView.settings.javaScriptCanOpenWindowsAutomatically = true
//
//        binding.privacyWebView.webChromeClient = mWebChromeClient
//        binding.privacyWebView.webViewClient = object : WebViewClient() {
//            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
//                loadingDialog.dismiss()
//                view.loadUrl(url)
//                return true
//            }
//        }
//        binding.privacyWebView.loadUrl(url!!)

        //requireActivity().setContentView(mWebView)
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
            getMainActivity()?.notificationDialog(dialog,childFragmentManager,"${requireActivity().resources.getString(R.string.notifications)}")
        }

        binding.sentMsg.setOnClickListener {
//            val userToken = getCurrentUserToken()!!
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val message = binding.etMessage.text.toString()

            if(name.isNullOrEmpty()){
                binding.etName.setError("Name required!")
                binding.etName.requestFocus()
                return@setOnClickListener
            }
            if(email.isNullOrEmpty()){
                binding.etEmail.setError("Email required!")
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }
            if(message.isNullOrEmpty()){
                binding.etMessage.setError("Message required!")
                binding.etMessage.requestFocus()
                return@setOnClickListener
            }
            binding.pg.visibility = View.VISIBLE
            contactUs(name,email,message)
        }
    }

    private fun contactUs(
        name: String,
        email: String,
        message: String
//        token : String
    ) {
        lifecycleScope.launch(Dispatchers.Main) {

            val userToken = getCurrentUserToken()!!
            loadingDialog.show()
            val client = OkHttpClient()
            val mediaType = "application/json".toMediaType()
            val body = "{\r\n    \"name\": \"$name\",\r\n    \"email\": \"$email\",\r\n    \"message\": \"$message\"\r\n}".toRequestBody(mediaType)
            val request = Request.Builder()
                .url(BuildConfig.BASE_URL+"api/contact-us/")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()
            val response = client.newCall(request).execute()

            if(response.isSuccessful){
                loadingDialog.dismiss()
                binding.pg.visibility = View.GONE

                val responseBody = response.body?.string()
                val jsonObject = JSONObject(responseBody)
                val isSuccess = jsonObject.getBoolean("success")

                if (isSuccess) {
                    binding.root.snackbar(getString(R.string.email_sent))
                    findNavController().popBackStack()
                }
                else {
                    binding.root.snackbar(getString(R.string.somethig_went_wrong_please_try_again))
                }
            }
        }
    }
}