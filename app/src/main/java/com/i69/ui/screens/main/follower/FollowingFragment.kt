package com.i69.ui.screens.main.follower

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollographql.apollo3.exception.ApolloException
import com.google.gson.Gson
import com.i69.*
import com.i69.applocalization.AppStringConstant1
import com.i69.databinding.FragmentFollowersBinding
import com.i69.profile.vm.VMProfile
import com.i69.ui.adapters.AdaptersFollowing
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.screens.main.search.userProfile.SearchUserProfileFragment
import com.i69.utils.*
import kotlinx.coroutines.launch
import timber.log.Timber

class FollowingFragment : BaseFragment<FragmentFollowersBinding>(),
    AdaptersFollowing.FollowingListListener {
    var giftsAdapter: AdaptersFollowing? = null
    var list: MutableList<GetUserQuery.FollowingUser?> = mutableListOf()

    private val viewModel : VMProfile by activityViewModels()
    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentFollowersBinding.inflate(inflater, container, false)

    override fun setupTheme() {
        giftsAdapter = AdaptersFollowing(requireContext(), list, this@FollowingFragment)
        binding.recyclerViewFolowers.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.recyclerViewFolowers.setHasFixedSize(true)
        binding.recyclerViewFolowers.adapter = giftsAdapter

        viewModel.getupdateFollowingListResultWith()?.observe(viewLifecycleOwner, {
            list.clear()
            list.addAll(it)
            giftsAdapter!!.notifyDataSetChanged()
        })

//        getUserFollowingData()
//        subscribeForUserUpdate()

    }

//    private fun subscribeForUserUpdate() {
//
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                apolloClientSubscription(requireActivity(), getCurrentUserToken()!!).subscription(
//                    FollowSubscription()
//                ).toFlow()
//                    .retryWhen { cause, attempt ->
//                        Timber.d("reealltime retry $attempt ${cause.message}")
//
//                        delay(attempt * 1000)
//                        true
//                    }.collect { newStory ->
//                        if (newStory.hasErrors()) {
//
//                            Timber.d("reealltime response error = ${newStory.errors?.get(0)?.message}")
//                        } else {
//                            //   Timber.d("reealltime onNewMessage ${newMessage.data?.onNewMessage?.message?.timestamp}")
//                            Log.e(
//                                "followUserSubscript",
//                                "story realtime DeleteStory ${newStory.data}"
//
//                            )
//
//                            getUserFollowingData(false)
//
//                        }
//                    }
//
//
//            } catch (e2: Exception) {
//                e2.printStackTrace()
//                Log.d("UserMomentSubsc", "story realtime exception= ${e2.message}")
//                Timber.d("reealltime exception= ${e2.message}")
//            }
//        }
//    }


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

                list.clear()
                var foolowerList = res.data!!.user!!.followingUsers

                Log.e("foolowerList", Gson().toJson(foolowerList))


                val x = mutableListOf<GetUserQuery.FollowingUser?>().apply {
                    if (foolowerList != null) {
                        addAll(foolowerList)
                    }
                }
                list.addAll(x)
                giftsAdapter!!.notifyDataSetChanged()
//                Timber.d("LIKE")

                hideProgressView()
            }
        }


    }

    override fun onItemClick(followinfUser: GetUserQuery.FollowingUser?) {
        Log.e("clickedOnItems", "clickedOnItems")
        userUnFollowMutationCall(followinfUser)
    }

    override fun onUserProfileClick(followinfUser: GetUserQuery.FollowingUser?) {
        lifecycleScope.launch {
            var bundle = Bundle()
            bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
            bundle.putString("userId", followinfUser!!.id.toString())

            if (getCurrentUserId()!! == followinfUser!!.id) {
                MainActivity.getMainActivity()?.binding?.bottomNavigation?.selectedItemId =
                    R.id.nav_user_profile_graph
            } else {
                findNavController().navigate(
                    destinationId = R.id.action_global_otherUserProfileFragment,
                    popUpFragId = null,
                    animType = AnimationTypes.SLIDE_ANIM,
                    inclusive = true,
                    args = bundle
                )

            }
        }
    }


    override fun setupClickListeners() {}


    private fun userUnFollowMutationCall(followinfUser: GetUserQuery.FollowingUser?) {
        if (followinfUser!!.isConnected != null && followinfUser!!.isConnected!!) {
            unFollowConfirmation(followinfUser)
        }else{
            userFollowMutationCall(followinfUser)
        }

    }

    private fun userFollowMutationCall(followinfUser: GetUserQuery.FollowingUser?){
        Log.e("FollowUserIds",followinfUser!!.id!!.toString())

        lifecycleScope.launchWhenResumed {
            showProgressView()

            val res = try {
                apolloClient(
                    requireContext(),
                    getCurrentUserToken()!!
                ).mutation(UserFollowMutation(followinfUser!!.id!!.toString()))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception ${e.message}")
//                    Toast.makeText(requireContext(), " ${e.message}", Toast.LENGTH_LONG).show()

                hideProgressView()
                return@launchWhenResumed
            }

            if (res.hasErrors()) {
                hideProgressView()
                val errorMessage = res.errors?.get(0)?.message
                Log.e("errorAllPackage", "$errorMessage")
                Log.e("res.errorsFollowers", "${res.errors}")

                if (errorMessage != null) {
                    binding.root.snackbar(errorMessage)
                }
            } else {


                binding.root.snackbar(AppStringConstant1.successfully)

                getUserFollowingData()
            }
        }
    }


    private fun unFollowConfirmation(followinfUser: GetUserQuery.FollowingUser?) {


//        var message = resources.getString(R.string.are_you_sure_you_want_to_unfollow_user)
        var message = AppStringConstant1.are_you_sure_you_want_to_unfollow_user
            .plus(followinfUser!!.fullName)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.app_name))
        builder.setMessage("${message}")
            .setCancelable(false)
            .setPositiveButton(android.R.string.yes) { dialog, which ->
                userUnFollowCall(followinfUser)
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

    fun userUnFollowCall(followinfUser: GetUserQuery.FollowingUser?) {
        Log.e("FollowingUserIds", followinfUser!!.id!!.toString())

        lifecycleScope.launchWhenResumed {
            showProgressView()
            var unfollowMutation = UserUnfollowMutation(followinfUser!!.id!!.toString())
            Log.e("UnfollowMutationList", Gson().toJson(unfollowMutation))
            val res = try {
                apolloClient(
                    requireContext(),
                    getCurrentUserToken()!!
                ).mutation(unfollowMutation)
//                    .mutation(UserUnfollowMutation(followinfUser!!.id!!.toString()))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception ${e.message}")
//                    Toast.makeText(requireContext(), " ${e.message}", Toast.LENGTH_LONG).show()

                hideProgressView()
                return@launchWhenResumed
            }

            if (res.hasErrors()) {
                hideProgressView()
                val errorMessage = res.errors?.get(0)?.message
                Log.e("errorAllPackage", "$errorMessage")
                Log.e("res.errorsFollowig", "${res.errors}")
                if (errorMessage != null) {
                    binding.root.snackbar(errorMessage)
                }
            } else {


//                binding.root.snackbar(getString(R.string.successfully))

                getUserFollowingData()
            }
        }

    }

}