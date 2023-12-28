package com.i69.ui.screens.main.profile

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.i69.BuildConfig
import com.i69.R
import com.i69.applocalization.AppStringConstant
import com.i69.data.config.Constants.INTEREST_MOVIE
import com.i69.data.config.Constants.INTEREST_MUSIC
import com.i69.data.config.Constants.INTEREST_SPORT_TEAM
import com.i69.data.config.Constants.INTEREST_TV_SHOW
import com.i69.data.models.Photo
import com.i69.data.models.User
import com.i69.languages.LanguageAdapter
import com.i69.ui.base.profile.BaseEditProfileFragment
import com.i69.ui.base.profile.PUBLIC
import com.i69.ui.interfaces.AlertDialogCallback
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.viewModels.UserViewModel
import com.i69.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*

@AndroidEntryPoint
class UserEditProfileFragment : BaseEditProfileFragment(), AdapterView.OnItemSelectedListener {

    private val viewModel: UserViewModel by activityViewModels()

    var user: User? = null
    private var token: String? = null
    lateinit var sharedPref: SharedPref
    var Removed_Photos: ArrayList<Photo> = java.util.ArrayList()
    var isDeductCoins = false
    lateinit var stringConstant : AppStringConstant

//    override fun moveUp() = view?.findNavController()?.navigateUp()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.languagesLayout.setOnClickListener {
            changeLanguageDialog()
        }

        binding.edAbout.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.tvAboutTitleCount.text = "${s?.length}/250"
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

    }

    override fun setupScreen() {
        viewStringConstModel.data.observe(this@UserEditProfileFragment) { data ->
            try {
                binding.stringConstant = data
                stringConstant = data
            }catch (e : IOException){
                Log.wtf("IOException", e.toString())
            }
        }
        viewStringConstModel.data.also {
            binding.stringConstant = it.value
            stringConstant = it.value?: AppStringConstant(requireContext())
        }
        TempConstants.LanguageChanged = false
        sharedPref = SharedPref(requireContext())
        sharedPref.setLanguage(false)
        sharedPref.setLanguageFromSettings(false)
        binding.titleLabel.text = stringConstant.profile_edit_title
//            getString(R.string.profile_edit_title)
        Removed_Photos = ArrayList()
        lifecycleScope.launch(Dispatchers.Main) {
            token = getCurrentUserToken()!!
            val method = com.i69.data.enums.DeductCoinMethod.PROFILE_PICTURE.name

            viewModel.getCoinSettingsByRegion(token!!,method).observe(viewLifecycleOwner) {
                it?.let { coinSettings ->
                    profilePictureAmt =
                        it.coinsNeeded
//                    coinSettings.forEach { coinSetting ->
//                        if (coinSetting.method == com.i69app.data.enums.DeductCoinMethod.PROFILE_PICTURE.name) {
//
                            Log.d("coinSetting",it.coinsNeeded.toString())
//                        }
//                    }
                }
            }
            viewModel.getCoinSettings(token!!).observe(viewLifecycleOwner) {
                it?.let { coinSettings ->
                    coinSettings.forEach { coinSetting ->
                        if (coinSetting.method == com.i69.data.enums.DeductCoinMethod.PROFILE_PICTURE.name) {
                            if(profilePictureAmt==0 || profilePictureAmt == null)
                            profilePictureAmt =
                                coinSetting.coinsNeeded
                            Log.d("coinSetting",coinSetting.coinsNeeded.toString())
                        }
                    }
                }
            }
            Log.d("DefaultPicker", "Token Value $token")
            viewModel.getDefaultPickers(token!!).observe(viewLifecycleOwner) {
                it?.let { defaultPickerValue ->
                    defaultPicker = defaultPickerValue
                    Log.d(
                        "DefaultPicker",
                        "FamilyPicker  ${defaultPickerValue.familyPicker} \n Ethnicity ${defaultPickerValue.ethnicityPicker}"
                    )
                    binding.defaultPicker = defaultPicker!!
                }
            }

            Log.d("BaseEditProfileFragment", "userid ${getCurrentUserId()} token $token")
            viewModel.getCurrentUser(getCurrentUserId()!!, token = token!!, true)
                .observe(viewLifecycleOwner) { userDetails ->
                    userDetails?.let {
                        if(user==null)
                            setupUserLookingFor(it)
                        user = it
                        binding.user = user!!
                        prefillEditProfile(user!!)
                        setLanguage()
                        Log.e("cddfvf",user?.gender.toString())
                       /* binding.genderPicker.setSelection(it.gender!!)*/
                    }
                }
        }


    }

    fun setLanguage() {
        binding.languageSpinner.isEnabled = false;
        binding.languageSpinner.isClickable = false;

        val allLangs = fetchLanguages()
        lifecycleScope.launch(Dispatchers.Main) {
            val adapter = LanguageAdapter(requireContext(), allLangs)
            binding.languageSpinner.adapter = adapter
            binding.languageSpinner.onItemSelectedListener = this@UserEditProfileFragment
            val sl = MainActivity.getMainActivity()?.pref?.getString("language", "en")
            for (i in 0 until allLangs.size) {
                if (allLangs[i].supportedLangCode.equals(sl)) {
                    binding.languageSpinner.setSelection(i)
                }
            }
        }
    }

    override fun getInterestedInValues(interestsType: Int): List<String> = when (interestsType) {
        INTEREST_MUSIC -> if (user?.music.isNullOrEmpty()) emptyList() else user?.music!!
        INTEREST_MOVIE -> if (user?.movies.isNullOrEmpty()) emptyList() else user?.movies!!
        INTEREST_TV_SHOW -> if (user?.tvShows.isNullOrEmpty()) emptyList() else user?.tvShows!!
        INTEREST_SPORT_TEAM -> if (user?.sportsTeams.isNullOrEmpty()) emptyList() else user?.sportsTeams!!
        else -> if (user?.sportsTeams.isNullOrEmpty()) emptyList() else user?.sportsTeams!!
    }

    override fun setInterestedInToViewModel(interestType: Int, interestValue: List<String>) {
        when (interestType) {
            INTEREST_MUSIC -> user?.music = interestValue.toMutableList()
            INTEREST_MOVIE -> user?.movies = interestValue.toMutableList()
            INTEREST_TV_SHOW -> user?.tvShows = interestValue.toMutableList()
            INTEREST_SPORT_TEAM -> user?.sportsTeams = interestValue.toMutableList()
        }
    }

    override fun callparentmethod(pos: Int, photo_url: String) {
        Log.e("ssse1--", BuildConfig.BASE_URL)
        Log.e("ssse2--", photo_url)
        if (photo_url.replace("https", "https").startsWith(BuildConfig.BASE_URL)) {
            val previousPhotos = user!!.avatarPhotos!!

            var  i =0;
            previousPhotos.forEach { photo ->
             /*   Log.e("sss1--", photo.url!!)
                Log.e("sss2--", photo_url)*/
                if(photo.type.equals(PUBLIC)) {
                    i++
                }
            }


            val photos=   previousPhotos.get(pos)
            if(i==1  && photos!!.type.equals(PUBLIC)){
                binding.root.snackbar("Public photo can't be empty")
            }else{
                previousPhotos.forEach { photo ->
                  /*  Timber.tag("sss1--").e(photo.url)
                    Log.e("sss2--", photo_url)*/

                    if (photo.url?.replace(
                            "http://95.216.208.1:8000/media/",
                            "${BuildConfig.BASE_URL}media/"
                        ).equals(photo_url)
                    ) {
                        Removed_Photos.add(photo)
                        photosnewAdapter.removeItem(pos)
                    }
                }
            }
        }
    }

    override fun onDoneClick(increment: Boolean) {
        if (!isProfileValid()) return
        val previousPhotos = user!!.avatarPhotos!!
        user = getViewModelUser(user!!, increment = increment)
        val userData = getApiUser(user!!)
        userData.interestedIn = listOfInterestedIn
        Log.d("UserEditProfileFragment", "UserData $userData")
        Timber.e("USER: $userData")
        // if (photosAdapter.photos.size > userData.photosQuota) {
        if (photosnewAdapter.photos.size > userData.photosQuota) {
            showBuyDialog(photoQuota = userData.photosQuota, coinSpendAmt = profilePictureAmt)
            return
        }
        showProgressView()
        lifecycleScope.launch(Dispatchers.IO) {
            val newPhotosFilePath =
                photosnewAdapter.photos.filter { !it.link.startsWith(BuildConfig.BASE_URL) }
            //            photosAdapter.photos.filter { !it.startsWith(BuildConfig.BASE_URL) }
            //  val newUrlPhotos = photosAdapter.photos.filter {
            val newUrlPhotos = photosnewAdapter.photos.filter {
                it.link.startsWith(BuildConfig.BASE_URL)
            }.map {
                it.link
            }
            var needImageUpdate = false
            Log.e("previousPhotos", "" + previousPhotos)
            Log.e("newUrlPhotos", "" + newUrlPhotos)
            val toRemovePhotoIds = previousPhotos
                .filter { !newUrlPhotos.contains(it?.url) }
                .map { it.id }
            LogUtil.debug("New photos size : : ${newPhotosFilePath.size}")
            if (!newPhotosFilePath.isNullOrEmpty()) {
                needImageUpdate = true
                newPhotosFilePath.forEach { photo ->
                    Log.d("UserEditProfileFragment", "Photo $photo")
                    if (photo.link.contains("content")) {
                        val result = photo.link.toUri().path
                        val openInputStream =
                            requireActivity().contentResolver?.openInputStream(photo.link.toUri())
                        val type = if (result?.contains("video") == true) ".mp4" else ".jpg"
                        val outputFile =
                            requireContext().filesDir.resolve("${System.currentTimeMillis()}$type")
                        openInputStream?.copyTo(outputFile.outputStream())
                        val file = File(outputFile.toURI())
                        viewModel.uploadImage2(
                            userId = userData.id,
                            token!!,
                            file = file,
                            photo.type
                        )
                    } else {
                        viewModel.uploadImage(
                            userId = userData.id,
                            token!!,
                            filePath = photo.link,
                            photo.type
                        )
                    }
                }
            }

            Log.e("toRemovePhotoIds", "" + toRemovePhotoIds)
            if (!toRemovePhotoIds.isNullOrEmpty()) {
                needImageUpdate = true
                toRemovePhotoIds.forEach { photoId ->

                    Removed_Photos.forEach { photo ->

                        if (photo.id.equals(photoId)) {
                            Log.e("RemovePhotoType", "" + photo.type)

                            if(photo.type.equals(PUBLIC)){
                                val res1 =
                                    viewModel.deleteUserPublicPhotos(token = token!!, photoId = photoId)
                                Log.e("RemovePublicPhoto", "" + res1.data.toString())
                            }else {
                                val res1 =
                                    viewModel.deleteUserPhotos(token = token!!, photoId = photoId)
                                Log.e("RemovePrivatePhoto", "" + res1.data.toString())
                            }

                        }

                    }

                }
            }
            //    Log.e("avatarPhotosINDEX", "" + photosAdapter.avtar_index)

            withContext(Dispatchers.Main) {
                when (val response = viewModel.updateProfile(user = userData, token = token!!)) {
                    is Resource.Success -> {
                        viewModel.getCurrentUser(userId = user!!.id, token = token!!, true)
                        hideProgressView()
                        if (needImageUpdate) {
                            if (activity is MainActivity) {
                                (activity as MainActivity).loadUser()
                            }
                        }
                        if (!isDeductCoins) {
                            view?.findNavController()?.navigateUp()
                        } else {
                            isDeductCoins = false
                            showChooseImageDialog()
                        }
                    }
                    is Resource.Error -> {
                        hideProgressView()
                        Timber.e("${response.message}")
                        binding.root.snackbar("${response.message}")
                    }else -> {

                }
                }
            }
        }

    }

    override fun onRemoveBtnClick(ids: Int, s: String) {
        Log.e("ssss", "" + s + " - " + ids)
//        binding.root.snackbar("Clicked " + ids)
//        lifecycleScope.launch(Dispatchers.IO) {
//
//                    viewModel.deleteUserPhotos(token = token!!, photoId = ids.toString())
//
//        }
    }


    override fun showBuyDialog(photoQuota: Int, coinSpendAmt: Int) {
        Log.d("UserEditProfileFragment", "coinSpendAmt $coinSpendAmt user $user")
        requireContext().showAlertDialog(
//            positionBtnText = getString(R.string.yes),

            positionBtnText = stringConstant.yes,
            title = getString(R.string.app_name),
            subTitle = String.format(
                stringConstant.upload_image_warning,
//                getString(R.string.upload_image_warning),
                photoQuota,
                coinSpendAmt
            ),
            listener = object : AlertDialogCallback {
                override fun onNegativeButtonClick(dialog: DialogInterface) {
                    dialog.dismiss()
                }

                override fun onPositiveButtonClick(dialog: DialogInterface) {
                    dialog.dismiss()

                    val totalCoins = user!!.purchaseCoins + user!!.giftCoins

                    if (coinSpendAmt > totalCoins) {

                        findNavController().navigate(R.id.actionGoToPurchaseFragment)

                    } else {
                        lifecycleScope.launch(Dispatchers.Main) {
                            when (val response = viewModel.deductCoin(
                                userId = user!!.id,
                                token = token!!,
                                com.i69.data.enums.DeductCoinMethod.PROFILE_PICTURE
                            )) {
                                is Resource.Success -> {
                                    isDeductCoins = true
                                    onDoneClick(true)
                                }
//                                is Resource.Error -> binding.root.snackbar("${getString(R.string.something_went_wrong)} ${response.message}")
                                is Resource.Error -> binding.root.snackbar("${ stringConstant.something_went_wrong} ${response.message}")
                                else->{}
                            }
                        }
                    }
                }
            }
        )
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {

        //if (position > 0) {
        val selectedlanguage = fetchLanguages()[position].supportedLangCode
        val selectedlanguageid = fetchLanguages()[position].id
        Log.d("DefaultPickerLang",selectedlanguage)
        if (MainActivity.getMainActivity()?.pref?.getString("language", "en")
                .equals(selectedlanguage)
        ) {

        } else {

            sharedPref.setLanguage(true)
            sharedPref.setLanguageFromSettings(false)

            lifecycleScope.launch(Dispatchers.Main) {
                MainActivity.getMainActivity()?.pref?.edit()
                    ?.putString("language", selectedlanguage)
                    ?.apply()
                async(Dispatchers.IO) {
//                            result?.list?.let { userRepository.insertDropDownData(it) }
                    viewModel.updateLanguage(
                        languageCode = if (selectedlanguage == "pt") "pt_pt" else selectedlanguage,
                        userid = user?.id!!,
                        token = token!!
                    )
                    viewModel.updateLanguageId(
                        languageCode = selectedlanguageid,
                        userid = user?.id!!,
                        token = token!!
                    )
                    //     Log.d("DefaultPicker","langauage")
                    //    Log.d("DefaultPicker","Language message ${res.message} data ${res.data} code ${res.code}")
                }.await()

                //  updateLanguageLocally(selectedlanguage)
                /* TempConstants.LanguageChanged = true
                 TempConstants.isFromSettings = false*/
                //  (activity as MainActivity).refreshActivity()
                /*             val intent = Intent(requireActivity(),ProgressBarActivity::class.java)
                             startActivity(intent)*/
                //  requireActivity().finish()


                triggerRebirth(requireContext())


//                    try {
////                        requireActivity().recreate()
//
//                    }catch (e:Exception){
//
//                    }

            }

        }

        //}

    }

    override fun onNothingSelected(p0: AdapterView<*>?) {

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
            binding.languageSpinner.performClick()
        }

        dialog.show()

    }
}