package com.congnguyencn.kmpstreamtv.feature.short

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.FragmentShortActionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

internal enum class ShortMoreAction {
    COPY_LINK,
    NOT_INTERESTED,
    REPORT,
}

internal class ShortActionsBottomSheetFragment : BottomSheetDialogFragment() {
    private var bindingRef: FragmentShortActionsBinding? = null
    private val binding get() = requireNotNull(bindingRef)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingRef = FragmentShortActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.copyLink.setOnClickListener { finish(ShortMoreAction.COPY_LINK) }
        binding.notInterested.setOnClickListener { finish(ShortMoreAction.NOT_INTERESTED) }
        binding.report.setOnClickListener { finish(ShortMoreAction.REPORT) }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)
            ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onDestroyView() {
        bindingRef = null
        super.onDestroyView()
    }

    private fun finish(action: ShortMoreAction) {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            Bundle().apply {
                putString(RESULT_ACTION, action.name)
                putString(RESULT_ITEM_ID, arguments?.getString(ARG_ITEM_ID))
            },
        )
        dismiss()
    }

    companion object {
        const val TAG = "short-more-actions"
        const val RESULT_KEY = "short-more-action-result"
        const val RESULT_ACTION = "short-more-action"
        const val RESULT_ITEM_ID = "short-more-item-id"
        private const val ARG_ITEM_ID = "short-more-item"

        fun newInstance(itemId: String) =
            ShortActionsBottomSheetFragment().apply {
                arguments = Bundle().apply { putString(ARG_ITEM_ID, itemId) }
            }
    }
}
