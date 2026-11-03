package net.kdt.pojavlaunch.aerix.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.components.luciferGlass
import net.kdt.pojavlaunch.aerix.guide.AerixGuideState
import net.kdt.pojavlaunch.aerix.guide.guideAnchor

/** Left column of glass buttons, the landscape replacement of the bottom dock. */
@Composable
fun LuciferSideNav(
    selectedSection: LauncherSection,
    onNavigate: (LauncherSection) -> Unit,
    onCreateInstance: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(190.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 4.dp, bottom = 6.dp)
                .clickable(role = Role.Button) { onNavigate(LauncherSection.HOME) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_aerix_mark),
                contentDescription = "Lucifer Launcher",
                modifier = Modifier.size(38.dp)
            )
            Column {
                Text("LUCIFER", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                Text("LAUNCHER", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            }
        }
        NavButton(R.drawable.aerix_ic_home_filled, stringResource(R.string.aerix_nav_home),
            selectedSection == LauncherSection.HOME) { onNavigate(LauncherSection.HOME) }
        NavButton(R.drawable.aerix_ic_client_crystal, "Aerix Client",
            selectedSection == LauncherSection.AERIX_CLIENT) { onNavigate(LauncherSection.AERIX_CLIENT) }
        NavButton(R.drawable.aerix_ic_add, stringResource(R.string.create_instance),
            selectedSection == LauncherSection.CREATE_INSTANCE, AerixGuideState.CREATE, onCreateInstance)
        NavButton(R.drawable.aerix_ic_dashboard_filled, stringResource(R.string.aerix_nav_library),
            librarySelected(selectedSection), AerixGuideState.LIBRARY) { onNavigate(LauncherSection.LIBRARY) }
        NavButton(R.drawable.aerix_ic_public, stringResource(R.string.aerix_nav_discover),
            discoverSelected(selectedSection)) { onNavigate(LauncherSection.DISCOVER) }
        NavButton(R.drawable.aerix_ic_rocket_launch_filled, "Other releases",
            selectedSection == LauncherSection.OTHER_RELEASES) { onNavigate(LauncherSection.OTHER_RELEASES) }
        NavButton(R.drawable.aerix_ic_format_paint_outlined, stringResource(R.string.aerix_nav_wallpapers),
            wallpapersSelected(selectedSection), AerixGuideState.WALLPAPERS) { onNavigate(LauncherSection.WALLPAPERS) }
        NavButton(R.drawable.aerix_ic_settings_filled, stringResource(R.string.aerix_nav_settings),
            settingsSelected(selectedSection), AerixGuideState.SETTINGS) { onNavigate(LauncherSection.SETTINGS) }
        NavButton(R.drawable.aerix_ic_group_outlined, stringResource(R.string.aerix_nav_contribution),
            selectedSection == LauncherSection.CONTRIBUTION || selectedSection == LauncherSection.CONTRIBUTION_DETAIL) {
            onNavigate(LauncherSection.CONTRIBUTION)
        }
        NavButton(R.drawable.aerix_ic_person_outlined, stringResource(R.string.aerix_nav_accounts),
            selectedSection == LauncherSection.ACCOUNTS || selectedSection == LauncherSection.ADD_ACCOUNT ||
                selectedSection == LauncherSection.OFFLINE_ACCOUNT) { onNavigate(LauncherSection.ACCOUNTS) }
    }
}

@Composable
private fun NavButton(
    iconRes: Int,
    label: String,
    selected: Boolean,
    guideKey: String? = null,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .then(if (guideKey != null) Modifier.guideAnchor(guideKey) else Modifier)
            .fillMaxWidth()
            .height(40.dp)
            .luciferGlass(shape, strong = selected)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White.copy(alpha = if (selected) 1f else 0.85f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label.uppercase(),
            color = Color.White.copy(alpha = if (selected) 1f else 0.85f),
            fontSize = 12.sp,
            letterSpacing = 1.4.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
