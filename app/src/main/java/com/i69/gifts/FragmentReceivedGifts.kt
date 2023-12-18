package com.i69.gifts

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.apollographql.apollo3.exception.ApolloException
import com.i69.GetReceivedGiftsQuery
import com.i69.R
import com.i69.databinding.FragmentReceivedSentGiftsBinding
import com.i69.ui.adapters.AdapterReceiveGifts
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.SplashActivity
import com.i69.ui.screens.main.search.userProfile.PicViewerFragment
import com.i69.ui.screens.main.search.userProfile.SearchUserProfileFragment
import com.i69.utils.AnimationTypes
import com.i69.utils.apolloClient
import com.i69.utils.navigate
import com.i69.utils.snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

class FragmentReceivedGifts: BaseFragment<FragmentReceivedSentGiftsBinding>(),AdapterReceiveGifts.ReceivedGiftPicUserPicInterface{
    var giftsAdapter: AdapterReceiveGifts ?= null
    var list : MutableList<GetReceivedGiftsQuery.Edge?> = mutableListOf()

    private var userId: String? = null
    private var userToken: String? = null

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentReceivedSentGiftsBinding.inflate(inflater, container, false)

    override fun setupTheme() {

        lifecycleScope.launch {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            Timber.i("usertokenn $userToken")
        }
        Timber.i("userID $userId")
        Log.e("currentUserId","userID :$userId")

        giftsAdapter = AdapterReceiveGifts(requireContext(),this@FragmentReceivedGifts, list)
        binding.recyclerViewGifts.setHasFixedSize(true)
        binding.recyclerViewGifts.adapter = giftsAdapter

        loadGifts()
    }

    fun loadGifts() {

//        lifecycleScope.launchWhenResumed {
        lifecycleScope.launchWhenStarted {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetReceivedGiftsQuery(userId!!)).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception received gift ${e.message}")
                Log.e("FragmentReceivedGift", "${e.message}")
                Log.e("FragmentReceivedGift111", "${e.message}")
                binding.root.snackbar(" ${e.message}")
                hideProgressView()
//                return@launchWhenResumed
                return@launchWhenStarted
            }
            if(res.hasErrors())
            {

                if(JSONObject(res.errors!!.get(0).toString()).getString("code").equals("InvalidOrExpiredToken"))
                {
                    // error("User doesn't exist")

                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        requireActivity().finishAffinity()
                    }                       }
            }

            val allreceivedgift = res.data?.allUserGifts!!.edges
            if(allreceivedgift.size!=0) {
                list.clear()
                list.addAll(allreceivedgift)
                giftsAdapter?.notifyDataSetChanged()
            }

            if (binding.recyclerViewGifts.itemDecorationCount == 0) {
                binding.recyclerViewGifts.addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.top = 10
                    }
                })
            }

        }
    }

    override fun setupClickListeners() {

    }

    override fun onpiclicked(url: String) {


        val dialog = PicViewerFragment()
        val b = Bundle()
        b.putString("url", url)
        b.putString("mediatype", "image")

        dialog.arguments = b
        dialog.show(childFragmentManager, "GiftpicViewer")
    }

    override fun onuserpiclicked(userid: String) {
        val bundle = Bundle()
        bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
        bundle.putString("userId", userid)
        findNavController().navigate(
            destinationId = R.id.action_global_otherUserProfileFragment,
            popUpFragId = null,
            animType = AnimationTypes.SLIDE_ANIM,
            inclusive = true,
            args = bundle
        )
    }


}