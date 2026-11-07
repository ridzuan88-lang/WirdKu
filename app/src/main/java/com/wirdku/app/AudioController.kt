package com.wirdku.app

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/** Plays audio bundled in assets (asset:///...). Never synthesizes speech. */
class AudioController(ctx: Context) {
    private val player = ExoPlayer.Builder(ctx.applicationContext).build()

    var currentPath: String? = null
        private set
    var clipped: Boolean = false
        private set
    fun isFull(path: String?) = path != null && currentPath == path && !clipped

    fun playClip(assetPath: String, startMs: Long, endMs: Long?, speed: Float = 1f, repeat: Boolean = false) {
        val cb = MediaItem.ClippingConfiguration.Builder().setStartPositionMs(startMs)
        if (endMs != null) cb.setEndPositionMs(endMs)
        val item = MediaItem.Builder().setUri(Uri.parse("asset:///$assetPath")).setClippingConfiguration(cb.build()).build()
        player.setMediaItem(item)
        player.repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.playbackParameters = PlaybackParameters(speed)
        player.prepare()
        player.play()
        currentPath = assetPath; clipped = true
    }

    val isPlaying get() = player.isPlaying
    val positionMs get() = player.currentPosition
    val durationMs get() = if (player.duration < 0) 0L else player.duration

    fun play(assetPath: String, speed: Float = 1f, repeat: Boolean = false) {
        player.setMediaItem(MediaItem.fromUri(Uri.parse("asset:///$assetPath")))
        player.repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.playbackParameters = PlaybackParameters(speed)
        player.prepare()
        player.play()
        currentPath = assetPath; clipped = false
    }
    fun toggle() { if (player.isPlaying) player.pause() else player.play() }
    fun stop() { player.stop(); player.clearMediaItems(); currentPath = null; clipped = false }
    fun seek(ms: Long) = player.seekTo(ms)
    fun setSpeed(s: Float) { player.playbackParameters = PlaybackParameters(s) }
    fun setRepeat(r: Boolean) { player.repeatMode = if (r) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }
    fun release() = player.release()
}
