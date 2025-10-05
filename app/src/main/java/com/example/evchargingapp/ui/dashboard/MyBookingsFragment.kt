package com.example.evchargingapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.evchargingapp.R
import com.example.evchargingapp.data.local.BookingStatus
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MyBookingsFragment : Fragment() {
    
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_bookings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupViewPager()
        setupTabLayout()
    }
    
    private fun initViews(view: View) {
        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)
    }
    
    private fun setupViewPager() {
        val adapter = BookingsPagerAdapter(requireActivity())
        viewPager.adapter = adapter
    }
    
    private fun setupTabLayout() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Pending"
                1 -> "Approved"
                2 -> "Past"
                else -> ""
            }
        }.attach()
    }
    
    private class BookingsPagerAdapter(fragmentActivity: FragmentActivity) : 
        FragmentStateAdapter(fragmentActivity) {
        
        override fun getItemCount(): Int = 3
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> BookingListFragment.newInstance(BookingStatus.PENDING)
                1 -> BookingListFragment.newInstance(BookingStatus.APPROVED)
                2 -> BookingListFragment.newInstance(BookingStatus.PAST)
                else -> BookingListFragment.newInstance(BookingStatus.PENDING)
            }
        }
    }
}