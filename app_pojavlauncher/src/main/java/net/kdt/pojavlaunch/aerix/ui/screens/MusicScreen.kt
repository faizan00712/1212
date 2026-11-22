package net.kdt.pojavlaunch.aerix.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.components.LuciferGlassButton
import net.kdt.pojavlaunch.aerix.components.ModrinthCompactSearchField
import net.kdt.pojavlaunch.aerix.components.luciferGlass
import net.kdt.pojavlaunch.aerix.music.LuciferMusic
import net.kdt.pojavlaunch.aerix.music.LuciferMusicApi
import net.kdt.pojavlaunch.aerix.music.MusicTrack
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing

/** Music: every song of the catalog in a list with search; each row plays, stops and downloads its song. */
@Composable
fun MusicScreen(activity: FragmentActivity) {
    var query by rememberSaveable { mutableStateOf("") }
    var songs by remember { mutableStateOf(LuciferMusic.lastTrending) }
    var loading by remember { mutableStateOf(songs.isEmpty()) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }

    LaunchedEffect(query, retry) {
        val text = query.trim()
        if (text.isEmpty() && retry == 0 && LuciferMusic.lastTrending.isNotEmpty()) {
            songs = LuciferMusic.lastTrending
            loading = false
            failed = false
            return@LaunchedEffect
        }
        if (text.isNotEmpty()) delay(450)
        loading = true
        failed = false
        withContext(Dispatchers.IO) { runCatching { LuciferMusicApi.fetch(text) } }
            .onSuccess {
                songs = it
                if (text.isEmpty()) LuciferMusic.lastTrending = it
            }
            .onFailure { failed = true }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AerixSpacing.md, vertical = AerixSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.aerix_ic_music),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = "MUSIC",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
            Text(
                text = if (query.isBlank()) "TRENDING \u00B7 ${songs.size}" else "RESULTS \u00B7 ${songs.size}",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            ModrinthCompactSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search songs",
                modifier = Modifier.width(260.dp)
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                songs.isEmpty() && loading -> CenterNote("Loading songs") { CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp)) }
                songs.isEmpty() && failed -> CenterNote("Could not load songs. Check your connection") {
                    LuciferGlassButton(text = "RETRY", onClick = { retry++ }, height = 38.dp, fontSize = 11.sp)
                }
                songs.isEmpty() -> CenterNote("No songs found", null)
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 340.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(songs, key = { it.id }) { track -> SongBox(activity, track) }
                }
            }
            if (loading && songs.isNotEmpty()) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.align(Alignment.TopEnd).size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CenterNote(text: String, extra: (@Composable () -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        extra?.invoke()
        Text(text = text, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, letterSpacing = 1.sp)
    }
}

/** One box: song photo, song name, then the play, stop and download buttons. */
@Composable
private fun SongBox(activity: FragmentActivity, track: MusicTrack) {
    val active = LuciferMusic.current?.id == track.id
    val playing = active && LuciferMusic.playing
    val buffering = active && LuciferMusic.buffering
    val cover = rememberRemoteBitmap(track.coverUrl, 120)
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .luciferGlass(shape, strong = active)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (cover != null) {
                    Image(
                        bitmap = cover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.aerix_ic_music),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist + if (track.durationSec > 0) " \u00B7 " + clock(track.durationSec * 1000) else "",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (buffering) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            }
            RoundButton(
                iconRes = if (playing) R.drawable.lucifer_ic_pause else R.drawable.lucifer_ic_play,
                description = if (playing) "Pause" else "Play",
                strong = playing
            ) { LuciferMusic.play(activity, track) }
            RoundButton(
                iconRes = R.drawable.lucifer_ic_stop,
                description = "Stop",
                alpha = if (active) 1f else 0.4f
            ) { if (active) LuciferMusic.stop(activity) }
            RoundButton(
                iconRes = R.drawable.aerix_ic_download,
                description = "Download"
            ) { LuciferMusic.download(activity, track) }
        }
        if (active) {
            val total = LuciferMusic.durationMs.coerceAtLeast(1)
            val fraction = (LuciferMusic.positionMs.toFloat() / total).coerceIn(0f, 1f)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(clock(LuciferMusic.positionMs), color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(Color.White)
                    )
                }
                Text(clock(total), color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun RoundButton(
    iconRes: Int,
    description: String,
    strong: Boolean = false,
    alpha: Float = 1f,
    size: Dp = 38.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .luciferGlass(CircleShape, strong)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = Color.White.copy(alpha = alpha),
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun clock(ms: Int): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
