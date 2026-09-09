package com.congnguyencn.kmpstreamtv.core.ui.recyclerview

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

/** Shared RecyclerView plumbing modelled after on-tv-android's base/rcv package. */
abstract class BaseListAdapter<T>(diffCallback: DiffUtil.ItemCallback<T>) :
    ListAdapter<T, BindableViewHolder<T>>(diffCallback) {
    final override fun onBindViewHolder(holder: BindableViewHolder<T>, position: Int) {
        holder.bind(getItem(position))
    }
}
