/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Copying a track out of the APK into a file the phone keeps: what the download button on a Music
 * row does.
 *
 * The MP3 inside the APK is the same file the player streams, so what lands on the phone is the
 * finished song - nothing is re-encoded and nothing is fetched from a link that could stop working.
 * On Android 10 and newer the file is published to the public Music folder through MediaStore, so it
 * shows up in any music player straight away. On older versions it goes to the app's external
 * Music folder, which needs no storage permission.
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

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Where a track ended up after a successful save. */
data class SavedTrack(val displayName: String, val folder: String, val uri: Uri?)

object MusicDownloads {
    /** The id of the track being written right now, so only its row shows a bar. */
    var downloadingId by mutableStateOf<String?>(null)
        private set

    /** 0..1 of the current write. */
    var progress by mutableFloatStateOf(0f)
        private set

    /** The last failure, for the banner under the header. */
    var failure by mutableStateOf<String?>(null)
        private set

    /** "Music/Aerix" on Android 10+, the app's own Music folder before that. */
    private const val PUBLIC_FOLDER = "Music/Aerix"

    fun clearFailure() {
        failure = null
    }

    /** The private copy that playback prefers once a track has been downloaded. */
    fun savedFile(context: Context, track: MusicTrack): File =
        File(File(context.filesDir, "music"), track.file)

    /** True when this track has already been written to the phone. */
    fun isSaved(context: Context, track: MusicTrack): Boolean {
        val copy = savedFile(context, track)
        return copy.isFile && copy.length() > 0
    }

    /**
     * Writes [track] to the phone and returns where it went.
     *
     * The bytes come out of the APK in chunks so the progress bar moves, and the finished file is
     * checked against the size of the packaged MP3 before it is announced as saved.
     */
    suspend fun save(context: Context, track: MusicTrack): SavedTrack? = withContext(Dispatchers.IO) {
        if (downloadingId != null) return@withContext null
        downloadingId = track.id
        progress = 0f
        failure = null
        val privateCopy = savedFile(context, track)
        val partial = File(privateCopy.parentFile, "${track.file}.part")
        var pendingUri: Uri? = null
        // The size of the MP3 inside the APK, so the bar knows where the end is.
        val total = runCatching {
            context.resources.openRawResourceFd(track.rawRes)?.use { it.length.toLong() } ?: 0L
        }.getOrDefault(0L)
        try {
            privateCopy.parentFile?.mkdirs()
            partial.parentFile?.mkdirs()
            val result = context.resources.openRawResource(track.rawRes).use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        written += read
                        // The packaged MP3's length is the target: the same bytes, in the same order.
                        if (total > 0) progress = (written.toFloat() / total).coerceIn(0f, 0.99f)
                    }
                    written
                }
            }
            require(result > 0) { "The track could not be read" }
            if (privateCopy.exists()) privateCopy.delete()
            require(partial.renameTo(privateCopy)) { "The track could not be saved" }
            progress = 0.99f

            val saved = if (Build.VERSION.SDK_INT >= 29) publishToMediaStore(context, track, privateCopy)
            else publishToAppMusicFolder(context, track, privateCopy)
            pendingUri = saved.uri
            progress = 1f
            AerixMusicPlayer.markSaved()
            saved
        } catch (e: Exception) {
            partial.delete()
            pendingUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            failure = e.message ?: "The download failed"
            null
        } finally {
            downloadingId = null
        }
    }

    /**
     * Publishes the written file as a song in Music/Aerix, so the phone's own gallery and any music
     * player see it without waiting for a media scan.
     */
    private fun publishToMediaStore(context: Context, track: MusicTrack, source: File): SavedTrack {
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        // Re-saving the same track replaces the old copy instead of leaving two of them behind.
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.Audio.Media._ID),
            "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?",
            arrayOf(track.file, "%$PUBLIC_FOLDER%"),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val existing = android.content.ContentUris.withAppendedId(collection, cursor.getLong(0))
                runCatching { context.contentResolver.delete(existing, null, null) }
            }
        }
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, track.file)
            put(MediaStore.Audio.Media.TITLE, track.title)
            put(MediaStore.Audio.Media.ARTIST, "Aerix Launcher")
            put(MediaStore.Audio.Media.ALBUM, "Aerix Music")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, PUBLIC_FOLDER)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(context.contentResolver.insert(collection, values)) {
            "The Music folder is not available"
        }
        try {
            val descriptor = requireNotNull(context.contentResolver.openFileDescriptor(uri, "w")) {
                "The track could not be written"
            }
            descriptor.use { target ->
                java.io.FileOutputStream(target.fileDescriptor).use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                }
            }
            context.contentResolver.update(
                uri,
                ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                    put(MediaStore.Audio.Media.SIZE, source.length())
                },
                null,
                null
            )
        } catch (e: Exception) {
            runCatching { context.contentResolver.delete(uri, null, null) }
            throw e
        }
        return SavedTrack(track.file, PUBLIC_FOLDER, uri)
    }

    private fun publishToAppMusicFolder(context: Context, track: MusicTrack, source: File): SavedTrack {
        val folder = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir,
            "Aerix"
        )
        check(folder.isDirectory || folder.mkdirs()) { "The Music folder is not available" }
        val target = File(folder, track.file)
        source.inputStream().use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("audio/mpeg"), null)
        return SavedTrack(track.file, target.parent ?: "", Uri.fromFile(target))
    }
}
