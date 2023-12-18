package com.i69.ui.base

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.i69.data.preferences.UserPreferences
import com.i69.singleton.App
import com.i69.utils.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

abstract class BaseActivity<dataBinding : ViewDataBinding> : AppCompatActivity() {

    protected lateinit var userPreferences: UserPreferences
    lateinit var binding: dataBinding
    protected lateinit var loadingDialog: Dialog


    override fun onCreate(savedInstanceState: Bundle?) {
        userPreferences = App.userPreferences
        setDefaultLanguage()
        super.onCreate(savedInstanceState)


        getSupportActionBar()?.hide();

        binding = getActivityBinding(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            lifecycleOwner = this@BaseActivity
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        loadingDialog = createLoadingDialog()
        setupTheme(savedInstanceState)
        setupClickListeners()


    }

    fun setDefaultLanguage() {
        lifecycleScope.launch {
            var lang = ""
            val pref: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this@BaseActivity)

            val config = resources.configuration
//            lang = Locale.getDefault().language
            lang = if (getCurrentUserId() == null)
                Locale.getDefault().language
            else if (pref.getString("language", "").isNullOrEmpty())
                Locale.getDefault().language
            else
                pref.getString("language", "en").toString()

            val locale = Locale(lang)
            Locale.setDefault(locale)



            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                config.setLocale(locale)
            else
                config.locale = locale

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                createConfigurationContext(config)
            resources.updateConfiguration(config, resources.displayMetrics)
            //toast("Locale changed")

        }
    }


    override fun attachBaseContext(newBase: Context?) {

        var pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(newBase)
        var language = pref.getString("language", "en")
        var newLocale = Locale(language)
        // .. create or get your new Locale object here.
        val context: Context = ContextWrapper.wrap(newBase, newLocale)
        super.attachBaseContext(context)
    }

/*
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        baseContext.resources.updateConfiguration(newConfig, getBaseContext().getResources().getDisplayMetrics());
        toast("Configured")

        getSupportActionBar()?.hide();

        userPreferences = App.userPreferences
        binding = getActivityBinding(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            lifecycleOwner = this@BaseActivity
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        loadingDialog = createLoadingDialog()
        setupClickListeners()
    }
*/

    abstract fun getActivityBinding(inflater: LayoutInflater): dataBinding

    abstract fun setupTheme(savedInstanceState: Bundle?)

    abstract fun setupClickListeners()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    protected fun showProgressView() {
        Log.e("TAG", "showProgressView: " )
        runOnUiThread { loadingDialog.show() }
    }

    protected fun hideProgressView() {
        Log.e("TAG", "hideProgressView: " )
        runOnUiThread { loadingDialog.dismiss() }
    }

    suspend fun getCurrentUserName() = userPreferences.userName.first()

    suspend fun getCurrentUserId() = userPreferences.userId.first()

    suspend fun getCurrentUserToken() = userPreferences.userToken.first()

    suspend fun getChatUserId() = userPreferences.chatUserId.first()

    fun transact(fr: Fragment, addToBackStack: Boolean = false) =
        supportFragmentManager.transact(fr, addToBackStack)

}