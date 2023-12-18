package com.i69.ui.base.search

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.apollographql.apollo3.exception.ApolloException
import com.i69.GetNotificationCountQuery
import com.i69.R
import com.i69.databinding.FragmentSearchInterestedInBinding
import com.i69.ui.adapters.SearchInterestedServerAdapter
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.screens.main.notification.NotificationDialogFragment
import com.i69.ui.viewModels.UserViewModel
import com.i69.utils.apolloClient
import com.i69.utils.snackbar
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class BaseSearchFragment : BaseFragment<FragmentSearchInterestedInBinding>(), SearchInterestedServerAdapter.SearchInterestedListener {

    private var userToken: String? = null
    private var userId: String? = null


    companion object {
        var showAnim = true
    }
    val mViewModel: UserViewModel by viewModels()
    protected val viewModel: UserViewModel by activityViewModels()
    protected lateinit var adapter: SearchInterestedServerAdapter

    abstract fun setScreenTitle()

    abstract fun setupChiledTheme()

    abstract fun initDrawerStatus()

    abstract fun getItems(): List<SearchInterestedServerAdapter.MenuItemString>

    abstract fun onAdapterItemClick(pos: Int)


    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentSearchInterestedInBinding.inflate(inflater, container, false)

    override fun setupTheme() {
        setScreenTitle()
        Log.e("ffff","111")
        navController = findNavController()
        Log.e("ffff","222")
                lifecycleScope.launch {
            userToken = getCurrentUserToken()!!
                    userId = getCurrentUserId()!!
                    Log.e("ffff","222")
                    Timber.i("usertokenn $userToken")
        }
        Log.e("ffff","222")
        adapter = SearchInterestedServerAdapter(0, getAnim(), this)
        showAnim = false
//        adapter.setItems(getItems())
        binding.searchChoiceItems.adapter = adapter
        setupChiledTheme()
        initDrawerStatus()
    }

        override fun onResume() {
        getNotificationIndex()
        super.onResume()
    }
    private fun getNotificationIndex() {

        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetNotificationCountQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception NotificationIndex${e.message}")
                binding.root.snackbar(" ${e.message}")
                return@launchWhenResumed
            }
            Timber.d("apolloResponse NotificationIndex ${res.hasErrors()}")
Log.e("hhhhhh","1111")


            val NotificationCount = res.data?.unseenCount
            Log.e("hhhhhh","eeeeee")
            if(NotificationCount==null || NotificationCount == 0)
            {
                binding.counter.visibility = View.GONE
                binding.bell.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.notification1))

            }
            else
            {
                binding.counter.visibility = View.VISIBLE
//                binding.counter.setText(""+NotificationCount)
                if(NotificationCount>10){
                    binding.counter.text = "9+"
                }else {
                    binding.counter.text = "" + NotificationCount
                }
                binding.bell.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.notification2))

            }


        }
    }
    override fun setupClickListeners() {
        binding.toolbarHamburger.setOnClickListener {
            getMainActivity().drawerSwitchState()

        }

        binding.bell.setOnClickListener {
            val dialog = NotificationDialogFragment(
                userToken,
                binding.counter,
                userId,
                binding.bell
            )
//            dialog.show(childFragmentManager, "${ requireActivity().resources.getString(R.string.notifications) }")
            getMainActivity()?.notificationDialog(dialog,childFragmentManager,"${requireActivity().resources.getString(R.string.notifications)}")

        }
    }

    override fun onViewClick(pos: Int) {
        onAdapterItemClick(pos)
    }

    protected open fun getAnim(): Boolean = showAnim

    fun getMainActivity() = activity as MainActivity

}