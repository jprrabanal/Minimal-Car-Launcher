package com.minimal.carlauncher.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.carlauncher.ui.theme.AccentCyan
import com.minimal.carlauncher.ui.theme.CarBorder
import com.minimal.carlauncher.ui.theme.CarSurface
import com.minimal.carlauncher.ui.theme.CarSurfaceVariant
import com.minimal.carlauncher.ui.theme.TextMuted
import com.minimal.carlauncher.ui.theme.TextPrimary
import com.minimal.carlauncher.ui.theme.TextSecondary

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ClockWidget(
    timeFlow: StateFlow<String>,
    secondsFlow: StateFlow<String>,
    dateFlow: StateFlow<String>,
    modifier: Modifier = Modifier
) {
    val time by timeFlow.collectAsState()
    val seconds by secondsFlow.collectAsState()
    val date by dateFlow.collectAsState()

    ClockWidget(
        time = time,
        seconds = seconds,
        date = date,
        modifier = modifier
    )
}

@Composable
fun ClockWidget(
    time: String,
    seconds: String,
    date: String,
    modifier: Modifier = Modifier
) {
    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            CarSurfaceVariant.copy(alpha = 0.5f),
            CarSurface
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(cardGradient)
            .border(1.dp, CarBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Shrink the clock to fit its card: "21:41" + seconds is about 3.2x the
        // font size wide, and time + date is about 1.6x the font size tall.
        val timeSize = minOf(maxWidth.value / 3.2f, maxHeight.value / 1.6f).coerceIn(24f, 68f)
        val secondsSize = timeSize * 0.35f
        val dateSize = (timeSize * 0.24f).coerceIn(11f, 16f)

        Column(verticalArrangement = Arrangement.Center) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (time.isNotBlank()) time else "--:--",
                    fontSize = timeSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-1).sp,
                    maxLines = 1,
                    softWrap = false
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (seconds.isNotBlank()) seconds else "00",
                    fontSize = secondsSize.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentCyan,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = (timeSize * 0.09f).dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (date.isNotBlank()) date else "Loading date…",
                fontSize = dateSize.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
