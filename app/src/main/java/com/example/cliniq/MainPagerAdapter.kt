package com.example.cliniq

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/** One page per bottom-bar tab: Home, Appointment, History, Profile. */
class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount() = 4

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> HomeFragment()
        1 -> PlaceholderFragment.newInstance("Appointment")
        2 -> HistoryFragment()
        else -> ProfileFragment()
    }
}
