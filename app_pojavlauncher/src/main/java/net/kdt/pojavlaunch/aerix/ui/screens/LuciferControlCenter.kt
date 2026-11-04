package net.kdt.pojavlaunch.aerix.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.bridge.AerixPreferences
import net.kdt.pojavlaunch.aerix.bridge.PojavInstanceBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavLaunchBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavSettingsBridge
import net.kdt.pojavlaunch.aerix.components.LuciferGlassButton
import net.kdt.pojavlaunch.aerix.components.luciferGlass
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.ui.AerixNav
import net.kdt.pojavlaunch.prefs.LauncherPreferences

/** Settings landing page: a grid of glass tiles, each a switch, a slider or a shortcut to the full tab. */
@Composable
fun LuciferControlCenter(activity: FragmentActivity, onOpenTab: (String) -> Unit) {
    val revision = PojavSettingsBridge.revision
    val haptics = remember(revision) { LauncherPreferences.PREF_CONTROL_THEME_HAPTICS }
    val fps = remember(revision) { LauncherPreferences.PREF_SHOW_FPS }
    val resolution = remember(revision) { PojavSettingsBridge.resolutionPercent() }
    val rendererName = remember(revision) {
        runCatching {
            PojavSettingsBridge.renderers(activity)
                .firstOrNull { it.first == LauncherPreferences.PREF_RENDERER }?.second
        }.getOrNull() ?: LauncherPreferences.PREF_RENDERER ?: "AUTO"
    }
    val javaLabel = remember(revision) {
        val runtime = LauncherPreferences.PREF_DEFAULT_RUNTIME
        val digits = runtime?.let { Regex("\\d+").find(it)?.value }
        if (digits != null) "JAVA $digits" else "AUTO"
    }
    val language = remember { java.util.Locale.getDefault().displayLanguage.ifBlank { "SYSTEM" } }
    val ramLabel = remember(revision) {
        String.format("%.1f GB", LauncherPreferences.PREF_RAM_ALLOCATION / 1024f).replace(".0 GB", " GB")
    }
    val selected = PojavInstanceBridge.selected

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SwitchTile(
                title = "FOG GLOW",
                iconRes = R.drawable.aerix_ic_format_paint_outlined,
                checked = AerixPreferences.themeMotion,
                onChange = { AerixPreferences.saveThemeMotion(it) },
                modifier = Modifier.weight(1f)
            )
            SwitchTile(
                title = "HAPTICS",
                iconRes = R.drawable.aerix_ic_sports_esports_outlined,
                checked = haptics,
                onChange = { PojavSettingsBridge.writeBoolean(activity, "controlThemeHaptics", it) },
                modifier = Modifier.weight(1f)
            )
            SwitchTile(
                title = "DOTS",
                iconRes = R.drawable.aerix_ic_dashboard_outlined,
                checked = AerixPreferences.backgroundDots,
                onChange = { AerixPreferences.saveBackgroundDots(it) },
                modifier = Modifier.weight(1f)
            )
            SwitchTile(
                title = "FPS COUNTER",
                iconRes = R.drawable.aerix_ic_video_settings,
                checked = fps,
                onChange = { PojavSettingsBridge.writeBoolean(activity, "show_fps", it) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ResolutionTile(
                percent = resolution,
                onCommit = { PojavSettingsBridge.writeInt(activity, "resolutionRatio", it) },
                modifier = Modifier.weight(1f)
            )
            ChipTile(
                title = "LANGUAGE",
                iconRes = R.drawable.aerix_ic_info_outlined,
                chip = language.uppercase(),
                onClick = { openLanguageSettings(activity) },
                modifier = Modifier.weight(1f)
            )
            ChipTile(
                title = "JAVA RUNTIME",
                iconRes = R.drawable.aerix_ic_java,
                chip = javaLabel,
                onClick = { onOpenTab(AerixNav.TAB_JAVA) },
                modifier = Modifier.weight(1f)
            )
            ChipTile(
                title = "RENDERER",
                iconRes = R.drawable.aerix_ic_video_settings,
                chip = rendererName.uppercase(),
                onClick = { onOpenTab(AerixNav.TAB_RENDERER) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoChip(selected?.name ?: "NO INSTANCE", Modifier.widthIn(max = 180.dp))
            InfoChip(javaLabel)
            InfoChip(ramLabel)
            Spacer(Modifier.weight(1f))
            LuciferGlassButton(
                text = "LAUNCH",
                onClick = { if (selected != null) PojavLaunchBridge.play(activity) },
                iconRes = R.drawable.aerix_ic_rocket_launch_filled,
                height = 40.dp,
                strong = true,
                modifier = Modifier.widthIn(min = 150.dp)
            )
        }
    }
}

private fun openLanguageSettings(activity: FragmentActivity) {
    val appLocale = Intent("android.settings.APP_LOCALE_SETTINGS")
        .setData(Uri.fromParts("package", activity.packageName, null))
    try {
        activity.startActivity(appLocale)
    } catch (_: Exception) {
        runCatching { activity.startActivity(Intent(Settings.ACTION_LOCALE_SETTINGS)) }
    }
}

@Composable
private fun CenterTile(
    title: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .luciferGlass(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.6.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            content()
        }
    }
}

@Composable
private fun SwitchTile(
    title: String,
    @DrawableRes iconRes: Int,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier
) {
    CenterTile(title, iconRes, modifier, onClick = { onChange(!checked) }) {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.height(24.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = AerixSurface.textSecondary,
                uncheckedTrackColor = AerixSurface.panelTrack
            )
        )
    }
}

@Composable
private fun ChipTile(
    title: String,
    @DrawableRes iconRes: Int,
    chip: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    CenterTile(title, iconRes, modifier, onClick) {
        InfoChip(chip, Modifier.widthIn(max = 150.dp))
    }
}

@Composable
private fun ResolutionTile(percent: Int, onCommit: (Int) -> Unit, modifier: Modifier) {
    var value by remember(percent) { mutableFloatStateOf(percent.toFloat()) }
    CenterTile("RESOLUTION", R.drawable.aerix_ic_setting_launcher, modifier, onClick = {}) {
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onCommit(value.toInt()) },
            valueRange = 25f..100f,
            steps = 14,
            modifier = Modifier.height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        Text(
            text = "${value.toInt()}%",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun InfoChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .luciferGlass(RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}
