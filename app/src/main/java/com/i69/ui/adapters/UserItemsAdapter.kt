package com.i69.ui.adapters

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class UserItemsAdapter(manager: FragmentManager) : FragmentStatePagerAdapter(manager) {

    private val items = arrayListOf<Pair<String, Fragment>>()

    override fun getItem(position: Int): Fragment = items[position].second

    override fun getCount(): Int = items.size

    override fun getPageTitle(position: Int): CharSequence = items[position].first

    fun addFragItem(fr: Fragment, title: String) {
        Log.d("TAG", "addFragItem: "+title)
        items.add(Pair(title, fr))
    }
}