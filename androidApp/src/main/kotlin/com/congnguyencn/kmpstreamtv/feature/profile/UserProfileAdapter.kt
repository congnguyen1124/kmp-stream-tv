package com.congnguyencn.kmpstreamtv.feature.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.congnguyencn.kmpstreamtv.databinding.ItemProfileActionBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemProfileGapBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemProfileSectionBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemProfileSigninBinding

class UserProfileAdapter(
    private val onSignIn: () -> Unit,
    private val onAction: (ProfileAction) -> Unit,
) : ListAdapter<ProfileItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            ProfileItem.SignIn -> VIEW_TYPE_SIGN_IN
            ProfileItem.Gap -> VIEW_TYPE_GAP
            is ProfileItem.Section -> VIEW_TYPE_SECTION
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SIGN_IN -> {
                SignInViewHolder(ItemProfileSigninBinding.inflate(inflater, parent, false), onSignIn)
            }

            VIEW_TYPE_SECTION -> {
                SectionViewHolder(ItemProfileSectionBinding.inflate(inflater, parent, false), onAction)
            }

            else -> {
                GapViewHolder(ItemProfileGapBinding.inflate(inflater, parent, false))
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        when (holder) {
            is SignInViewHolder -> holder.bind()
            is SectionViewHolder -> holder.bind(getItem(position) as ProfileItem.Section)
        }
    }

    private class SignInViewHolder(
        private val binding: ItemProfileSigninBinding,
        private val onSignIn: () -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.signInAction.setOnClickListener { onSignIn() }
        }
    }

    private class SectionViewHolder(
        private val binding: ItemProfileSectionBinding,
        private val onAction: (ProfileAction) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(section: ProfileItem.Section) {
            binding.root.removeAllViews()
            section.actions.forEach { action ->
                val actionBinding =
                    ItemProfileActionBinding.inflate(
                        LayoutInflater.from(binding.root.context),
                        binding.root,
                        true,
                    )
                actionBinding.actionIcon.setImageResource(action.iconRes)
                actionBinding.actionTitle.setText(action.titleRes)
                actionBinding.root.setOnClickListener { onAction(action) }
            }
        }
    }

    private class GapViewHolder(
        binding: ItemProfileGapBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    private companion object {
        const val VIEW_TYPE_SIGN_IN = 0
        const val VIEW_TYPE_SECTION = 1
        const val VIEW_TYPE_GAP = 2

        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<ProfileItem>() {
                override fun areItemsTheSame(
                    oldItem: ProfileItem,
                    newItem: ProfileItem,
                ): Boolean = oldItem == newItem

                override fun areContentsTheSame(
                    oldItem: ProfileItem,
                    newItem: ProfileItem,
                ): Boolean = oldItem == newItem
            }
    }
}
