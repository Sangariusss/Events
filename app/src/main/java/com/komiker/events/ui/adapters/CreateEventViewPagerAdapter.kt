package com.komiker.events.ui.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.komiker.events.ui.fragments.CreateEventImagesFragment
import com.komiker.events.ui.fragments.CreateEventInfoFragment
import com.komiker.events.ui.fragments.CreateEventOtherFragment

class CreateEventViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CreateEventInfoFragment()
            1 -> CreateEventImagesFragment()
            2 -> CreateEventOtherFragment()
            else -> CreateEventInfoFragment()
        }
    }
}