package com.minimal.carlauncher.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.carlauncher.data.AppInfo
import com.minimal.carlauncher.data.DockSize
import com.minimal.carlauncher.service.BrightnessLevel
import com.minimal.carlauncher.service.NowPlaying
import com.minimal.carlauncher.service.WeatherInfo
import com.minimal.carlauncher.ui.theme.AccentCyan
import com.minimal.carlauncher.ui.theme.CarBorder
import com.minimal.carlauncher.ui.theme.CarSurface
import com.minimal.carlauncher.ui.theme.CarSurfaceVariant
import com.minimal.carlauncher.ui.theme.TextMuted
import com.minimal.carlauncher.ui.theme.TextPrimary
import com.minimal.carlauncher.ui.theme.TextSecondary

/**
 * Dashboard home screen: clock and weather, GPS speed, music controls and
 * quick buttons, with the same bottom bar as the Minimal style (bigger buttons).
 */
@Composable
fun DashboardHomeScreen(
    time: String,
    amPm: String,
    date: String,
    weather: WeatherInfo?,
    speedKmh: Int?,
    hasLocationPermission: Boolean,
    nowPlaying: NowPlaying?,
    hasMusicAccess: Boolean,
    brightnessLevel: BrightnessLevel,
    dockSize: DockSize,
    zlinkApp: AppInfo?,
    pinnedApps: List<AppInfo>,
    isUpdateAvailable: Boolean,
    onRequestLocation: () -> Unit,
    onMusicPlayPause: () -> Unit,
    onMusicNext: () -> Unit,
    onMusicPrevious: () -> Unit,
    onOpenMusicApp: () -> Unit,
    onAllowMusicAccess: () -> Unit,
    onBrightness: () -> Unit,
    onVolume: () -> Unit,
    onBluetooth: () -> Unit,
    onWifi: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onLaunchZLink: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onLongClickApp: (AppInfo) -> Unit,
    onAddApp: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val dockScale = dockSize.scale
        val dockHeight = (74 * dockScale).dp
        // Everything above the bar scales with the space that is left, so the
        // layout fits 1024x600 head units as well as 2000x1200 ones.
        val contentHeight = maxHeight - dockHeight - 12.dp
        val unit = minOf(contentHeight.value / 4.2f, maxWidth.value / 9f).coerceIn(52f, 150f)
        // Read here: inner layouts cannot use BoxWithConstraints values directly
        val fullWidth = maxWidth

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left: clock + weather, then speed
                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ClockWeatherCard(
                        time = time,
                        amPm = amPm,
                        date = date,
                        weather = weather,
                        hasLocationPermission = hasLocationPermission,
                        onRequestLocation = onRequestLocation,
                        unit = unit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.25f)
                    )
                    SpeedCard(
                        speedKmh = speedKmh,
                        hasLocationPermission = hasLocationPermission,
                        onRequestLocation = onRequestLocation,
                        unit = unit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                // Right: music, then quick buttons
                Column(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MusicCard(
                        nowPlaying = nowPlaying,
                        hasMusicAccess = hasMusicAccess,
                        onPlayPause = onMusicPlayPause,
                        onNext = onMusicNext,
                        onPrevious = onMusicPrevious,
                        onOpenMusicApp = onOpenMusicApp,
                        onAllowAccess = onAllowMusicAccess,
                        unit = unit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.6f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickButton(
                            icon = when (brightnessLevel) {
                                BrightnessLevel.LOW -> Icons.Default.BrightnessLow
                                BrightnessLevel.MEDIUM -> Icons.Default.BrightnessMedium
                                BrightnessLevel.HIGH -> Icons.Default.BrightnessHigh
                            },
                            label = brightnessLevel.label,
                            onClick = onBrightness,
                            unit = unit
                        )
                        QuickButton(Icons.Default.VolumeUp, "Volume", onVolume, unit)
                        QuickButton(Icons.Default.Bluetooth, "Bluetooth", onBluetooth, unit)
                        QuickButton(Icons.Default.Wifi, "Wi-Fi", onWifi, unit)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                scale = dockScale,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = fullWidth)
            )
        }
    }
}

/** Rounded card used by every Dashboard panel. */
@Composable
private fun DashCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(CarSurface)
            .border(1.dp, CarBorder.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        content = content
    )
}

@Composable
private fun ClockWeatherCard(
    time: String,
    amPm: String,
    date: String,
    weather: WeatherInfo?,
    hasLocationPermission: Boolean,
    onRequestLocation: () -> Unit,
    unit: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // Sizes in dp, not sp, so a large system font cannot break the layout
    val timeSize = with(density) { (unit * 0.95f).dp.toSp() }
    val amPmSize = with(density) { (unit * 0.26f).dp.toSp() }
    val textSize = with(density) { (unit * 0.2f).coerceAtLeast(13f).dp.toSp() }

    DashCard(
        modifier = modifier,
        onClick = if (!hasLocationPermission) onRequestLocation else null
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = time.ifBlank { "--:--" },
                fontSize = timeSize,
                fontWeight = FontWeight.Light,
                color = TextPrimary,
                letterSpacing = (-1).sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = amPm,
                fontSize = amPmSize,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = (unit * 0.16f).dp)
            )
        }
        Text(
            text = date,
            fontSize = textSize,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height((unit * 0.1f).dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val iconSize = (unit * 0.28f).coerceAtLeast(18f).dp
            when {
                weather != null -> {
                    Icon(
                        imageVector = weatherIcon(weather),
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(iconSize)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buildString {
                            append("${weather.temperatureC}°C · ${weather.description}")
                            weather.place?.let { append(" · $it") }
                        },
                        fontSize = textSize,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                !hasLocationPermission -> {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(iconSize)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tap to allow location for weather",
                        fontSize = textSize,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(iconSize)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Weather needs internet",
                        fontSize = textSize,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun weatherIcon(weather: WeatherInfo): ImageVector = when (weather.code) {
    0, 1 -> if (weather.isDay) Icons.Default.WbSunny else Icons.Default.NightsStay
    2 -> Icons.Default.WbCloudy
    3 -> Icons.Default.Cloud
    45, 48 -> Icons.Default.Dehaze
    in 51..67, in 80..82 -> Icons.Default.Umbrella
    in 71..77, 85, 86 -> Icons.Default.AcUnit
    95, 96, 99 -> Icons.Default.FlashOn
    else -> Icons.Default.Cloud
}

@Composable
private fun SpeedCard(
    speedKmh: Int?,
    hasLocationPermission: Boolean,
    onRequestLocation: () -> Unit,
    unit: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val speedSize = with(density) { (unit * 0.85f).dp.toSp() }
    val unitSize = with(density) { (unit * 0.22f).coerceAtLeast(13f).dp.toSp() }

    DashCard(
        modifier = modifier,
        onClick = if (!hasLocationPermission) onRequestLocation else null
    ) {
        Spacer(modifier = Modifier.weight(1f))
        if (!hasLocationPermission) {
            Text(
                text = "Speed",
                fontSize = unitSize,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Text(
                text = "Tap to allow location",
                fontSize = unitSize,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = speedKmh?.toString() ?: "--",
                    fontSize = speedSize,
                    fontWeight = FontWeight.Light,
                    color = if (speedKmh != null) TextPrimary else TextMuted,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (speedKmh != null) "km/h" else "km/h · waiting for GPS",
                    fontSize = unitSize,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = (unit * 0.14f).dp)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MusicCard(
    nowPlaying: NowPlaying?,
    hasMusicAccess: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenMusicApp: () -> Unit,
    onAllowAccess: () -> Unit,
    unit: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val titleSize = with(density) { (unit * 0.24f).coerceAtLeast(14f).dp.toSp() }
    val smallSize = with(density) { (unit * 0.18f).coerceAtLeast(12f).dp.toSp() }

    if (!hasMusicAccess) {
        DashCard(modifier = modifier, onClick = onAllowAccess) {
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size((unit * 0.4f).dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Music Controls", fontSize = titleSize, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        text = "Tap here, then turn on Car Launcher in Notification Access",
                        fontSize = smallSize,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        return
    }

    DashCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable(enabled = nowPlaying != null) { onOpenMusicApp() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            val art = nowPlaying?.albumArt
            val artBitmap = remember(art) { art?.asImageBitmap() }
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.9f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CarSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (artBitmap != null) {
                    Image(
                        bitmap = artBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size((unit * 0.4f).dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nowPlaying?.title?.ifBlank { null } ?: "No music playing",
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = nowPlaying?.artist?.ifBlank { null }
                        ?: if (nowPlaying == null) "Start a song in your music app" else "",
                    fontSize = smallSize,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (nowPlaying != null) {
                    Text(
                        text = nowPlaying.appLabel,
                        fontSize = smallSize,
                        color = AccentCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val buttonSize = (unit * 0.62f).coerceIn(44f, 80f).dp
            MusicButton(Icons.Default.SkipPrevious, "Previous", onPrevious, buttonSize, enabled = nowPlaying != null)
            MusicButton(
                icon = if (nowPlaying?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                description = if (nowPlaying?.isPlaying == true) "Pause" else "Play",
                onClick = onPlayPause,
                size = buttonSize * 1.2f,
                enabled = nowPlaying != null,
                highlighted = true
            )
            MusicButton(Icons.Default.SkipNext, "Next", onNext, buttonSize, enabled = nowPlaying != null)
        }
    }
}

@Composable
private fun MusicButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    size: Dp,
    enabled: Boolean,
    highlighted: Boolean = false
) {
    val tint = when {
        !enabled -> TextMuted
        highlighted -> AccentCyan
        else -> TextPrimary
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (highlighted) AccentCyan.copy(alpha = 0.15f) else CarSurfaceVariant.copy(alpha = 0.8f))
            .border(1.dp, if (highlighted) AccentCyan.copy(alpha = 0.6f) else CarBorder.copy(alpha = 0.45f), CircleShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@Composable
private fun RowScope.QuickButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    unit: Float
) {
    val density = LocalDensity.current
    val labelSize = with(density) { (unit * 0.17f).coerceAtLeast(12f).dp.toSp() }
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(CarSurface)
            .border(1.dp, CarBorder.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AccentCyan,
            modifier = Modifier.size((unit * 0.36f).coerceIn(22f, 44f).dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = labelSize,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
