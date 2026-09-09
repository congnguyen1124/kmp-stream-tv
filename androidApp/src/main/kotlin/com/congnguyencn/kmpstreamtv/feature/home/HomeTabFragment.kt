package com.congnguyencn.kmpstreamtv.feature.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.FragmentHomeTabBinding
import com.congnguyencn.kmpstreamtv.feature.placeholder.PlaceholderFragment

class HomeTabFragment : Fragment(R.layout.fragment_home_tab) {
    private var _binding: FragmentHomeTabBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeTabBinding.bind(view)

        val categories = listOf(
            HomeCategory("home", getString(R.string.category_home)),
            HomeCategory("movies", getString(R.string.category_movies)),
            HomeCategory("series", getString(R.string.category_series)),
            HomeCategory("live", getString(R.string.category_live)),
            HomeCategory("more", getString(R.string.category_more)),
        )
        val restoredCategory = childFragmentManager.findFragmentById(R.id.fragmentContainerHome)
            ?.tag
            ?.removePrefix(CATEGORY_TAG_PREFIX)
            ?: "home"
        binding.categories.submit(
            items = categories,
            selected = restoredCategory,
            onSelected = ::showCategory,
        )
        binding.search.setOnClickListener { showUnavailable(R.string.search_coming_soon) }
        binding.notifications.setOnClickListener { showUnavailable(R.string.notifications_coming_soon) }
        binding.profile.setOnClickListener { showUnavailable(R.string.profile_coming_soon) }

        if (childFragmentManager.findFragmentById(R.id.fragmentContainerHome) == null) {
            showCategory(HomeCategory("home", getString(R.string.category_home)))
        }
    }

    fun updateToolbarForScroll(offset: Int) {
        if (_binding == null) return
        val fraction = (offset / 180f).coerceIn(0f, 1f)
        binding.topBarScrim.alpha = 0.32f + (0.68f * fraction)
        binding.topBarDivider.isVisible = fraction > 0.75f
    }

    private fun showCategory(category: HomeCategory) {
        val tag = "$CATEGORY_TAG_PREFIX${category.id}"
        if (childFragmentManager.findFragmentById(R.id.fragmentContainerHome)?.tag == tag) return
        val fragment = if (category.id == "home") {
            HomeFragment()
        } else {
            PlaceholderFragment.newInstance(
                title = category.title,
                description = getString(R.string.placeholder_home_category, category.title),
            )
        }
        childFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerHome, fragment, tag)
            .commit()
    }

    private fun showUnavailable(message: Int) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val CATEGORY_TAG_PREFIX = "home-category-"
    }
}
