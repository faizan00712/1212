/*
 * Aerix Launcher UI layer for PojavLauncher.
 * "Create New Instance" page overhauled following mirai-launcher-:
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/download/game/SelectGameVersionScreen.kt
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/download/game/DownloadGameWithAddonScreen.kt
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/download/game/DownloadGameScreen.kt
 * Every install is executed by net.kdt.pojavlaunch.aerix.bridge.PojavCreateInstanceBridge, which calls the
 * PojavLauncher backend paths of the installer fragments.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package net.kdt.pojavlaunch.aerix.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import net.kdt.pojavlaunch.aerix.components.LuciferGlassButton
import net.kdt.pojavlaunch.aerix.components.luciferGlass
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.bridge.AerixIconCropper
import net.kdt.pojavlaunch.aerix.bridge.AerixLoader
import net.kdt.pojavlaunch.aerix.bridge.AerixLoaderVersion
import net.kdt.pojavlaunch.aerix.bridge.AerixVersionType
import net.kdt.pojavlaunch.aerix.bridge.PojavCreateInstanceBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavInstanceBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavInstaller
import net.kdt.pojavlaunch.aerix.bridge.PojavNavigator
import net.kdt.pojavlaunch.aerix.components.AerixGhostButton
import net.kdt.pojavlaunch.aerix.components.AerixImageWell
import net.kdt.pojavlaunch.aerix.components.AerixPrimaryButton
import net.kdt.pojavlaunch.aerix.components.ModrinthCompactSearchField
import net.kdt.pojavlaunch.aerix.components.ModrinthMetaPill
import net.kdt.pojavlaunch.aerix.components.QuietEmptyState
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.theme.AerixThemeManager

/** Version tabs, the same grouping Pojav's VersionListAdapter builds. */
private val versionTabs = listOf(
    AerixVersionType.INSTALLED to R.string.mcl_setting_veroption_installed,
    AerixVersionType.RELEASE to R.string.mcl_setting_veroption_release,
    AerixVersionType.SNAPSHOT to R.string.mcl_setting_veroption_snapshot,
    AerixVersionType.OLD_BETA to R.string.mcl_setting_veroption_oldbeta,
    AerixVersionType.OLD_ALPHA to R.string.mcl_setting_veroption_oldalpha
)

@Composable
fun CreateInstanceScreen(activity: FragmentActivity, onClose: () -> Unit) {
    val accent = AerixThemeManager.currentAccent()
    val revision = PojavCreateInstanceBridge.versionTableRevision
    val versions = remember(revision) { PojavCreateInstanceBridge.gameVersions() }

    var tab by rememberSaveable { mutableIntStateOf(1) }
    var filter by rememberSaveable { mutableStateOf("") }
    var selectedVersion by remember { mutableStateOf<String?>(null) }
    var loader by remember { mutableStateOf(AerixLoader.VANILLA) }
    var onlyStable by remember { mutableStateOf(true) }
    var name by rememberSaveable { mutableStateOf("") }
    var nameEdited by rememberSaveable { mutableStateOf(false) }
    var icon by remember { mutableStateOf<Bitmap?>(null) }
    var loaderVersions by remember { mutableStateOf<List<AerixLoaderVersion>?>(null) }
    var loaderBuild by remember { mutableStateOf<AerixLoaderVersion?>(null) }
    var loadingBuilds by remember { mutableStateOf(false) }
    var buildsFailed by remember { mutableStateOf(false) }

    LaunchedEffect(versions) {
        if (selectedVersion == null) {
            selectedVersion = versions.firstOrNull { it.type == AerixVersionType.RELEASE }?.id
                ?: versions.firstOrNull()?.id
        }
    }

    LaunchedEffect(loader, selectedVersion) {
        loaderBuild = null
        loaderVersions = null
        buildsFailed = false
        if (!loader.usesGameVersion) {
            loadingBuilds = true
            loaderVersions = withContext(Dispatchers.IO) {
                runCatching { PojavCreateInstanceBridge.loadLoaderVersions(loader, null) }
                    .onFailure { buildsFailed = true }
                    .getOrDefault(emptyList())
            }
            loadingBuilds = false
        } else if (selectedVersion != null) {
            loadingBuilds = true
            loaderVersions = withContext(Dispatchers.IO) {
                runCatching { PojavCreateInstanceBridge.loadLoaderVersions(loader, selectedVersion) }
                    .onFailure { buildsFailed = true }
                    .getOrDefault(emptyList())
            }
            loadingBuilds = false
        } else {
            loaderVersions = null
        }
    }

    val visibleBuilds = remember(loaderVersions, onlyStable) {
        loaderVersions?.filter { !onlyStable || it.stable } ?: emptyList()
    }
    LaunchedEffect(visibleBuilds) {
        loaderBuild = visibleBuilds.firstOrNull()
    }

    val suggestedName = remember(loader, selectedVersion, loaderBuild) {
        PojavCreateInstanceBridge.suggestedName(loader, selectedVersion, loaderBuild)
    }
    LaunchedEffect(suggestedName) {
        if (!nameEdited) name = suggestedName
    }

    // Vanilla has no builds to choose, so it is ready as soon as a Minecraft version is picked;
    // every other loader also needs the build of the loader itself.
    val canInstall = !PojavCreateInstanceBridge.installing &&
        selectedVersion != null &&
        (!loader.usesGameVersion || !loader.hasBuilds || loaderBuild != null)

    val filterText = filter.trim()
    val installedIds = remember(versions) {
        versions.filter { it.type == AerixVersionType.INSTALLED }.map { it.id }.toSet()
    }
    val releaseIds = remember(versions, filterText) {
        versions.filter { it.type == AerixVersionType.RELEASE && it.id.contains(filterText, ignoreCase = true) }.map { it.id }
    }
    val snapshotIds = remember(versions, filterText) {
        versions.filter { it.type == AerixVersionType.SNAPSHOT && it.id.contains(filterText, ignoreCase = true) }.map { it.id }
    }
    val oldIds = remember(versions, filterText) {
        versions.filter {
            (it.type == AerixVersionType.OLD_BETA || it.type == AerixVersionType.OLD_ALPHA) &&
                it.id.contains(filterText, ignoreCase = true)
        }.map { it.id }
    }
    val selectedType = versions.firstOrNull { it.id == selectedVersion && it.type != AerixVersionType.INSTALLED }?.type
    val onPick: (String) -> Unit = { selectedVersion = it }
    val showBuilds = loader != AerixLoader.VANILLA && loader.hasBuilds

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AerixSpacing.md, vertical = AerixSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .luciferGlass(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.aerix_ic_arrow_back),
                    contentDescription = stringResource(R.string.aerix_create_close),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "VERSIONS",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.weight(1f))
            ModrinthCompactSearchField(
                value = filter,
                onValueChange = { filter = it },
                placeholder = stringResource(R.string.aerix_create_filter_hint),
                modifier = Modifier.width(170.dp)
            )
            LuciferGlassButton(
                text = stringResource(R.string.aerix_create_import_modpack),
                onClick = { PojavNavigator.openInstaller(activity, PojavInstaller.MODPACK) },
                iconRes = R.drawable.aerix_ic_import_modpack,
                height = 36.dp,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DimensionPanel(
                title = "OVERWORLD",
                subtitle = "Releases \u00B7 ${releaseIds.size}",
                banner = Color(0xFF4C9A3F),
                ids = releaseIds,
                installed = installedIds,
                selected = selectedVersion,
                emptyText = stringResource(if (filter.isBlank() && versions.isEmpty()) R.string.aerix_create_versions_empty else R.string.aerix_create_filter_empty),
                onPick = onPick,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            DimensionPanel(
                title = "NETHER",
                subtitle = "Snapshots \u00B7 ${snapshotIds.size}",
                banner = Color(0xFFB03030),
                ids = snapshotIds,
                installed = installedIds,
                selected = selectedVersion,
                emptyText = stringResource(R.string.aerix_create_filter_empty),
                onPick = onPick,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            if (showBuilds) {
                DimensionPanel(
                    title = loader.displayName.uppercase(),
                    subtitle = "Loader builds \u00B7 ${visibleBuilds.size}",
                    banner = Color(0xFF7A45D6),
                    ids = visibleBuilds.map { it.id },
                    labels = visibleBuilds.associate { it.id to it.label },
                    installed = emptySet(),
                    selected = loaderBuild?.id,
                    emptyText = when {
                        loadingBuilds -> stringResource(R.string.aerix_create_loader_loading)
                        buildsFailed -> stringResource(R.string.aerix_create_builds_failed)
                        else -> stringResource(
                            R.string.aerix_create_loader_empty,
                            loader.displayName,
                            selectedVersion ?: stringResource(R.string.aerix_create_versions_empty)
                        )
                    },
                    onPick = { id -> loaderBuild = visibleBuilds.firstOrNull { it.id == id } },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    trailing = {
                        Text(
                            text = stringResource(R.string.aerix_create_only_stable).uppercase(),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            letterSpacing = 1.sp
                        )
                        Switch(
                            checked = onlyStable,
                            onCheckedChange = { onlyStable = it },
                            modifier = Modifier.height(22.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color.White,
                                uncheckedThumbColor = AerixSurface.textSecondary,
                                uncheckedTrackColor = AerixSurface.panelTrack
                            )
                        )
                    }
                )
            } else {
                DimensionPanel(
                    title = "THE END",
                    subtitle = "Old beta & alpha \u00B7 ${oldIds.size}",
                    banner = Color(0xFF7A45D6),
                    ids = oldIds,
                    installed = installedIds,
                    selected = selectedVersion,
                    emptyText = stringResource(R.string.aerix_create_filter_empty),
                    onPick = onPick,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .luciferGlass(RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AerixImageWell(
                    image = icon?.asImageBitmap(),
                    fallback = R.drawable.aerix_loader_vanilla,
                    size = 46.dp,
                    modifier = Modifier.clickable(role = Role.Button) {
                        AerixIconCropper.start(activity) { icon = it }
                    }
                )
                Column(modifier = Modifier.widthIn(min = 84.dp)) {
                    Text(
                        text = selectedVersion ?: "...",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Text(
                        text = when (selectedType) {
                            AerixVersionType.RELEASE -> "RELEASE"
                            AerixVersionType.SNAPSHOT -> "SNAPSHOT"
                            AerixVersionType.OLD_BETA -> "OLD BETA"
                            AerixVersionType.OLD_ALPHA -> "OLD ALPHA"
                            else -> "VERSION"
                        } + if (selectedVersion in installedIds) " \u00B7 INSTALLED" else "",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1.25f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AerixLoader.entries.forEach { entry ->
                        LoaderSlot(
                            loader = entry,
                            selected = entry == loader,
                            onClick = {
                                loader = entry
                                onlyStable = true
                            }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .height(40.dp)
                        .luciferGlass(RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameEdited = it.isNotEmpty()
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp, letterSpacing = 1.sp),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (name.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.aerix_create_name_hint),
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 13.sp
                                )
                            }
                            inner()
                        }
                    )
                }
                LuciferGlassButton(
                    text = stringResource(
                        if (PojavCreateInstanceBridge.installing) R.string.aerix_create_installing
                        else R.string.aerix_create_install_short
                    ),
                    onClick = {
                        if (canInstall) {
                            PojavCreateInstanceBridge.install(
                                context = activity,
                                loader = loader,
                                gameVersion = selectedVersion,
                                loaderVersion = loaderBuild,
                                name = name,
                                icon = icon
                            ) { created ->
                                if (created) onClose() else PojavInstanceBridge.reload()
                            }
                        }
                    },
                    iconRes = R.drawable.aerix_ic_download,
                    height = 46.dp,
                    strong = true,
                    modifier = Modifier.alpha(if (canInstall) 1f else 0.4f)
                )
            }

        }
    }
}

@Composable
private fun DimensionPanel(
    title: String,
    subtitle: String,
    banner: Color,
    ids: List<String>,
    installed: Set<String>,
    selected: String?,
    emptyText: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    labels: Map<String, String> = emptyMap(),
    trailing: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null
) {
    Column(modifier = modifier.luciferGlass(RoundedCornerShape(22.dp))) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Brush.verticalGradient(listOf(banner.copy(alpha = 0.55f), banner.copy(alpha = 0.08f))))
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp
                )
                Text(
                    text = subtitle.uppercase(),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (trailing != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    content = trailing
                )
            }
        }
        if (ids.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emptyText, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(76.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(ids, key = { it }) { id ->
                    VersionSlot(
                        id = id,
                        label = labels[id] ?: id,
                        installed = id in installed,
                        selected = id == selected,
                        onClick = { onPick(id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionSlot(id: String, label: String, installed: Boolean, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(shape)
            .background(if (selected) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.45f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = Color.White.copy(alpha = if (selected) 1f else 0.22f),
                shape = shape
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (installed) {
            Spacer(Modifier.width(4.dp))
            Text(text = "\u2713", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LoaderSlot(loader: AerixLoader, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(shape)
                .background(if (selected) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.45f))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = Color.White.copy(alpha = if (selected) 1f else 0.22f),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(loader.iconRes),
                contentDescription = loader.displayName,
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = loader.displayName.uppercase(),
            color = Color.White.copy(alpha = if (selected) 1f else 0.65f),
            fontSize = 7.sp,
            letterSpacing = 0.8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
