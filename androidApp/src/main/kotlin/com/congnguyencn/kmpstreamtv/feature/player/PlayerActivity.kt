package com.congnguyencn.kmpstreamtv.feature.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.ActivityPlayerBinding
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel
import com.congnguyencn.streamplayer.StreamTvPlayerManager
import com.congnguyencn.streamplayer.config.StreamTvPlayerConfig
import com.congnguyencn.streamplayer.exoPlayer
import com.congnguyencn.streamplayer.loadAndPlay
import com.congnguyencn.streamplayer.pause
import com.congnguyencn.streamplayer.seekBack
import com.congnguyencn.streamplayer.seekForward
import com.congnguyencn.streamplayer.togglePlayPause
import com.congnguyencn.streamplayer.togglePlayPauseAtDefaultPosition
import com.congnguyencn.streamplayer.model.StreamTvPlaybackState
import com.congnguyencn.streamplayer.model.StreamTvPlayerState
import kotlinx.coroutines.launch

/** Native Android player UI backed by the shared android_stream_player engine. */
class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var player: StreamTvPlayerManager
    private val isLive by lazy { intent.getBooleanExtra(EXTRA_IS_LIVE, false) }
    private val isShort by lazy { intent.getBooleanExtra(EXTRA_IS_SHORT, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra(EXTRA_VIDEO_URL).orEmpty()
        if (url.isBlank()) {
            finish()
            return
        }

        binding.title.text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        binding.back.setOnClickListener { finish() }
        binding.playPause.setOnClickListener {
            if (isLive) player.togglePlayPauseAtDefaultPosition() else player.togglePlayPause()
        }
        binding.rewind.setOnClickListener { player.seekBack() }
        binding.forward.setOnClickListener { player.seekForward() }
        binding.rewind.isVisible = !isLive
        binding.forward.isVisible = !isLive
        binding.liveBadge.isVisible = isLive

        val playerConfig = if (isShort) StreamTvPlayerConfig.Feed else StreamTvPlayerConfig.Tv
        player = StreamTvPlayerManager.create(applicationContext, playerConfig)
        binding.playerView.player = player.exoPlayer()
        player.loadAndPlay(url.toUri())

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                player.playerState.collect(::render)
            }
        }
    }

    override fun onStop() {
        if (::player.isInitialized) player.pause()
        super.onStop()
    }

    override fun onDestroy() {
        binding.playerView.player = null
        if (::player.isInitialized) player.close()
        super.onDestroy()
    }

    private fun render(state: StreamTvPlayerState) = with(binding) {
        loading.isVisible = state.playbackState == StreamTvPlaybackState.Buffering
        playPause.setText(if (state.isPlaying) R.string.pause else R.string.play)
        error.text = state.playbackError?.toString().orEmpty()
        error.isVisible = state.playbackError != null

        if (!isLive) {
            val duration = state.duration.inWholeSeconds.coerceAtLeast(0)
            val position = state.position.inWholeSeconds.coerceIn(0, duration.coerceAtLeast(0))
            progress.max = duration.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            progress.progress = position.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            time.text = getString(R.string.elapsed_time, position.asTimestamp(), duration.asTimestamp())
        } else {
            time.setText(R.string.live)
        }
    }

    companion object {
        private const val EXTRA_VIDEO_URL = "video_url"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_IS_LIVE = "is_live"
        private const val EXTRA_IS_SHORT = "is_short"

        fun intent(context: Context, content: HomeContentUiModel) =
            Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, content.videoUrl)
                putExtra(EXTRA_TITLE, content.title)
                putExtra(EXTRA_IS_LIVE, content.isLive)
                putExtra(EXTRA_IS_SHORT, content.isShort)
            }
    }
}

private fun Long.asTimestamp(): String {
    val hours = this / 3_600
    val minutes = (this % 3_600) / 60
    val seconds = this % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
