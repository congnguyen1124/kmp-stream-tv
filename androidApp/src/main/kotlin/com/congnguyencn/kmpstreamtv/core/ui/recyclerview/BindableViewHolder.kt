package com.congnguyencn.kmpstreamtv.core.ui.recyclerview

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BindableViewHolder<T> protected constructor(root: View) : RecyclerView.ViewHolder(root) {
    protected constructor(binding: ViewBinding) : this(binding.root)

    abstract fun bind(item: T)

    open fun bind(item: T, position: Int) = bind(item)
}
