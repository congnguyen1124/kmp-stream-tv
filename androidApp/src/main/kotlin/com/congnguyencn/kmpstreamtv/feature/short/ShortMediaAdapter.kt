package com.congnguyencn.kmpstreamtv.feature.short

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
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
            onTogglePlayback = { holder ->
                holder.animatePlaybackToggle(holder.manager?.exoPlayer()?.isPlaying == true)
                holder.manager?.togglePlayPause()
            },
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
        holder.hidePlayIndicator()
        holder.fadeChrome(1f)
        playerPool.pauseAllExcept(key)
    }

    private companion object {
        const val PLAYER_POOL_SIZE = 3
        const val SCROLLING_CHROME_ALPHA = 0.4f
    }
}

@OptIn(UnstableApi::class)
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
    private var pendingFollowProviderId: String? = null
    private val hideFollowAction =
        Runnable {
            val item = boundItem
            if (item?.providerId == pendingFollowProviderId && item?.isFollowingProvider == true) {
                binding.follow.isInvisible = true
            }
            pendingFollowProviderId = null
        }

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
        binding.follow.setOnClickListener { toggleFollow() }
        binding.likeAction.setOnClickListener { dispatch(ShortAction.LIKE) }
        binding.commentAction.setOnClickListener { dispatch(ShortAction.COMMENT) }
        binding.shareAction.setOnClickListener { dispatch(ShortAction.SHARE) }
        binding.moreAction.setOnClickListener { dispatch(ShortAction.MORE) }
    }

    fun bind(item: ShortItemUiModel) {
        val changedItem = boundItem?.id != item.id
        if (changedItem) {
            binding.follow.removeCallbacks(hideFollowAction)
            pendingFollowProviderId = null
        }
        boundItem = item
        if (changedItem) binding.artwork.isVisible = true
        binding.artwork.load(item.thumbnailUrl) { crossfade(true) }
        binding.profileAvatar.load(item.providerAvatarUrl) { crossfade(true) }
        binding.providerName.text = item.providerName
        binding.description.text =
            binding.root.context.getString(
                R.string.short_description_format,
                item.title,
                item.description,
            )
        binding.follow.isActivated = item.isFollowingProvider
        binding.follow.isInvisible =
            item.isFollowingProvider && pendingFollowProviderId != item.providerId
        binding.likeAction.isActivated = item.isLiked
        binding.likeAction.text = item.likeCountLabel
        binding.likeAction.contentDescription =
            binding.root.context.getString(
                R.string.short_action_with_count,
                binding.root.context.getString(if (item.isLiked) R.string.unlike else R.string.like),
                item.likeCountLabel,
            )
        binding.commentAction.text = item.commentCountLabel
        binding.commentAction.contentDescription =
            binding.root.context.getString(
                R.string.short_action_with_count,
                binding.root.context.getString(R.string.comment),
                item.commentCountLabel,
            )
        binding.shareAction.setText(R.string.share)
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
                    binding.errorGroup.isVisible = state.error != null
                    if (state.error != null) hidePlayIndicator()
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

    fun animatePlaybackToggle(wasPlaying: Boolean) {
        binding.playIndicator.clearAnimation()
        binding.playIndicator.isVisible = true
        binding.playIndicator.alpha = 1f
        val animation =
            AnimationUtils.loadAnimation(
                binding.root.context,
                if (wasPlaying) R.anim.scale_down_in else R.anim.fade_out_short,
            )
        if (!wasPlaying) {
            animation.setAnimationListener(
                object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) = Unit

                    override fun onAnimationRepeat(animation: Animation?) = Unit

                    override fun onAnimationEnd(animation: Animation?) = hidePlayIndicator()
                },
            )
        }
        binding.playIndicator.startAnimation(animation)
    }

    fun hidePlayIndicator() {
        binding.playIndicator.clearAnimation()
        binding.playIndicator.isVisible = false
        binding.playIndicator.alpha = 0f
    }

    private fun dispatch(action: ShortAction) {
        boundItem?.let { onAction(it, action) }
    }

    private fun toggleFollow() {
        val item = boundItem ?: return
        binding.follow.removeCallbacks(hideFollowAction)
        binding.follow.isActivated = !item.isFollowingProvider
        binding.follow.isInvisible = false
        pendingFollowProviderId = item.providerId.takeIf { binding.follow.isActivated }
        if (pendingFollowProviderId != null) {
            binding.follow.postDelayed(hideFollowAction, FOLLOW_INVISIBLE_DELAY_MILLIS)
        }
        dispatch(ShortAction.FOLLOW)
    }

    fun release() {
        binding.follow.removeCallbacks(hideFollowAction)
        detachPlayer()
        scope.cancel()
    }

    private companion object {
        const val FOLLOW_INVISIBLE_DELAY_MILLIS = 1_000L
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
