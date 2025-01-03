package com.youxel.library

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.youxel.library.globalEnums.EnumAspectRatio
import com.youxel.library.globalEnums.EnumMute
import com.youxel.library.globalEnums.EnumRepeatMode
import com.youxel.library.globalEnums.EnumResizeMode
import com.youxel.library.globalEnums.EnumScreenMode
import com.youxel.library.globalInterfaces.AndExoPlayerListener
import com.youxel.library.utils.DoubleClick
import com.youxel.library.utils.DoubleClickListener
import com.youxel.library.utils.PublicFunctions
import com.youxel.library.utils.PublicValues
import com.youxel.library.utils.loadImage

class AndExoPlayerView(
    context: Context,
    attributeSet: AttributeSet
) : AndExoPlayerRoot(context, attributeSet), Player.Listener {

    private lateinit var currSource: String
    private var player: SimpleExoPlayer = SimpleExoPlayer.Builder(context).build()
    private var andExoPlayerListener: AndExoPlayerListener? = null
    private var currPlayWhenReady: Boolean = true
    private var playbackPosition: Long = 0
    private var currentWindow: Int = 0
    private var currVolume: Float = 0f

    override var customClickListener: DoubleClick
        get() = DoubleClick(object : DoubleClickListener {
            override fun onSingleClickEvent(view: View) {
                when (view.id) {
                    retryButton.id -> {
                        hideRetryView()
                        restartPlayer()
                    }

                    mute.id -> {
                        unMutePlayer()
                    }

                    unMute.id -> {
                        mutePlayer()
                    }

                    enterFullScreen.id -> {
                        setScreenMode(EnumScreenMode.FULLSCREEN)
                    }

                    exitFullScreen.id -> {
                        setScreenMode(EnumScreenMode.MINIMISE)
                    }
                }
            }

            override fun onDoubleClickEvent(view: View) {
                when (view.id) {
                    backwardView.id -> {
                        seekBackward()
                    }

                    forwardView.id -> {
                        seekForward()
                    }
                }
            }
        })
        set(value) {}

    init {
        player.addListener(this)
        extractAttrs(attributeSet)
    }

    private fun extractAttrs(attributeSet: AttributeSet?) {

        attributeSet.let {

            val typedArray: TypedArray =
                context.obtainStyledAttributes(it, R.styleable.AndExoPlayerView)

            if (typedArray.hasValue(R.styleable.AndExoPlayerView_andexo_aspect_ratio)) {
                val aspectRatio = typedArray.getInteger(
                    R.styleable.AndExoPlayerView_andexo_aspect_ratio,
                    EnumAspectRatio.ASPECT_16_9.value
                )
                setAspectRatio(EnumAspectRatio[aspectRatio])
            }

            if (typedArray.hasValue(R.styleable.AndExoPlayerView_andexo_resize_mode)) {
                val resizeMode: Int = typedArray.getInteger(
                    R.styleable.AndExoPlayerView_andexo_resize_mode,
                    EnumResizeMode.FILL.value
                )
                setResizeMode(EnumResizeMode[resizeMode])
            }

            if (typedArray.hasValue(R.styleable.AndExoPlayerView_andexo_play_when_ready)) {
                setPlayWhenReady(
                    typedArray.getBoolean(
                        R.styleable.AndExoPlayerView_andexo_play_when_ready,
                        true
                    )
                )
            }

            if (typedArray.hasValue(R.styleable.AndExoPlayerView_andexo_mute)) {
                val muteValue = typedArray.getInteger(
                    R.styleable.AndExoPlayerView_andexo_mute,
                    EnumMute.UNMUTE.value
                )
                setMute(EnumMute[muteValue])
            }

            if (typedArray.hasValue(R.styleable.AndExoPlayerView_andexo_show_controller)) {
                setShowControllers(
                    typedArray.getBoolean(
                        R.styleable.AndExoPlayerView_andexo_show_controller,
                        true
                    )
                )
            }

            if (typedArray.hasValue(R.styleable.AndExoPlayerView_andexo_show_full_screen)) {
                setShowFullScreenButton(
                    typedArray.getBoolean(
                        R.styleable.AndExoPlayerView_andexo_show_full_screen,
                        true
                    )
                )
            }

            typedArray.recycle()
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
    }

    override fun onPlayerError(error: PlaybackException) {
        showRetryView(error.message)
        andExoPlayerListener?.let {
            andExoPlayerListener!!.onExoPlayerError(errorMessage = error.message)
        }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> {
                andExoPlayerListener?.onExoBuffering()
            }

            ExoPlayer.STATE_ENDED -> {
                andExoPlayerListener?.onExoEnded()
            }

            ExoPlayer.STATE_IDLE -> {
                andExoPlayerListener?.onExoIdle()
            }

            ExoPlayer.STATE_READY -> {
                andExoPlayerListener?.onExoReady()
            }

            else -> {

            }
        }
    }

    override fun onLoadingChanged(isLoading: Boolean) {
    }

    override fun onPositionDiscontinuity(reason: Int) {
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
    }

    fun releasePlayer() {
        currPlayWhenReady = player.playWhenReady
        playbackPosition = player.currentPosition
        currentWindow = player.currentWindowIndex
        player.stop()
        player.release()
    }

    private fun restartPlayer() {
        player.seekTo(0);
        player.prepare()
    }

    private fun buildMediaItem(source: String, extraHeaders: HashMap<String, String>): MediaItem {

        return when (PublicFunctions.getMimeTypeV2(source)) {

            PublicValues.KEY_MP4 -> buildMediaItemMP4(source, extraHeaders)
            PublicValues.KEY_M3U8 -> buildMediaHLS(source, extraHeaders)
            PublicValues.KEY_MP3 -> buildMediaItemMP4(source, extraHeaders)

            else -> buildMediaGlobal(source, extraHeaders)
        }
    }

    private fun buildMediaItemMP4(
        source: String,
        extraHeaders: HashMap<String, String>
    ): MediaItem {
        return MediaItem.Builder()
            .setUri(source)
            .setMimeType(MimeTypes.APPLICATION_MP4)
            .setDrmLicenseRequestHeaders(extraHeaders)
            .build()
    }

    private fun buildMediaHLS(source: String, extraHeaders: HashMap<String, String>): MediaItem {
        return MediaItem.Builder()
            .setUri(source)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .setDrmLicenseRequestHeaders(extraHeaders)
            .build()
    }

    private fun buildMediaDash(source: String, extraHeaders: HashMap<String, String>): MediaItem {
        return MediaItem.Builder()
            .setUri(source)
            .setMimeType(MimeTypes.APPLICATION_MPD)
            .setDrmLicenseRequestHeaders(extraHeaders)
            .build()
    }

    private fun buildMediaGlobal(source: String, extraHeaders: HashMap<String, String>): MediaItem {
        return MediaItem.Builder()
            .setUri(source)
            .setDrmLicenseRequestHeaders(extraHeaders)
            .build()
    }

    fun setAndExoPlayerListener(andExoPlayerListener: AndExoPlayerListener) {
        this.andExoPlayerListener = andExoPlayerListener
    }

    fun setSource(
        source: String,
        mediaItem: MediaItem? = null,
        extraHeaders: HashMap<String, String> = hashMapOf<String, String>()
    ) {

        currSource = source
        playerView.player = player
        player.playWhenReady = currPlayWhenReady
        if (mediaItem != null) {
            player.setMediaItem(mediaItem)
        } else {
            player.setMediaItem(buildMediaItem(source, extraHeaders))
        }
        player.prepare()
    }

    fun setSourceWithToken(
        source: String,
        token: String,
    ) {
        val mediaItem = MediaItem.Builder()
            .setUri(source)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))

        val mediaSource = ProgressiveMediaSource.Factory(
            httpDataSourceFactory
        ).createMediaSource(mediaItem)

        player.apply {
            setMediaSource(mediaSource)
            player.playWhenReady = currPlayWhenReady
            seekTo(0, 0L)
            prepare()
        }.also {
            playerView.player = it
        }
    }

    fun seekBackward(backwardValue: Int = 10000) {
        var seekValue = player.currentPosition - backwardValue
        if (seekValue < 0) seekValue = 0
        player.seekTo(seekValue)
    }

    fun seekForward(ForwardValue: Int = 10000) {
        var seekValue = player.currentPosition + ForwardValue
        if (seekValue > player.duration) seekValue = player.duration
        player.seekTo(seekValue)
    }

    fun setShowControllers(showControllers: Boolean = true) {
        if (showControllers)
            setShowTimeOut(4000)
        else
            setShowTimeOut(0)
    }

    fun setShowTimeOut(showTimeoutMs: Int) {
        playerView.controllerShowTimeoutMs = showTimeoutMs
        if (showTimeoutMs == 0)
            playerView.controllerHideOnTouch = false
    }

    fun setMute(mute: EnumMute) {
        when (mute) {
            EnumMute.MUTE -> {
                mutePlayer()
            }

            EnumMute.UNMUTE -> {
                unMutePlayer()
            }

            else -> {
                unMutePlayer()
            }
        }
    }

    fun mutePlayer() {
        currVolume = player.volume
        player.volume = 0f
        showMuteButton()
    }

    fun unMutePlayer() {
        player.volume = currVolume
        showUnMuteButton()
    }

    fun setRepeatMode(repeatMode: EnumRepeatMode = EnumRepeatMode.REPEAT_OFF) {
        this.currRepeatMode = repeatMode
        when (repeatMode) {
            EnumRepeatMode.REPEAT_OFF -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
            }

            EnumRepeatMode.REPEAT_ONE -> {
                player.repeatMode = Player.REPEAT_MODE_ONE
            }

            EnumRepeatMode.REPEAT_ALWAYS -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
            }

            else -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
            }
        }
    }

    fun setAspectRatio(aspectRatio: EnumAspectRatio) {
        this.currAspectRatio = aspectRatio
        val value = PublicFunctions.getScreenWidth()
        when (aspectRatio) {
            EnumAspectRatio.ASPECT_1_1 -> playerView.layoutParams =
                FrameLayout.LayoutParams(value, value)

            EnumAspectRatio.ASPECT_4_3 -> playerView.layoutParams =
                FrameLayout.LayoutParams(value, 3 * value / 4)

            EnumAspectRatio.ASPECT_16_9 -> playerView.layoutParams =
                FrameLayout.LayoutParams(value, 9 * value / 16)

            EnumAspectRatio.ASPECT_MATCH -> playerView.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            EnumAspectRatio.ASPECT_MP3 -> {
                playerView.controllerShowTimeoutMs = 0
                playerView.controllerHideOnTouch = false
                val mp3Height =
                    context.resources.getDimensionPixelSize(R.dimen.player_controller_base_height)
                playerView.layoutParams =
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mp3Height)
            }

            EnumAspectRatio.UNDEFINE -> {
                val baseHeight = resources.getDimension(R.dimen.player_base_height).toInt()
                playerView.layoutParams =
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, baseHeight)
            }
        }
    }

    fun setResizeMode(resizeMode: EnumResizeMode) {
        when (resizeMode) {
            EnumResizeMode.FIT -> playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            EnumResizeMode.FILL -> playerView.resizeMode =
                AspectRatioFrameLayout.RESIZE_MODE_FILL

            EnumResizeMode.ZOOM -> playerView.resizeMode =
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM

            else -> playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    fun setPlayWhenReady(playWhenReady: Boolean = true) {
        this.currPlayWhenReady = playWhenReady
        player.playWhenReady = playWhenReady
    }

    fun loadArtWort(imageUrl: String) {
        try {
            val imageView = this.findViewById<ImageView>(R.id.exo_artwork)
            playerView.useArtwork = true
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            val thumbnailUri = Uri.parse(imageUrl)
            imageView.loadImage(thumbnailUri) {
                playerView.defaultArtwork = it
            }
        } catch (e: Exception) {
            Log.d("artwork", "exo_artwork not found")
        }
    }

    fun loadArtWork(artWorkDrawableId: Int) {
        try {
            val imageView = this.findViewById<ImageView>(R.id.exo_artwork)
            playerView.useArtwork = true
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            playerView.defaultArtwork =
                AppCompatResources.getDrawable(context, artWorkDrawableId);
        } catch (e: Exception) {
            Log.d("artwork", "exo_artwork not found")
        }
    }

    fun loadArtWork(artWorkDrawable: Drawable) {
        try {
            val imageView = this.findViewById<ImageView>(R.id.exo_artwork)
            playerView.useArtwork = true
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            playerView.defaultArtwork = artWorkDrawable
        } catch (e: Exception) {
            Log.d("artwork", "exo_artwork not found")
        }
    }


    fun pausePlayer() {
        player.playWhenReady = false
        player.playbackState
    }

    fun stopPlayer() {
        player.stop()
        player.playbackState
    }

    fun startPlayer() {
        player.playWhenReady = true
        player.playbackState
    }

    fun setScreenMode(screenMode: EnumScreenMode = EnumScreenMode.MINIMISE) {

        when (screenMode) {
            EnumScreenMode.FULLSCREEN -> {
                getActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            }

            EnumScreenMode.MINIMISE -> {
                getActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            }

            else -> {
                getActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            }
        }

        currScreenMode = screenMode
        setShowScreenModeButton(currScreenMode)
    }

    fun getCurrentPlayerPosition(): Long {
        var currentPosition = 0L
        if (player.isPlaying)
            currentPosition = player.currentPosition
        return currentPosition
    }

    fun getActivity(): Activity? {
        var context = context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        releasePlayer()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        // Checking the orientation of the screen
        // Checking the orientation of the screen
        if (newConfig!!.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // First Hide other objects (list-view or recyclerview), better hide them using Gone.
            hideSystemUI()
            val params = playerView.layoutParams as FrameLayout.LayoutParams
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            playerView.layoutParams = params
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // un hide your objects here.
            showSystemUI()
            setAspectRatio(currAspectRatio)
        }
    }

}