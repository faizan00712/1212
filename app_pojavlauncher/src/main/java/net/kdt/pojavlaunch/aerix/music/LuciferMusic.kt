package net.kdt.pojavlaunch.aerix.music

import android.app.DownloadManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.kdt.pojavlaunch.utils.DownloadUtils
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One song of the catalog. */
class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationSec: Int,
    val coverUrl: String?
)

/** Song catalog: Audius, a public streaming catalog that needs no key. */
object LuciferMusicApi {
    private const val APP_NAME = "LUCIFER_LAUNCHER"
    private const val API = "https://api.audius.co/v1"

    fun streamUrl(track: MusicTrack): String = "$API/tracks/${track.id}/stream?app_name=$APP_NAME"

    /** Trending songs for a blank query, search results otherwise. */
    @Throws(IOException::class)
    fun fetch(query: String): List<MusicTrack> {
        val url = if (query.isBlank()) {
            "$API/tracks/trending?limit=60&app_name=$APP_NAME"
        } else {
            "$API/tracks/search?query=${URLEncoder.encode(query, "UTF-8")}&limit=60&app_name=$APP_NAME"
        }
        val data: org.json.JSONArray?
        try {
            data = JSONObject(DownloadUtils.downloadString(url)).optJSONArray("data")
        } catch (e: org.json.JSONException) {
            throw IOException("Unreadable song list", e)
        }
        if (data == null) return emptyList()
        val out = ArrayList<MusicTrack>()
        for (i in 0 until data.length()) {
            val t = data.optJSONObject(i) ?: continue
            val id = t.optString("id")
            if (id.isBlank() || t.optBoolean("is_delete") || t.optBoolean("is_unlisted")) continue
            if (!t.optBoolean("is_available", true) || t.optBoolean("is_stream_gated")) continue
            if (t.optJSONObject("access")?.optBoolean("stream", true) == false) continue
            val cover = t.optJSONObject("artwork")?.let { it.optString("480x480").ifBlank { it.optString("150x150") } }
            out.add(
                MusicTrack(
                    id = id,
                    title = t.optString("title").trim().ifBlank { "Untitled" },
                    artist = t.optJSONObject("user")?.optString("name").orEmpty().ifBlank { "Unknown artist" },
                    durationSec = t.optInt("duration", 0),
                    coverUrl = cover?.takeIf { it.isNotBlank() }
                )
            )
        }
        return out
    }

    /** Follows the stream redirect so the player and the downloader get the file address itself. */
    @Throws(IOException::class)
    fun resolve(url: String): String {
        var current = url
        repeat(6) {
            val conn = URL(current).openConnection() as HttpURLConnection
            try {
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 15000
                conn.readTimeout = 20000
                conn.setRequestProperty("User-Agent", DownloadUtils.USER_AGENT)
                val code = conn.responseCode
                if (code in 300..399) {
                    val next = conn.getHeaderField("Location") ?: throw IOException("Redirect without a target")
                    current = URL(URL(current), next).toString()
                } else if (code == HttpURLConnection.HTTP_OK) {
                    return current
                } else {
                    throw IOException("Server returned HTTP $code")
                }
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("Too many redirects")
    }
}

/** The one player of the launcher: it keeps playing when you leave the Music page. */
object LuciferMusic {
    var current by mutableStateOf<MusicTrack?>(null)
        private set
    var playing by mutableStateOf(false)
        private set
    var buffering by mutableStateOf(false)
        private set
    var positionMs by mutableIntStateOf(0)
        private set
    var durationMs by mutableIntStateOf(0)
        private set

    /** Trending list kept between visits so the page opens instantly. */
    var lastTrending: List<MusicTrack> = emptyList()

    @Volatile
    var onStateChanged: (() -> Unit)? = null

    private val main = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var generation = 0

    private val ticker = object : Runnable {
        override fun run() {
            val p = player ?: return
            if (!playing) return
            positionMs = runCatching { p.currentPosition }.getOrDefault(positionMs)
            main.postDelayed(this, 500)
        }
    }

    private fun changed() {
        onStateChanged?.invoke()
    }

    private fun toast(context: Context, text: String) {
        Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    /** Play [track]; pressing it again on the playing song pauses, and on a paused song resumes. */
    fun play(context: Context, track: MusicTrack) {
        val app = context.applicationContext
        if (current?.id == track.id && player != null && !buffering) {
            if (playing) pause() else resume()
            return
        }
        release()
        val gen = ++generation
        current = track
        buffering = true
        playing = false
        positionMs = 0
        durationMs = track.durationSec * 1000
        MusicService.start(app)
        changed()
        Thread {
            val url = try {
                LuciferMusicApi.resolve(LuciferMusicApi.streamUrl(track))
            } catch (e: Exception) {
                main.post { if (gen == generation) fail(app, "Could not load this song") }
                return@Thread
            }
            main.post { if (gen == generation) startPlayer(app, url, gen) }
        }.start()
    }

    private fun startPlayer(app: Context, url: String, gen: Int) {
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setWakeMode(app, android.os.PowerManager.PARTIAL_WAKE_LOCK)
            mp.setDataSource(url)
            mp.setOnPreparedListener {
                if (gen != generation) return@setOnPreparedListener
                it.start()
                playing = true
                buffering = false
                if (it.duration > 0) durationMs = it.duration
                main.removeCallbacks(ticker)
                main.post(ticker)
                changed()
            }
            mp.setOnCompletionListener { if (gen == generation) stop(app) }
            mp.setOnErrorListener { _, _, _ ->
                if (gen == generation) fail(app, "Could not play this song")
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            fail(app, "Could not play this song")
        }
    }

    fun pause() {
        val p = player ?: return
        runCatching { p.pause() }
        playing = false
        changed()
    }

    fun resume() {
        val p = player ?: return
        runCatching { p.start() }
        playing = true
        main.removeCallbacks(ticker)
        main.post(ticker)
        changed()
    }

    fun toggle() {
        if (playing) pause() else resume()
    }

    /** Stop and forget the song; also what happens when a song reaches its end. */
    fun stop(context: Context) {
        release()
        current = null
        playing = false
        buffering = false
        positionMs = 0
        durationMs = 0
        MusicService.stop(context.applicationContext)
        changed()
    }

    private fun fail(app: Context, message: String) {
        toast(app, message)
        stop(app)
    }

    private fun release() {
        generation++
        main.removeCallbacks(ticker)
        player?.let { p ->
            runCatching { p.setOnPreparedListener(null) }
            runCatching { p.setOnCompletionListener(null) }
            runCatching { p.setOnErrorListener(null) }
            runCatching { p.release() }
        }
        player = null
    }

    /** Saves the song into Music/LuciferLauncher through the system downloader. */
    fun download(context: Context, track: MusicTrack) {
        val app = context.applicationContext
        toast(app, "Downloading ${track.title}")
        Thread {
            try {
                val url = LuciferMusicApi.resolve(LuciferMusicApi.streamUrl(track))
                val name = track.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(80).ifBlank { "song" } + ".mp3"
                val manager = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                fun request(): DownloadManager.Request = DownloadManager.Request(Uri.parse(url))
                    .setTitle(track.title)
                    .setDescription(track.artist)
                    .setMimeType("audio/mpeg")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                try {
                    manager.enqueue(
                        request().setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "LuciferLauncher/$name")
                    )
                } catch (e: Exception) {
                    manager.enqueue(
                        request().setDestinationInExternalFilesDir(app, Environment.DIRECTORY_MUSIC, name)
                    )
                }
            } catch (e: Exception) {
                main.post { toast(app, "Download failed. Check your connection") }
            }
        }.start()
    }
}
