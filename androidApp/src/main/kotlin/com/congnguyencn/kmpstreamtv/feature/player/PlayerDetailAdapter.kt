package com.congnguyencn.kmpstreamtv.feature.player

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.ItemPlayerDetailBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemPlayerProviderBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemPlayerRecommendationBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemPlayerRecommendationHeaderBinding
import kotlin.math.min

internal enum class PlayerDetailAction { WATCH_LATER, PRODUCTS, LIKE, COMMENT, SHARE }

/** Native RecyclerView counterpart of on-tv-android's DetailAdapter for the dummy catalogue. */
internal class PlayerDetailAdapter(
    private val onAction: (PlayerDetailAction) -> Unit,
    private val onRecommendation: (PlayerMedia) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var media: PlayerMedia? = null
    private var recommendations: List<PlayerMedia> = emptyList()
    private val savedIds = mutableSetOf<String>()
    private val likedIds = mutableSetOf<String>()
    private val followedProviders = mutableSetOf<String>()

    init {
        setHasStableIds(true)
    }

    fun submit(media: PlayerMedia, recommendations: List<PlayerMedia>) {
        val oldCount = itemCount
        this.media = media
        this.recommendations = recommendations
        val newCount = itemCount
        val sharedCount = min(oldCount, newCount)
        if (sharedCount > 0) notifyItemRangeChanged(0, sharedCount)
        if (newCount > oldCount) notifyItemRangeInserted(oldCount, newCount - oldCount)
        if (oldCount > newCount) notifyItemRangeRemoved(newCount, oldCount - newCount)
    }

    override fun getItemCount(): Int = if (media == null) 0 else recommendations.size + FIXED_ITEM_COUNT

    override fun getItemId(position: Int): Long = when (position) {
        DETAIL_POSITION -> media?.id.orEmpty().hashCode().toLong()
        PROVIDER_POSITION -> media?.providerName.orEmpty().hashCode().toLong()
        HEADER_POSITION -> Long.MIN_VALUE
        else -> recommendations[position - FIXED_ITEM_COUNT].id.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int = when (position) {
        DETAIL_POSITION -> TYPE_DETAIL
        PROVIDER_POSITION -> TYPE_PROVIDER
        HEADER_POSITION -> TYPE_HEADER
        else -> TYPE_RECOMMENDATION
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DETAIL -> DetailHolder(ItemPlayerDetailBinding.inflate(inflater, parent, false))
            TYPE_PROVIDER -> ProviderHolder(ItemPlayerProviderBinding.inflate(inflater, parent, false))
            TYPE_HEADER -> HeaderHolder(ItemPlayerRecommendationHeaderBinding.inflate(inflater, parent, false))
            else -> RecommendationHolder(ItemPlayerRecommendationBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DetailHolder -> holder.bind(requireNotNull(media))
            is ProviderHolder -> holder.bind(requireNotNull(media))
            is RecommendationHolder -> holder.bind(recommendations[position - FIXED_ITEM_COUNT])
        }
    }

    private inner class DetailHolder(
        private val binding: ItemPlayerDetailBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PlayerMedia) = with(binding) {
            title.text = item.title
            val secondary = listOfNotNull(
                item.viewCountLabel.takeIf(String::isNotBlank),
                root.context.getString(R.string.player_like_count),
            ).joinToString(" | ")
            userGraph.text = root.context.getString(R.string.player_user_graph, secondary)
            metadataRow.isVisible = false
            age.text = item.ageRestriction
            age.isVisible = item.ageRestriction.isNullOrBlank().not()
            subInfo.text = when {
                item.isLive -> root.context.getString(R.string.live)
                item.episodeCount > 0 -> root.resources.getQuantityString(
                    R.plurals.player_episode_count,
                    item.episodeCount,
                    item.episodeCount,
                )
                else -> item.durationLabel
            }
            description.text = item.description
            description.isVisible = false
            userGraph.setOnClickListener {
                description.isVisible = item.description.isNotBlank() && !description.isVisible
            }

            watchLater.isActivated = item.id in savedIds
            watchLater.setIconResource(
                if (watchLater.isActivated) R.drawable.ic_player_check else R.drawable.ic_playlist_plus,
            )
            like.isActivated = item.id in likedIds
            like.setIconResource(if (like.isActivated) R.drawable.ic_player_heart_fill else R.drawable.ic_player_heart)

            watchLater.setOnClickListener {
                savedIds.toggle(item.id)
                notifyItemChanged(DETAIL_POSITION)
                onAction(PlayerDetailAction.WATCH_LATER)
            }
            products.setOnClickListener { onAction(PlayerDetailAction.PRODUCTS) }
            like.setOnClickListener {
                likedIds.toggle(item.id)
                notifyItemChanged(DETAIL_POSITION)
                onAction(PlayerDetailAction.LIKE)
            }
            comment.setOnClickListener { onAction(PlayerDetailAction.COMMENT) }
            share.setOnClickListener { onAction(PlayerDetailAction.SHARE) }
        }
    }

    private inner class ProviderHolder(
        private val binding: ItemPlayerProviderBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PlayerMedia) = with(binding) {
            providerLogo.setImageResource(R.mipmap.ic_launcher_round)
            providerName.text = item.providerName.ifBlank { root.context.getString(R.string.app_name) }
            val followed = item.providerName in followedProviders
            follow.isActivated = followed
            follow.text = root.context.getString(if (followed) R.string.following else R.string.follow)
            follow.setIconResource(if (followed) R.drawable.ic_player_check else R.drawable.ic_player_add)
            follow.setOnClickListener {
                followedProviders.toggle(item.providerName)
                notifyItemChanged(PROVIDER_POSITION)
            }
        }
    }

    private inner class RecommendationHolder(
        private val binding: ItemPlayerRecommendationBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PlayerMedia) = with(binding) {
            thumbnail.load(item.thumbnailUrl) { crossfade(true) }
            title.text = item.title
            metadata.text = item.subtitle.ifBlank { item.viewCountLabel }
            root.contentDescription = item.title
            root.setOnClickListener { onRecommendation(item) }
        }
    }

    private class HeaderHolder(binding: ItemPlayerRecommendationHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    private fun MutableSet<String>.toggle(value: String) {
        if (!add(value)) remove(value)
    }

    private companion object {
        const val DETAIL_POSITION = 0
        const val PROVIDER_POSITION = 1
        const val HEADER_POSITION = 2
        const val FIXED_ITEM_COUNT = 3
        const val TYPE_DETAIL = 0
        const val TYPE_PROVIDER = 1
        const val TYPE_HEADER = 2
        const val TYPE_RECOMMENDATION = 3
    }
}
