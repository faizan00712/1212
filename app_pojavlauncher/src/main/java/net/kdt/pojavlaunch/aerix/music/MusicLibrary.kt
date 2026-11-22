/*
 * Aerix Launcher UI layer for PojavLauncher.
 * The Music section's catalogue: twenty original instrumentals rendered for this launcher and
 * shipped inside the APK, so the section works with no connection and nothing to fetch. Eight of
 * them are written in the raags and rhythms Indian film music uses; the other twelve are the
 * Minecraft-flavoured set.
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

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.ui.graphics.Color
import net.kdt.pojavlaunch.R
import java.util.Locale

/**
 * One track of the Music section.
 *
 * @param file name the saved MP3 carries once it is downloaded to the phone
 * @param rawRes the encoded MP3 inside the APK, so playback starts without touching the network
 * @param cover square artwork, small enough to draw a whole list without a visible delay
 */
data class MusicTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val seconds: Int,
    val bpm: Int,
    val file: String,
    @RawRes val rawRes: Int,
    @DrawableRes val cover: Int,
    val accent: Color
) {
    /** "2:14", the length the row shows before the track has ever been played. */
    val lengthLabel: String get() = formatTrackTime(seconds * 1000L)

    /** The row's searchable text: title and mood line, matched case-insensitively. */
    fun matches(query: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        return title.contains(needle, ignoreCase = true) ||
            subtitle.contains(needle, ignoreCase = true) ||
            keywords.contains(needle, ignoreCase = true)
    }
}

/** Milliseconds as m:ss, used by the rows, the mini player and the seek bar. */
fun formatTrackTime(millis: Long): String {
    val total = (millis / 1000L).coerceAtLeast(0L)
    return String.format(Locale.US, "%d:%02d", total / 60L, total % 60L)
}

object AerixMusic {
    /**
     * Every track of the section, in the order the list shows them.
     *
     * The audio and the artwork are both the launcher's own, so the download button hands the
     * player a real MP3 rather than a link that might stop working later.
     */
    val tracks: List<MusicTrack> = listOf(
        // ---- Indian set -------------------------------------------------------------------
        // Eight originals in the raags and rhythms Indian film songs are built on. The words in
        // the subtitle line are there so a search finds them in Hindi or in English.
        MusicTrack(
            id = "chandni_raat",
            title = "Chandni Raat",
            subtitle = "चाँदनी रात · Moonlit night · Raag Yaman · 76 BPM",
            seconds = 59,
            bpm = 76,
            file = "Chandni Raat.mp3",
            keywords = "moonlight moonlit night raat chandni romantic evening",
            indian = true,
            rawRes = R.raw.aerix_song_chandni_raat,
            cover = R.drawable.aerix_cover_chandni_raat,
            accent = Color(0xFFBFD4E8)
        ),
        MusicTrack(
            id = "monsoon_aangan",
            title = "Monsoon Aangan",
            subtitle = "मानसून आँगन · First rain · Raag Pahadi · 84 BPM",
            seconds = 54,
            bpm = 84,
            file = "Monsoon Aangan.mp3",
            keywords = "rain monsoon barish barsaat saawan courtyard romantic",
            indian = true,
            rawRes = R.raw.aerix_song_monsoon_aangan,
            cover = R.drawable.aerix_cover_monsoon_aangan,
            accent = Color(0xFF8FC4B0)
        ),
        MusicTrack(
            id = "desi_dhoon",
            title = "Desi Dhoon",
            subtitle = "देसी धुन · Festival tune · Raag Khamaj · 102 BPM",
            seconds = 43,
            bpm = 102,
            file = "Desi Dhoon.mp3",
            keywords = "desi festival tune folk dhol wedding celebration",
            indian = true,
            rawRes = R.raw.aerix_song_desi_dhoon,
            cover = R.drawable.aerix_cover_desi_dhoon,
            accent = Color(0xFFF2B25E)
        ),
        MusicTrack(
            id = "ghungroo",
            title = "Ghungroo",
            subtitle = "घुंघरू · Dance beat · Raag Bhairavi · 112 BPM",
            seconds = 39,
            bpm = 112,
            file = "Ghungroo.mp3",
            keywords = "dance ghungroo anklet bells navratri garba beat",
            indian = true,
            rawRes = R.raw.aerix_song_ghungroo,
            cover = R.drawable.aerix_cover_ghungroo,
            accent = Color(0xFFE06A5C)
        ),
        MusicTrack(
            id = "bansuri_ki_dhun",
            title = "Bansuri Ki Dhun",
            subtitle = "बाँसुरी की धुन · Flute theme · Raag Desh · 66 BPM",
            seconds = 68,
            bpm = 66,
            file = "Bansuri Ki Dhun.mp3",
            keywords = "flute bansuri instrumental peaceful theme",
            indian = true,
            rawRes = R.raw.aerix_song_bansuri_ki_dhun,
            cover = R.drawable.aerix_cover_bansuri_ki_dhun,
            accent = Color(0xFFA8C0B8)
        ),
        MusicTrack(
            id = "shaadi_baraat",
            title = "Shaadi Baraat",
            subtitle = "शादी बारात · Wedding march · Raag Khamaj · 118 BPM",
            seconds = 37,
            bpm = 118,
            file = "Shaadi Baraat.mp3",
            keywords = "wedding shaadi baraat marriage march celebration",
            indian = true,
            rawRes = R.raw.aerix_song_shaadi_baraat,
            cover = R.drawable.aerix_cover_shaadi_baraat,
            accent = Color(0xFFF0707A)
        ),
        MusicTrack(
            id = "yaadon_ki_shaam",
            title = "Yaadon Ki Shaam",
            subtitle = "यादों की शाम · Evening of memories · Raag Bhairavi · 72 BPM",
            seconds = 63,
            bpm = 72,
            file = "Yaadon Ki Shaam.mp3",
            keywords = "memories yaad nostalgia sad evening old song",
            indian = true,
            rawRes = R.raw.aerix_song_yaadon_ki_shaam,
            cover = R.drawable.aerix_cover_yaadon_ki_shaam,
            accent = Color(0xFFD9A17C)
        ),
        MusicTrack(
            id = "rajasthan_route",
            title = "Rajasthan Route",
            subtitle = "राजस्थान · Desert road · Raag Kirwani · 126 BPM",
            seconds = 35,
            bpm = 126,
            file = "Rajasthan Route.mp3",
            keywords = "rajasthan desert road travel folk dholak energetic",
            indian = true,
            rawRes = R.raw.aerix_song_rajasthan_route,
            cover = R.drawable.aerix_cover_rajasthan_route,
            accent = Color(0xFFE8A05C)
        ),

        // ---- Minecraft set ---------------------------------------------------------------
        MusicTrack(
            id = "dawn_over_the_valley",
            title = "Dawn Over The Valley",
            subtitle = "Warm sunrise · 84 BPM",
            seconds = 88,
            bpm = 84,
            file = "Dawn Over The Valley.mp3",
            rawRes = R.raw.aerix_song_dawn_over_the_valley,
            cover = R.drawable.aerix_cover_dawn_over_the_valley,
            accent = Color(0xFFE8B36A)
        ),
        MusicTrack(
            id = "glass_rain",
            title = "Glass Rain",
            subtitle = "Rainy night · 72 BPM",
            seconds = 102,
            bpm = 72,
            file = "Glass Rain.mp3",
            rawRes = R.raw.aerix_song_glass_rain,
            cover = R.drawable.aerix_cover_glass_rain,
            accent = Color(0xFF7FB4D8)
        ),
        MusicTrack(
            id = "pixel_harbour",
            title = "Pixel Harbour",
            subtitle = "Breezy docks · 104 BPM",
            seconds = 71,
            bpm = 104,
            file = "Pixel Harbour.mp3",
            rawRes = R.raw.aerix_song_pixel_harbour,
            cover = R.drawable.aerix_cover_pixel_harbour,
            accent = Color(0xFF6FD9C4)
        ),
        MusicTrack(
            id = "midnight_mine",
            title = "Midnight Mine",
            subtitle = "Deep caves · 92 BPM",
            seconds = 80,
            bpm = 92,
            file = "Midnight Mine.mp3",
            rawRes = R.raw.aerix_song_midnight_mine,
            cover = R.drawable.aerix_cover_midnight_mine,
            accent = Color(0xFF8F7BE8)
        ),
        MusicTrack(
            id = "lantern_fields",
            title = "Lantern Fields",
            subtitle = "Evening fields · 96 BPM",
            seconds = 77,
            bpm = 96,
            file = "Lantern Fields.mp3",
            rawRes = R.raw.aerix_song_lantern_fields,
            cover = R.drawable.aerix_cover_lantern_fields,
            accent = Color(0xFFE8C86A)
        ),
        MusicTrack(
            id = "snowfall_lullaby",
            title = "Snowfall Lullaby",
            subtitle = "Snowy night · 64 BPM",
            seconds = 114,
            bpm = 64,
            file = "Snowfall Lullaby.mp3",
            rawRes = R.raw.aerix_song_snowfall_lullaby,
            cover = R.drawable.aerix_cover_snowfall_lullaby,
            accent = Color(0xFFBFE0F2)
        ),
        MusicTrack(
            id = "redstone_heartbeat",
            title = "Redstone Heartbeat",
            subtitle = "Pulse · 112 BPM",
            seconds = 66,
            bpm = 112,
            file = "Redstone Heartbeat.mp3",
            rawRes = R.raw.aerix_song_redstone_heartbeat,
            cover = R.drawable.aerix_cover_redstone_heartbeat,
            accent = Color(0xFFF0705F)
        ),
        MusicTrack(
            id = "cloud_kite",
            title = "Cloud Kite",
            subtitle = "Bright sky · 100 BPM",
            seconds = 74,
            bpm = 100,
            file = "Cloud Kite.mp3",
            rawRes = R.raw.aerix_song_cloud_kite,
            cover = R.drawable.aerix_cover_cloud_kite,
            accent = Color(0xFF9FD8A0)
        ),
        MusicTrack(
            id = "enders_garden",
            title = "Ender's Garden",
            subtitle = "Far away · 76 BPM",
            seconds = 97,
            bpm = 76,
            file = "Ender's Garden.mp3",
            rawRes = R.raw.aerix_song_enders_garden,
            cover = R.drawable.aerix_cover_enders_garden,
            accent = Color(0xFFC0A0F0)
        ),
        MusicTrack(
            id = "moonlit_crafting",
            title = "Moonlit Crafting",
            subtitle = "Quiet night · 88 BPM",
            seconds = 84,
            bpm = 88,
            file = "Moonlit Crafting.mp3",
            rawRes = R.raw.aerix_song_moonlit_crafting,
            cover = R.drawable.aerix_cover_moonlit_crafting,
            accent = Color(0xFFA8B8F0)
        ),
        MusicTrack(
            id = "sunrise_sprint",
            title = "Sunrise Sprint",
            subtitle = "Fast start · 118 BPM",
            seconds = 63,
            bpm = 118,
            file = "Sunrise Sprint.mp3",
            rawRes = R.raw.aerix_song_sunrise_sprint,
            cover = R.drawable.aerix_cover_sunrise_sprint,
            accent = Color(0xFFF2A26A)
        ),
        MusicTrack(
            id = "the_long_way_home",
            title = "The Long Way Home",
            subtitle = "Slow walk · 68 BPM",
            seconds = 108,
            bpm = 68,
            file = "The Long Way Home.mp3",
            rawRes = R.raw.aerix_song_the_long_way_home,
            cover = R.drawable.aerix_cover_the_long_way_home,
            accent = Color(0xFFE2C08A)
        )
    )

    fun byId(id: String?): MusicTrack? = if (id == null) null else tracks.firstOrNull { it.id == id }

    /** Total running time of the section, in whole minutes. */
    val totalMinutes: Int get() = (tracks.sumOf { it.seconds } + 30) / 60

    fun search(query: String): List<MusicTrack> =
        if (query.isBlank()) tracks else tracks.filter { it.matches(query) }
}
