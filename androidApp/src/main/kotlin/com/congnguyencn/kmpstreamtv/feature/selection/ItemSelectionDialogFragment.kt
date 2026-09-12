package com.congnguyencn.kmpstreamtv.feature.selection

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.FragmentItemSelectionBinding

class ItemSelectionDialogFragment : DialogFragment() {
    private var bindingRef: FragmentItemSelectionBinding? = null
    private val binding get() = requireNotNull(bindingRef)

    var callback: ((SelectionItem) -> Unit)? = null
    var onDismissCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_KmpStreamTv_SelectionDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingRef = FragmentItemSelectionBinding.inflate(inflater, container, false)
        initializeViews(readItems())
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val transparentBackground = Color.TRANSPARENT.toDrawable()
        dialog?.window?.apply {
            WindowCompat.setDecorFitsSystemWindows(this, false)
            setBackgroundDrawable(transparentBackground)
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setWindowAnimations(R.style.DialogAnimationFade)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback?.invoke()
    }

    override fun onDestroyView() {
        binding.categoryList.adapter = null
        bindingRef = null
        super.onDestroyView()
    }

    private fun initializeViews(items: List<SelectionItem>) {
        if (items.isEmpty()) {
            dismissAllowingStateLoss()
            return
        }
        val layoutManager = SliderLayoutManager(requireContext())
        val adapter =
            ItemSelectionAdapter { position ->
                if (layoutManager.selectedPosition == position) {
                    select(adapterItemAt(position))
                } else {
                    binding.categoryList.smoothScrollToPosition(position)
                }
            }
        binding.categoryList.layoutManager = layoutManager
        binding.categoryList.adapter = adapter
        binding.categoryList.doOnLayout { list ->
            val itemHeight = resources.getDimensionPixelSize(R.dimen.selected_category_size)
            val verticalPadding = ((list.height - itemHeight) / 2).coerceAtLeast(0)
            list.setPadding(0, verticalPadding, 0, verticalPadding)
            val selectedIndex = items.indexOfFirst(SelectionItem::isSelected).coerceAtLeast(0)
            binding.categoryList.scrollToPosition(selectedIndex)
            list.post { binding.categoryList.smoothScrollToPosition(selectedIndex) }
        }
        adapter.submitList(items)
        binding.backAction.setOnClickListener { dismiss() }
        binding.title.setOnClickListener { dismiss() }
        binding.continueAction.setOnClickListener {
            val position = layoutManager.selectedPosition
            if (position != RecyclerView.NO_POSITION) select(adapter.getItemAt(position))
        }
    }

    private fun adapterItemAt(position: Int): SelectionItem? =
        (binding.categoryList.adapter as? ItemSelectionAdapter)?.getItemAt(position)

    private fun select(item: SelectionItem?) {
        item ?: return
        dismiss()
        callback?.invoke(item)
    }

    private fun readItems(): List<SelectionItem> {
        val ids = arguments?.getStringArrayList(ARG_IDS).orEmpty()
        val titles = arguments?.getStringArrayList(ARG_TITLES).orEmpty()
        val imageUrls = arguments?.getStringArrayList(ARG_IMAGE_URLS).orEmpty()
        val selectedId = arguments?.getString(ARG_SELECTED_ID)
        return ids.indices.mapNotNull { index ->
            val title = titles.getOrNull(index) ?: return@mapNotNull null
            SelectionItem(
                id = ids[index],
                title = title,
                imageUrl = imageUrls.getOrNull(index)?.takeIf(String::isNotEmpty),
                isSelected = ids[index] == selectedId,
            )
        }
    }

    companion object {
        private const val TAG = "item_selection_dialog"
        private const val ARG_IDS = "selection_ids"
        private const val ARG_TITLES = "selection_titles"
        private const val ARG_IMAGE_URLS = "selection_image_urls"
        private const val ARG_SELECTED_ID = "selection_selected_id"

        fun show(
            fragmentManager: FragmentManager,
            items: List<SelectionItem>,
        ): ItemSelectionDialogFragment =
            ItemSelectionDialogFragment().apply {
                arguments =
                    Bundle().apply {
                        putStringArrayList(ARG_IDS, ArrayList(items.map(SelectionItem::id)))
                        putStringArrayList(ARG_TITLES, ArrayList(items.map(SelectionItem::title)))
                        putStringArrayList(ARG_IMAGE_URLS, ArrayList(items.map { it.imageUrl.orEmpty() }))
                        putString(ARG_SELECTED_ID, items.firstOrNull(SelectionItem::isSelected)?.id)
                    }
                show(fragmentManager, TAG)
            }
    }
}
