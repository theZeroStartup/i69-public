package com.i69.ui.screens.interest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import com.i69.data.config.Constants.EXTRA_INTEREST_TYPE
import com.i69.data.config.Constants.EXTRA_INTEREST_VALUE
import com.i69.databinding.ActivityContainerBinding
import com.i69.ui.base.BaseActivity

fun getInterestsListActivityIntent(context: Context, interestsType: Int, interestedInValues: List<String>) =
    Intent(context, InterestListActivity::class.java).apply {
        val arrayList = ArrayList<String>()
        arrayList.addAll(interestedInValues)

        putExtra(EXTRA_INTEREST_TYPE, interestsType)
        putExtra(EXTRA_INTEREST_VALUE, arrayList)
    }

class InterestListActivity : BaseActivity<ActivityContainerBinding>() {

    override fun getActivityBinding(inflater: LayoutInflater) = ActivityContainerBinding.inflate(inflater)

    override fun setupTheme(savedInstanceState: Bundle?) {
        transact(
            InterestsListFragment.newInstance(
                intent?.extras?.getInt(EXTRA_INTEREST_TYPE)!!,
                intent?.extras?.getStringArrayList(EXTRA_INTEREST_VALUE)!!
            )
        )
    }

    override fun setupClickListeners() {
    }

}