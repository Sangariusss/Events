package com.komiker.events.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.komiker.events.data.database.models.Event
import com.komiker.events.ui.fragments.EventDetailDescriptionFragment
import com.komiker.events.ui.fragments.EventDetailImagesFragment
import com.komiker.events.ui.fragments.EventDetailLocationFragment

class EventDetailPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val event: Event
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> EventDetailDescriptionFragment.newInstance(event)
            1 -> EventDetailImagesFragment.newInstance(event)
            2 -> EventDetailLocationFragment.newInstance(event)
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}