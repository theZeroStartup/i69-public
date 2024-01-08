package com.i69.ui.screens.main.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.apollographql.apollo3.exception.ApolloException
import com.google.gson.Gson
import com.i69.UserSubscriptionQuery
import com.i69.applocalization.AppStringConstant
import com.i69.databinding.FragmentUserSubscriptionDetailBinding
import com.i69.ui.base.BaseFragment
import com.i69.ui.viewModels.UserViewModel
import com.i69.utils.*
import kotlinx.coroutines.launch
import timber.log.Timber

class UserSubscriptionDetailFragment : BaseFragment<FragmentUserSubscriptionDetailBinding>() {

    private val viewModel: UserViewModel by activityViewModels()
    private var userId: String = ""
    private var userToken: String = ""

    lateinit var sharedPref: SharedPref
    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentUserSubscriptionDetailBinding.inflate(inflater, container, false)

    override fun setupTheme() {

        sharedPref = SharedPref(requireContext())
        sharedPref.setLanguage(false)
        sharedPref.setLanguageFromSettings(false)
        binding.toolBarTitle.text = AppStringConstant(requireContext()).subscription
        binding.tvSubScription.text = AppStringConstant(requireContext()).no_active_subscription
        navController = findNavController()

        lifecycleScope.launch {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            //userChatID= getChatUserId()!!
        }

        userSubScription()


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

//        binding.subScriptionLayout.setOnClickListener {
//
//        }
    }


    fun userSubScription() {

        showProgressView()
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
                hideProgressView()

                if (response.data!!.userSubscription!!.`package` != null) {
                    if (response.data?.userSubscription?.`package`?.id?.contains("1", true) == true
                        || response.data?.userSubscription?.`package`?.id?.contains("2", true) == true) {
                        binding.llActionsRoot.setViewVisible()
                        binding.llButtonSubscribe.setViewGone()
                        binding.llButtonUpgrade.setViewVisible()
                        binding.llButtonUpgrade.setOnClickListener {
                            if (response.data?.userSubscription?.`package`?.id?.contains("2", true) == true)
                                navigatePlanPurchase("Gold")
                            else
                                navigatePlanPurchase("Silver")
                        }
                    }

                    binding.tvSubScription.text =
                        requireContext().resources.getString(com.i69.R.string.your_subscription)
                            .plus(response.data!!.userSubscription!!.`package`!!.name).plus("(")
                            .plus(response.data!!.userSubscription!!.plan!!.title).plus(")")
                }
                if (response.data?.userSubscription?.isActive == false) {
                    binding.llActionsRoot.setViewVisible()
                    binding.llButtonSubscribe.setViewVisible()
                    binding.llButtonUpgrade.setViewGone()
                    binding.llButtonSubscribe.setOnClickListener {
                        navigatePlanPurchase("")
                    }
                }
            }

        }


    }

    private fun navigatePlanPurchase(type: String) {
        val bundle = Bundle()
        bundle.putString("type", type)
        findNavController().navigate(com.i69.R.id.action_global_plan, bundle)
    }
}