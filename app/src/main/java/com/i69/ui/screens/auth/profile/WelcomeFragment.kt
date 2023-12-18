package com.i69.ui.screens.auth.profile

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import com.i69.R
import com.i69.databinding.FragmentWelcomeNewBinding
import com.i69.di.modules.AppModule
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.auth.AuthActivity
import com.i69.ui.screens.main.MainActivity
import com.i69.utils.getResponse
import com.i69.utils.startActivity
import kotlinx.coroutines.launch
import org.json.JSONObject

@AndroidEntryPoint
class WelcomeFragment : BaseFragment<FragmentWelcomeNewBinding>() {

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?)= FragmentWelcomeNewBinding.inflate(inflater,container,false)
    private var userId: String? = null
    private var userToken: String? = null
    override fun setupTheme() {
        (activity as AuthActivity).updateStatusBarColor(ContextCompat.getColor(requireActivity(), R.color.colorPrimary))
    }

    override fun setupClickListeners() {
        binding.start.setOnClickListener {
            lifecycleScope.launch {
                Log.e("u id","-->"+getCurrentUserId())
                Log.e("userToken","-->"+getCurrentUserToken())
                userId = getCurrentUserId()!!
                userToken = getCurrentUserToken()!!


                val queryName = "sendNotification"
                val query = StringBuilder()
                    .append("mutation {")
                    .append("$queryName (")
                    .append("userId: \"${userId}\", ")
                    .append("notificationSetting: \"WELCOME\" ")
                  //  .append("data: {momentId:${item!!.node!!.pk}}")
                    .append(") {")
                    .append("sent")
                    .append("}")
                    .append("}")
                    .toString()

                val result= AppModule.provideGraphqlApi().getResponse<JSONObject>(
                    query,
                    queryName, userToken)
                Log.e("resFirstMessage",Gson().toJson(result))

            }

            requireActivity().startActivity<MainActivity>()
            requireActivity().finish()
        }
    }

}