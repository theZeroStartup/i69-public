package com.i69.gifts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.exception.ApolloException
import com.i69.GiftPurchaseMutation
import com.i69.data.models.ModelGifts
import com.i69.di.modules.AppModule
import com.i69.R
import com.i69.databinding.FragmentGiftsBinding
import com.i69.ui.base.profile.BaseGiftsFragment
import com.i69.utils.apolloClient
import com.i69.utils.getResponse
import com.i69.utils.snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class FragmentGifts: BaseGiftsFragment() {

    var purchaseGiftFor: String?=""

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentGiftsBinding.inflate(inflater, container, false)

    override fun setupClickListeners() {
        binding.purchaseButton.setOnClickListener {
            var items: MutableList<ModelGifts.Data.AllRealGift> = mutableListOf()
            fragVirtualGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }
            fragRealGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }

            lifecycleScope.launchWhenCreated() {
                if (items.size > 0) {
                    showProgressView()
                    items.forEach { gift ->

                        var res: ApolloResponse<GiftPurchaseMutation.Data>? = null
                        try {
                            res = apolloClient(
                                requireContext(),
                                getCurrentUserToken()!!
                            ).mutation(GiftPurchaseMutation(gift.id,purchaseGiftFor!!, getCurrentUserId()!!)).execute()
                        } catch (e: ApolloException) {
                            Timber.d("apolloResponse Exception ${e.message}")
                            binding.root.snackbar(" ${e.message}")
                            //hideProgressView()
                            //return@launchWhenResumed
                        }
                        if (res?.hasErrors() == false) {
                            binding.root.snackbar(context?.resources?.getString(R.string.you_bought)+" ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} "+context?.resources?.getString(
                                R.string.successfully))
                            //fireGiftBuyNotificationforreceiver(gift.id)

                        }
                        if(res!!.hasErrors())
                        {
                            binding.root.snackbar(""+ res.errors!![0].message)

                        }
                        Timber.d("apolloResponse ${res.hasErrors()} ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName}")
                    }
                    hideProgressView()
                }
            }
        }
    }
    fun fireGiftBuyNotificationforreceiver(gid: String) {

        lifecycleScope.launchWhenResumed {


            val queryName = "sendNotification"
            val query = StringBuilder()
                .append("mutation {")
                .append("$queryName (")
                .append("userId: \"${purchaseGiftFor!!}\", ")
                .append("notificationSetting: \"GIFT\", ")
                .append("data: {giftId:${gid}}")
                .append(") {")
                .append("sent")
                .append("}")
                .append("}")
                .toString()

            val result= AppModule.provideGraphqlApi().getResponse<Boolean>(
                query,
                queryName, getCurrentUserToken()!!)
            Timber.d("RSLT",""+result.message)

        }








    }
    override fun setupScreen() {
        purchaseGiftFor = requireArguments().getString("userId")
    }
}