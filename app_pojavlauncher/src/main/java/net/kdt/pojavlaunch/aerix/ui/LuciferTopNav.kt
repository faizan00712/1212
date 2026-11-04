package net.kdt.pojavlaunch.aerix.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.bridge.AssetType
import net.kdt.pojavlaunch.aerix.bridge.PojavAccountBridge
import net.kdt.pojavlaunch.aerix.components.AerixImageWell
import net.kdt.pojavlaunch.aerix.components.luciferGlass
import net.kdt.pojavlaunch.aerix.guide.AerixGuideState
import net.kdt.pojavlaunch.aerix.guide.guideAnchor
import net.kdt.pojavlaunch.aerix.ui.screens.AerixDownloadRequest

private var discoverType by mutableStateOf<AssetType?>(null)

/** Top bar: brand on the left, a floating glass pill of sections in the middle, the player on the right. */
@Composable
fun LuciferTopNav(
    selectedSection: LauncherSection,
    onNavigate: (LauncherSection) -> Unit,
    onCreateInstance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val account = PojavAccountBridge.current
    val playerName = account?.username?.takeIf { it.isNotBlank() } ?: "PLAYER"
    val inDiscover = discoverSelected(selectedSection)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { onNavigate(LauncherSection.HOME) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_aerix_mark),
                    contentDescription = "Lucifer Launcher",
                    modifier = Modifier.size(34.dp)
                )
                Column {
                    Text("LUCIFER", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                    Text("LAUNCHER", color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                }
            }
        }
        Row(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .luciferGlass(RoundedCornerShape(26.dp))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 5.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopNavItem(R.drawable.aerix_ic_home_filled, "Home", selectedSection == LauncherSection.HOME) {
                onNavigate(LauncherSection.HOME)
            }
            TopNavItem(R.drawable.aerix_ic_client_crystal, "Client", selectedSection == LauncherSection.AERIX_CLIENT) {
                onNavigate(LauncherSection.AERIX_CLIENT)
            }
            TopNavItem(R.drawable.aerix_ic_dashboard_filled, "Versions", librarySelected(selectedSection), AerixGuideState.LIBRARY) {
                onNavigate(LauncherSection.LIBRARY)
            }
            TopNavItem(R.drawable.aerix_ic_add, "Install", selectedSection == LauncherSection.CREATE_INSTANCE, AerixGuideState.CREATE, onCreateInstance)
            TopNavItem(R.drawable.aerix_ic_extension_outlined, "Mods", inDiscover && discoverType != AssetType.SHADER_PACK) {
                discoverType = AssetType.MOD
                AerixDownloadRequest.open(AssetType.MOD, onNavigate)
            }
            TopNavItem(R.drawable.aerix_ic_lightbulb, "Shaders", inDiscover && discoverType == AssetType.SHADER_PACK) {
                discoverType = AssetType.SHADER_PACK
                AerixDownloadRequest.open(AssetType.SHADER_PACK, onNavigate)
            }
            TopNavItem(R.drawable.aerix_ic_folder_filled, "Files", selectedSection == LauncherSection.FILES) {
                onNavigate(LauncherSection.FILES)
            }
            TopNavItem(R.drawable.aerix_ic_rocket_launch_filled, "Releases", selectedSection == LauncherSection.OTHER_RELEASES) {
                onNavigate(LauncherSection.OTHER_RELEASES)
            }
            TopNavItem(R.drawable.aerix_ic_format_paint_outlined, "Wallpapers", wallpapersSelected(selectedSection), AerixGuideState.WALLPAPERS) {
                onNavigate(LauncherSection.WALLPAPERS)
            }
            TopNavItem(R.drawable.aerix_ic_settings_filled, "Settings", settingsSelected(selectedSection), AerixGuideState.SETTINGS) {
                onNavigate(LauncherSection.SETTINGS)
            }
            TopNavItem(
                R.drawable.aerix_ic_group_outlined, "Team",
                selectedSection == LauncherSection.CONTRIBUTION || selectedSection == LauncherSection.CONTRIBUTION_DETAIL
            ) { onNavigate(LauncherSection.CONTRIBUTION) }
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            val accountsActive = selectedSection == LauncherSection.ACCOUNTS ||
                selectedSection == LauncherSection.ADD_ACCOUNT || selectedSection == LauncherSection.OFFLINE_ACCOUNT
            Row(
                modifier = Modifier
                    .widthIn(max = 220.dp)
                    .luciferGlass(RoundedCornerShape(26.dp), strong = accountsActive)
                    .clickable(role = Role.Button) { onNavigate(LauncherSection.ACCOUNTS) }
                    .padding(start = 5.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AerixImageWell(image = account?.face, fallback = R.drawable.aerix_head_steve, size = 34.dp, radius = 17.dp)
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = playerName.uppercase(),
                        color = Color.White,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = account?.authType?.name?.uppercase() ?: "NO ACCOUNT",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 8.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                Box(Modifier.size(7.dp).background(if (account != null) Color.White else Color.White.copy(alpha = 0.3f), CircleShape))
            }
        }
    }
}

@Composable
private fun TopNavItem(
    iconRes: Int,
    label: String,
    selected: Boolean,
    guideKey: String? = null,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .then(if (guideKey != null) Modifier.guideAnchor(guideKey) else Modifier)
            .height(34.dp)
            .then(if (selected) Modifier.luciferGlass(shape, strong = true) else Modifier.clip(shape))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (selected) 12.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = Color.White.copy(alpha = if (selected) 1f else 0.78f),
            modifier = Modifier.size(18.dp)
        )
        if (selected) {
            Text(
                text = label.uppercase(),
                color = Color.White,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
