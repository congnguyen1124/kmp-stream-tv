package com.congnguyencn.kmpstreamtv

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.congnguyencn.kmpstreamtv.databinding.ActivityMainBinding
import com.congnguyencn.kmpstreamtv.feature.home.HomeTabFragment
import com.congnguyencn.kmpstreamtv.feature.placeholder.PlaceholderFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bottomNavMain.isItemActiveIndicatorEnabled = false

        binding.bottomNavMain.setOnItemSelectedListener { item ->
            showDestination(item.itemId)
            true
        }
        binding.bottomNavMain.selectedItemId = savedInstanceState
            ?.getInt(STATE_SELECTED_DESTINATION, R.id.home)
            ?: R.id.home
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SELECTED_DESTINATION, binding.bottomNavMain.selectedItemId)
        super.onSaveInstanceState(outState)
    }

    private fun showDestination(@IdRes destinationId: Int) {
        val tag = destinationId.toString()
        val existing = supportFragmentManager.findFragmentByTag(tag)
        val target = existing ?: createDestination(destinationId)

        supportFragmentManager.beginTransaction().apply {
            supportFragmentManager.fragments.forEach(::hide)
            if (existing == null) {
                add(R.id.fragmentContainerMain, target, tag)
            } else {
                show(target)
            }
            setPrimaryNavigationFragment(target)
        }.commitNow()
    }

    private fun createDestination(@IdRes destinationId: Int): Fragment = when (destinationId) {
        R.id.home -> HomeTabFragment()
        R.id.tvcab -> PlaceholderFragment.newInstance(
            title = getString(R.string.nav_music),
            description = getString(R.string.placeholder_music),
        )
        R.id.shorts -> PlaceholderFragment.newInstance(
            title = getString(R.string.nav_shorts),
            description = getString(R.string.placeholder_shorts),
        )
        R.id.playlist -> PlaceholderFragment.newInstance(
            title = getString(R.string.nav_playlist),
            description = getString(R.string.placeholder_playlist),
        )
        else -> HomeTabFragment()
    }

    private companion object {
        const val STATE_SELECTED_DESTINATION = "selected_destination"
    }
}
