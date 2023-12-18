package com.i69.ui.screens.main.search

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.exception.ApolloException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.i69.*
import com.i69.R
import com.i69.applocalization.AppStringConstant1
import dagger.hilt.android.AndroidEntryPoint
import com.i69.data.enums.InterestedInGender
import com.i69.type.UserInterestedInCategoryName
import com.i69.ui.adapters.SearchInterestedServerAdapter
import com.i69.ui.base.search.BaseSearchFragment
import com.i69.ui.interfaces.CallInterestedIn
import com.i69.ui.screens.SplashActivity
import com.i69.ui.screens.main.MainActivity
import com.i69.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SearchInterestedInFragment : BaseSearchFragment(), CallInterestedIn {

    companion object {
        fun setShowAnim(show: Boolean) {
            showAnim = show
        }
    }

    private var userId: String? = null
    private var userToken: String? = null

    private val locPermissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun setupChiledTheme() {
        Log.e("callInterestedInAPi", "callInterestedInAPi")
        showProgressView()
        lifecycleScope.launch {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
        }

        adapter.setItems(viewModel.listItemsFromViewModel)
        adapter.notifyDataSetChanged()
        hideProgressView()
        callInterestedInAPi()
        updateLocation()
        subscribeForUserUpdate()

//        adapter.setItems(getItems())


    }

    override fun setScreenTitle() {
        binding.title.text =   AppStringConstant1.interested_in

        binding.title.setOnClickListener {
            getMainActivity().updateLanguageChanged()
            lifecycleScope.launch() {
                delay(5000)
                activity?.recreate()
            }
        }
//        binding.title.text = getString(R.string.interested_in)
//        binding.title.text = getString(R.string.interested_in)


    }

    override fun callInterestedInApi() {
        callInterestedInAPi()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toast("onConfigurationChanged")
    }

    override fun initDrawerStatus() {
        try {
            getMainActivity().enableNavigationDrawer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun subscribeForUserUpdate() {


        lifecycleScope.launch(Dispatchers.IO) {


            try {
                apolloClientSubscription(requireActivity(), getCurrentUserToken()!!).subscription(
                    HideInterestedInSubscription()
                ).toFlow().catch {
                    it.printStackTrace()
                    Timber.d("reealltime exception= ${it.message}")
                    binding.root.snackbar("Exception= ${it.message}")
                }.retryWhen { cause, attempt ->
                        Timber.d("reealltime retry $attempt ${cause.message}")

                        delay(attempt * 1000)
                        true
                    }.collect { newStory ->
                        if (newStory.hasErrors()) {

                            Timber.d("reealltime response error = ${newStory.errors?.get(0)?.message}")
                        } else {
                            //   Timber.d("reealltime onNewMessage ${newMessage.data?.onNewMessage?.message?.timestamp}")
                            Log.e(
                                "followUserSubscript",
                                "story realtime DeleteStory ${newStory.data}"

                            )

                            callInterestedInAPi()

                        }
                    }


            } catch (e2: Exception) {
                e2.printStackTrace()
                Log.d("UserMomentSubsc", "story realtime exception= ${e2.message}")
                Timber.d("reealltime exception= ${e2.message}")
            }
        }
    }


    fun callInterestedInAPi() {

        lifecycleScope.launch {
            val userToken = getCurrentUserToken()!!

            val response = try {
                apolloClient(requireContext(), userToken).query(
                    UserInterestsQuery()
                ).execute()

            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                hideProgressView()
                return@launch
            }


            if (response.hasErrors()) {
                hideProgressView()
                val errorMessage = response.errors?.get(0)?.message
                Log.e("errorInInterest", "$errorMessage")

                if (errorMessage != null) {
                    binding.root.snackbar(errorMessage)
                }
            } else {
                var listItems = mutableListOf<SearchInterestedServerAdapter.MenuItemString>()

                Log.e("ggettedResponse", Gson().toJson(response.data))
                response.data!!.userInterests!!.indices.forEach { i ->
                    if (response.data!!.userInterests!!.get(i)!!.categoryName == UserInterestedInCategoryName.SERIOUS_RELATIONSHIP) {
                        listItems.add(
                            SearchInterestedServerAdapter.MenuItemString(
                                response.data!!.userInterests!!.get(i)!!.strName.toString(),
                                R.drawable.ic_relationship
                            )
                        )
                    } else if (response.data!!.userInterests!!.get(i)!!.categoryName == UserInterestedInCategoryName.CAUSAL_DATING) {
                        listItems.add(
                            SearchInterestedServerAdapter.MenuItemString(
                                response.data!!.userInterests!!.get(i)!!.strName.toString(),
                                R.drawable.ic_causal_menu_dating
                            )
                        )
                    } else if (response.data!!.userInterests!!.get(i)!!.categoryName == UserInterestedInCategoryName.NEW_FRIENDS) {
                        listItems.add(
                            SearchInterestedServerAdapter.MenuItemString(
                                response.data!!.userInterests!!.get(i)!!.strName.toString(),
                                R.drawable.ic_new_friend_unchecked
                            )
                        )
                    } else if (response.data!!.userInterests!!.get(i)!!.categoryName == UserInterestedInCategoryName.ROOM_MATES) {
                        listItems.add(
                            SearchInterestedServerAdapter.MenuItemString(
                                response.data!!.userInterests!!.get(i)!!.strName.toString(),
                                R.drawable.ic_roommates_unchecked
                            )
                        )
                    } else if (response.data!!.userInterests!!.get(i)!!.categoryName == UserInterestedInCategoryName.BUSINESS_CONTACTS) {
                        listItems.add(
                            SearchInterestedServerAdapter.MenuItemString(
                                response.data!!.userInterests!!.get(i)!!.strName.toString(),
                                R.drawable.ic_business_contacts_unchecked
                            )
                        )
                    }
//                    SearchInterestedAdapter.MenuItem(R.string.serious_relationship, R.drawable.ic_relationship),
//                    SearchInterestedAdapter.MenuItem(R.string.casual_dating, R.drawable.ic_causal_menu_dating),
//                    SearchInterestedAdapter.MenuItem(R.string.new_friends, R.drawable.ic_new_friend_unchecked),
//                    SearchInterestedAdapter.MenuItem(
//                        R.string.roommates_2_lines,
//                        R.drawable.ic_roommates_unchecked
//                    ),
//                    SearchInterestedAdapter.MenuItem(
//                        R.string.business_contacts,
//                        R.drawable.ic_business_contacts_unchecked
//                    )


                }

                if(viewModel.listItemsFromViewModel.size != listItems.size )
                {
                    viewModel.listItemsFromViewModel = listItems
                    adapter.setItems(listItems)
                    adapter.notifyDataSetChanged()
                }





            }
        }
    }


    private fun updateLocation() {
        if (hasLocationPermission(requireContext(), locPermissions)) {
            showProgressView()
            if (isLocationEnabled()){
                shareLocation()
            }else{
                enableLocation()
            }
        } else {
            (requireActivity() as MainActivity).permissionReqLauncher.launch(locPermissions)
//            locationPermissionResponse.launch(locPermissions)
        }
    }

    private fun shareLocation(){
        val locationService = LocationServices.getFusedLocationProviderClient(requireContext())
        locationService.lastLocation.addOnSuccessListener { location: Location? ->
            hideProgressView()
            val lat: Double? = location?.latitude
            val lon: Double? = location?.longitude
//                toast("lat = $lat lng = $lon")
            if (lat != null && lon != null) {
                // Update Location
                lifecycleScope.launch(Dispatchers.Main) {
                    var res = mViewModel.updateLocation(
                        userId = userId!!,
                        location = arrayOf(lat, lon),
                        token = userToken!!
                    )

                    if (res.message.equals("User doesn't exist")) {
                        //error("User doesn't exist")
                        lifecycleScope.launch(Dispatchers.Main) {
                            userPreferences.clear()
                            //App.userPreferences.saveUserIdToken("","","")
                            val intent = Intent(requireContext(), SplashActivity::class.java)
                            startActivity(intent)
                            requireActivity().finishAffinity()
                        }
                    }else{
                        hideProgressView()
                      //  callInterestedInAPi()
                    }
                    //  Log.e("aaaaaaaa",""+res.data!!.errorMessage)
                    Log.e("aaaaaaaa", "" + res.message)

                }
            }else{
                shareLocation()
            }
        }.addOnFailureListener {
            shareLocation()
            //hideProgressView()
            Log.d("ProgressError", "Location fetch failed")
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        return locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun enableLocation() {
        val locationRequest = LocationRequest.create()
        locationRequest.apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 30 * 1000.toLong()
            fastestInterval = 5 * 1000.toLong()
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result =
            LocationServices.getSettingsClient(requireContext())
                .checkLocationSettings(builder.build())
        result.addOnCompleteListener {
            try {
                val response: LocationSettingsResponse = it.getResult(ApiException::class.java)
                println("location>>>>>>> ${response.locationSettingsStates.isGpsPresent}")
                if (response.locationSettingsStates.isGpsPresent) {
                    showProgressView()
                    shareLocation()
                }
                //do something
            } catch (e: ApiException) {
                when (e.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val intentSenderRequest =
                            e.status.resolution?.let { it1 ->
                                IntentSenderRequest.Builder(it1).build()
                            }
                        launcher.launch(intentSenderRequest)
                    } catch (e: IntentSender.SendIntentException) {
                    }
                }
            }
        }.addOnCanceledListener {

        }
    }

    private var launcher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("okayDialog", "OK")
//                shareCurrentLocation()
                showProgressView()
                shareLocation()
            } else {
                Log.d("CancelDialog", "CANCEL")
                binding.root.snackbar(
                    AppStringConstant1.location_enable_message,
//                    getString(R.string.location_enable_message),
                    Snackbar.LENGTH_INDEFINITE,
                    callback = {
                        enableLocation()
//                        context?.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    })
//            requireContext().toast("Please Accept Location enable for use this App.")
            }
        }

    override fun getItems(): List<SearchInterestedServerAdapter.MenuItemString> = listOf(
//        SearchInterestedAdapter.MenuItem(R.string.serious_relationship, R.drawable.ic_relationship),
//        SearchInterestedAdapter.MenuItem(R.string.casual_dating, R.drawable.ic_causal_menu_dating),
//        SearchInterestedAdapter.MenuItem(R.string.new_friends, R.drawable.ic_new_friend_unchecked),
//        SearchInterestedAdapter.MenuItem(
//            R.string.roommates_2_lines,
//            R.drawable.ic_roommates_unchecked
//        ),
//        SearchInterestedAdapter.MenuItem(
//            R.string.business_contacts,
//            R.drawable.ic_business_contacts_unchecked
//        )
    )

    override fun onAdapterItemClick(pos: Int) {
        val item = when (pos) {
            0 -> com.i69.data.enums.InterestedInGender.SERIOUS_RELATIONSHIP_ONLY_MALE
            1 -> com.i69.data.enums.InterestedInGender.CAUSAL_DATING_ONLY_MALE
            2 -> InterestedInGender.NEW_FRIENDS_ONLY_MALE
            3 -> com.i69.data.enums.InterestedInGender.ROOM_MATES_ONLY_MALE
            4 -> com.i69.data.enums.InterestedInGender.BUSINESS_CONTACTS_ONLY_MALE
            else -> com.i69.data.enums.InterestedInGender.SERIOUS_RELATIONSHIP_ONLY_MALE
        }
        navController.navigate(
            R.id.action_searchInterestedInFragment_to_searchGenderFragment,
            bundleOf("interested_in" to item)
        )
    }


    override fun onResume() {
        super.onResume()
        getMainActivity().setDrawerItemCheckedUnchecked(R.id.nav_search_graph)

    }
}
