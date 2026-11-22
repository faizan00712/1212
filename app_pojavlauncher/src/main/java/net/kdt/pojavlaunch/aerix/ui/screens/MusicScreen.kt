/*
 * Aerix Launcher UI layer for PojavLauncher.
 * The Music section: the twelve instrumentals that ship with the launcher, one row per song with its
 * artwork, its name, a play/pause button and a stop button, and a search field above the list.
 *
 * The player itself lives in AerixMusicPlayer, outside the composition, so the sound keeps going
 * when the section is left or the launcher goes to the background. A row's download button writes
 * the song's own MP3 to the phone's Music folder.
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

package net.kdt.pojavlaunch.aerix.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.components.liquidGlass
import net.kdt.pojavlaunch.aerix.music.AerixMusic
import net.kdt.pojavlaunch.aerix.music.AerixMusicPlayer
import net.kdt.pojavlaunch.aerix.music.MusicDownloads
import net.kdt.pojavlaunch.aerix.music.MusicTrack
import net.kdt.pojavlaunch.aerix.music.formatTrackTime
import net.kdt.pojavlaunch.aerix.theme.AerixMetrics
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.theme.AerixThemeManager

@Composable
fun MusicScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = AerixThemeManager.currentAccent()
    var query by remember { mutableStateOf("") }
    var savedTick by remember { mutableStateOf(0) }

    val playingTrack = AerixMusicPlayer.current
    val playing = AerixMusicPlayer.playing
    val savedRevision = AerixMusicPlayer.savedRevision
    // Which tracks already have a file on the phone. Recomputed only when a download finishes,
    // because the check is a disk call and there are twelve of them.
    val savedIds = remember(savedRevision, savedTick) {
        AerixMusic.tracks.filter { MusicDownloads.isSaved(context, it) }.map { it.id }.toSet()
    }
    val results = remember(query) { AerixMusic.search(query) }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(AerixSpacing.smPlus)) {
        MusicHeader(playingTrack = playingTrack)
        MusicSearchField(
            query = query,
            onQueryChange = { query = it },
            hint = "Search songs by name…",
            modifier = Modifier.fillMaxWidth()
        )

        val failure = MusicDownloads.failure
        if (failure != null) {
            Surface(
                shape = RoundedCornerShape(AerixRadii.cardSmall),
                color = AerixSurface.danger.copy(alpha = 0.14f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AerixSpacing.md, vertical = AerixSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.aerix_ic_warning_outlined),
                        contentDescription = null,
                        tint = AerixSurface.danger,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(AerixSpacing.sm))
                    Text(
                        text = "Download failed: $failure",
                        color = AerixSurface.textPrimary,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Dismiss",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { MusicDownloads.clearFailure() }
                    )
                }
            }
        }
        if (playingTrack != null) {
            NowPlayingBar(
                track = playingTrack,
                playing = playing,
                accent = accent,
                onToggle = { AerixMusicPlayer.toggle(context, playingTrack) },
                onStop = { AerixMusicPlayer.stop() },
                onSeek = { AerixMusicPlayer.seekTo(context, it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // List on the left, the section's own panel on the right once there is room for it.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // The list needs about 430dp of its own before the side panel fits next to it.
            val showSide = maxWidth >= 700.dp
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(AerixSpacing.md)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (results.isEmpty()) {
                        EmptySearch(query)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = AerixSpacing.xl),
                            verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
                        ) {
                            items(results, key = { it.id }) { track ->
                                val isSaved = track.id in savedIds
                                SongRow(
                                    track = track,
                                    isCurrent = playingTrack?.id == track.id,
                                    isPlaying = playingTrack?.id == track.id && playing,
                                    saved = isSaved,
                                    downloading = MusicDownloads.downloadingId == track.id,
                                    onToggle = {
                                        AerixMusicPlayer.clearError()
                                        AerixMusicPlayer.toggle(context, track)
                                    },
                                    onStop = {
                                        if (playingTrack?.id == track.id) AerixMusicPlayer.stop()
                                    },
                                    onDownload = {
                                        if (isSaved) {
                                            Toast.makeText(
                                                context,
                                                "\"${track.title}\" is already saved on this phone",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            scope.launch {
                                                val saved = MusicDownloads.save(context, track)
                                                if (saved != null) {
                                                    savedTick++
                                                    Toast.makeText(
                                                        context,
                                                        "Saved to ${saved.folder}/${saved.displayName}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                if (showSide) {
                    MusicSidePanel(
                        accent = accent,
                        trackCount = AerixMusic.tracks.size,
                        savedCount = savedIds.size,
                        playingTitle = playingTrack?.title,
                        repeatAll = AerixMusicPlayer.repeatAll,
                        onToggleRepeat = { AerixMusicPlayer.toggleRepeat() },
                        modifier = Modifier.widthIn(min = 220.dp, max = 268.dp).fillMaxHeight()
                    )
                }
            }
        }
    }
}

/** Title, the count of songs, and the note about where the sound comes from. */
@Composable
private fun MusicHeader(playingTrack: MusicTrack?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.md)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smPlus)
        ) {
            // The section's logo: the note, in plain white, on the same glass well the rail uses.
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(AerixRadii.control))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(
                        BorderStroke(AerixSpacing.hairline, Color.White.copy(alpha = 0.22f)),
                        RoundedCornerShape(AerixRadii.control)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.aerix_ic_music_note_filled),
                    contentDescription = "Music",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = "Music",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (playingTrack != null) "Now playing: ${playingTrack.title}"
                    else "${AerixMusic.tracks.size} songs · ${AerixMusic.totalMinutes} min · playable offline",
                    color = AerixSurface.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** The search field above the list. One box, the magnifier, the text and a clear button. */
@Composable
private fun MusicSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier
) {
    val accent = AerixThemeManager.currentAccent()
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(AerixRadii.control))
            .background(AerixSurface.panelRaised)
            .border(AerixSpacing.hairline, AerixSurface.borderSoft, RoundedCornerShape(AerixRadii.control))
            .padding(horizontal = AerixSpacing.smPlus),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smCompact)
    ) {
        Icon(
            painter = painterResource(R.drawable.aerix_ic_search),
            contentDescription = null,
            tint = AerixSurface.textSecondary,
            modifier = Modifier.size(16.dp)
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = hint,
                    color = AerixSurface.textMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {}),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                painter = painterResource(R.drawable.aerix_ic_close),
                contentDescription = "Clear search",
                tint = AerixSurface.textSecondary,
                modifier = Modifier
                    .size(15.dp)
                    .clickable { onQueryChange("") }
            )
        }
    }
}

/** One song: artwork, name, play/pause, stop, length and the download button. */
@Composable
private fun SongRow(
    track: MusicTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    saved: Boolean,
    downloading: Boolean,
    onToggle: () -> Unit,
    onStop: () -> Unit,
    onDownload: () -> Unit
) {
    val accent = AerixThemeManager.currentAccent()
    val shape = RoundedCornerShape(AerixRadii.cardSmall)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCurrent) Modifier.liquidGlass(
                    shape = shape,
                    tint = accent,
                    strength = 0.86f,
                    elevation = AerixMetrics.glassSubtleElevation
                ) else Modifier
            ),
        shape = shape,
        color = if (isCurrent) accent.copy(alpha = 0.12f) else AerixSurface.panel,
        contentColor = AerixSurface.textPrimary,
        border = BorderStroke(
            if (isCurrent) 1.5.dp else AerixSpacing.hairline,
            if (isCurrent) accent else AerixSurface.borderSoft
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AerixSpacing.smPlus, vertical = AerixSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smPlus)
            ) {
                // ( Song photo )
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(AerixRadii.controlSmall))
                        .border(
                            AerixSpacing.hairline,
                            if (isCurrent) accent else AerixSurface.borderSoft,
                            RoundedCornerShape(AerixRadii.controlSmall)
                        )
                ) {
                    Image(
                        painter = painterResource(track.cover),
                        contentDescription = "${track.title} artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))))
                        )
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.aerix_ic_pause else R.drawable.aerix_ic_play_arrow_filled
                            ),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                // ( song name )
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
                        text = track.subtitle,
                        color = AerixSurface.textSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smCompact)
                    ) {
                        Text(
                            text = if (isCurrent) formatTrackTime(AerixMusicPlayer.positionMs) else track.lengthLabel,
                            color = if (isCurrent) accent else AerixSurface.textMuted,
                            fontSize = 10.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                        if (saved) {
                            Icon(
                                painter = painterResource(R.drawable.aerix_ic_check_circle),
                                contentDescription = "Saved on this phone",
                                tint = AerixSurface.success,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                // ( play button ) ( stop button ) and the download button.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smCompact)
                ) {
                    CircleAction(
                        icon = if (isPlaying) R.drawable.aerix_ic_pause else R.drawable.aerix_ic_play_arrow_filled,
                        label = if (isPlaying) "Pause ${track.title}" else "Play ${track.title}",
                        filled = true,
                        accent = accent,
                        onClick = onToggle
                    )
                    CircleAction(
                        icon = R.drawable.aerix_ic_stop_filled,
                        label = "Stop ${track.title}",
                        filled = false,
                        accent = accent,
                        enabled = isCurrent,
                        onClick = onStop
                    )
                    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        if (downloading) {
                            // The bar for the track being written; only this row reads the value.
                            LinearProgressIndicator(
                                progress = { MusicDownloads.progress },
                                modifier = Modifier
                                    .width(30.dp)
                                    .height(3.dp)
                                    .clip(CircleShape),
                                color = accent,
                                trackColor = AerixSurface.panelTrack
                            )
                        } else {
                            CircleAction(
                                icon = if (saved) R.drawable.aerix_ic_check_circle else R.drawable.aerix_ic_download,
                                label = if (saved) "${track.title} is saved" else "Download ${track.title}",
                                filled = false,
                                accent = if (saved) AerixSurface.success else accent,
                                enabled = true,
                                onClick = onDownload
                            )
                        }
                    }
                }
            }
            if (isCurrent) {
                val fraction = if (AerixMusicPlayer.durationMs > 0L) {
                    (AerixMusicPlayer.positionMs.toFloat() / AerixMusicPlayer.durationMs).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = accent,
                    trackColor = AerixSurface.panelTrack
                )
            }
        }
    }
}

/** Round play, pause, stop and download buttons. */
@Composable
private fun CircleAction(
    icon: Int,
    label: String,
    filled: Boolean,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val background = when {
        !enabled -> AerixSurface.panelRaised.copy(alpha = 0.45f)
        filled -> accent
        else -> Color.White.copy(alpha = 0.09f)
    }
    val tint = when {
        !enabled -> AerixSurface.textMuted.copy(alpha = 0.6f)
        filled -> AerixSurface.onAccent
        else -> Color.White
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(background)
            .border(
                BorderStroke(
                    AerixSpacing.hairline,
                    if (filled && enabled) Color.Transparent else AerixSurface.borderSoft
                ),
                CircleShape
            )
            .clickable(role = Role.Button, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(17.dp)
        )
    }
}

/** The bar under the header: what is playing, with its own play/pause, stop and seek. */
@Composable
private fun NowPlayingBar(
    track: MusicTrack,
    playing: Boolean,
    accent: Color,
    onToggle: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AerixRadii.cardSmall)
    val duration = AerixMusicPlayer.durationMs.takeIf { it > 0L } ?: (track.seconds * 1000L)
    val position = AerixMusicPlayer.positionMs.coerceIn(0L, duration)
    Surface(
        modifier = modifier.liquidGlass(
            shape = shape,
            tint = accent,
            strength = 0.9f,
            elevation = AerixMetrics.glassSubtleElevation
        ),
        shape = shape,
        color = accent.copy(alpha = 0.10f),
        contentColor = AerixSurface.textPrimary,
        border = BorderStroke(AerixSpacing.hairline, accent.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(horizontal = AerixSpacing.smPlus, vertical = AerixSpacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smPlus)
            ) {
                Image(
                    painter = painterResource(track.cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(AerixRadii.controlSmall))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (playing) "Playing" else "Paused",
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${formatTrackTime(position)} / ${formatTrackTime(duration)}",
                    color = AerixSurface.textSecondary,
                    fontSize = 10.sp
                )
                CircleAction(
                    icon = if (playing) R.drawable.aerix_ic_pause else R.drawable.aerix_ic_play_arrow_filled,
                    label = if (playing) "Pause" else "Play",
                    filled = true,
                    accent = accent,
                    onClick = onToggle
                )
                CircleAction(
                    icon = R.drawable.aerix_ic_stop_filled,
                    label = "Stop",
                    filled = false,
                    accent = accent,
                    onClick = onStop
                )
            }
            // A bar you drag across to move through the song. The drag is taken here so the page
            // behind the bar does not scroll at the same time.
            var dragging by remember { mutableStateOf(false) }
            var dragFraction by remember { mutableStateOf(0f) }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .pointerInput(duration) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            dragging = true
                            dragFraction = (down.position.x / size.width).coerceIn(0f, 1f)
                            drag(down.id) { change ->
                                dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                change.consume()
                            }
                            onSeek((dragFraction * duration).toLong())
                            dragging = false
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                val fraction = if (dragging) dragFraction else {
                    (position.toFloat() / duration.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(AerixSurface.panelTrack)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Box(
                    modifier = Modifier
                        .offset(x = maxWidth * fraction - 6.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
        }
    }
}

/** The panel beside the list, when the screen is wide enough for one. */
@Composable
private fun MusicSidePanel(
    accent: Color,
    trackCount: Int,
    savedCount: Int,
    playingTitle: String?,
    repeatAll: Boolean,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AerixRadii.cardSmall)
    Surface(
        modifier = modifier,
        shape = shape,
        color = AerixSurface.panel,
        contentColor = AerixSurface.textPrimary,
        border = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft)
    ) {
        Column(
            modifier = Modifier.padding(AerixSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
        ) {
            Text(
                text = "YOUR LIBRARY",
                color = AerixSurface.textSecondary,
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.ExtraBold
            )
            StatLine("Songs", "$trackCount")
            StatLine("Saved on this phone", "$savedCount")
            StatLine("Playing", playingTitle ?: "Nothing")
            Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(AerixSurface.borderSoft))
            Text(
                text = "When the last song ends",
                color = AerixSurface.textSecondary,
                fontSize = 10.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AerixRadii.controlSmall))
                    .background(if (repeatAll) accent.copy(alpha = 0.16f) else AerixSurface.panelRaised)
                    .border(
                        AerixSpacing.hairline,
                        if (repeatAll) accent else AerixSurface.borderSoft,
                        RoundedCornerShape(AerixRadii.controlSmall)
                    )
                    .clickable(role = Role.Switch, onClick = onToggleRepeat)
                    .padding(horizontal = AerixSpacing.sm, vertical = AerixSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                Icon(
                    painter = painterResource(R.drawable.aerix_ic_replay),
                    contentDescription = null,
                    tint = if (repeatAll) accent else AerixSurface.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (repeatAll) "Start the list again" else "Stop after the last song",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(AerixSurface.borderSoft))
            Text(
                text = "Every song is written for Aerix and ships inside the launcher. The download " +
                    "button saves the same MP3 to Music/Aerix on this phone, so it plays in any music " +
                    "player, offline.",
                color = AerixSurface.textMuted,
                fontSize = 10.sp,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AerixSurface.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(
            text = value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Shown when the search matches nothing. */
@Composable
private fun EmptySearch(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AerixSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.aerix_ic_music_note_filled),
            contentDescription = null,
            tint = AerixSurface.textSecondary,
            modifier = Modifier.size(34.dp)
        )
        Spacer(Modifier.height(AerixSpacing.sm))
        Text(
            text = if (query.isBlank()) "No songs here yet" else "No song matches \"$query\"",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Try another word from the song's name.",
            color = AerixSurface.textSecondary,
            fontSize = 11.sp
        )
    }
}
