package com.congnguyencn.kmpstreamtv.feature.player.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.streamplayer.model.StreamTvPlayerState
import java.util.Locale

/** Full-screen settings sheet ported from onmediaplayer-android's column layout. */
internal class PlayerSettingsView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr) {
        private val columns =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }
        private var visibleTypes: List<PlayerSettingType> = emptyList()
        private var latestState = StreamTvPlayerState.Initial
        private var selectedSpeed = DEFAULT_SPEED

        var onOptionSelected: ((PlayerSettingType, String) -> Unit)? = null
        var onDismiss: (() -> Unit)? = null

        init {
            setBackgroundColor(Color.BLACK)
            isClickable = true
            isFocusable = true
            val padding = resources.getDimensionPixelSize(R.dimen.player_setting_layout_padding)
            addView(
                columns,
                LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                    setMargins(padding, padding, padding, padding)
                },
            )
            addView(
                ImageButton(context).apply {
                    setImageResource(R.drawable.ic_player_close)
                    setBackgroundColor(Color.TRANSPARENT)
                    contentDescription = context.getString(R.string.close_settings)
                    setPadding(resources.getDimensionPixelSize(R.dimen.player_controller_padding_medium))
                    setOnClickListener { dismiss() }
                },
                LayoutParams(dp(48), dp(48), Gravity.TOP or Gravity.END).apply {
                    setMargins(0, dp(8), dp(8), 0)
                },
            )
        }

        fun show(
            types: List<PlayerSettingType>,
            state: StreamTvPlayerState,
            speed: Float,
        ) {
            visibleTypes = types
            latestState = state
            selectedSpeed = speed
            rebuildColumns()
            isVisible = true
        }

        fun render(
            state: StreamTvPlayerState,
            speed: Float,
        ) {
            latestState = state
            selectedSpeed = speed
            if (isVisible) rebuildColumns()
        }

        fun dismiss(): Boolean {
            if (!isVisible) return false
            isVisible = false
            onDismiss?.invoke()
            return true
        }

        private fun rebuildColumns() {
            columns.removeAllViews()
            visibleTypes.forEach { type ->
                columns.addView(
                    createColumn(type, optionsFor(type)),
                    LinearLayout.LayoutParams(0, MATCH_PARENT, 1f).apply {
                        marginEnd = dp(20)
                    },
                )
            }
        }

        private fun createColumn(
            type: PlayerSettingType,
            options: List<PlayerSettingOption>,
        ) = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                TextView(context).apply {
                    setText(type.titleRes)
                    setTextAppearance(R.style.MediaPlayerActionSettingHeaderTextAppearance)
                },
            )
            addView(
                RecyclerView(context).apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter =
                        PlayerSettingAdapter(options) { option ->
                            onOptionSelected?.invoke(type, option.value)
                        }
                    overScrollMode = OVER_SCROLL_NEVER
                },
                LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.player_setting_content_padding)
                },
            )
        }

        private fun optionsFor(type: PlayerSettingType): List<PlayerSettingOption> =
            when (type) {
                PlayerSettingType.SPEED -> {
                    SPEEDS.map { speed ->
                        PlayerSettingOption(
                            title = speed.displaySpeed(),
                            value = speed.toString(),
                            selected = speed == selectedSpeed,
                        )
                    }
                }

                PlayerSettingType.VIDEO -> {
                    val tracks = latestState.videoTracks
                    val autoSelected = tracks.count { it.isSelected } != 1
                    listOf(
                        PlayerSettingOption(
                            title = context.getString(R.string.player_setting_auto),
                            value = VIDEO_AUTO_ID,
                            selected = autoSelected,
                        ),
                    ) +
                        tracks.map { track ->
                            val bitrate = (track.bitrate / 1_000).takeIf { it > 0 }
                            val label =
                                buildString {
                                    append(
                                        if (track.height >
                                            0
                                        ) {
                                            "${track.height}p"
                                        } else {
                                            context.getString(R.string.player_setting_unknown)
                                        },
                                    )
                                    bitrate?.let { append(" · $it Kbps") }
                                }
                            PlayerSettingOption(label, track.id, track.isSelected && !autoSelected)
                        }
                }

                PlayerSettingType.AUDIO -> {
                    latestState.audioTracks.map { track ->
                        PlayerSettingOption(track.displayLabel(), track.id, track.isSelected)
                    }
                }

                PlayerSettingType.SUBTITLE -> {
                    listOf(
                        PlayerSettingOption(
                            title = context.getString(R.string.player_setting_no_subtitle),
                            value = TEXT_OFF_ID,
                            selected = latestState.textTracks.none { it.isSelected },
                        ),
                    ) +
                        latestState.textTracks.map { track ->
                            PlayerSettingOption(
                                track.label.ifBlank { track.language.displayLanguage() },
                                track.id,
                                track.isSelected,
                            )
                        }
                }
            }

        private fun com.congnguyencn.streamplayer.model.StreamTvAudioTrack.displayLabel(): String =
            label.ifBlank { language.displayLanguage() }

        private fun String.displayLanguage(): String =
            takeIf { it.isNotBlank() }
                ?.let { Locale.forLanguageTag(it).displayLanguage }
                ?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.player_setting_unknown)

        private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

        private fun Float.displaySpeed(): String = if (this == toInt().toFloat()) "${toInt()}x" else "${this}x"

        private companion object {
            const val DEFAULT_SPEED = 1f
            const val VIDEO_AUTO_ID = "auto"
            const val TEXT_OFF_ID = "off"
            val SPEEDS = listOf(0.5f, 1f, 1.5f, 2f)
        }
    }

internal enum class PlayerSettingType(
    @StringRes val titleRes: Int,
) {
    VIDEO(R.string.player_setting_video_quality),
    AUDIO(R.string.player_setting_audio),
    SUBTITLE(R.string.player_setting_subtitle),
    SPEED(R.string.player_setting_speed),
}

private data class PlayerSettingOption(
    val title: String,
    val value: String,
    val selected: Boolean,
)

private class PlayerSettingAdapter(
    private val items: List<PlayerSettingOption>,
    private val onClick: (PlayerSettingOption) -> Unit,
) : RecyclerView.Adapter<PlayerSettingAdapter.OptionViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): OptionViewHolder =
        OptionViewHolder(
            TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                minHeight = (48 * resources.displayMetrics.density).toInt()
                gravity = Gravity.CENTER_VERTICAL
                compoundDrawablePadding = (8 * resources.displayMetrics.density).toInt()
            },
        )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(
        holder: OptionViewHolder,
        position: Int,
    ) {
        holder.bind(items[position], onClick)
    }

    class OptionViewHolder(
        private val textView: TextView,
    ) : RecyclerView.ViewHolder(textView) {
        fun bind(
            item: PlayerSettingOption,
            onClick: (PlayerSettingOption) -> Unit,
        ) = with(textView) {
            text = item.title
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (item.selected) R.drawable.ic_player_check else 0,
                0,
                0,
                0,
            )
            setTextAppearance(
                if (item.selected) {
                    R.style.MediaPlayerActionSettingActiveTextAppearance
                } else {
                    R.style.MediaPlayerActionSettingInActiveTextAppearance
                },
            )
            setOnClickListener { onClick(item) }
        }
    }
}
