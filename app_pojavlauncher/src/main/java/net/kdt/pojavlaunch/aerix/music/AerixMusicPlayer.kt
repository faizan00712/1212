/*
 * Aerix Launcher UI layer for PojavLauncher.
 * The Music section's player: one MediaPlayer for the whole launcher, so the sound keeps going
 * while you walk from the list to the library, and while the launcher is in the background.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package net.kdt.pojavlaunch.aerix.music

import android.content.Context
import android.media.MediaPlayer
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * What the Music section plays, kept outside the composable.
 *
 * A track that has been downloaded plays from its file; every other track plays from the copy that
 * sits inside the APK. The two sound identical - the file is the same MP3.
 */
object AerixMusicPlayer {
    private var player: MediaPlayer? = null
    private var ticker: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** The track loaded right now, whether it is playing or paused. */
    var current by mutableStateOf<MusicTrack?>(null)
        private set
    var playing by mutableStateOf(false)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    /** Set when a track could not be started, so the row can say so instead of failing quietly. */
    var error by mutableStateOf<String?>(null)
        private set

    /** Flipping this restarts the whole list from the top when the last track ends. */
    var repeatAll by mutableStateOf(true)
        private set

    /**
     * Bumped after a download finishes. The list recomputes its saved badges from this, because
     * whether a file is on disk is not something a composable can observe by itself.
     */
    var savedRevision by mutableIntStateOf(0)
        private set

    fun toggleRepeat() {
        repeatAll = !repeatAll
    }

    fun clearError() {
        error = null
    }

    /** Called after a download wrote a track's file, so its row shows the saved badge at once. */
    fun markSaved() {
        savedRevision++
    }

    /**
     * Starts [track], or pauses it when it is already the one playing.
     */
    fun toggle(context: Context, track: MusicTrack) {
        if (current?.id == track.id) {
            if (playing) pause() else resume(context)
            return
        }
        play(context, track)
    }

    fun play(context: Context, track: MusicTrack) {
        error = null
        stopTicker()
        releasePlayer()
        current = track
        positionMs = 0L
        durationMs = track.seconds * 1000L
        val created = MediaPlayer()
        player = created
        try {
            val file = MusicDownloads.savedFile(context, track)
            if (file.isFile && file.length() > 0) {
                created.setDataSource(file.absolutePath)
            } else {
                val descriptor = context.resources.openRawResourceFd(track.rawRes)
                    ?: error("The track could not be opened")
                descriptor.use { created.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            }
            created.setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            created.isLooping = false
            // A finished track moves straight on to the next one, so a listen does not stop
            // halfway through the list. The last track either starts the list again or stays
            // stopped at its own end, depending on the switch beside it.
            created.setOnCompletionListener {
                playing = false
                positionMs = durationMs
                nextAfterCompletion(context)
            }
            created.setOnErrorListener { _, _, _ ->
                playing = false
                error = "This track could not be played"
                true
            }
            created.prepare()
            durationMs = created.duration.toLong().takeIf { it > 0 } ?: track.seconds * 1000L
            created.start()
            playing = true
            startTicker()
        } catch (e: Exception) {
            playing = false
            error = e.message ?: "This track could not be played"
            releasePlayer()
        }
    }

    private fun nextAfterCompletion(context: Context) {
        val list = AerixMusic.tracks
        val index = list.indexOfFirst { it.id == current?.id }
        if (index < 0) return
        val last = index == list.lastIndex
        if (last && !repeatAll) {
            // The list ends here: the last track stays loaded, stopped at its own end.
            positionMs = durationMs
            return
        }
        play(context, list[(index + 1) % list.size])
    }

    fun resume(context: Context) {
        val active = player ?: run {
            current?.let { play(context, it) }
            return
        }
        runCatching {
            active.start()
            playing = true
            startTicker()
        }.onFailure {
            error = "This track could not be played"
        }
    }

    fun pause() {
        playing = false
        stopTicker()
        runCatching { player?.takeIf { it.isPlaying }?.pause() }
        positionMs = player?.currentPosition?.toLong() ?: positionMs
    }

    /** Stops the sound and unloads the track; the list goes back to its resting state. */
    fun stop() {
        stopTicker()
        releasePlayer()
        current = null
        playing = false
        positionMs = 0L
        durationMs = 0L
    }

    /** Jumps to [millis] of the current track. */
    fun seekTo(context: Context, millis: Long) {
        val active = player ?: return
        val target = millis.coerceIn(0L, durationMs)
        runCatching { active.seekTo(target.toInt()) }
        positionMs = target
        if (!playing) return
        // Some codecs need a nudge after a seek while playing.
        runCatching { if (!active.isPlaying) active.start() }
    }

    private fun startTicker() {
        stopTicker()
        ticker = scope.launch {
            while (isActive) {
                val active = player
                if (active != null && playing) {
                    positionMs = runCatching { active.currentPosition.toLong() }.getOrDefault(positionMs)
                }
                delay(400)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun releasePlayer() {
        val active = player
        player = null
        if (active != null) {
            runCatching { if (active.isPlaying) active.stop() }
            runCatching { active.reset() }
            runCatching { active.release() }
        }
    }
}
