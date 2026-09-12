package com.congnguyencn.kmpstreamtv.feature.short

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.isVisible
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.FragmentShortCommentsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

internal class ShortCommentBottomSheetFragment : BottomSheetDialogFragment() {
    private var bindingRef: FragmentShortCommentsBinding? = null
    private val binding get() = requireNotNull(bindingRef)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingRef = FragmentShortCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val comments = arguments?.getStringArrayList(ARG_COMMENTS).orEmpty()
        binding.title.text = getString(R.string.short_comments_title, arguments?.getInt(ARG_COUNT) ?: 0)
        binding.close.setOnClickListener { dismiss() }
        binding.send.setOnClickListener { submitComment() }
        binding.commentInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitComment()
                true
            } else {
                false
            }
        }
        renderComments(comments)
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.let { bottomSheetDialog ->
            bottomSheetDialog
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundColor(Color.TRANSPARENT)
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        bindingRef = null
        super.onDestroyView()
    }

    private fun renderComments(comments: List<String>) {
        binding.emptyComments.isVisible = comments.isEmpty()
        comments.forEach { comment ->
            binding.commentList.addView(
                TextView(requireContext()).apply {
                    setText(comment)
                    setTextAppearance(R.style.Text_Body2)
                    setTextColor(context.getColor(R.color.white))
                    setPadding(0, resources.getDimensionPixelSize(R.dimen.margin_small), 0, 0)
                },
            )
        }
    }

    private fun submitComment() {
        val comment =
            binding.commentInput.text
                ?.toString()
                ?.trim()
                .orEmpty()
        if (comment.isEmpty()) return
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            Bundle().apply {
                putString(RESULT_ITEM_ID, arguments?.getString(ARG_ITEM_ID))
                putString(RESULT_COMMENT, comment)
            },
        )
        dismiss()
    }

    companion object {
        const val TAG = "short-comments"
        const val RESULT_KEY = "short-comment-result"
        const val RESULT_ITEM_ID = "short-comment-item-id"
        const val RESULT_COMMENT = "short-comment-text"
        private const val ARG_ITEM_ID = "short-comment-item"
        private const val ARG_COUNT = "short-comment-count"
        private const val ARG_COMMENTS = "short-local-comments"

        fun newInstance(
            itemId: String,
            count: Int,
            comments: List<String>,
        ) = ShortCommentBottomSheetFragment().apply {
            arguments =
                Bundle().apply {
                    putString(ARG_ITEM_ID, itemId)
                    putInt(ARG_COUNT, count)
                    putStringArrayList(ARG_COMMENTS, ArrayList(comments))
                }
        }
    }
}
