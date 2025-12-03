package com.example.moodpress.feature.stats.presentation.view.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.moodpress.feature.stats.presentation.view.fragment.MonthlyStatsFragment
import com.example.moodpress.feature.stats.presentation.view.fragment.WeeklyStatsFragment

// THAY ĐỔI: Nhận vào 'fragment: Fragment' thay vì 'fragmentActivity: FragmentActivity'
class StatsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WeeklyStatsFragment()
            1 -> MonthlyStatsFragment()
            else -> WeeklyStatsFragment()
        }
    }
}