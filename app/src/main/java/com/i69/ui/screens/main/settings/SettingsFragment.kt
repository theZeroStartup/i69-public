package com.i69.ui.screens.main.settings

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.apollographql.apollo3.exception.ApolloException
import com.google.gson.Gson
import com.i69.DeleteAccountAllowedQuery
import com.i69.R
import com.i69.UserSubscriptionQuery
import com.i69.applocalization.AppStringConstant
import com.i69.applocalization.AppStringConstantViewModel
import com.i69.databinding.FragmentSettingsBinding
import com.i69.firebasenotification.FCMHandler
import com.i69.languages.LanguageAdapter
import com.i69.singleton.App
import com.i69.ui.base.BaseFragment
import com.i69.ui.interfaces.AlertDialogCallback
import com.i69.ui.screens.SplashActivity
import com.i69.ui.screens.main.MainActivity.Companion.getMainActivity
import com.i69.ui.viewModels.UserViewModel
import com.i69.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber


class SettingsFragment : BaseFragment<FragmentSettingsBinding>(),
    AdapterView.OnItemSelectedListener {

    private val viewModel: UserViewModel by activityViewModels()
    private val viewStringConstModel: AppStringConstantViewModel by activityViewModels()

    private var userId: String = ""
    private var userToken: String = ""
    var userChatID: Int = 0
    var allLangs = fetchLanguages()
    lateinit var sharedPref: SharedPref
    lateinit var stringConstant : AppStringConstant

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSettingsBinding.inflate(inflater, container, false).apply {
            stringConstant = AppStringConstant(requireContext())

            Log.e("stringConstantAded", "stringConstantAded")
        }


    private fun changeLanguageDialog() {

        val dialogLayout = layoutInflater.inflate(R.layout.dialog_delete, null)
        val headerTitle = dialogLayout.findViewById<TextView>(R.id.header_title)
        val noButton = dialogLayout.findViewById<TextView>(R.id.no_button)
        val yesButton = dialogLayout.findViewById<TextView>(R.id.yes_button)

        headerTitle.text = "${AppStringConstant(requireContext()).are_you_sure_you_want_to_change_language}"
        noButton.text = "${AppStringConstant(requireContext()).no}"
        yesButton.text = "${AppStringConstant(requireContext()).yes}"

        val builder = AlertDialog.Builder(requireContext(),R.style.DeleteDialogTheme)
        builder.setView(dialogLayout)
        val dialog = builder.create()

        noButton.setOnClickListener {
            dialog.dismiss();
        }

        yesButton.setOnClickListener {
            dialog.dismiss();
            binding.spinnerLang.performClick()
        }

        dialog.show()

    }

    override fun onResume() {
        super.onResume()
        Log.e("callonResume", "callonResume")

    }

    override fun setupTheme() {
//        updateLanguageTranslation()

        val config: Configuration = resources.configuration
        if (config.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            //in Right To Left layout
            binding.ivBuyArrow.rotation = 180f
            binding.ivBlockUserArrow.rotation  = 180f
            binding.ivPrivacyArrow.rotation  = 180f
            binding.ivTermsArrow.rotation  = 180f
            binding.ivSubscriptionArrow.rotation  = 180f
            // toast("View is RTL")
        }

        Log.e("callSetupScreen", "callSetupScreen")
        viewStringConstModel.data.observe(this@SettingsFragment) { data ->
            binding.stringConstant = data
            stringConstant = data
        }
        viewStringConstModel.data.also {
            binding.stringConstant = it.value
            stringConstant = it.value?: AppStringConstant(requireContext())
        }

        lifecycleScope.launch {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            //userChatID= getChatUserId()!!
        }

        sharedPref = SharedPref(requireContext())
        sharedPref.setLanguage(false)
        sharedPref.setLanguageFromSettings(false)
        setLocalTempConstants()
        getLanguages()

        navController = findNavController()

//        lifecycleScope.launch {
//            userId = getCurrentUserId()!!
//            userToken = getCurrentUserToken()!!
//            //userChatID= getChatUserId()!!
//        }

        isDeleteAccountAllowed()
//        userSubScription()

        binding.languagesLayout.setOnClickListener {
            changeLanguageDialog()

        }
        binding.spinnerLang.setEnabled(false);
        binding.spinnerLang.setClickable(false);
        binding.spinnerLang.adapter = LanguageAdapter(requireContext(), allLangs)


        Log.e("dvvfb ",allLangs.toString())
        binding.spinnerLang.onItemSelectedListener = this
//

        lifecycleScope.launch(Dispatchers.Main) {
//            var sl = viewModel.getLanguage(userId, token = userToken)
//            getMainActivity()?.pref?.edit()?.putString("language", sl?.userLanguageCode)?.apply()
            val language = getMainActivity()?.pref?.getString("language", "en")

            for (i in 0 until allLangs.size) {
                if (allLangs[i].supportedLangCode.equals(language)) {
                    binding.spinnerLang.setSelection(i)
                }
            }
        }

    }

    fun setLocalTempConstants() {
        TempConstants.LanguageChanged = false
        TempConstants.isFromSettings = false
    }

    override fun setupClickListeners() {
        binding.toolbarHamburger.setOnClickListener {
            //activity?.onBackPressed()
            findNavController().popBackStack()
            //navController.navigate(R.id.nav_search_graph)
            //activity?.finish()
            //getActivity()?.getFragmentManager()?.popBackStack();
            //activity?.finishAfterTransition()
        }

        binding.subScriptionLayout.setOnClickListener {
            navController.navigate(R.id.action_settingsFragment_to_userSubScriptionDetailFragment)
        }

        binding.buyCoinsContainer.setOnClickListener {
            navController.navigate(R.id.actionGoToPurchaseFragment)
        }
        binding.blockedContainer.setOnClickListener {
            navController.navigate(R.id.action_settingsFragment_to_blockedUsersFragment)
        }
        binding.privacyContainer.setOnClickListener {
            /*val intent = Intent(activity, PrivacyOrTermsConditionsActivity::class.java)
            intent.putExtra("type", "privacy")
            startActivity(intent)*/
            getMainActivity()?.pref?.edit()?.putString("typeview", "privacy")?.apply()
            navController.navigate(R.id.actionGoToPrivacyFragment)
        }
        binding.termsContainer.setOnClickListener {
            /*val intent = Intent(activity, PrivacyOrTermsConditionsActivity::class.java)
             intent.putExtra("type", "terms_and_conditions")
             startActivity(intent)*/
            getMainActivity()?.pref?.edit()?.putString("typeview", "terms_and_conditions")?.apply()
            navController.navigate(R.id.actionGoToPrivacyFragment)
        }
        binding.logoutContainer.setOnClickListener {
            Log.e("iddd", "--> before-->" + App.userPreferences.userId)
            lifecycleScope.launch(Dispatchers.IO) {

                App.userPreferences.clear()

            }
            viewModel.logOut(userId = userId, token = userToken) {

                Log.e("iddd", "--> before-->" + App.userPreferences.userId)

                FCMHandler.disableFCM()
                startNewActivity()
            }
        }
        binding.deleteContainer.setOnClickListener {
            showDeleteProfile()
        }


//        countrySelectionDialog()
    }

//    private fun countrySelectionDialog() {
//        val language =getMainActivity()?.pref?.getString("language","en")
//        binding.ccp.setDefaultCountryUsingNameCode(language)
//        binding.ccp.resetToDefaultCountry()
//        binding.ccp.setOnCountryChangeListener(object : CountryCodePicker.OnCountryChangeListener {
//            override fun onCountrySelected() {
//                Toast.makeText(
//                    requireContext(),
//                    "Updated " + binding.ccp.selectedCountryNameCode,
//                    Toast.LENGTH_SHORT
//                ).show();
//                getMainActivity()?.pref?.edit()?.putString("language", binding.ccp.selectedCountryNameCode)?.apply()
//
//                requireActivity().recreate()
////                val config = resources.configuration
////                val lang = binding.ccp.selectedCountryNameCode // your language code
////                val locale = Locale(lang)
////                Locale.setDefault(locale)
////                config.setLocale(locale)
////
////                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
////                    requireActivity().createConfigurationContext(config)
////                resources.updateConfiguration(config, resources.displayMetrics)
//
//            }
//        })
//    }

    private fun showDeleteProfile() {
        requireContext().showAlertDialog(
            getString(R.string.yes),
            getString(R.string.delete_account),
            getString(R.string.are_you_sure),
            object :
                AlertDialogCallback {
                override fun onNegativeButtonClick(dialog: DialogInterface) {
                    dialog.dismiss()
                }

                override fun onPositiveButtonClick(dialog: DialogInterface) {
                    showProgressView()
                    deleteAccount()
                }
            })
    }

    private fun deleteAccount() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.logOut(userId = userId, token = userToken) {

                lifecycleScope.launch(Dispatchers.Main) {
                    when (val response = viewModel.deleteProfile(userId, token = userToken)) {
                        is Resource.Success -> {
                            hideProgressView()
                            startNewActivity()
                        }
                        is Resource.Error -> {
                            hideProgressView()
                            Timber.e("${response.message}")
                            binding.root.snackbar("${response.message}")
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun getLanguages() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.getLanguages()
            val response = viewModel.getLanguages()
            if (response.body()?.defaultPickers?.languages?.isNotEmpty() == true){
                Timber.tag("csvccvs").e(response.body()?.defaultPickers?.languages?.size.toString())
                }
                else {


                }


            }
    }


    private fun startNewActivity() {
        lifecycleScope.launch(Dispatchers.Main) {
            userPreferences.clear()
            //App.userPreferences.saveUserIdToken("","","")
            val intent = Intent(requireActivity(), SplashActivity::class.java)
            requireActivity().startActivity(intent)
            requireActivity().finishAffinity()
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
        //if (position > 0) {
            val selectedlanguage = fetchLanguages()[position].supportedLangCode
            val selectedlanguageid = fetchLanguages()[position].id
            if (getMainActivity()?.pref?.getString("language", "en").equals(selectedlanguage)) {

//                lifecycleScope.launch(Dispatchers.Main) {
//                    viewModel.updateLanguage(languageCode = selectedlanguage, userid = userId, token = userToken)
//                }

            } else {

                sharedPref.setLanguage(true)
                sharedPref.setLanguageFromSettings(true)

                lifecycleScope.launch(Dispatchers.Main) {
                    getMainActivity()?.pref?.edit()?.putString("language", selectedlanguage)
                        ?.apply()
                    async(Dispatchers.IO) {
//                            result?.list?.let { userRepository.insertDropDownData(it) }
                        viewModel.updateLanguage(
                            languageCode = if (selectedlanguage == "pt") "pt_pt" else selectedlanguage,
                            userid = userId,
                            token = userToken
                        )
                        viewModel.updateLanguageId(
                            languageCode = selectedlanguageid,
                            userid = userId,
                            token = userToken
                        )
                        Log.d("languge", "langauage")
                    }.await()

                    //    toast("Language set up")

                    //   childFragmentManager.beginTransaction().detach(this@SettingsFragment).attach(this@SettingsFragment).commit()
                    /*       TempConstants.LanguageChanged = true
                           TempConstants.isFromSettings = true*/
                    //  (activity as MainActivity).refreshActivity()
                    /*            val intent = Intent(requireActivity(), ProgressBarActivity::class.java)
                                startActivity(intent)*/
                    //updateLanguageLocally(selectedlanguage)
                    triggerRebirth(requireContext())
//                    try {
////                        requireActivity().recreate()
//
//                    }catch (e:Exception){
//
//                    }

                }
//                requireActivity().recreate()
//                Toast.makeText(requireContext(),selectedlanguage  ,Toast.LENGTH_LONG).show()
            }


        //}


    }


//    fun updateLanguageLocally(language : String){
//        val locale = Locale(language)
//        Locale.setDefault(locale)
//        val config = resources.configuration
//        config.setLocale(locale)
//        requireActivity().createConfigurationContext(config)
//        resources.updateConfiguration(config,resources.displayMetrics)
//     //   toast("onConfiguration called")
//        binding.tvBuyCoinsContainer.text = getString(R.string.settings_buy)
//        binding.toolBarTitle.text = getString(R.string.settings)
//        binding.tvBlockedaccounts.text = getString(R.string.blocked_accounts)
//        binding.tvPrivacy.text = getString(R.string.privacy)
//        binding.tvTermsandconditions.text = getString(R.string.terms_and_conditions)
//        binding.tvlogout.text = getString(R.string.settings_logout)
//        binding.tvDeleteaccount.text = getString(R.string.delete_account)
//    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    fun userSubScription() {

//        showProgressView()
        lifecycleScope.launchWhenResumed {

            val userToken = getCurrentUserToken()!!
            val response = try {
                apolloClient(requireContext(), userToken).query(
                    UserSubscriptionQuery()
                ).execute()

            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                hideProgressView()
                return@launchWhenResumed
            }


            if (response.hasErrors()) {
                hideProgressView()
                val errorMessage = response.errors?.get(0)?.message
                Log.e("errorAllPackage", "$errorMessage")

                if (errorMessage != null) {
                    binding.root.snackbar(errorMessage)
                }
            } else {


                Log.e(
                    "userCurentSubScription",
                    Gson().toJson(response.data)
                )
//                hideProgressView()

//                if(response.data!!.userSubscription!!.`package`!=null){
//                    binding.tvSubScription.text =  requireContext().resources.getString(com.i69app.R.string.your_subscription).plus(response.data!!.userSubscription!!.`package`!!.name).plus("(").plus(response.data!!.userSubscription!!.plan!!.title).plus(")")
//
////    your_subscription
//                }
            }

        }


    }

    fun isDeleteAccountAllowed() {

        lifecycleScope.launchWhenResumed {

            val userToken = getCurrentUserToken()!!
            val response = try {
                apolloClient(requireContext(), userToken).query(
                    DeleteAccountAllowedQuery()

                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
//                binding.root.snackbar("Exception Stripe Publish key${e.message}")
                hideProgressView()
                return@launchWhenResumed
            }


            if (response.hasErrors()) {
                val errorMessage = response.errors?.get(0)?.message
                Log.e("isDeleteAccountVi", "$errorMessage")

                if (errorMessage != null) {
                    binding.root.snackbar(errorMessage)
                }
            } else {

                Log.e("isDeleteAccountVisi", Gson().toJson(response))

//
                lifecycleScope.launch(Dispatchers.Main) {

                    if (!response.data!!.deleteAccountAllowed.isNullOrEmpty() && response.data!!.deleteAccountAllowed!!.get(
                            0
                        )?.isDeleteAccountAllowed == true
                    ) {
                        binding.logoutDivider.visibility = View.VISIBLE
                        binding.deleteContainer.visibility = View.VISIBLE
                    } else {
                        binding.logoutDivider.visibility = View.GONE
                        binding.deleteContainer.visibility = View.GONE
                    }

                }


            }

        }

    }


}