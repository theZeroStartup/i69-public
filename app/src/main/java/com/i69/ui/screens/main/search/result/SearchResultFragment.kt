package com.i69.ui.screens.main.search.result

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.apollographql.apollo3.exception.ApolloException
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.i69.GetNotificationCountQuery
import dagger.hilt.android.AndroidEntryPoint
import com.i69.R
import com.i69.applocalization.AppStringConstant
import com.i69.applocalization.AppStringConstant1
import com.i69.applocalization.AppStringConstantViewModel
import com.i69.data.remote.requests.SearchRequest
import com.i69.databinding.FragmentSearchResultBinding
import com.i69.ui.adapters.PageResultAdapter
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.screens.main.notification.NotificationDialogFragment
import com.i69.ui.viewModels.SearchViewModel
import com.i69.utils.*
import kotlinx.coroutines.launch
import timber.log.Timber


@AndroidEntryPoint
class SearchResultFragment : BaseFragment<FragmentSearchResultBinding>() {
    private var userToken: String? = null
    private var userId: String? = null
    private var hasSkip: Boolean = false
    private val mViewModel: SearchViewModel by activityViewModels()
    private val viewStringConstModel: AppStringConstantViewModel by activityViewModels()


    override fun getStatusBarColor() = R.color.toolbar_search_color

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSearchResultBinding.inflate(inflater, container, false).apply {
            stringConstant = AppStringConstant(requireContext())
        }

    override fun setupTheme() {
        Log.e("callSetupScreen", "callSetupScreen")
        viewStringConstModel.data.observe(this@SearchResultFragment) { data ->

            binding.stringConstant = data

        }
        viewStringConstModel.data.also {
            binding.stringConstant = it.value
    //            Log.e("MydataBasesss", it.value!!.messages)
        }

        lifecycleScope.launch {
            userToken = getCurrentUserToken()!!
            userId = getCurrentUserId()!!
            if (requireArguments().containsKey("hasSkip")) {
                hasSkip = requireArguments().getBoolean("hasSkip", false)
            }

            Log.e("hasSkipgetted", "$hasSkip")
            Timber.i("usertokenn $userToken")
        }

        initSearch()

        mViewModel.getupdateSearchResultWithCoin()?.observe(viewLifecycleOwner) {
            Log.e("getUpdateListQuery", "getUpdateListQuery")
            callSearchPopularUserQuery()
            callSearchMostActiveUserQuery()
        }
    }

    override fun onStart() {
        super.onStart()

        callSearchRandomPeopleQuery()
//        callSearchPopularUserQuery()
//        callSearchMostActiveUserQuery()
    }

    fun setupViewPagerData() {

//        val tabs = arrayOf(
//            getString(R.string.random),
//            getString(R.string.popular),
//            getString(R.string.most_active)
//        )


        val tabs = arrayOf(
            AppStringConstant1.random,
            AppStringConstant1.popular,
            AppStringConstant1.most_active
        )
        val pagerAdapter = PageResultAdapter(this, tabs)
        binding.searchPageViewPager.apply {
            adapter = pagerAdapter
            offscreenPageLimit = 3
        }

        binding.searchPageViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback()
        {

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
               // pagerAdapter.listFrags.get(position).usersAdapter.notifyDataSetChanged()
            }
        })

        val tabConfigurationStrategy =
            TabLayoutMediator.TabConfigurationStrategy { tab: TabLayout.Tab, pos: Int ->
                tab.text = tabs[pos]
            }
        TabLayoutMediator(
            binding.slidingTabs,
            binding.searchPageViewPager,
            true,
            tabConfigurationStrategy
        ).attach()

    }


    private fun isLocationEnabled(): Boolean {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        return locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }


    fun callSearchRandomPeopleQuery() {
        showProgressView()
        val interestedIn = requireArguments().getInt("interestedIn")
        val searchKey = requireArguments().getString("searchKey")
        Log.e("interestedInRandom ", "$interestedIn")
        Log.e("searchKey ", "$searchKey")

        val searchRequest = SearchRequest(
            interestedIn = interestedIn,
            id = userId!!,
            searchKey = searchKey,
        )

//        if (mViewModel.maxDistanceValue.get().roundToInt() == 0) {
        if (!isLocationEnabled()) {


        } else {

            val locationService =
                LocationServices.getFusedLocationProviderClient(requireActivity())

            locationService.lastLocation.addOnSuccessListener { location: Location? ->
//                            val searchKey: String = binding.keyInput.text.toString()

                var lat: Double? = null
                var lon: Double? = null
                if (searchKey!!.isEmpty()) {
                    lat = location?.latitude
                    lon = location?.longitude
                }
                var searchRequest = SearchRequest(
                    interestedIn = interestedIn,
                    id = userId!!,
                    searchKey = searchKey,
                    lat = lat,
                    long = lon
                )

            }
        }

        Log.e("search params", Gson().toJson(searchRequest))
        Log.d("ExtraSearchCalls", "callSearchRandomPeopleQuery: ")
        mViewModel.getSearchUsers(
            _searchRequest = searchRequest,
            token = userToken!!,
            context = requireContext(),
            hasSkip = hasSkip,
        ) { error ->
            hideProgressView()
            setupViewPagerData()
            if (error == null) {
                Log.e("calllUpdateQuery", "calllUpdateQuery")
                mViewModel.setUpdateUserListQuery("")
                Log.e("MyRandomUserDatra", "" + mViewModel.getRandomUsers().toString())
//                    setupViewPagerData()
//                    hideProgressView()
//                    navController.navigate(R.id.action_searchFiltersFragment_to_searchResultFragment)
            } else {
//                    hideProgressView()
//                    binding.root.snackbar(error)
            }
        }

    }


    fun callSearchPopularUserQuery() {

        val interestedIn = requireArguments().getInt("interestedIn")
        val searchKey = requireArguments().getString("searchKey")
        Log.e("interestedIn ", "$interestedIn")
        Log.e("searchKey ", "$searchKey")

//        val locationService =
//            LocationServices.getFusedLocationProviderClient(requireActivity())
//
//        locationService.lastLocation.addOnSuccessListener { location: Location? ->
////                            val searchKey: String = binding.keyInput.text.toString()
//
//            var lat: Double? = null
//            var lon: Double? = null
//            if (searchKey!!.isEmpty()) {
//                lat = location?.latitude
//                lon = location?.longitude
//            }
//            val searchRequest = SearchRequest(
//                interestedIn = interestedIn,
//                id = userId!!,
//                searchKey = searchKey,
//                lat = lat,
//                long = lon
//            )
//            Log.e("search params", Gson().toJson(searchRequest))
        mViewModel.getSearchPopularUsers(
//                _searchRequest = searchRequest,
            token = userToken!!,
            interestedIn = interestedIn,
            context = requireContext(),
            hasSkip = hasSkip
        ) { error ->
            if (error == null) {
                Log.e("calllUpdateQuery", "calllUpdateQuery")
                mViewModel.setUpdateUserListQuery("")
                Log.e("MySearchPopularUsers", "" + mViewModel.getPopularUsers().size)
//                    setupViewPagerData()
//                    hideProgressView()
//                    navController.navigate(R.id.action_searchFiltersFragment_to_searchResultFragment)
            } else {
                Log.e("searchPopularError", "" + error.toString())
//                    hideProgressView()
//                    binding.root.snackbar(error)
            }
        }
//        }
    }


    fun callSearchMostActiveUserQuery() {

        val interestedIn = requireArguments().getInt("interestedIn")
        val searchKey = requireArguments().getString("searchKey")
        Log.e("interestedIn ", "$interestedIn")
        Log.e("searchKey ", "$searchKey")

//        val locationService =
//            LocationServices.getFusedLocationProviderClient(requireActivity())
//
//        locationService.lastLocation.addOnSuccessListener { location: Location? ->
////                            val searchKey: String = binding.keyInput.text.toString()
//
//            var lat: Double? = null
//            var lon: Double? = null
//            if (searchKey!!.isEmpty()) {
//                lat = location?.latitude
//                lon = location?.longitude
//            }
//            val searchRequest = SearchRequest(
//                interestedIn = interestedIn,
//                id = userId!!,
//                searchKey = searchKey,
//                lat = lat,
//                long = lon
//            )


//            Log.e("search params", Gson().toJson(searchRequest))
        mViewModel.getSearchMostActiveUsers(
//                _searchRequest = searchRequest,
            token = userToken!!,
            interestedIn = interestedIn,
            context = requireContext(),
            hasSkip = hasSkip
        ) { error ->
            if (error == null) {
                Log.e("calllUpdateQuery", "calllUpdateQuery")
                mViewModel.setUpdateUserListQuery("")
                Log.e("MyMostActiveUsers", "" + mViewModel.getMostActiveUsers().size)
//                    setupViewPagerData()
//                    hideProgressView()
//                    navController.navigate(R.id.action_searchFiltersFragment_to_searchResultFragment)
            } else {
                Log.e("mostActiveError", "" + error.toString())
//                    hideProgressView()
//                    binding.root.snackbar(error)
            }
        }
//        }
    }

    override fun onResume() {
//        callSearchResultQuery()


        getNotificationIndex()
        super.onResume()
    }


    private fun initSearch() {
        binding.interestsIcon.setOnClickListener {
            if (binding.keyInput.text.toString().isNotEmpty()) {
                binding.keyInput.hideKeyboard()
                mViewModel.setSearchUserQuery(binding.keyInput.text.toString())
            }
        }

        binding.keyInput.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.keyInput.hideKeyboard()
                mViewModel.setSearchUserQuery(binding.keyInput.text.toString())
                return@OnEditorActionListener true
            }
            false
        })

        binding.keyInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                Log.e("myTextChanged", binding.keyInput.text.toString())
                if (binding.keyInput.text.length != 0) {
                    Log.e("myTextChanged1", binding.keyInput.text.toString())
                    lifecycleScope.launchWhenResumed {
//                        mViewModel.setFilteredSearchUserQuery(binding.keyInput.text.toString())
                    }
                }


            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
    }

    override fun setupClickListeners() {

        binding.bell.setOnClickListener {
            val dialog = NotificationDialogFragment(
                userToken,
                binding.counter,
                userId,
                binding.bell
            )
//            dialog.show(
//                childFragmentManager,
//                "${requireActivity().resources.getString(R.string.notifications)}"
//            )
            if(activity is MainActivity)
                (activity as MainActivity).notificationDialog(dialog,childFragmentManager,"${requireActivity().resources.getString(R.string.notifications)}")
        }

        binding.search.setOnClickListener {
            binding.searchChiledContainer.visibility = View.VISIBLE
            binding.search.visibility = View.GONE
            binding.cross.visibility = View.VISIBLE
            binding.title.visibility = View.GONE
        }

        binding.cross.setOnClickListener {
            binding.searchChiledContainer.visibility = View.GONE
            binding.search.visibility = View.VISIBLE
            binding.cross.visibility = View.GONE
            binding.title.visibility = View.VISIBLE
            binding.keyInput.text.clear()
            binding.keyInput.hideKeyboard()
            mViewModel.setSearchUserQuery("")
            callSearchRandomPeopleQuery()
            callSearchPopularUserQuery()
            callSearchMostActiveUserQuery()
        }
    }

    override fun onDetach() {
        super.onDetach()

        Log.d("PSRF", "onDetach: ")
        binding.keyInput.text.clear()
        mViewModel.setSearchUserQuery("")
    }

    private fun getNotificationIndex() {

        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetNotificationCountQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception NotificationIndex  ${e.message}")
                binding.root.snackbar("${e.message}")
                return@launchWhenResumed
            }
            Timber.d("apolloResponse NotificationIndex ${res.hasErrors()}")

            val NotificationCount = res.data?.unseenCount
            if (NotificationCount == null || NotificationCount == 0) {
                binding.counter.visibility = View.GONE
                //  binding.bell.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.notification1))
            } else {
                binding.counter.visibility = View.VISIBLE

                if (NotificationCount > 10) {
                    binding.counter.text = "9+"
                } else {
                    binding.counter.text = "" + NotificationCount
                }

//                binding.counter.setText(""+NotificationCount)
                // binding.bell.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.notification2))

            }


        }
    }

//    private fun initSearch() {
//
//        binding.keyInput.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
//                binding.keyInput.hideKeyboard()
//                lifecycleScope.launch {
//                    userToken = getCurrentUserToken()!!
//                    userId = getCurrentUserId()!!
//
//                    Timber.i("usertokenn $userToken")
//                }
//
//                val searchRequest = SearchRequestNew(
//
//                    name = binding.keyInput.text.toString()
//
//                )
//
//                Log.e("search params", Gson().toJson(searchRequest))
//                mViewModel.getSearchUsersTemp(
//                    _searchRequest = searchRequest,
//                    token = userToken!!
//                ) { error ->
//                    if (error == null) {
//                        hideProgressView()
//
//                        if (mViewModel.getRandomUsersSearched().isNullOrEmpty()) {
//                            binding.noUsersLabel.setViewVisible()
//
//                        } else {
//                            binding.noUsersLabel.setViewGone()
//
//                        }
//                        usersAdapter.updateItems(mViewModel.getRandomUsersSearched())
//                        //  navController.navigate(R.id.action_searchFiltersFragment_to_searchResultFragment)
//                    } else {
//                        hideProgressView()
//                        //binding.root.snackbar(error)
//                    }
//                }
//                return@OnEditorActionListener true
//            }
//            false
//        })
//
//        binding.interestsIcon.setOnClickListener {
//
//
//            if(binding.keyInput.text.toString().length>0)
//            {
//                binding.keyInput.hideKeyboard()
//                lifecycleScope.launch {
//                    userToken = getCurrentUserToken()!!
//                    userId = getCurrentUserId()!!
//
//                    Timber.i("usertokenn $userToken")
//                }
//
//                val searchRequest = SearchRequestNew(
//
//                    name = binding.keyInput.text.toString()
//
//                )
//
//                Log.e("search params", Gson().toJson(searchRequest))
//                mViewModel.getSearchUsersTemp(
//                    _searchRequest = searchRequest,
//                    token = userToken!!
//                ) { error ->
//                    if (error == null) {
//                        hideProgressView()
//
//                        if (mViewModel.getRandomUsersSearched().isNullOrEmpty()) {
//                            binding.noUsersLabel.setViewVisible()
//
//                        } else {
//                            binding.noUsersLabel.setViewGone()
//
//                        }
//                        usersAdapter.updateItems(mViewModel.getRandomUsersSearched())
//                        //  navController.navigate(R.id.action_searchFiltersFragment_to_searchResultFragment)
//                    } else {
//                        hideProgressView()
//                        //binding.root.snackbar(error)
//                    }
//                }
//            }
//            else
//            {
//                binding.keyInput.requestFocus()
//                binding.keyInput.showKeyboard()
//            }
//
//
//        }
//
//        binding.keyInput.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//
//            }
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                val filteredUsers = when (mPage) {
//                    0 ->
//                    {
//                        val searchKey: String = binding.keyInput.text.toString()
//                        if(searchKey.length==0)
//                        {
//                            binding.keyInput.hideKeyboard()
//                            Log.e("iff","iff")
//                            if (mViewModel.getRandomUsers().isNullOrEmpty()) {
//                                binding.noUsersLabel.setViewVisible()
//
//                            } else {
//                                binding.noUsersLabel.setViewGone()
//
//                            }
//
////                            if(usersAdapter.isInitialized)
//                            if(checkIsInitalize()) {
//                                usersAdapter.updateItems(mViewModel.getRandomUsers())
//                            } else {
//                                Log.e("PageSearchFragment_keyInput", "not Initialized")
//                            }
//
//                        }
//                        else
//                        {
//
//                            /*      lifecycleScope.launch {
//                                      userToken = getCurrentUserToken()!!
//                                      userId = getCurrentUserId()!!
//
//                                      Timber.i("usertokenn $userToken")
//                                  }
//
//                                  val searchRequest = SearchRequestNew(
//
//                                      name = searchKey
//
//                                  )
//
//                                  Log.e("search params", Gson().toJson(searchRequest))
//                                  mViewModel.getSearchUsersTemp(
//                                      _searchRequest = searchRequest,
//                                      token = userToken!!
//                                  ) { error ->
//                                      if (error == null) {
//                                          hideProgressView()
//                                          usersAdapter.updateItems(mViewModel.getRandomUsersSearched())
//                                        //  navController.navigate(R.id.action_searchFiltersFragment_to_searchResultFragment)
//                                      } else {
//                                          hideProgressView()
//                                          //binding.root.snackbar(error)
//                                      }
//                                  }*/
//                        }
//
//                    }
//                    1 ->
//                    {
//                        // filterUsers(s.toString(), mViewModel.getPopularUsers())
//                        val searchKey: String = binding.keyInput.text.toString()
//                        if(searchKey.length==0)
//                        {
//                            if (mViewModel.getPopularUsers().isNullOrEmpty()) {
//                                binding.noUsersLabel.setViewVisible()
//
//                            } else {
//                                binding.noUsersLabel.setViewGone()
//
//                            }
//                            usersAdapter.updateItems(mViewModel.getPopularUsers())
//                        }
//                        else
//                        {
//
//                            /*     lifecycleScope.launch {
//                                     userToken = getCurrentUserToken()!!
//                                     userId = getCurrentUserId()!!
//
//                                     Timber.i("usertokenn $userToken")
//                                 }
//
//                                 val searchRequest = SearchRequestNew(
//
//                                     name = searchKey
//
//                                 )
//
//                                 Log.e("search params", Gson().toJson(searchRequest))
//                                 mViewModel.getSearchUsersTemp(
//                                     _searchRequest = searchRequest,
//                                     token = userToken!!
//                                 ) { error ->
//                                     if (error == null) {
//                                         hideProgressView()
//                                         usersAdapter.updateItems(mViewModel.getRandomUsersSearched())
//                                         //  navController.navigate(R.id.action_searchFiltersFragment_to_searchResultFragment)
//                                     } else {
//                                         hideProgressView()
//                                       //  binding.root.snackbar(error)
//                                     }
//                                 }*/
//                        }
//                    }
//                    2 -> {
//
//                        //  filterUsers(s.toString(), mViewModel.getMostActiveUsers())
//                        val searchKey: String = binding.keyInput.text.toString()
//                        if(searchKey.length==0)
//                        {
//                            if (mViewModel.getMostActiveUsers().isNullOrEmpty()) {
//                                binding.noUsersLabel.setViewVisible()
//
//                            } else {
//                                binding.noUsersLabel.setViewGone()
//
//                            }
//                            usersAdapter.updateItems(mViewModel.getMostActiveUsers())
//                        }
//                        else
//                        {
//
//                            /*       lifecycleScope.launch {
//                                       userToken = getCurrentUserToken()!!
//                                       userId = getCurrentUserId()!!
//
//                                       Timber.i("usertokenn $userToken")
//                                   }
//
//                                   val searchRequest = SearchRequestNew(
//
//                                       name = searchKey
//
//                                   )
//
//                                   Log.e("search params", Gson().toJson(searchRequest))
//                                   mViewModel.getSearchUsersTemp(
//                                       _searchRequest = searchRequest,
//                                       token = userToken!!
//                                   ) { error ->
//                                       if (error == null) {
//                                           hideProgressView()
//                                           usersAdapter.updateItems(mViewModel.getRandomUsersSearched())
//                                           //  navController.navigate(R.id.action_searchFiltersFragment_to_searchResultFragment)
//                                       } else {
//                                           hideProgressView()
//                                          // binding.root.snackbar(error)
//                                       }
//                                   }*/
//                        }
//                    }
//                    else -> mViewModel.getRandomUsers()
//                }
//
//                /*  val filteredUsers = when (mPage) {
//                    0 -> filterUsers(s.toString(), mViewModel.getRandomUsers())
//                    1 -> filterUsers(s.toString(), mViewModel.getPopularUsers())
//                    2 -> filterUsers(s.toString(), mViewModel.getMostActiveUsers())
//                    else -> arrayListOf()
//                }*/
//
//            }
//
//            override fun afterTextChanged(s: Editable?) {
//
//            }
//        })
//    }


    override fun onPause() {
        super.onPause()
       // binding.slidingTabs.clearOnTabSelectedListeners()
    }

}