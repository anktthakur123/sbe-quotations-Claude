package com.sidhbaba.quotemaker

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sidhbaba.quotemaker.ui.CreateFragment
import com.sidhbaba.quotemaker.ui.CustomerMasterFragment
import com.sidhbaba.quotemaker.ui.HistoryFragment
import com.sidhbaba.quotemaker.ui.SettingsFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 4
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> CreateFragment()
        1 -> CustomerMasterFragment()
        2 -> HistoryFragment()
        else -> SettingsFragment()
    }

    companion object {
        val TITLES = listOf("Create", "Customer Master", "History", "Settings")
    }
}
