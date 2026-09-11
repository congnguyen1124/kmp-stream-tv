package com.congnguyencn.kmpstreamtv

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.congnguyencn.kmpstreamtv.databinding.ActivityMainBinding
import com.congnguyencn.kmpstreamtv.feature.home.HomeTabFragment
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel
import com.congnguyencn.kmpstreamtv.feature.placeholder.PlaceholderFragment
import com.congnguyencn.kmpstreamtv.feature.player.PlayerFragment
import com.congnguyencn.kmpstreamtv.feature.player.PlayerPresentation

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var systemBarInsets = Insets.NONE
    private var isPlayerExpanded = false

    /**
     * Last laid-out height of the bottom navigation, kept because the player hides that bar while
     * expanded — a `GONE` view measures 0, so the mini player's travel range would lose the very
     * chrome it has to stay clear of if it read the height at minimize time.
     */
    private var bottomBarHeightPx = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            applyRootInsets()
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
        binding.bottomNavMain.isItemActiveIndicatorEnabled = false
        binding.bottomNavMain.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            if (view.isVisible && view.height > 0) bottomBarHeightPx = view.height
        }

        binding.bottomNavMain.setOnItemSelectedListener { item ->
            showDestination(item.itemId)
            true
        }
        binding.bottomNavMain.selectedItemId = savedInstanceState
            ?.getInt(STATE_SELECTED_DESTINATION, R.id.home)
            ?: R.id.home
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SELECTED_DESTINATION, binding.bottomNavMain.selectedItemId)
        super.onSaveInstanceState(outState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        activePlayer()?.onHostConfigurationChanged()
    }

    fun openPlayer(content: HomeContentUiModel) {
        val existing = activePlayer()
        if (existing != null) {
            existing.play(content)
            existing.expand()
            return
        }

        binding.playerFragmentContainer.isVisible = true
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.playerFragmentContainer,
                PlayerFragment.newInstance(content),
                PlayerFragment.TAG,
            ).commitNow()
    }

    internal fun presentPlayer(presentation: PlayerPresentation) {
        val isMini = presentation == PlayerPresentation.MINI
        val fullscreen = presentation == PlayerPresentation.FULLSCREEN
        // The overlay stays window-sized at every presentation, including mini: the mini player is
        // a floating card that travels the whole window, so the container cannot be docked to a
        // strip above the bottom bar any more. It paints no background of its own and nothing
        // outside the card is clickable, so touches beside a minimized player fall through to the
        // destination behind it.
        binding.playerFragmentContainer.isVisible = true
        binding.bottomNavMain.isVisible = isMini
        binding.fragmentContainerMain.importantForAccessibility =
            if (isMini) {
                View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            } else {
                View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            }
        setSystemBarsHidden(fullscreen)
        if (isMini) disableAutoEnterPictureInPicture()
    }

    /** Height the mini player has to stay clear of at the bottom of the window. */
    internal fun bottomBarHeight(): Int = bottomBarHeightPx

    internal fun enterPlayerPictureInPicture() {
        if (isInPictureInPictureMode) return
        val player = activePlayer() ?: return
        val aspectRatio = player.prepareForSystemPictureInPicture() ?: return
        presentPlayer(PlayerPresentation.FULLSCREEN)
        val params =
            PictureInPictureParams
                .Builder()
                .setAspectRatio(aspectRatio)
                .apply {
                    player.pictureInPictureSourceRect()?.let(::setSourceRectHint)
                }.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setSeamlessResizeEnabled(true)
                        setAutoEnterEnabled(true)
                    }
                }.build()
        if (!enterPictureInPictureMode(params)) {
            player.onSystemPictureInPictureModeChanged(false)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (activePlayer()?.shouldAutoEnterPictureInPicture() == true) {
            enterPlayerPictureInPicture()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) disableAutoEnterPictureInPicture()
        activePlayer()?.onSystemPictureInPictureModeChanged(isInPictureInPictureMode)
    }

    internal fun closePlayer(fragment: PlayerFragment) {
        if (fragment.isAdded) {
            supportFragmentManager.beginTransaction().remove(fragment).commitNowAllowingStateLoss()
        }
        binding.playerFragmentContainer.isVisible = false
        binding.bottomNavMain.isVisible = true
        binding.fragmentContainerMain.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        setSystemBarsHidden(false)
        disableAutoEnterPictureInPicture()
    }

    private fun showDestination(
        @IdRes destinationId: Int,
    ) {
        val tag = destinationId.toString()
        val existing = supportFragmentManager.findFragmentByTag(tag)
        val target = existing ?: createDestination(destinationId)

        supportFragmentManager
            .beginTransaction()
            .apply {
                supportFragmentManager.fragments
                    .filterNot { it.tag == PlayerFragment.TAG }
                    .forEach(::hide)
                if (existing == null) {
                    add(R.id.fragmentContainerMain, target, tag)
                } else {
                    show(target)
                }
                setPrimaryNavigationFragment(target)
            }.commitNow()
    }

    private fun createDestination(
        @IdRes destinationId: Int,
    ): Fragment =
        when (destinationId) {
            R.id.home -> {
                HomeTabFragment()
            }

            R.id.tvcab -> {
                PlaceholderFragment.newInstance(
                    title = getString(R.string.nav_music),
                    description = getString(R.string.placeholder_music),
                )
            }

            R.id.shorts -> {
                PlaceholderFragment.newInstance(
                    title = getString(R.string.nav_shorts),
                    description = getString(R.string.placeholder_shorts),
                )
            }

            R.id.playlist -> {
                PlaceholderFragment.newInstance(
                    title = getString(R.string.nav_playlist),
                    description = getString(R.string.placeholder_playlist),
                )
            }

            else -> {
                HomeTabFragment()
            }
        }

    private fun activePlayer(): PlayerFragment? =
        supportFragmentManager.findFragmentByTag(PlayerFragment.TAG) as? PlayerFragment

    private fun setSystemBarsHidden(hidden: Boolean) {
        isPlayerExpanded = hidden
        applyRootInsets()
        WindowInsetsControllerCompat(window, binding.root).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (hidden) {
                hide(WindowInsetsCompat.Type.systemBars())
            } else {
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
        binding.root.requestApplyInsets()
    }

    private fun applyRootInsets() {
        binding.root.setPadding(
            0,
            if (isPlayerExpanded) 0 else systemBarInsets.top,
            0,
            if (isPlayerExpanded) 0 else systemBarInsets.bottom,
        )
    }

    private fun disableAutoEnterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setPictureInPictureParams(
                PictureInPictureParams.Builder().setAutoEnterEnabled(false).build(),
            )
        }
    }

    private companion object {
        const val STATE_SELECTED_DESTINATION = "selected_destination"
    }
}
