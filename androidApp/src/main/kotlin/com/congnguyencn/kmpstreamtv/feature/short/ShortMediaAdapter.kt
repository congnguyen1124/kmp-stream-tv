package com.congnguyencn.kmpstreamtv.feature.short

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.ItemShortMediaBinding
import com.congnguyencn.kmpstreamtv.feature.short.presentation.ShortItemUiModel
import com.congnguyencn.streamplayer.StreamTvPlayerManager
import com.congnguyencn.streamplayer.StreamTvPlayerPool
import com.congnguyencn.streamplayer.config.StreamTvPlayerConfig
import com.congnguyencn.streamplayer.exoPlayer
import com.congnguyencn.streamplayer.loadAndPlay
import com.congnguyencn.streamplayer.model.StreamTvMediaKey
import com.congnguyencn.streamplayer.model.StreamTvPlaybackState
import com.congnguyencn.streamplayer.pause
import com.congnguyencn.streamplayer.play
import com.congnguyencn.streamplayer.replay
import com.congnguyencn.streamplayer.togglePlayPause
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal enum class ShortAction {
    PROFILE,
    FOLLOW,
    LIKE,
    COMMENT,
    SHARE,
    MORE,
}

internal class ShortMediaAdapter(
    parentContext: android.content.Context,
    private val onAction: (ShortItemUiModel, ShortAction) -> Unit,
) : ListAdapter<ShortItemUiModel, ShortMediaViewHolder>(ShortItemDiffCallback) {
    private val playerPool =
        StreamTvPlayerPool.create(
            context = parentContext,
            size = PLAYER_POOL_SIZE,
            config = StreamTvPlayerConfig.Feed,
        )
    private val attachedHolders = mutableMapOf<Int, ShortMediaViewHolder>()
    private val managerOwners = mutableMapOf<StreamTvPlayerManager, ShortMediaViewHolder>()
    private var activePosition = RecyclerView.NO_POSITION
    private var hostStarted = false

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ShortMediaViewHolder =
        ShortMediaViewHolder(
            binding = ItemShortMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onAction = onAction,
            onTogglePlayback = { holder -> holder.manager?.togglePlayPause() },
            onRetry = { holder -> play(holder, forceReload = true) },
            onPlaybackEnded = { holder ->
                if (holder.bindingAdapterPosition == activePosition && hostStarted) {
                    holder.manager?.replay()
                }
            },
        )

    override fun onBindViewHolder(
        holder: ShortMediaViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
        attachedHolders[position] = holder
        if (position == activePosition && hostStarted) play(holder)
    }

    override fun onViewRecycled(holder: ShortMediaViewHolder) {
        attachedHolders.entries.removeAll { it.value === holder }
        holder.detachPlayer()
        super.onViewRecycled(holder)
    }

    override fun onViewDetachedFromWindow(holder: ShortMediaViewHolder) {
        if (holder.bindingAdapterPosition == activePosition) holder.manager?.pause()
        attachedHolders.entries.removeAll { it.value === holder }
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewAttachedToWindow(holder: ShortMediaViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            attachedHolders[position] = holder
            if (position == activePosition && hostStarted) play(holder)
        }
    }

    fun setActivePosition(position: Int) {
        if (position !in currentList.indices) return
        val previous = attachedHolders[activePosition]
        activePosition = position
        previous?.fadeChrome(1f)
        if (!hostStarted) return
        attachedHolders[position]?.let(::play)
    }

    fun onScrollStarted() {
        attachedHolders[activePosition]?.apply {
            fadeChrome(SCROLLING_CHROME_ALPHA)
            manager?.pause()
        }
    }

    fun start() {
        hostStarted = true
        attachedHolders[activePosition]?.let(::play)
    }

    fun stop() {
        hostStarted = false
        attachedHolders[activePosition]?.manager?.pause()
    }

    fun release() {
        hostStarted = false
        (attachedHolders.values + managerOwners.values).toSet().forEach(ShortMediaViewHolder::release)
        attachedHolders.clear()
        managerOwners.clear()
        playerPool.close()
    }

    private fun play(
        holder: ShortMediaViewHolder,
        forceReload: Boolean = false,
    ) {
        val item = holder.boundItem ?: return
        val key = StreamTvMediaKey(item.id)
        val manager =
            playerPool.acquire(
                key = key,
                onReused = { player ->
                    if (forceReload) player.loadAndPlay(item.videoUrl.toUri()) else player.play()
                },
                onAssigned = { player -> player.loadAndPlay(item.videoUrl.toUri()) },
            )
        managerOwners[manager]?.takeIf { it !== holder }?.detachPlayer()
        managerOwners[manager] = holder
        holder.attachPlayer(manager)
        holder.fadeChrome(1f)
        playerPool.pauseAllExcept(key)
    }

    private companion object {
        const val PLAYER_POOL_SIZE = 3
        const val SCROLLING_CHROME_ALPHA = 0.4f
    }
}

internal class ShortMediaViewHolder(
    private val binding: ItemShortMediaBinding,
    private val onAction: (ShortItemUiModel, ShortAction) -> Unit,
    onTogglePlayback: (ShortMediaViewHolder) -> Unit,
    onRetry: (ShortMediaViewHolder) -> Unit,
    private val onPlaybackEnded: (ShortMediaViewHolder) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null
    var boundItem: ShortItemUiModel? = null
        private set
    var manager: StreamTvPlayerManager? = null
        private set

    init {
        binding.mediaSurface.apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            setShutterBackgroundColor(context.getColor(R.color.black))
        }
        binding.playbackTouchTarget.setOnClickListener { onTogglePlayback(this) }
        binding.retry.setOnClickListener { onRetry(this) }
        binding.profileAvatar.setOnClickListener { dispatch(ShortAction.PROFILE) }
        binding.providerName.setOnClickListener { dispatch(ShortAction.PROFILE) }
        binding.follow.setOnClickListener { dispatch(ShortAction.FOLLOW) }
        binding.likeAction.setOnClickListener { dispatch(ShortAction.LIKE) }
        binding.commentAction.setOnClickListener { dispatch(ShortAction.COMMENT) }
        binding.shareAction.setOnClickListener { dispatch(ShortAction.SHARE) }
        binding.moreAction.setOnClickListener { dispatch(ShortAction.MORE) }
    }

    fun bind(item: ShortItemUiModel) {
        val changedItem = boundItem?.id != item.id
        boundItem = item
        if (changedItem) binding.artwork.isVisible = true
        binding.artwork.load(item.thumbnailUrl) { crossfade(true) }
        binding.profileAvatar.load(item.providerAvatarUrl) { crossfade(true) }
        binding.providerName.text = item.providerName
        binding.description.text = "${item.title}\n${item.description}"
        binding.follow.text =
            binding.root.context.getString(
                if (item.isFollowingProvider) R.string.following else R.string.follow,
            )
        binding.follow.isSelected = item.isFollowingProvider
        binding.likeIcon.setImageResource(
            if (item.isLiked) R.drawable.ic_player_heart_fill else R.drawable.ic_player_heart,
        )
        binding.likeCount.text = item.likeCountLabel
        binding.commentCount.text = item.commentCountLabel
        binding.shareCount.text = item.shareCountLabel
        binding.root.contentDescription = item.title
    }

    fun attachPlayer(player: StreamTvPlayerManager) {
        if (manager === player && binding.mediaSurface.player === player.exoPlayer()) return
        detachPlayer()
        manager = player
        binding.mediaSurface.player = player.exoPlayer()
        binding.artwork.isVisible = true
        stateJob =
            scope.launch {
                player.playerState.collect { state ->
                    binding.loading.isVisible = state.playbackState is StreamTvPlaybackState.Buffering
                    binding.playIndicator.isVisible =
                        !state.isPlaying &&
                        state.playbackState !is StreamTvPlaybackState.Buffering &&
                        state.error == null
                    binding.errorGroup.isVisible = state.error != null
                    if (state.playbackState is StreamTvPlaybackState.Ready) binding.artwork.isVisible = false
                    if (state.playbackState is StreamTvPlaybackState.Ended) onPlaybackEnded(this@ShortMediaViewHolder)
                }
            }
    }

    fun detachPlayer() {
        stateJob?.cancel()
        stateJob = null
        binding.mediaSurface.player = null
        manager = null
    }

    fun fadeChrome(alpha: Float) {
        binding.chrome.alpha = alpha
    }

    private fun dispatch(action: ShortAction) {
        boundItem?.let { onAction(it, action) }
    }

    fun release() {
        detachPlayer()
        scope.cancel()
    }
}

private object ShortItemDiffCallback : DiffUtil.ItemCallback<ShortItemUiModel>() {
    override fun areItemsTheSame(
        oldItem: ShortItemUiModel,
        newItem: ShortItemUiModel,
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: ShortItemUiModel,
        newItem: ShortItemUiModel,
    ): Boolean = oldItem == newItem
}
