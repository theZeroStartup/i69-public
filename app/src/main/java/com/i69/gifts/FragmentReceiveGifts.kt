package com.i69.gifts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.apollographql.apollo3.exception.ApolloException
import com.i69.GetUserReceiveGiftQuery
import com.i69.data.models.ModelGifts
import com.i69.databinding.FragmentRealGiftsBinding
import com.i69.ui.adapters.AdapterGifts
import com.i69.ui.base.BaseFragment
import com.i69.utils.apolloClient
import com.i69.utils.snackbar
import timber.log.Timber

class FragmentReceiveGifts(val userId: String? = null) :
    BaseFragment<FragmentRealGiftsBinding>() {

    var giftsAdapter: AdapterGifts? = null

    var list: MutableList<ModelGifts.Data.AllRealGift> = mutableListOf()

    //private val viewModel: UserViewModel by activityViewModels()
    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentRealGiftsBinding.inflate(inflater, container, false)

    override fun setupTheme() {
        giftsAdapter = AdapterGifts(requireContext(), list)
        binding.recyclerViewGifts.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerViewGifts.setHasFixedSize(true)
        binding.recyclerViewGifts.adapter = giftsAdapter
        showProgressView()
        getReceivedGiftIndex()
    }

    override fun setupClickListeners() {}

    private fun getReceivedGiftIndex() {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(
                    requireContext(),
                    userId!!
                ).query(GetUserReceiveGiftQuery(receiverId = userId))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception getGiftIndex${e.message}")
                binding.root.snackbar(" ${e.message}")
                return@launchWhenResumed
            }
            Timber.d("apolloResponse getGiftIndex ${res.hasErrors()}")

            val receiveGiftList = res.data?.allUserGifts?.edges
            res.data?.allUserGifts?.edges?.forEach { it ->
                Timber.d("apolloResponse getGiftIndex ${it?.node?.gift?.giftName}")
            }
            list.clear()
            receiveGiftList?.forEach { edge ->
                edge?.node?.gift?.let {
                    list.add(
                        ModelGifts.Data.AllRealGift(
                            id = it.id,
                            cost = it.cost,
                            giftName = it.giftName,
                            picture = it.picture, type = it.type.rawValue
                        )
                    )
                }
            }
            giftsAdapter?.notifyDataSetChanged()
            hideProgressView()
        }
    }
}