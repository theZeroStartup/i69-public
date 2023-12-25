package com.i69.ui.screens.auth.login

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.StrictMode
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.exception.ApolloException
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.i69.*
import com.i69.data.models.Photo
import com.i69.data.models.User
import com.i69.data.remote.requests.LoginRequest
import com.i69.data.remote.responses.LoginResponse
import com.i69.data.remote.responses.ResponseBody
import com.i69.databinding.FragmentLoginBinding
import com.i69.firebasenotification.FCMHandler
import com.i69.singleton.App
import com.i69.ui.base.BaseFragment
import com.i69.ui.base.profile.PUBLIC
import com.i69.ui.screens.auth.AuthActivity
import com.i69.ui.viewModels.AuthViewModel
import com.i69.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class LoginFragment : BaseFragment<FragmentLoginBinding>() {

    private val viewModel: AuthViewModel by activityViewModels()
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private lateinit var loginManager: LoginManager

    private val googleLoginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            Log.e("LoginFragment", "Data : : ${result.data}")
            Log.e("LoginFragment", "Code : : ${result.resultCode}")
            try {
                val account = task.getResult(ApiException::class.java)!!
                showProgressView()
                Log.e("LoginFragment", "Google Token : : ${account.idToken}")
                Timber.e("Google Token: ${account.idToken}")
                onLoginSuccess(
                    provider = com.i69.data.enums.LoginProvider.GOOGLE,
                    name = account.displayName,
                    photo = "",
                    accessToken = account.idToken!!
                )

            } catch (e: ApiException) {
                Timber.e("${getString(R.string.sign_in_failed)} ${e.message}")
                Log.e("LoginFragment", "${e.message}")
                Log.e(
                    "LoginFragment", "Message: ${e.localizedMessage}"
                )
                if (e.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                    binding.root.snackbar("${e.message}") {}
                }
            }
        }

    private val webLoginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val data = result.data
            if (result.resultCode == RESULT_OK) {
                val accessToken = data?.getStringExtra(WebLoginActivity.ARGS_ACCESS_TOKEN)
                val accessVerifier = data?.getStringExtra(WebLoginActivity.ARGS_ACCESS_VERIFIER)

                showProgressView()
                onLoginSuccess(
                    provider = com.i69.data.enums.LoginProvider.TWITTER,
                    photo = "",
                    accessToken = accessToken.toString(),
                    accessVerifier = accessVerifier.toString()
                )
            }
        }

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentLoginBinding.inflate(inflater, container, false)


    override fun setupTheme() {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        Log.d("TAG", "setupThemes: "+ Locale.getDefault().language)
        availavleSocialLogin()

        binding.signInLogo.defaultAnimate(300, 800)
        binding.signInAppTitle.defaultAnimate(300, 800)
        //binding.signInAppDescription.defaultAnimate(300, 800)
        binding.signInButtonWithFb.defaultAnimate(300, 800)
        binding.signInButtonWithTwitter.defaultAnimate(300, 800)
        binding.signInButtonWithGoogle.defaultAnimate(300, 800)

        initializeGoogleSignIn()
        initializeFacebookSignIn()
    }

    override fun setupClickListeners() {
        binding.signInButtonWithFb.setOnClickListener {
            loginToFacebook()
        }
        binding.signInButtonWithTwitter.setOnClickListener { loginToTwitter() }
        binding.signInButtonWithGoogle.setOnClickListener { loginToGoogle() }
    }

    private fun availavleSocialLogin() {
        Log.e("callSocialAuth", "callSocialAuth")
        lifecycleScope.launch(Dispatchers.IO) {

            val response = try {
                apolloClient(requireContext(), "").query(
                    GetAllSocialAuthStatusQuery()
                ).execute()

            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                hideProgressView()
                return@launch
            }


//            val response =    try {
//                apolloClient(requireActivity(), getCurrentUserToken()!!).query(
//                    GetAllSocialAuthStatusQuery()
//                ).execute()
//                Timber.d("reealltime 2")
//            } catch (e2: Exception) {
//                e2.message?.let { Log.e("exceptionFoundInScroll==>", it) }
//                e2.printStackTrace()
//
//                Timber.d("reealltime exception= ${e2.message}")
//            }
            if (response.hasErrors()) {
                val error = response.errors?.get(0)?.message
                Timber.d("Exception momentCommentDelete $error")
                Log.e("reportsOnComens", "Error : ${error}")

                binding.root.snackbar(" $error")
            } else {
                Log.e("reportsOnComens", "data : ${response.data!!}")
                lifecycleScope.launch {
                    for (socialauth in response.data!!.allSocialAuthStatus!!) {

                        if (socialauth!!.provider!!.contains("google", ignoreCase = true)) {
                            if (socialauth!!.status!!.name.contains(
                                    "DISABLED",
                                    ignoreCase = true
                                )
                            ) {
                                binding.signInButtonWithGoogle.visibility = View.GONE
                            } else {
                                binding.signInButtonWithGoogle.visibility = View.VISIBLE
                            }

                        } else if (socialauth!!.provider!!.contains("twitter", ignoreCase = true)) {
                            if (socialauth!!.status!!.name.contains(
                                    "DISABLED",
                                    ignoreCase = true
                                )
                            ) {
                                binding.signInButtonWithTwitter.visibility = View.GONE
                            } else {
                                binding.signInButtonWithTwitter.visibility = View.GONE
                            }

                        } else if (socialauth!!.provider!!.contains(
                                "facebook",
                                ignoreCase = true
                            )
                        ) {
                            if (socialauth!!.status!!.name.contains(
                                    "DISABLED",
                                    ignoreCase = true
                                )
                            ) {
                                binding.signInButtonWithFb.visibility = View.GONE
                            } else {
                                binding.signInButtonWithFb.visibility = View.VISIBLE
                            }

                        }
                    }
                }

            }

        }
    }

    // 37873016176-m7aloihs6revp8b8ckc9mq0vmf6kk57o.apps.googleusercontent.com

    // have to add  dynamic server client ID from the actual project.
    private fun initializeGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestIdToken("930450586375-39dqp90019nb94o8ovkn0f7sf86b1cii.apps.googleusercontent.com")
            .requestIdToken("223314224724-skuq309vrsk69a7m0cmhkl1ska4qtd82.apps.googleusercontent.com")
            //  .requestIdToken(getString(R.string.server_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun initializeFacebookSignIn() {
        callbackManager = CallbackManager.Factory.create()
        loginManager = LoginManager.getInstance()
        loginManager.setLoginBehavior(LoginBehavior.WEB_ONLY)
        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult>{
            override fun onSuccess(result: LoginResult) {
                Timber.d("Facebook: onSuccess: $result")
                Log.e("TAG","Facebook: onSuccess: $result")
                result?.let {
                    showProgressView()
                    viewModel.getUserDataFromFacebook(loginResult = result) { name, photos ->
                        onLoginSuccess(
                            com.i69.data.enums.LoginProvider.FACEBOOK,
                            name = name,
                            photo = photos?.firstOrNull() ?: "",
                            accessToken = it.accessToken.token
                        )
                    }
                }
            }
            override fun onCancel() {
                Timber.d("Facebook: onCancel")
            }
            override fun onError(exception: FacebookException) {
                Timber.e("Facebook: onError: $exception")
                binding.root.snackbar("${getString(R.string.sign_in_failed)} ${getString(R.string.try_again_later)}")
            }
        })

//        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult?> {
//            override fun onSuccess(result: LoginResult?) {
//                Timber.d("Facebook: onSuccess: $result")
//                result?.let {
//                    showProgressView()
//                    viewModel.getUserDataFromFacebook(loginResult = result) { name, photos ->
//                        onLoginSuccess(
//                            com.i69app.data.enums.LoginProvider.FACEBOOK,
//                            name = name,
//                            photo = photos?.firstOrNull() ?: "",
//                            accessToken = it.accessToken.token
//                        )
//                    }
//                }
//            }
//
//            override fun onCancel() {
//                Timber.d("Facebook: onCancel")
//            }
//
//            override fun onError(exception: FacebookException) {
//                Timber.e("Facebook: onError: $exception")
//                binding.root.snackbar("${getString(R.string.sign_in_failed)} ${getString(R.string.try_again_later)}")
//            }
//        })
    }

    private fun loginToGoogle() {
        val signInIntent = mGoogleSignInClient.signInIntent
        googleLoginLauncher.launch(signInIntent)
    }

    private fun loginToFacebook() {
//        loginManager.logInWithReadPermissions(
//            this,
//            mutableListOf("email", "public_profile", "user_friends")
//        )
        loginManager.logInWithReadPermissions(this, Arrays.asList("email", "public_profile", "user_friends"))
    }

    private fun loginToTwitter() {
        val intent = Intent(requireActivity(), WebLoginActivity::class.java)
        webLoginLauncher.launch(intent)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onLoginSuccess(
        provider: com.i69.data.enums.LoginProvider,
        name: String? = "",
        photo: String,
        accessToken: String,
        accessVerifier: String = ""
    ) {
        lifecycleScope.launch(Dispatchers.Main) {
            val loginRequest = LoginRequest(
                accessToken = accessToken,
                accessVerifier = accessVerifier,
                provider = provider.provider
            )

            when (val response = viewModel.login(loginRequest)) {

                is Resource.Success -> {
                    val nameValue =
                        if (provider == com.i69.data.enums.LoginProvider.TWITTER) response.data?.data?.username else name
                    val compressedFilePath =
                        if (photo.isNotEmpty()) requireContext().getCompressedImageFilePath(photo) else ""
                    val photos =
                        if (compressedFilePath.isNullOrEmpty()) mutableListOf() else mutableListOf(
                            Photo(
                                id = "0", compressedFilePath,
                                PUBLIC
                            )
                        )

                    hideProgressView()

                    FCMHandler.enableFCM()

                    navigateToNextScreen(response, nameValue, photos)
                }
                is Resource.Error -> {
                    hideProgressView()
                    Log.d(
                        "LoginFragment",
                        "${getString(R.string.sign_in_failed)} ${response.message}"
                    )
                    Timber.e("${getString(R.string.sign_in_failed)} ${response.message}")

                    if (response.message.toString().contains(getString(R.string.contact_us), true)){
                        binding.root.snackbar(getString(R.string.account_deleted_error)) {
                            Log.d("LoginFragment", "Api response")
                            moveTo(LoginFragmentDirections.actionLoginFragmentToContactFragment())
                        }
                    }
                    else {
                        binding.root.snackbar("${response.message}") {}
                    }
                }
                else -> {

                }
            }
        }
    }

    private suspend fun navigateToNextScreen(
        response: Resource.Success<ResponseBody<LoginResponse>>,
        name: String?,
        photos: MutableList<Photo>
    ) {
        val loginResult = response.data!!.data

        if (loginResult?.isNew == true) {
            val email = loginResult.email?.substring(0, loginResult.email.indexOf("@")) ?: ""

            val names = name?.replace("_twitter_twitter", "")!!.replace("_twitter", "")

            viewModel.setAuthUser(
                User(
                    id = loginResult.id,
                    email = loginResult.email ?: "",
//                    fullName = names ?: email,
                    fullName = names ?: email,
                    avatarPhotos = photos,
                )
            )
            viewModel.token = loginResult.token
            moveTo(R.id.action_login_to_interested_in)

        } else {
            val names = name?.replace("_twitter_twitter", "")!!.replace("_twitter", "")
            userPreferences.saveUserIdToken(
                userId = loginResult!!.id,
                token = loginResult.token,
                names
            )
            Log.e("pppp2222", "ppp")
            App.updateFirebaseToken(viewModel.userUpdateRepository)
            App.updateOneSignal(viewModel.userUpdateRepository)

            updateLanguage(loginResult.id,loginResult.token)

        }
    }

    private fun updateLanguage(id: String, token: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val languageModel = viewModel.getLanguage(id,token)
            val userLang = languageModel?.userLanguageCode
            val pref = PreferenceManager.getDefaultSharedPreferences(requireActivity())
            val prefLanguage = pref.getString("language","")
            val deviceLocale = userLang?: if (prefLanguage.isNullOrEmpty()) Locale.getDefault().language else prefLanguage
//            val response = viewModel.updateLanguage(
//                languageCode = deviceLocale,
//                userid = id,
//                token = token
//            )
//            lifecycleScope.launch(Dispatchers.Main) {
//                getAuthActivity().updateLanguageChanged()
//            }

            when(val response = viewModel.updateLanguage(
                languageCode = deviceLocale,
                userid = id,
                token = token
            )){
                is Resource.Success -> {
                    pref?.edit()?.putString("language", deviceLocale)
                        ?.apply()
                    lifecycleScope.launch(Dispatchers.Main) {
                        getAuthActivity().updateLanguageChanged()
                    }
//                    getAuthActivity().updateLanguageChanged()
//                    Timber.e("${"LanguageUpdate Success"} ${response.message}")
                }

                is Resource.Error -> {
                    lifecycleScope.launch(Dispatchers.Main) {
                        Timber.e("${"LanguageUpdate Failed"} ${response.message}")
                        binding.root.snackbar("${"LanguageUpdate Failed"} ${response.message}")
                    }

                }else -> {

            }
            }

        }



        /*viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val response = viewModel.updateLanguage(
                languageCode = deviceLocale,
                userid = id,
                token = token
            )
            lifecycleScope.launch(Dispatchers.Main) {
                getAuthActivity().updateLanguageChanged()
            }

        }*/

/*
        lifecycleScope.launch(Dispatchers.Main) {
            async(Dispatchers.IO) {
                viewModel.updateLanguage(
                    languageCode = deviceLocale,
                    userid = id,
                    token = token
                )
                Log.d("languge", "langauage")
            }.await()

        }
*/

    //    getAuthActivity().updateLanguageChanged()

/*
        lifecycleScope.launch() {
            delay(2000)
//            val sharedPref = SharedPref(requireContext())
//            sharedPref.setLanguage(true)
//            sharedPref.setLanguageFromSettings(true)

            requireActivity().startActivity<MainActivity>()
            requireActivity().finish()
        }
*/


    }


    fun getAuthActivity() = activity as AuthActivity



}