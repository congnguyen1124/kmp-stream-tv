package com.congnguyencn.kmpstreamtv.feature.selection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import coil3.load
import coil3.request.crossfade
import com.congnguyencn.kmpstreamtv.core.ui.recyclerview.BaseListAdapter
import com.congnguyencn.kmpstreamtv.core.ui.recyclerview.BindableViewHolder
import com.congnguyencn.kmpstreamtv.databinding.ItemSelectionBinding

class ItemSelectionAdapter(
    private val onItemClick: (position: Int) -> Unit,
) : BaseListAdapter<SelectionItem>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BindableViewHolder<SelectionItem> =
        SelectionViewHolder(
            binding = ItemSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onItemClick = onItemClick,
        )

    fun getItemAt(position: Int): SelectionItem? = position.takeIf { it in 0 until itemCount }?.let(::getItem)

    private class SelectionViewHolder(
        private val binding: ItemSelectionBinding,
        private val onItemClick: (position: Int) -> Unit,
    ) : BindableViewHolder<SelectionItem>(binding) {
        init {
            binding.root.setOnClickListener {
                bindingAdapterPosition
                    .takeUnless { it == androidx.recyclerview.widget.RecyclerView.NO_POSITION }
                    ?.let(onItemClick)
            }
        }

        override fun bind(item: SelectionItem) {
            val hasImage = !item.imageUrl.isNullOrBlank()
            binding.selectionImage.isVisible = hasImage
            binding.selectionTitle.isVisible = !hasImage
            binding.selectionTitle.text = item.title
            if (hasImage) {
                binding.selectionImage.load(item.imageUrl) { crossfade(true) }
            } else {
                binding.selectionImage.setImageDrawable(null)
            }
        }
    }

    private companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<SelectionItem>() {
                override fun areItemsTheSame(
                    oldItem: SelectionItem,
                    newItem: SelectionItem,
                ): Boolean = oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: SelectionItem,
                    newItem: SelectionItem,
                ): Boolean = oldItem == newItem
            }
    }
}
