package com.minimal.carlauncher.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.carlauncher.data.AppInfo
import com.minimal.carlauncher.data.AppRepository
import com.minimal.carlauncher.ui.theme.AccentCyan
import com.minimal.carlauncher.ui.theme.AccentRed
import com.minimal.carlauncher.ui.theme.AccentZLink
import com.minimal.carlauncher.ui.theme.CarBorder
import com.minimal.carlauncher.ui.theme.CarSurface
import com.minimal.carlauncher.ui.theme.CarSurfaceVariant
import com.minimal.carlauncher.ui.theme.TextPrimary
import com.minimal.carlauncher.ui.theme.TextSecondary
import com.minimal.carlauncher.util.BitmapHelper

/**
 * Minimal home screen: a large 12-hour clock with the date in the center, and one
 * centered bottom bar with All Apps, Android Auto (ZLink), pinned apps and About.
 */
@Composable
fun MinimalHomeScreen(
    time: String,
    amPm: String,
    date: String,
    zlinkApp: AppInfo?,
    pinnedApps: List<AppInfo>,
    isUpdateAvailable: Boolean,
    onOpenAppDrawer: () -> Unit,
    onLaunchZLink: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onLongClickApp: (AppInfo) -> Unit,
    onAddApp: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Scale the clock with the screen so it fits every head unit, from
        // 1024x600 (low density) to 2000x1200, and phones. Sizes are set in dp,
        // not sp, so a large system font size cannot push the clock off screen.
        val density = LocalDensity.current
        val clockDp = minOf(maxHeight.value * 0.30f, maxWidth.value * 0.17f).coerceIn(56f, 190f)
        val timeSize = with(density) { clockDp.dp.toSp() }
        val amPmSize = with(density) { (clockDp * 0.24f).dp.toSp() }
        val dateSize = with(density) { (clockDp * 0.19f).coerceAtLeast(16f).dp.toSp() }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = time.ifBlank { "--:--" },
                    fontSize = timeSize,
                    fontWeight = FontWeight.Light,
                    color = TextPrimary,
                    letterSpacing = (-2).sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = amPm,
                    fontSize = amPmSize,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = (clockDp * 0.2f).dp)
                )
            }
            Text(
                text = date,
                fontSize = dateSize,
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
                maxLines = 1
            )
        }

        MinimalDock(
            zlinkApp = zlinkApp,
            pinnedApps = pinnedApps,
            isUpdateAvailable = isUpdateAvailable,
            onOpenAppDrawer = onOpenAppDrawer,
            onLaunchZLink = onLaunchZLink,
            onLaunchApp = onLaunchApp,
            onLongClickApp = onLongClickApp,
            onAddApp = onAddApp,
            onOpenAbout = onOpenAbout,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = maxWidth)
        )
    }
}

@Composable
private fun MinimalDock(
    zlinkApp: AppInfo?,
    pinnedApps: List<AppInfo>,
    isUpdateAvailable: Boolean,
    onOpenAppDrawer: () -> Unit,
    onLaunchZLink: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onLongClickApp: (AppInfo) -> Unit,
    onAddApp: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(37.dp))
            .background(CarSurface)
            .border(1.dp, CarBorder.copy(alpha = 0.55f), RoundedCornerShape(37.dp))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(onClick = onOpenAppDrawer) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = "All Apps",
                tint = AccentCyan,
                modifier = Modifier.size(26.dp)
            )
        }

        AndroidAutoButton(zlinkApp = zlinkApp, onClick = onLaunchZLink)

        // Pinned apps scroll if they do not all fit
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pinnedApps.forEach { app ->
                DockAppIcon(
                    app = app,
                    onClick = { onLaunchApp(app) },
                    onLongClick = { onLongClickApp(app) }
                )
            }
            if (pinnedApps.size < AppRepository.MAX_DOCK_APPS) {
                DockAddButton(onClick = onAddApp)
            }
        }

        RoundIconButton(onClick = onOpenAbout) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "About & Settings",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            if (isUpdateAvailable) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(AccentRed)
                )
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    onClick: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(CarSurfaceVariant.copy(alpha = 0.8f))
            .border(1.dp, CarBorder.copy(alpha = 0.45f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun AndroidAutoButton(
    zlinkApp: AppInfo?,
    onClick: () -> Unit
) {
    val iconBitmap = zlinkApp?.let { BitmapHelper.safeDrawableToImageBitmap(it.icon, 72, 72) }
    Row(
        modifier = Modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(AccentZLink.copy(alpha = 0.15f))
            .border(1.dp, AccentZLink.copy(alpha = 0.5f), RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = AccentZLink,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(28.dp)
            )
        }
        Text(
            text = "Android Auto",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
