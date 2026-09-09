package com.congnguyencn.kmpstreamtv.feature.placeholder

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.FragmentPlaceholderBinding

class PlaceholderFragment : Fragment(R.layout.fragment_placeholder) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentPlaceholderBinding.bind(view)
        binding.placeholderTitle.text = requireArguments().getString(ARG_TITLE)
        binding.placeholderDescription.text = requireArguments().getString(ARG_DESCRIPTION)
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"

        fun newInstance(title: String, description: String) = PlaceholderFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_DESCRIPTION, description)
            }
        }
    }
}
