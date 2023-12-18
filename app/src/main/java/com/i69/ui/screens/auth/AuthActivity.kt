package com.i69.ui.screens.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.exception.ApolloException
import com.google.gson.Gson
import com.i69.AttrTranslationQuery
import com.i69.applocalization.AppStringConstantViewModel
import com.i69.applocalization.getLoalizations
import com.i69.applocalization.getLoalizationsStringList
import com.i69.applocalization.updateLoalizationsConstString
import dagger.hilt.android.AndroidEntryPoint
import com.i69.databinding.ActivitySignInBinding
import com.i69.singleton.App
import com.i69.ui.base.BaseActivity
import com.i69.ui.screens.main.MainActivity
import com.i69.utils.SharedPref
import com.i69.utils.apolloClient
import com.i69.utils.startActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class AuthActivity : BaseActivity<ActivitySignInBinding>() {

    override fun getActivityBinding(inflater: LayoutInflater) = ActivitySignInBinding.inflate(inflater)

    override fun setupTheme(savedInstanceState: Bundle?) {}
    private val viewModessl: AppStringConstantViewModel by viewModels()

    override fun setupClickListeners() {}

    fun updateStatusBarColor(color: Int) {// Color must be in hexadecimal format
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = color
    }

    fun updateLanguageChanged() {
        showProgressView()
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
                    getLoalizations(this@AuthActivity, isUpdate = true)
                viewModessl.data.postValue(consted)
            } else {
                val res = try {
                    apolloClient(this@AuthActivity, userToken!!).query(
                        AttrTranslationQuery(
                            locaLizationString
                        )
                    )
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponse ${e.message}")
                    Toast.makeText(this@AuthActivity, " ${e.message}", Toast.LENGTH_LONG)
                        .show()

//                hideProgressView()
                    return@launch
                }
                if (res.hasErrors()) {
                } else {


                    var consted =
                        getLoalizations(this@AuthActivity, res.data?.attrTranslation)

//                  Log.e(":responsegetted", Gson().toJson( res.data))
                    Log.e("responsegetted", Gson().toJson(consted))
                    viewModessl.data.postValue(consted)
                    val sharedPref = SharedPref(this@AuthActivity)
                    sharedPref.setAttrTranslater(consted)
                    updateLoalizationsConstString(this@AuthActivity, consted)

                }
                delay(1200)

                lifecycleScope.launch(Dispatchers.Main) {
                    hideProgressView()
                    startActivity<MainActivity>()
                    finish()
                }
            }

        }

    }


}
