package com.congnguyencn.kmpstreamtv.feature.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.FragmentHomeTabBinding
import com.congnguyencn.kmpstreamtv.feature.placeholder.PlaceholderFragment
import com.congnguyencn.kmpstreamtv.feature.profile.UserProfileActivity
import com.congnguyencn.kmpstreamtv.feature.selection.ItemSelectionDialogFragment
import com.congnguyencn.kmpstreamtv.feature.selection.SelectionItem

class HomeTabFragment : Fragment(R.layout.fragment_home_tab) {
    private var bindingRef: FragmentHomeTabBinding? = null
    private val binding get() = requireNotNull(bindingRef)
    private lateinit var categories: List<HomeCategory>

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        bindingRef = FragmentHomeTabBinding.bind(view)

        categories =
            listOf(
                HomeCategory("home", getString(R.string.category_home)),
                HomeCategory("movies", getString(R.string.category_movies)),
                HomeCategory("series", getString(R.string.category_series)),
                HomeCategory("live", getString(R.string.category_live)),
                HomeCategory("more", getString(R.string.category_more)),
            )
        val restoredCategory =
            childFragmentManager
                .findFragmentById(R.id.fragmentContainerHomeTab)
                ?.tag
                ?.removePrefix(CATEGORY_TAG_PREFIX)
                ?: "home"
        binding.layoutTopCategories.submit(
            items = categories,
            selected = restoredCategory,
            onSelected = ::showCategory,
            onMoreSelected = ::showCategoryPicker,
        )
        binding.btnSearch.setOnClickListener { showUnavailable(R.string.search_coming_soon) }
        binding.btnNotification.setOnClickListener { showUnavailable(R.string.notifications_coming_soon) }
        binding.btnProfile.setOnClickListener {
            startActivity(Intent(requireContext(), UserProfileActivity::class.java))
        }
        binding.ivLogo.setOnClickListener { showCategory(categories.first()) }
        binding.tvMainMenu.setOnClickListener { showCategoryPicker() }

        updateMenuBar(restoredCategory)

        if (childFragmentManager.findFragmentById(R.id.fragmentContainerHomeTab) == null) {
            showCategory(HomeCategory("home", getString(R.string.category_home)))
        }
    }

    fun updateToolbarForScroll(offset: Int) {
        if (bindingRef == null) return
        val highlightOffset = resources.getDimensionPixelSize(R.dimen.highlight_topbar_offset).toFloat()
        binding.ivTopBarBehind.alpha = (offset / highlightOffset).coerceIn(0f, 1f)
    }

    private fun showCategory(category: HomeCategory) {
        if (category.id == MORE_CATEGORY_ID) {
            showCategoryPicker()
            return
        }
        val tag = "$CATEGORY_TAG_PREFIX${category.id}"
        if (childFragmentManager.findFragmentById(R.id.fragmentContainerHomeTab)?.tag == tag) return
        val fragment =
            if (category.id == "home") {
                HomeFragment()
            } else {
                PlaceholderFragment.newInstance(
                    title = category.title,
                    description = getString(R.string.placeholder_home_category, category.title),
                )
            }
        childFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainerHomeTab, fragment, tag)
            .commit()
        updateMenuBar(category.id)
    }

    private fun updateMenuBar(categoryId: String) {
        val isHome = categoryId == categories.first().id
        binding.layoutTopCategories.isVisible = isHome
        binding.layoutSubMenu.isVisible = !isHome
        binding.tvMainMenu.text = categories.firstOrNull { it.id == categoryId }?.title.orEmpty()
    }

    private fun showCategoryPicker() {
        val selectedId =
            childFragmentManager
                .findFragmentById(R.id.fragmentContainerHomeTab)
                ?.tag
                ?.removePrefix(CATEGORY_TAG_PREFIX)
        val selectableCategories = categories.filterNot { it.id == MORE_CATEGORY_ID }
        val selectionItems =
            selectableCategories.map { category ->
                SelectionItem(
                    id = category.id,
                    title = category.title,
                    isSelected = category.id == selectedId,
                )
            }
        ItemSelectionDialogFragment
            .show(childFragmentManager, selectionItems)
            .apply {
                callback = { selectedItem ->
                    selectableCategories
                        .firstOrNull { it.id == selectedItem.id }
                        ?.let(::showCategory)
                }
            }
    }

    private fun showUnavailable(message: Int) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        bindingRef = null
        super.onDestroyView()
    }

    private companion object {
        const val CATEGORY_TAG_PREFIX = "home-category-"
        const val MORE_CATEGORY_ID = "more"
    }
}
