package com.i69.ui.screens.main.follower

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager
import com.apollographql.apollo3.exception.ApolloException
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.i69.FollowSubscription
import com.i69.GetUserQuery
import com.i69.applocalization.AppStringConstant1
import com.i69.databinding.FragmentUserFollowFolllowersBinding
import com.i69.profile.vm.VMProfile
import com.i69.ui.adapters.UserItemsAdapter
import com.i69.ui.base.BaseFragment
import com.i69.utils.SharedPref
import com.i69.utils.apolloClient
import com.i69.utils.apolloClientSubscription
import com.i69.utils.snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import timber.log.Timber


class UserFolloweFollowingFragment : BaseFragment<FragmentUserFollowFolllowersBinding>() {

//    private val viewModel: VMProfile by activityViewModels()
    private var userId: String = ""
    private var userToken: String = ""

    private var chatBundle = Bundle()
    private val viewModel: VMProfile by activityViewModels()

    lateinit var sharedPref: SharedPref
    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentUserFollowFolllowersBinding.inflate(inflater, container, false)

    override fun setupTheme() {


        navController = findNavController()

        lifecycleScope.launch {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            //userChatID= getChatUserId()!!
        }
        val userFulNAme =      requireArguments().getString("userFulNAme")

//
        if (userFulNAme != null) {
            Log.e("userFulNAmeaaaa", userFulNAme)
        }

        binding.toolbarTitle.text = "$userFulNAme"


//        val specificUserid =      requireArguments().getString("userId")

//        viewModel.getProfile(specificUserid)


        binding.userFollowTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {
                binding.userFollowPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
            }
        })
        binding.userFollowTabs.setupWithViewPager(binding.userFollowPager)
        setupViewPager(binding.userFollowPager)

        getUserFollowingData()
        subscribeForUserUpdate()

    }


    @SuppressLint("SuspiciousIndentation")
    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = UserItemsAdapter(childFragmentManager)
     var   fragFollowers = FollowersFragment()
      var  fragFollowing = FollowingFragment()
        val userid =      requireArguments().getString("userId")

        var bundle = Bundle()
        bundle.putString("userId",userid)

        fragFollowers.arguments =bundle
        fragFollowing.arguments =bundle

        adapter.addFragItem(fragFollowers!!, AppStringConstant1.followers)
        adapter.addFragItem(fragFollowing!!,  AppStringConstant1.following_tab)

//        adapter.addFragItem(fragFollowers!!, getString(R.string.followers))
//        adapter.addFragItem(fragFollowing!!, getString(R.string.following_tab))
        viewPager.adapter = adapter
    }

    override fun setupClickListeners() {
        binding.purchaseClose.setOnClickListener {
            findNavController().popBackStack()
        }

    }

    private fun subscribeForUserUpdate() {


        lifecycleScope.launch(Dispatchers.IO) {


            try {
                apolloClientSubscription(requireActivity(), getCurrentUserToken()!!).subscription(
                    FollowSubscription()
                ).toFlow().catch {
                    it.printStackTrace()
                    Timber.d("reealltime exception= ${it.message}")
                }
                    .retryWhen { cause, attempt ->
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

                            getUserFollowingData(false)

                        }
                    }


            } catch (e2: Exception) {
                e2.printStackTrace()
                Log.d("UserMomentSubsc", "story realtime exception= ${e2.message}")
                Timber.d("reealltime exception= ${e2.message}")
            }
        }
    }



    private fun getUserFollowingData(isShowLoader: Boolean = true) {
        val userid = requireArguments().getString("userId")
        if (userid != null) {
            Log.e("getUserId", userid)
        }
        lifecycleScope.launch {
            if (isShowLoader) {
                showProgressView()
            }

            val res = try {
                apolloClient(
                    requireContext(),
                    getCurrentUserToken()!!
                )
                    .query(GetUserQuery(userid!!))
//                    .query(GetUserQuery(getCurrentUserId()!!))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception ${e.message}")
                Log.e("errorAApolloException", "$e")
//                    Toast.makeText(requireContext(), " ${e.message}", Toast.LENGTH_LONG).show()

                hideProgressView()
                return@launch
            }

            if (res.hasErrors()) {
                hideProgressView()
                val errorMessage = res.errors?.get(0)?.message
                Log.e("FollowingFragmntError", "${res.errors}")
                Log.e("FollowingFragmntError1", "$errorMessage")

                if (errorMessage != null) {
                    binding.root.snackbar(errorMessage)
                }
            } else {

//                list.clear()
                var foolowingList = res.data!!.user!!.followingUsers
                var foolowerList = res.data!!.user!!.followerUsers

                Log.e("foolowerList", Gson().toJson(foolowerList))


                val x = mutableListOf<GetUserQuery.FollowingUser?>().apply {

                        if (foolowingList != null) {
                            addAll(foolowingList)
                        }

                }

                val xy = mutableListOf<GetUserQuery.FollowerUser?>().apply {

                    if (foolowerList != null) {
                        addAll(foolowerList)
                    }

                }

                viewModel.setupdateFollowingListResultWith(x)
                viewModel.updateFollowerListResultWith(xy)
//                list.addAll(x)
//                giftsAdapter!!.notifyDataSetChanged()
//                Timber.d("LIKE")

                hideProgressView()
            }
        }


    }


}
