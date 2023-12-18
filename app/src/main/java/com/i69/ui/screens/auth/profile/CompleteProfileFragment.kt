package com.i69.ui.screens.auth.profile

import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.apollographql.apollo3.exception.ApolloException
import com.google.gson.Gson
import com.i69.AttrTranslationQuery
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.i69.R
import com.i69.applocalization.*
import com.i69.data.config.Constants.INTEREST_MOVIE
import com.i69.data.config.Constants.INTEREST_MUSIC
import com.i69.data.config.Constants.INTEREST_SPORT_TEAM
import com.i69.data.config.Constants.INTEREST_TV_SHOW
import com.i69.languages.LanguageAdapter
import com.i69.singleton.App
import com.i69.ui.base.profile.BaseEditProfileFragment
import com.i69.ui.interfaces.AlertDialogCallback
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.viewModels.AuthViewModel
import com.i69.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.File
import java.util.*

@AndroidEntryPoint
class CompleteProfileFragment : BaseEditProfileFragment(), AdapterView.OnItemSelectedListener {

    private val viewModel: AuthViewModel by activityViewModels()
    private val viewModessl: AppStringConstantViewModel by activityViewModels()
    var selectedLanguage = ""


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
       
        binding.edAbout.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.tvAboutTitleCount.text = "${s?.length}/250"
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })



    }

    override fun callparentmethod(pos: Int, photo_url: String) {

    }

    override fun showBuyDialog(photoQuota: Int, coinSpendAmt: Int) {
        requireContext().showAlertDialog(
            positionBtnText = getString(R.string.yes),
            title = getString(R.string.app_name),
            subTitle = String.format(
                getString(R.string.upload_image_warning),
                photoQuota,
                coinSpendAmt
            ),
            listener = object : AlertDialogCallback {
                override fun onNegativeButtonClick(dialog: DialogInterface) {
                    dialog.dismiss()
                }

                override fun onPositiveButtonClick(dialog: DialogInterface) {
                    dialog.dismiss()
                    if (coinSpendAmt > binding.user!!.purchaseCoins) {
                        findNavController().navigate(R.id.actionGoToPurchaseFragment)

                    } else {
                        lifecycleScope.launch(Dispatchers.Main) {
                            when (val response = viewModel.deductCoin(
                                userId = binding.user!!.id,
                                token =viewModel.token!!,
                                com.i69.data.enums.DeductCoinMethod.PROFILE_PICTURE
                            )) {
                                is Resource.Success -> {
                                    onDoneClick(true)
                                }
                                is Resource.Error -> binding.root.snackbar("${getString(R.string.something_went_wrong)} ${response.message}")
                                else ->{

                                }
                            }
                        }
                    }
                }
            }
        )
    }


    override fun setupScreen() {
        binding.titleLabel.text = getString(R.string.profile_complete_title)
        viewModel.token?.let {
            viewModel.getDefaultPickers(it).observe(viewLifecycleOwner) {
                it?.let { defaultPickerValue ->
                    defaultPicker = defaultPickerValue
                    binding.defaultPicker = defaultPicker!!
                }
            }
        }
        viewModel.getAuthUser()?.let {
            binding.user = it
        }
        val appStringConstant = AppStringConstant(requireContext())
        binding.stringConstant = appStringConstant
        prefillEditProfile(viewModel.getAuthUser()!!)
        setLanguage()
    }

    fun setLanguage() {
        var allLangs = fetchLanguages()
        binding.languageSpinner.isClickable = false
        lifecycleScope.launch(Dispatchers.Main) {
            val adapter = LanguageAdapter(requireContext(), allLangs)
            binding.languageSpinner.adapter = adapter
            binding.languageSpinner.onItemSelectedListener = this@CompleteProfileFragment
            val  sl = Locale.getDefault().language
            selectedLanguage = Locale.getDefault().language
            for (i in 0 until allLangs.size) {
                if (allLangs[i].supportedLangCode.equals(sl)) {
                    binding.languageSpinner.setSelection(i)
                }
            }
        }
    }

    override fun getInterestedInValues(interestsType: Int): List<String> = when (interestsType) {
        INTEREST_MUSIC -> if (viewModel.getAuthUser()?.music.isNullOrEmpty()) emptyList() else viewModel.getAuthUser()?.music!!
        INTEREST_MOVIE -> if (viewModel.getAuthUser()?.movies.isNullOrEmpty()) emptyList() else viewModel.getAuthUser()?.movies!!
        INTEREST_TV_SHOW -> if (viewModel.getAuthUser()?.tvShows.isNullOrEmpty()) emptyList() else viewModel.getAuthUser()?.tvShows!!
        INTEREST_SPORT_TEAM -> if (viewModel.getAuthUser()?.sportsTeams.isNullOrEmpty()) emptyList() else viewModel.getAuthUser()?.sportsTeams!!
        else -> if (viewModel.getAuthUser()?.sportsTeams.isNullOrEmpty()) emptyList() else viewModel.getAuthUser()?.sportsTeams!!
    }

    override fun setInterestedInToViewModel(interestType: Int, interestValue: List<String>) {
        when (interestType) {
            INTEREST_MUSIC -> viewModel.getAuthUser()?.music = interestValue.toMutableList()
            INTEREST_MOVIE -> viewModel.getAuthUser()?.movies = interestValue.toMutableList()
            INTEREST_TV_SHOW -> viewModel.getAuthUser()?.tvShows = interestValue.toMutableList()
            INTEREST_SPORT_TEAM -> viewModel.getAuthUser()?.sportsTeams = interestValue.toMutableList()
        }
    }

    override fun onDoneClick(increment: Boolean) {
        if (!isProfileValid(isLogin = true)) return
        showProgressView()
        viewModel.setAuthUser(getViewModelUser(viewModel.getAuthUser()!!, login = true))
        val user = getApiUser(viewModel.getAuthUser()!!.copy())
        user.apply {
            this.userLanguageCode = selectedLanguage
        }

        Timber.e("USER: $user")
        lifecycleScope.launch(Dispatchers.IO) {
            user.avatarPhotos?.forEach { photo ->
                Log.d("avatarPhotos",""+user.avatarPhotos)
                 if(photo.url?.contains("content")!!){
                    val result = photo.url!!.toUri().path
                    val openInputStream = requireActivity().contentResolver?.openInputStream(photo.url!!.toUri())
                    val type = if (result?.contains("video") == true) ".mp4" else ".jpg"
                    val outputFile = requireContext().filesDir.resolve("${System.currentTimeMillis()}$type")
                    openInputStream?.copyTo(outputFile.outputStream())
                    val file = File(outputFile.toURI())
                    viewModel.uploadImage2(userId = user.id,  viewModel.token!!, filePath = file)
                }else{
                    viewModel.uploadImage(userId = user.id,viewModel.token!!, filePath = photo.url!!)
                }
                //   val res=viewModel.uploadImage(userId = user.id, viewModel.token!!, filePath = photo.url)
                //    Log.d("newPhotos",""+res.data.toString())

            }
            withContext(Dispatchers.Main) {
                when (val response = viewModel.updateProfile(user = user, viewModel.token!!)) {
                    is Resource.Success -> {
                        userPreferences.saveUserIdToken(userId = response.data!!.data!!.id, token = viewModel.token!!,user.fullName)
                        Log.e("pppp333","ppp")
                        App.updateFirebaseToken(viewModel.userUpdateRepository)

                        App.updateOneSignal(viewModel.userUpdateRepository)
                        hideProgressView()
                        updateLanguage(user.id, viewModel.token!!)
                    }
                    is Resource.Error -> {
                        Log.e("gggggg","yes")
                        hideProgressView()
                        Timber.e("${getString(R.string.sign_in_failed)} ${response.message}")
                        binding.root.snackbar("${getString(R.string.sign_in_failed)} ${response.message}")
                    } else -> {

                    }
                }
            }
        }
    }

    override fun onRemoveBtnClick(position: Int, s: String) {

    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
        selectedLanguage = fetchLanguages()[position].supportedLangCode
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    fun updateLanguageChanged() {
        Log.e("UpdatageTranslation", "UpdateLanguageTranslation")

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
                    getLoalizations(requireContext(), isUpdate = true)
                viewModessl.data.postValue(consted)
            }
            else {
                val res = try {
                    apolloClient(requireContext(), userToken!!).query(
                        AttrTranslationQuery(
                            locaLizationString
                        )
                    )
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponse ${e.message}")
                    Toast.makeText(requireActivity(), " ${e.message}", Toast.LENGTH_LONG)
                        .show()

//                hideProgressView()
                    return@launch
                }
                if (res.hasErrors()) {

                    Log.e("responsegetted", Gson().toJson(res.errors))
                } else {

                    res.data?.attrTranslation?.forEach {
                        if (it?.nameTranslated == "") {
                            Log.v("TransaltionNw", it?.name + " " + it?.nameTranslated)
                        }
                    }

                    var consted =
                        getLoalizations(requireContext(), res.data?.attrTranslation)

//                  Log.e(":responsegetted", Gson().toJson( res.data))
                    val tarnslationJson = Gson().toJson(consted)
                    /* val veryLongString = tarnslationJson.toString()
                     val maxLogSize = 1000
                     for (i in 0..veryLongString.length / maxLogSize) {
                         val start = i * maxLogSize
                         var end = (i + 1) * maxLogSize
                         end = if (end > veryLongString.length) veryLongString.length else end
                         Log.v("TAG", veryLongString.substring(start, end))
                     }*/
                    Log.e("responsegetted", tarnslationJson)
//                    viewModessl.data.postValue(consted)
                    val sharedPref = SharedPref(requireContext())
                    sharedPref.setAttrTranslater(consted)
                    updateLoalizationsConstString(requireContext(), consted)
                    Log.v("WalletTranslation", consted.wallet)
//                    AppStringConstant1.feed = consted.feed
//                    AppStringConstant1(this@SplashActivity).feed= consted.feed


//                    lifecycleScope.launch {
//                        updateLanguage(getCurrentUserId()!!, getCurrentUserToken()!!)
//                    }

                    delay(1200)

                    lifecycleScope.launch(Dispatchers.Main) {
                        Log.d("languge123", "langauage")
                        moveTo(CompleteProfileFragmentDirections.actionProfileCompleteToWelcome())
                    }

                }


//            if (res?.hasErrors() == false) {
////                                views.snackbar("You bought ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} successfully!")
//                Toast.makeText(
//                    requireContext(),
//                    context?.resources?.getString(R.string.you_bought) + " ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} " + context?.resources?.getString(
//                        R.string.successfully
//                    ),
//                    Toast.LENGTH_LONG
//                ).show()
//
//                //fireGiftBuyNotificationforreceiver(gift.id,giftUserid!!)
//
//            }
//            if (res!!.hasErrors()) {
////                                views.snackbar(""+ res.errors!![0].message)
//                Toast.makeText(
//                    requireContext(),
//                    "" + res.errors!![0].message,
//                    Toast.LENGTH_LONG
//                ).show()
//
//            }
//            Timber.d("apolloResponse ${res.hasErrors()} ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName}")
            }

        }

    }


    private fun updateLanguage(id: String, token: String) {
        val deviceLocale = Locale.getDefault().language
        lifecycleScope.launch(Dispatchers.Main) {

            when(val response = viewModel.updateLanguage(
                languageCode = deviceLocale,
                userid = id,
                token = token
            )){
                is Resource.Success -> {
                        Log.e("LanguageUpdate Success", response.message + "")
                        MainActivity.getMainActivity()?.pref?.edit()
                            ?.putString("language", deviceLocale)
                            ?.apply()
                        updateLanguageChanged()
                        Log.e("LanguageUpdate Success", response.message + "")
                        Timber.e("${"LanguageUpdate Success"} ${response.message}")
                }

                is Resource.Error -> {
                    Timber.e("${"LanguageUpdate Failed"} ${response.message}")
                    binding.root.snackbar("${"LanguageUpdate Failed"} ${response.message}")
                }else -> {

            }
            }
        }

        val sharedPref = SharedPref(requireActivity().applicationContext)
        sharedPref.setLanguage(true)
        sharedPref.setLanguageFromSettings(true)

//        lifecycleScope.launch(Dispatchers.Main) {
//            Log.d("languge123", "langauage")
//            if (getCurrentUserId() == null) goToAuthActivity() else startActivity<MainActivity>()
//        }

    }


}