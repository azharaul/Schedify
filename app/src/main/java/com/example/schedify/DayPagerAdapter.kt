package com.example.schedify

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class DayPagerAdapter(activity: MainActivity) : FragmentStateAdapter(activity) {
    private val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

    override fun getItemCount(): Int = days.size

    override fun createFragment(position: Int): Fragment {
        return DayScheduleFragment.newInstance(days[position])
    }

    fun getDayAt(position: Int): String = days[position]
}

