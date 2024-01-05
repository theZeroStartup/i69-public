package com.i69.ui.screens.main.search

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.cachapa.expandablelayout.ExpandableLayout
import com.i69.R
import com.i69.applocalization.AppStringConstant
import com.i69.applocalization.AppStringConstant1
import com.i69.applocalization.AppStringConstantViewModel
import com.i69.data.enums.InterestedInGender
import com.i69.data.remote.requests.SearchRequest
import com.i69.databinding.FragmentSearchFiltersBinding
import com.i69.permissions.PermissionHandler
import com.i69.permissions.Permissions
import com.i69.ui.base.BaseFragment
import com.i69.ui.viewModels.SearchViewModel
import com.i69.ui.views.ToggleImageView
import com.i69.utils.isCurrentLanguageFrench
import com.i69.utils.snackbar
import java.util.*

@AndroidEntryPoint
class SearchFiltersFragment : BaseFragment<FragmentSearchFiltersBinding>() {

    private val mViewModel: SearchViewModel by activityViewModels()
    private var userId: String? = null
    private var userToken: String? = null
    private val viewStringConstModel: AppStringConstantViewModel by activityViewModels()

    override fun getFragmentBinding(inflater: LayoutInflater, container : ViewGroup?) =
        FragmentSearchFiltersBinding.inflate(inflater, container, false).apply {
            stringConstant = AppStringConstant(requireContext())
        }

    override fun setupTheme() {
        viewStringConstModel.data.observe(this@SearchFiltersFragment) { data ->

            binding.stringConstant = data

        }
        viewStringConstModel.data?.also {
            binding.stringConstant = it.value
//            Log.e("MydataBasesss", it.value!!.messages)
        }
        navController = findNavController()
        val interestedIngender = arguments?.getSerializable("interested_in_gender") as InterestedInGender
        val interestedIn = arguments?.getSerializable("interested_in") as InterestedInGender
        mViewModel.interestedIn.value = interestedIn
        mViewModel.interestedIngender.value = interestedIngender
        binding.model = mViewModel
        val lookingFor = AppStringConstant1.looking_for
//        val lookingFor = getString(R.string.looking_for)

        lifecycleScope.launch {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!

            mViewModel.getDefaultPickers(userToken!!).observe(viewLifecycleOwner) {
                it?.let { defaultPicker ->
                    mViewModel.updateDefaultPicker(lookingFor, defaultPicker)
                    val agePicker = defaultPicker.agePicker

                    Log.e("MYrangePickerDataValues", "${agePicker[0].value.toFloat()}")
                    Log.e(
                        "MYrangePickerDataValues1",
                        "${agePicker[agePicker.size - 1].value.toFloat()}"
                    )
                    lifecycleScope.launchWhenResumed {

                        updateTags()
                        binding.ageRangeSeekBar.setRange(
                            agePicker[0].value.toFloat(),
                            agePicker[agePicker.size - 1].value.toFloat()
                        )
                    }
                }
            }
        }

        updateTags()

        binding.tagsBtn.setOnChipClickListener { tag, position ->
            if (mViewModel.tags.size - 1 >= position) {
                mViewModel.tags.removeAt(position)
                updateTags()
            }
        }

//        binding.personalLayoutItem.tagsBtn.setOnChipClickListener { tag, position ->
//            if (mViewModel.tags.size - 1 >= position) {
//                mViewModel.tags.removeAt(position)
//                updateTags()
//            }
//        }

        mViewModel.btnTagsAddListener = View.OnClickListener {
            navController.navigate(R.id.action_searchFiltersFragment_to_selectTagsFragment)
        }


        binding.tvSkip.setOnClickListener {

            showProgressView()
            val searchKey: String = ""
            var bundle = Bundle()
            bundle.putInt("interestedIn", interestedIn.id)
            bundle.putString("searchKey", searchKey)
            bundle.putBoolean("hasSkip", true)

            val searchRequest = SearchRequest(
                interestedIn = interestedIn.id,
                id = userId!!,
                searchKey = searchKey

            )
            Log.d("ExtraSearchCalls", "setupTheme: ")
            mViewModel.getSearchUsers(
                _searchRequest = searchRequest,
                token = userToken!!,
                context = requireContext(),
                hasSkip = true,
            ) { error ->
                if (error == null) {
                    hideProgressView()
//                    var bundle = Bundle()
//                    bundle.putInt("interestedIn", interestedIn.id)
//                    bundle.putString("searchKey", searchKey)

                    navController.navigate(
                        R.id.action_searchFiltersFragment_to_searchResultFragment,
                        args = bundle
                    )
                } else {
                    hideProgressView()
                    binding.root.snackbar(error)
                }
            }
        }

        mViewModel.searchBtnClickListener = View.OnClickListener {
            showProgressView()
//            if (mViewModel.maxDistanceValue.get().roundToInt() == 0) {
            if (!isLocationEnabled()) {
                val searchKey: String = ""
                val searchRequest = SearchRequest(
                    interestedIn = interestedIn.id,
                    id = userId!!,
                    searchKey = searchKey,
                )
                Log.d("ExtraSearchCalls", "setupTheme: 2")
                mViewModel.getSearchUsers(
                    _searchRequest = searchRequest,
                    token = userToken!!,
                    context = requireContext(),
                ) { error ->
                    if (error == null) {

//                    setupViewPagerData()
                        hideProgressView()

                        var bundle = Bundle()
                        bundle.putInt("interestedIn", interestedIn.id)
                        bundle.putString("searchKey", searchKey)

                        navController.navigate(
                            R.id.action_searchFiltersFragment_to_searchResultFragment,
                            args = bundle
                        )
//                    navController.navigate(R.id.action_searchFiltersFragment_to_searchResultFragment)
                    } else {
//                    hideProgressView()
//                    binding.root.snackbar(error)
                    }
                }
            }else {

                Permissions.check(
                    requireActivity(),
                    permission.ACCESS_COARSE_LOCATION,
                    null,
                    object : PermissionHandler() {
                        @SuppressLint("MissingPermission")
                        override fun onGranted() {
                            val locationService =
                                LocationServices.getFusedLocationProviderClient(activity!!)

                            locationService.lastLocation.addOnSuccessListener { location: Location? ->
//                            val searchKey: String = binding.keyInput.text.toString()
                                val searchKey: String = ""
                                var lat: Double? = null
                                var lon: Double? = null
                                if (searchKey.isEmpty()) {
                                    lat = location?.latitude
                                    lon = location?.longitude
                                }
                                val searchRequest = SearchRequest(
                                    interestedIn = interestedIn.id,
                                    id = userId!!,
                                    searchKey = searchKey,
                                    lat = lat,
                                    long = lon
                                )
                                Log.e("search params", Gson().toJson(searchRequest))
                                Log.d("ExtraSearchCalls", "onGranted: 2 $searchKey")
                                mViewModel.getSearchUsers(
                                    _searchRequest = searchRequest,
                                    token = userToken!!,
                                    context = requireContext(),
                                ) { error ->
                                    if (error == null) {
                                        hideProgressView()
                                        var bundle = Bundle()
                                        bundle.putInt("interestedIn", interestedIn.id)
                                        bundle.putString("searchKey", searchKey)

                                        mViewModel.clearDataOnStop()
                                        navController.navigate(
                                            R.id.action_searchFiltersFragment_to_searchResultFragment,
                                            args = bundle
                                        )
                                    } else {
                                        hideProgressView()
                                        binding.root.snackbar(error)
                                    }
                                }
                            }
                        }

                        override fun onDenied(
                            context: Context?,
                            deniedPermissions: ArrayList<String>?
                        ) {
                            binding.root.snackbar(getString(R.string.search_permission))
                            hideProgressView()
                        }
                    })
            }
        }
//        initGroups()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        return locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    override fun setupClickListeners() {}

    private fun updateTags() {
        Log.e("TagaddedinList","${mViewModel.tags.size}" )
        binding.tagsBtn.setInterests(mViewModel.tags.map { if (isCurrentLanguageFrench()) it.valueFr else it.value })
    }
//    private fun updateTags() {
//        binding.personalLayoutItem.tagsBtn.setInterests(mViewModel.tags.map { if (isCurrentLanguageFrench()) it.valueFr else it.value })
//    }

//    private fun initGroups() {
//        initExpandableLayout(binding.groupsExpand, binding.toggleGroupsExpand, binding.groups)
//        initExpandableLayout(binding.personalExpand, binding.togglePersonalExpand, binding.personal)
//    }

    private fun initExpandableLayout(
        button: View,
        toggleImageView: ToggleImageView,
        expandableLayout: ExpandableLayout
    ) {
        toggleImageView.onCheckedChangeListener =
            CompoundButton.OnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    expandableLayout.expand(false)
                } else {
                    expandableLayout.collapse()
                }
            }

        button.setOnClickListener {
            toggleImageView.toggle()
        }
    }

}