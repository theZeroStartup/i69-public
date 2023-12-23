package com.i69.ui.base

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.view.*
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.i69.R
import com.i69.data.config.Constants
import com.i69.data.preferences.UserPreferences
import com.i69.firebasenotification.NotificationBroadcast
import com.i69.singleton.App
import com.i69.utils.createLoadingDialog
import kotlinx.coroutines.flow.first


abstract class BaseFragment<dataBinding : ViewDataBinding> : Fragment() {


    protected lateinit var userPreferences: UserPreferences
    protected lateinit var binding: dataBinding
    lateinit var loadingDialog: Dialog
    lateinit var navController: NavController
    private var broadcast: NotificationBroadcast? = null

    var hasInitializedRootView = false
    private var rootView: View? = null

//    fun getPersistentView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?,
////        layout: Int
//    ): View? {
//        if (rootView == null) {
//            userPreferences = App.userPreferences
//            setStatusBarColor(getStatusBarColor())
//            binding = getFragmentBinding(inflater, container)
//            val contentView = binding.root
//            broadcast = NotificationBroadcast(this);
//            contentView.findViewById<View>(R.id.actionBack)?.setOnClickListener {
//                //activity?.onBackPressed()
//                //activity?.finishAfterTransition()
//                //getMainActivity()?.binding?.bottomNavigation?.selectedItemId= R.id.nav_search_graph
//                findNavController().popBackStack()
//            }
//            // Inflate the layout for this fragment
//            rootView = binding.root
//        } else {
//            // Do not inflate the layout again.
//            // The returned View of onCreateView will be added into the fragment.
//            // However it is not allowed to be added twice even if the parent is same.
//            // So we must remove rootView from the existing parent view group
//            // (it will be added back).
//            (rootView?.getParent() as? ViewGroup)?.removeView(rootView)
//        }
//
//        return rootView
//    }


//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? = getPersistentView(inflater, container, savedInstanceState)
//    ): View? = getPersistentView(inflater, container, savedInstanceState, getFragmentView())


    //this method is used to get the fragment layout file
//    abstract fun getFragmentView(): Int

    override fun onAttach(context: Context) {
        super.onAttach(context)
        userPreferences = App.userPreferences
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setStatusBarColor(getStatusBarColor())
        binding = getFragmentBinding(inflater, container)
        val contentView = binding.root
        broadcast = NotificationBroadcast(this);
        contentView.findViewById<View>(R.id.actionBack)?.setOnClickListener {
            //activity?.onBackPressed()
            //activity?.finishAfterTransition()
            //getMainActivity()?.binding?.bottomNavigation?.selectedItemId= R.id.nav_search_graph
            findNavController().popBackStack()
        }

        loadingDialog = requireContext().createLoadingDialog()
        binding.apply {
            lifecycleOwner = this@BaseFragment
        }

        setupTheme()
        setupClickListeners()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    abstract fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?): dataBinding

    abstract fun setupTheme()

    abstract fun setupClickListeners()

    suspend fun getCurrentUserId() = userPreferences.userId.first()

    suspend fun getCurrentUserName() = userPreferences.userName.first()

    suspend fun getCurrentUserToken() = userPreferences.userToken.first()

    suspend fun getChatUserId() = userPreferences.chatUserId.first()

    open fun getStatusBarColor() = R.color.colorPrimaryDark

    fun setStatusBarColor(@ColorRes color: Int) {
        val window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(requireActivity(), color)
    }

    protected fun showProgressView() {
        if (loadingDialog != null && !loadingDialog.isShowing) {
            loadingDialog.show()
        }
    }

    protected fun hideProgressView() {
        if (loadingDialog != null) {
            loadingDialog.dismiss()
        }
    }

    protected fun <T : Activity> getTypeActivity(): T? {
        return if (activity != null) activity as T else null
    }

    fun moveTo(direction: Int, args: Bundle? = null) =
        view?.findNavController()?.navigate(direction, args)

    fun moveTo(direction: NavDirections) = view?.findNavController()?.navigate(direction)

    open fun moveUp() = view?.findNavController()?.navigateUp()
    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(broadcast!!);
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            broadcast!!, IntentFilter(Constants.INTENTACTION)
        );
    }

}