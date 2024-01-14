package com.i69.ui.screens.main.subscription

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollographql.apollo3.exception.ApolloException
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.i69.*
import com.i69.applocalization.AppStringConstant1
import com.i69.applocalization.AppStringConstantViewModel
import com.i69.data.models.BaseAllPackageModel
import com.i69.R
import com.i69.databinding.SheetSubscriptionFragmentBinding
import com.i69.ui.adapters.PurchasePlanMainAdapter
import com.i69.ui.viewModels.SearchViewModel
import com.i69.utils.AnimationTypes
import com.i69.utils.apolloClient
import com.i69.utils.navigate
import com.i69.utils.snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber


@AndroidEntryPoint
class SubscriptionBSDialogFragment : BottomSheetDialogFragment() {

    private val mViewModel: SearchViewModel by activityViewModels()

    private lateinit var binding: SheetSubscriptionFragmentBinding
    var navController: NavController? = null

    var amount = 0.0

    private lateinit var purchasePlanMainAdapter: PurchasePlanMainAdapter

    var selectedPackageName = ""
    var selectedPackageId = 0

    var selectedPlanTitle = ""
    var selectedPlanID = 0
    private val viewStringConstModel: AppStringConstantViewModel by activityViewModels()

    var userToken: String? = ""
    var userId: String? = ""
    var purchaseCoinMessage: String? = ""

    companion object {
        fun newInstance(userToken: String?,
                        userId: String?,
                        purchaseCoinMessage: String?): SubscriptionBSDialogFragment {
            val args = Bundle()
            args.putString("userToken",userToken)
            args.putString("userId",userId)
            args.putString("purchaseCoinMessage",purchaseCoinMessage)
            val fragment = SubscriptionBSDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userToken = arguments?.getString("userToken")
        userId = arguments?.getString("userId")
        purchaseCoinMessage = arguments?.getString("purchaseCoinMessage")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = SheetSubscriptionFragmentBinding.inflate(inflater, container, false)
        viewStringConstModel.data.observe(this@SubscriptionBSDialogFragment) { data ->
            binding.stringConstant = data

        }
        viewStringConstModel.data.also {
            binding.stringConstant = it.value
        }
        setupTheme()
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), com.i69.R.style.MyBottomSheetDialogTheme)
    }

    fun setupTheme() {
        navController = findNavController()

        binding.icClose.setOnClickListener {
            dismiss()
        }

        binding.tvPurchaseUsingCoin.setOnClickListener {
            updateSearchResultWithCoin()

        }

        binding.purchaseUsingCoin.text = purchaseCoinMessage
        binding.llComparePlan.setOnClickListener {
            if (selectedPackageName.isNotEmpty()) {
                val bundle = Bundle()
                bundle.putInt("selectedPlanID", selectedPlanID)
                bundle.putString("selectedPlanTitle", selectedPlanTitle)
                bundle.putInt("selectedPackageId", selectedPackageId)
                bundle.putString("selectedPackageName", selectedPackageName)
                bundle.putBoolean("itisFromSubScriptionDialog", true)

                findNavController().navigate(
                    destinationId = R.id.action_global_plan_detail,
                    popUpFragId = null,
                    animType = AnimationTypes.SLIDE_ANIM,
                    inclusive = true,
                    args = bundle

                )
            }
        }
        getAllPackage()
    }

    fun updateSearchResultWithCoin() {
        var searchResutModel = mViewModel.getSearchRequest()

        if (searchResutModel != null) {
            Log.d("ExtraSearchCalls", "updateSearchResultWithCoin: ")
            mViewModel.getSearchUsers(
                _searchRequest = searchResutModel,
                token = userToken!!,
                autoDeductCoin = 1,
                context = requireContext()
            ) { error ->
                if (error == null) {
                    mViewModel.updateSearchResultWithCoin()
                    dismiss()
                } else {
                    if (error!!.contains(getString(R.string.no_enough_coins))) {
                        binding.root.snackbar(
                            AppStringConstant1.dont_have_enough_coin_upgrade_plan,
                            Snackbar.LENGTH_INDEFINITE,
                            callback = {
                                findNavController().navigate(
                                    destinationId = R.id.actionGoToPurchaseFragment,
                                    popUpFragId = null,
                                    animType = AnimationTypes.SLIDE_ANIM,
                                    inclusive = false,

                                    )
                            })
                    } else {
                        binding.root.snackbar(error)
                    }
                }
            }
        }
    }

    fun getAllPackage() {
        lifecycleScope.launchWhenResumed {
            val response = try {
                apolloClient(requireContext(), userToken!!).query(
                    GetAllPackagesQuery()
                ).execute()

            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                return@launchWhenResumed
            }

            if (response.hasErrors()) {
                val errorMessage = response.errors?.get(0)?.message
                Log.e("errorAllPackage", "$errorMessage")

                if (errorMessage != null) {
                    binding.root.snackbar(errorMessage)
                }
            } else {
                selectedPackageName = response.data!!.allPackages!!.get(0)!!.name
                selectedPackageId = response.data!!.allPackages!!.get(0)!!.id.toInt()

                binding.recyclerViewPlan.layoutManager = LinearLayoutManager(context)
                val list = arrayListOf<BaseAllPackageModel>()
                response.data!!.allPackages!!.forEach {
                    list.add(BaseAllPackageModel(false,it))
                }
                purchasePlanMainAdapter = PurchasePlanMainAdapter(requireContext(), list,viewStringConstModel.data.value ,object : PurchasePlanMainAdapter.PlanInterface {

                    override fun onSubscribeClick(
                        selectedPackageId: Int,
                        selectedPackageName: String,
                        selectedPlanID: Int,
                        selectedPlanTitle: String
                    ) {
                        this@SubscriptionBSDialogFragment.selectedPackageId = selectedPackageId
                        this@SubscriptionBSDialogFragment.selectedPackageName = selectedPackageName
                        this@SubscriptionBSDialogFragment.selectedPlanID = selectedPlanID
                        this@SubscriptionBSDialogFragment.selectedPlanTitle = selectedPlanTitle
                        purchaseSubScription(selectedPlanID)
                    }
                })
                binding.recyclerViewPlan.adapter = purchasePlanMainAdapter
            }
        }
    }

    fun purchaseSubScription(selectedPlanId: Int) {

        lifecycleScope.launchWhenResumed {
            val response = try {
                apolloClient(requireContext(), userToken!!).query(
                    UserSubscriptionQuery()
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                return@launchWhenResumed
            }
            if (response.hasErrors()) {
                val errorMessage = response.errors?.get(0)?.message
                Log.e("errorAllPackage", "$errorMessage")

                if (errorMessage != null) {
                    binding.root.snackbar(errorMessage)
                }
            } else {
                Log.e("userCurentSubScription", Gson().toJson(response.data))

                if (response.data!!.userSubscription!!.`package` != null) {
                    if (response.data!!.userSubscription!!.`package`!!.name.contains(
                            AppStringConstant1.silver,
                            true
                        )
                    ) {
                        if (response.data!!.userSubscription!!.`package`!!.name.contains(
                                selectedPackageName,
                                true
                            )
                        ) {
                            Log.e("silver", "silver")

                            purchaseSubsription(selectedPlanId)
                        } else if (!response.data!!.userSubscription!!.`package`!!.name.contains(
                                selectedPackageName,
                                true
                            ) &&
                            selectedPackageName.contains(AppStringConstant1.gold, true)
                        ) {
                            Log.e("silver", "gold")
                            upgradeSubsription(selectedPlanId)
                        } else if (!response.data!!.userSubscription!!.`package`!!.name.contains(
                                selectedPackageName,
                                true
                            ) &&
                            selectedPackageName.contains(AppStringConstant1.platinum, true)
                        ) {
                            Log.e("silver", "platinum")
                            upgradeSubsription(selectedPlanId)
                        } else {
                            Log.e("silver", "purchase")
                            purchaseSubsription(selectedPlanId)
                        }
                    } else if (response.data!!.userSubscription!!.`package`!!.name.contains(
                            AppStringConstant1.gold,
                            true
                        )
                    ) {
                        if (response.data!!.userSubscription!!.`package`!!.name.contains(
                                selectedPackageName,
                                true
                            )
                        ) {
                            Log.e("gold", "gold")
                            purchaseSubsription(selectedPlanId)
                        } else if (!response.data!!.userSubscription!!.`package`!!.name.contains(
                                selectedPackageName,
                                true
                            ) &&
                            selectedPackageName.contains(AppStringConstant1.silver, true)
                        ) {


                            Log.e("gold", "silver")
                            downGradeSubsription(selectedPlanId)
                        } else if (!response.data!!.userSubscription!!.`package`!!.name.contains(
                                selectedPackageName,
                                true
                            ) &&
                            selectedPackageName.contains(AppStringConstant1.platinum, true)
//                            selectedPackageTitle.contains("platimum", true)
                        ) {

                            Log.e("gold", "platinum")
                            upgradeSubsription(selectedPlanId)
                        } else {
                            Log.e("gold", "purchase")
                            purchaseSubsription(selectedPlanId)
                        }
                    } else if (response.data!!.userSubscription!!.`package`!!.name.contains(
                            AppStringConstant1.platinum,
                            true
                        )
                    ) {
                        if (response.data!!.userSubscription!!.`package`!!.name.contains(
                                selectedPackageName,
                                true
                            )
                        ) {
                            Log.e("platinum", "platinum")
                            purchaseSubsription(selectedPlanId)
                        } else if (!response.data!!.userSubscription!!.`package`!!.name.contains(
                                selectedPackageName,
                                true
                            ) && selectedPackageName.contains(AppStringConstant1.silver, true)
//                            selectedPackageTitle.contains("silver", true)
                        ) {
                            Log.e("platinum", "silver")
                            downGradeSubsription(selectedPlanId)
                        } else if (!response.data!!.userSubscription!!.`package`!!.name.contains(
                                selectedPackageName,
                                true
                            ) &&
                            selectedPackageName.contains(AppStringConstant1.gold, true)
//                            selectedPackageTitle.contains("gold", true)
                        ) {

                            Log.e("platinum", "gold")
                            downGradeSubsription(selectedPlanId)
                        } else {
                            Log.e("platinum", "purchase")
                            purchaseSubsription(selectedPlanId)
                        }
                    } else {
                        Log.e("callPurchaseInElse", "callPurchaseInElse")
                        purchaseSubsription(selectedPlanId)
                    }
                } else {
                    Log.e("callPurchaseInElseElse", "callPurchaseInElseElse")
                    purchaseSubsription(selectedPlanId)
                }
            }

        }

    }


    fun purchaseSubsription(selectedPlanId: Int) {
        lifecycleScope.launchWhenResumed {

//            val userToken = getCurrentUserToken()!!
            val response = try {
                apolloClient(requireContext(), userToken!!).mutation(
                    PurchasePackageMutation(selectedPackageId, selectedPlanId)
                ).execute()

            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
//                hideProgressView()
                return@launchWhenResumed
            }


            if (response.hasErrors()) {
//                hideProgressView()
                val errorMessage = response.errors?.get(0)?.message
                Log.e("errorAllPackagepurchase", "$errorMessage")

                if (errorMessage != null) {

                    if (errorMessage!!.contains(getString(R.string.no_enough_coins))) {

                        binding.root.snackbar(
                            AppStringConstant1.dont_have_enough_coin_upgrade_plan,
                            Snackbar.LENGTH_INDEFINITE,
                            callback = {
                                findNavController().navigate(
                                    destinationId = R.id.actionGoToPurchaseFragment,
                                    popUpFragId = null,
                                    animType = AnimationTypes.SLIDE_ANIM,
                                    inclusive = true,
                                )
                            })
                    } else {
                        binding.root.snackbar(errorMessage)
                    }
                }
            } else {


                Log.e(
                    "subscriptionBuy",
                    Gson().toJson(response.data)
                )
//                hideProgressView()

                if (response.data!!.purchasePackage!!.success!!) {
                    dismiss()
//                    findNavController().popBackStack()
                }

            }

        }

    }

    fun downGradeSubsription(selectedPlanId: Int) {
        lifecycleScope.launchWhenResumed {

//            val userToken = getCurrentUserToken()!!
            val response = try {
                apolloClient(requireContext(), userToken!!).mutation(
                    DowngradePackageMutation(selectedPackageId)
                ).execute()

            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
//                hideProgressView()
                return@launchWhenResumed
            }


            if (response.hasErrors()) {
//                hideProgressView()
                val errorMessage = response.errors?.get(0)?.message
                Log.e("erroragedownGrade", "$errorMessage")

                if (errorMessage != null) {
                    if (errorMessage!!.contains(getString(R.string.no_enough_coins))) {

                        binding.root.snackbar(
                            AppStringConstant1.dont_have_enough_coin_upgrade_plan,
                            Snackbar.LENGTH_INDEFINITE,
                            callback = {
                                findNavController().navigate(
                                    destinationId = R.id.actionGoToPurchaseFragment,
                                    popUpFragId = null,
                                    animType = AnimationTypes.SLIDE_ANIM,
                                    inclusive = true,
                                )
                            })
                    } else {
                        binding.root.snackbar(errorMessage)
                    }
                }
            } else {


                Log.e(
                    "subscriptionBuy",
                    Gson().toJson(response.data)
                )

//                hideProgressView()
                response.data!!.downgradePackage!!.message?.let { binding.root.snackbar(it) }
                if (response.data!!.downgradePackage!!.success!!) {
//                    findNavController().popBackStack()
                    dismiss()
                }

            }

        }

    }

    fun upgradeSubsription(selectedPlanId: Int) {
        lifecycleScope.launchWhenResumed {

//            val userToken = getCurrentUserToken()!!
            val response = try {
                apolloClient(requireContext(), userToken!!).mutation(
                    UpgradePackageMutation(selectedPackageId)
                ).execute()

            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
//                hideProgressView()
                return@launchWhenResumed
            }


            if (response.hasErrors()) {
//                hideProgressView()
                val errorMessage = response.errors?.get(0)?.message
                Log.e("errorAllPackageupgrade", "$errorMessage")

                if (errorMessage != null) {
                    if (errorMessage!!.contains(getString(R.string.no_enough_coins))) {

                        binding.root.snackbar(
                            AppStringConstant1.dont_have_enough_coin_upgrade_plan,
                            Snackbar.LENGTH_INDEFINITE,
                            callback = {
                                findNavController().navigate(
                                    destinationId = R.id.actionGoToPurchaseFragment,
                                    popUpFragId = null,
                                    animType = AnimationTypes.SLIDE_ANIM,
                                    inclusive = true,
                                )
                            })
                    } else {
                        binding.root.snackbar(errorMessage)
                    }
                }
            } else {


                Log.e(
                    "subscriptionBuy",
                    Gson().toJson(response.data)
                )
//                hideProgressView()

                response.data!!.upgradePackage!!.message?.let { binding.root.snackbar(it) }
                if (response.data!!.upgradePackage!!.success!!) {
                    dismiss()
//                    findNavController().popBackStack()
                }


            }

        }

    }


}