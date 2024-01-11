package com.i69.ui.screens.main.search.result


import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.i69.R
import com.i69.applocalization.AppStringConstant
import com.i69.applocalization.AppStringConstant1
import com.i69.applocalization.AppStringConstantViewModel
import com.i69.data.models.MyPermission
import com.i69.data.models.User
import com.i69.data.remote.requests.SearchRequestNew
import com.i69.data.remote.responses.DefaultPicker
import com.i69.databinding.FragmentPageSearchResultBinding
import com.i69.ui.adapters.LockUsersSearchListAdapter
import com.i69.ui.adapters.UsersSearchListAdapter
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.main.MainActivity.Companion.getMainActivity
import com.i69.ui.screens.main.search.FiltersDialogFragment
import com.i69.ui.screens.main.search.userProfile.SearchUserProfileFragment
import com.i69.ui.screens.main.subscription.SubscriptionBSDialogFragment
import com.i69.ui.viewModels.SearchViewModel
import com.i69.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber


@AndroidEntryPoint
class PageSearchResultFragment : BaseFragment<FragmentPageSearchResultBinding>(),
    UsersSearchListAdapter.UserSearchListener, LockUsersSearchListAdapter.LockUserSearchListener {
    private var userToken: String? = null
    private var userId: String? = null
    private val viewStringConstModel: AppStringConstantViewModel by activityViewModels()


    companion object {
        private const val ARG_DATA_BY_PAGE_ID = "ARG_PAGE_ID"

        fun newInstance(page: Int): PageSearchResultFragment {
            val args = Bundle()
            args.putInt(ARG_DATA_BY_PAGE_ID, page)
            val fragment = PageSearchResultFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var mPage: Int = 0
    private val mViewModel: SearchViewModel by activityViewModels()
    private lateinit var usersAdapter: UsersSearchListAdapter
    private lateinit var usersLockAdapter: LockUsersSearchListAdapter

    private var mHandler: Handler? = null

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPageSearchResultBinding.inflate(inflater, container, false).apply {
            stringConstant = AppStringConstant(requireContext())
        }


    override fun setupTheme() {
        mHandler = Handler(Looper.getMainLooper())

        viewStringConstModel.data.observe(this@PageSearchResultFragment) { data ->

            binding.stringConstant = data

        }
        viewStringConstModel.data?.also {
            binding.stringConstant = it.value
//            Log.e("MydataBasesss", it.value!!.messages)
        }

        mViewModel.updateFilteredData.observe(this@PageSearchResultFragment) { data ->
            Log.e("observer_called", "" + requireArguments().getInt(ARG_DATA_BY_PAGE_ID, 0))
            updateFilterdData()
        }

        Log.e("calllFragmentfromStart", "calllFragmentfromStart")
        mPage = requireArguments().getInt(ARG_DATA_BY_PAGE_ID)
        navController = findNavController()

        if (this@PageSearchResultFragment::usersAdapter.isInitialized) {
//                    if (   usersAdapter == null) {
            Log.e("InitializedAdapter", "InitializedAdapter")
            binding.usersRecyclerView.adapter = usersAdapter
        }

        if (this@PageSearchResultFragment::usersLockAdapter.isInitialized) {
//                    if (   usersAdapter == null) {
            binding.usersLockRecyclerView.adapter = usersLockAdapter
        }


        initSearch()
    }

    override fun onResume() {
        super.onResume()

    }

    val userItems: ArrayList<User> = ArrayList()

    private fun initAdapterData(pickers: DefaultPicker) {

        if (!this@PageSearchResultFragment::usersAdapter.isInitialized) {
//                    if (   usersAdapter == null) {
            Log.e("InitializedAdapter", "InitializedAdapter")
            usersAdapter = UsersSearchListAdapter(this@PageSearchResultFragment, pickers, userItems)
            binding.usersRecyclerView.adapter = usersAdapter
        }

        if (!this@PageSearchResultFragment::usersLockAdapter.isInitialized) {
//                    if (   usersAdapter == null) {
            usersLockAdapter =
                LockUsersSearchListAdapter(this@PageSearchResultFragment, pickers)
            binding.usersLockRecyclerView.adapter = usersLockAdapter
        }

    }


    private fun updateFilterdData() {

        val users: ArrayList<User> = when (mPage) {
            0 -> mViewModel.getRandomUsers()

            1 -> mViewModel.getPopularUsers()

            else -> mViewModel.getMostActiveUsers()

        }


        val myPermission: MyPermission = when (mPage) {
            0 -> mViewModel.getMyPermission()

            1 -> mViewModel.getPopularUserMyPermission()

            else -> mViewModel.getMostActiveUserMyPermission()

        }


        val unLockUsers: ArrayList<User> = ArrayList()
        val lockUsers: ArrayList<User> = ArrayList()
        if (myPermission.hasPermission) {
            unLockUsers.addAll(users)
            binding.usersLockRecyclerView.setViewGone()
            binding.unlockLayout.setViewGone()
        } else {
            users.indices.forEach { i ->
                if (i < myPermission.freeUserLimit) {
                    unLockUsers.add(users.get(i))
                } else {
                    lockUsers.add(users.get(i))
                }
            }

            binding.usersLockRecyclerView.setViewVisible()
            binding.unlockLayout.setViewVisible()
            binding.usersRecyclerView.setViewVisible()
        }

        if (users.isNullOrEmpty()) {
            binding.noUsersLabel.setViewVisible()
            binding.usersLockRecyclerView.setViewGone()
            binding.unlockLayout.setViewGone()
            binding.usersRecyclerView.setViewGone()

        } else {
            binding.noUsersLabel.setViewGone()
            Handler().postDelayed(object : Runnable {
                override fun run() {
                    activity!!.runOnUiThread {
                        usersLockAdapter.updateItems(lockUsers, myPermission)
                        userItems.clear()
                        userItems.addAll(unLockUsers)
                        usersAdapter.notifyDataSetChanged()

                        binding.usersRecyclerView.setViewVisible()
                        binding.usersLockRecyclerView.setViewVisible()
                        binding.unlockLayout.setViewVisible()


                        // usersAdapter.updateItems(unLockUsers, myPermission)
                    }
                }
            }, 100)

//                        usersAdapter.updateItems(users, mViewModel.getMyPermission())
        }
    }

    private fun setupUserSearchAdapter() {
        lifecycleScope.launch {
            if (mViewModel.getSearchUserQuery() == null ||
                mViewModel.getSearchUserQuery()?.value == null ||
                mViewModel.getSearchUserQuery()?.value.toString().isEmpty()) {

                val userToken = getCurrentUserToken()!!

                mViewModel.getDefaultPickers(userToken).observe(viewLifecycleOwner) { pickers ->
                    pickers?.let { picker ->

//                    Log.d("BackBtnBug", "setupUserSearchAdapter: $picker")
                        initAdapterData(pickers)


                        val users: ArrayList<User> = when (mPage) {
                            0 -> mViewModel.getRandomUsers()

                            1 -> mViewModel.getPopularUsers()

                            else -> mViewModel.getMostActiveUsers()

                        }


                        val myPermission: MyPermission = when (mPage) {
                            0 -> mViewModel.getMyPermission()

                            1 -> mViewModel.getPopularUserMyPermission()

                            else -> mViewModel.getMostActiveUserMyPermission()

                        }


                        val unLockUsers: ArrayList<User> = ArrayList()
                        val lockUsers: ArrayList<User> = ArrayList()
                        if (myPermission.hasPermission) {
                            unLockUsers.addAll(users)
                            binding.usersLockRecyclerView.setViewGone()
                            binding.unlockLayout.setViewGone()
                        } else {
                            users.indices.forEach { i ->
                                if (i < myPermission.freeUserLimit) {
                                    unLockUsers.add(users.get(i))
                                } else {
                                    lockUsers.add(users.get(i))
                                }
                            }

                            binding.usersLockRecyclerView.setViewVisible()
                            binding.unlockLayout.setViewVisible()
                            binding.usersRecyclerView.setViewVisible()
                        }
                        Log.e("UpdateUserList", "${lockUsers.size}")
                        Log.e("UpdateUserList1", "${unLockUsers.size}")
                        Log.e("UpdateUserList2", "${users.size}")
                        if (users.isEmpty()) {
                            binding.noUsersLabel.setViewVisible()
                            binding.usersLockRecyclerView.setViewGone()
                            binding.unlockLayout.setViewGone()
                            binding.usersRecyclerView.setViewGone()

                        } else {
                            mHandler?.post {
                                binding.noUsersLabel.setViewGone()
                                Log.d("PSRF", "setupUserSearchAdapter: ")
                                usersLockAdapter.updateItems(lockUsers, myPermission)
                                userItems.clear()
                                userItems.addAll(unLockUsers)
                                usersAdapter.notifyDataSetChanged()
                                binding.usersRecyclerView.setViewVisible()
                                binding.usersLockRecyclerView.setViewVisible()
                                binding.unlockLayout.setViewVisible()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun setupClickListeners() {

        binding.llButtonUnLock.setOnClickListener {
//            userSearchAllrequestCall()


            val text: String =
                java.lang.String.format(
                    AppStringConstant1.unlock_this_funtion,
//                    resources.getString(R.string.unlock_this_funtion),
                    "${this.mViewModel.getMyPermission().coinsToUnlock}"
                )
//            val dialog = SubscriptionDialogFragment(
//                userToken,
//                userId,
//                text
//
//            )
//            dialog.show(
//                childFragmentManager,
//                "${AppStringConstant1.subscription}"
//            )

            val subscriptionBSDialogFragment =
                SubscriptionBSDialogFragment.newInstance(userToken, userId, text)
            subscriptionBSDialogFragment.show(
                childFragmentManager,
                "${AppStringConstant1.subscription}"
            )

        }

        binding.filterButton.setOnClickListener {
            val dialog = FiltersDialogFragment(
                userToken,
                userId,

                )
            dialog.show(
                childFragmentManager,
                "${AppStringConstant1.filter}"
            )
        }
    }

    private fun initSearch() {
        mViewModel.getUpdateUserListQuery()?.observe(viewLifecycleOwner) {
            Log.e("getUpdateListQuery", "getUpdateListQuery")
            Log.e(
                "getUpdateListQuery", "" +
                        "${mViewModel.getRandomUsers().size}"
            )

//            if (mViewModel.getRandomUsers().size != 0) {
            setupUserSearchAdapter()
//            }
        }


        lifecycleScope.launch {
            userToken = getCurrentUserToken()!!
            userId = getCurrentUserId()!!

            Timber.i("usertokenn $userToken")
        }

        mViewModel.getSearchUserQuery()?.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                userToken = getCurrentUserToken()!!
                userId = getCurrentUserId()!!

                Timber.i("usertokenn $userToken")
            }
            if (it?.length != 0) {
                val searchRequest = SearchRequestNew(
                    name = it.toString()
//                name = binding.keyInput.text.toString()

                )

                Log.e("search params", Gson().toJson(searchRequest))
                mViewModel.getSearchUsersTemp(
                    _searchRequest = searchRequest,
                    token = userToken!!,
                    context = requireContext()
                ) { error ->
                    if (error == null) {
                        hideProgressView()

                        val unLockUsers: ArrayList<User> = ArrayList()
                        val lockUsers: ArrayList<User> = ArrayList()
                        if (mViewModel.getMyPermission().hasPermission) {
                            unLockUsers.addAll(mViewModel.getRandomUsersSearched())
                            binding.usersLockRecyclerView.setViewGone()
                            binding.unlockLayout.setViewGone()
                        } else {
                            mViewModel.getRandomUsersSearched().indices.forEach { i ->
//                                if (i <= 2) {
                                if (i < mViewModel.getMyPermission().freeUserLimit) {
                                    unLockUsers.add(mViewModel.getRandomUsersSearched().get(i))
                                } else {
                                    lockUsers.add(mViewModel.getRandomUsersSearched().get(i))
                                }
                            }

                            binding.usersLockRecyclerView.setViewVisible()
                            binding.unlockLayout.setViewVisible()
                        }

                        /* usersLockAdapter.updateItems(lockUsers, mViewModel.getMyPermission())
                         usersAdapter.updateItems(unLockUsers, mViewModel.getMyPermission())*/

                        mHandler?.post {
                            Log.d("PSRF", "manual search results: ")
                            usersLockAdapter.updateItems(lockUsers, mViewModel.getMyPermission())
                            userItems.clear()
                            userItems.addAll(unLockUsers)
                            usersAdapter.notifyDataSetChanged()
                            binding.usersRecyclerView.setViewVisible()
                            binding.usersLockRecyclerView.setViewVisible()
                            binding.unlockLayout.setViewVisible()


                            if (mViewModel.getRandomUsersSearched().isNullOrEmpty()) {
                                binding.noUsersLabel.setViewVisible()
                                binding.usersLockRecyclerView.setViewGone()
                                binding.unlockLayout.setViewGone()
                                binding.usersRecyclerView.setViewGone()

                            } else {
                                binding.noUsersLabel.setViewGone()
                                binding.usersLockRecyclerView.setViewVisible()
                                binding.unlockLayout.setViewVisible()
                                binding.usersRecyclerView.setViewVisible()

                            }
                        }
                    } else {
                        hideProgressView()
                    }
                }
            }
        }

    }


    fun checkIsInitalize(): Boolean {

        return this@PageSearchResultFragment::usersAdapter.isInitialized
//      return true

    }

    private fun filterUsers(text: String, fullListOfUsers: List<User>): List<User> {
        return if (text.trim().isEmpty()) {
            fullListOfUsers

        } else {

            val filteredList: ArrayList<User> = ArrayList()
            fullListOfUsers.forEach {
                if (it.fullName.lowercase().contains(text.lowercase())) filteredList.add(it)
            }
            filteredList
        }
    }

    override fun onItemClick(position: Int, user: User) {
        mViewModel.selectedUser.value = user
        var bundle = Bundle()
        bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
        bundle.putString("userId", user.id)
        bundle.putString("fullName", user.fullName)
        if (userId == user.id) {
            getMainActivity()?.binding?.bottomNavigation?.selectedItemId =
                R.id.nav_user_profile_graph
        } else {
            findNavController().navigate(
                destinationId = R.id.action_global_otherUserProfileFragment,
                popUpFragId = null,
                animType = AnimationTypes.SLIDE_ANIM,
                inclusive = false,
                args = bundle
            )
//            navController.navigate(
//
//            )
        }
    }

//    override fun onUnlockFeatureClick() {
//        navController.navigate(R.id.actionGoToPurchaseFragment)
//    }


    override fun onUnlockFeatureClick() {

        userSearchAllrequestCall()
    }

    override fun onLockItemClick(position: Int, user: User) {
        userSearchAllrequestCall()


    }


    private fun userSearchAllrequestCall() {
        val text =
            "${AppStringConstant1.unlock_this_funtion_} ${AppStringConstant1.to_view_more_profile}"
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.app_name))
        builder.setMessage(text)
//        builder.setMessage(getString(R.string.msg_coin_will_be_deducted_from_his))
            .setCancelable(false)
            .setPositiveButton(android.R.string.yes) { dialog, which ->
//                updateSearchResultWithCoin()

                val text: String =
                    java.lang.String.format(
                        AppStringConstant1.unlock_this_funtion,
                        "${this.mViewModel.getMyPermission().coinsToUnlock}"
                    )


                val subscriptionBSDialogFragment =
                    SubscriptionBSDialogFragment.newInstance(userToken, userId, text)
                subscriptionBSDialogFragment.show(
                    childFragmentManager,
                    "${AppStringConstant1.subscription}"
                )

            }
            .setNegativeButton(android.R.string.no) { dialog, which ->
                dialog.dismiss();
            }
        val alert = builder.create()
        alert.show()
        alert.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(resources.getColor(R.color.black));
        alert.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(resources.getColor(R.color.black));

    }
}
