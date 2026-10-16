package org.etrange.towards.ui.home.nearby

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.etrange.towards.ui.LineBadge
import org.etrange.towards.ui.icons.liveIcon
import org.etrange.towards.ui.icons.walkIcon
import org.etrange.towards.ui.parseHexColor
import org.etrange.towards.ui.preview.previewLinePreview
import org.etrange.towards.ui.readableTextColorOn
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

private val SheetInset = 12.dp
private val ThickRailWidth = 16.dp
private val CardHeight = 100.dp

private enum class Rail { None, Thin, Thick }

@Composable
fun LineSheet(
    preview: LinePreview,
    onStopClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(),
    sheetDragPx: Float = 0f,
    onSheetDrag: (Float) -> Unit = {},
    onSheetDragEnd: (Float) -> Unit = {},
    onDepartureClick: (Int) -> Unit = {},
) {
    var earlierExpanded by remember(preview) { mutableStateOf(false) }
    val accent = parseHexColor(preview.routeColor) ?: MaterialTheme.colorScheme.primary
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val deepAccent = if (darkTheme) accent else lerp(accent, Color.Black, 0.3f)
    val passed = preview.stops.withIndex().filter { !it.value.upcoming }
    val ahead = preview.stops.withIndex().filter { it.value.upcoming }
    val dragPx = rememberUpdatedState(sheetDragPx)
    val onDrag = rememberUpdatedState(onSheetDrag)
    val onDragEnd = rememberUpdatedState(onSheetDragEnd)
    val dismissScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val current = dragPx.value
                if (available.y < 0f && current > 0f) {
                    val consumed = -minOf(-available.y, current)
                    onDrag.value(consumed)
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (available.y > 0f) {
                    onDrag.value(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dragPx.value > 0f) {
                    onDragEnd.value(available.y)
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    Column(
        modifier = modifier.fillMaxSize().nestedScroll(dismissScroll)
            .background(MaterialTheme.colorScheme.background)
            .background(accent.copy(alpha = 0.07f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().draggable(
                state = rememberDraggableState { delta -> onSheetDrag(delta) },
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> onSheetDragEnd(velocity) },
            ),
        ) {
            DragHandle()
            LineTitle(preview = preview, isLoading = loading)
            if (preview.departures.isNotEmpty()) {
                DepartureCards(
                    preview = preview,
                    accent = accent,
                    onAccent = parseHexColor(preview.routeTextColor) ?: readableTextColorOn(accent),
                    deepAccent = deepAccent,
                    darkTheme = darkTheme,
                    enabled = !loading,
                    onDepartureClick = onDepartureClick,
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(
                top = 4.dp,
                bottom = 20.dp + contentPadding.calculateBottomPadding(),
            ),
        ) {
            if (passed.isNotEmpty()) {
                item(key = "earlier-stops") {
                    Column {
                        EarlierStopsRow(
                            count = passed.size,
                            expanded = earlierExpanded,
                            accent = accent,
                            onClick = { earlierExpanded = !earlierExpanded },
                        )
                        AnimatedVisibility(
                            visible = earlierExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Column {
                                passed.forEach { indexed ->
                                    StopRow(
                                        stop = indexed.value,
                                        accent = accent,
                                        deepAccent = deepAccent,
                                        above = Rail.Thin,
                                        below = Rail.Thin,
                                        walkMinutes = null,
                                        showDivider = true,
                                        onClick = { onStopClick(indexed.index) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            items(count = ahead.size, key = { position -> ahead[position].index }) { position ->
                val indexed = ahead[position]
                val first = position == 0
                val last = position == ahead.lastIndex
                StopRow(
                    stop = indexed.value,
                    accent = accent,
                    deepAccent = deepAccent,
                    above = when {
                        !first -> Rail.Thick
                        passed.isNotEmpty() -> Rail.Thin
                        else -> Rail.None
                    },
                    below = if (last) Rail.None else Rail.Thick,
                    walkMinutes = if (indexed.value.current) preview.walkMinutes else null,
                    showDivider = !last,
                    onClick = { onStopClick(indexed.index) },
                )
            }
        }
    }
}

@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            .semantics { contentDescription = "Drag down to close" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.width(36.dp).height(4.dp).background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(2.dp),
                ),
        )
    }
}

@Composable
private fun LineTitle(preview: LinePreview, isLoading: Boolean) {
    val headsign = preview.headsign.orEmpty().toDisplayCase()

    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(start = SheetInset, end = SheetInset, top = 6.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineBadge(
            label = preview.lineName,
            routeColor = preview.routeColor,
            routeTextColor = preview.routeTextColor,
            minHeight = 30.dp,
            fontSize = 16.sp,
        )
        Text(
            text = headsign.ifBlank { preview.lineName },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun DepartureCards(
    preview: LinePreview,
    accent: Color,
    onAccent: Color,
    deepAccent: Color,
    darkTheme: Boolean,
    enabled: Boolean,
    onDepartureClick: (Int) -> Unit,
) {
    val now by produceState(initialValue = Clock.System.now(), key1 = preview.departures) {
        while (true) {
            value = Clock.System.now()
            delay(30.seconds)
        }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = SheetInset, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(preview.departures, key = { _, trip -> trip.tripId }) { index, trip ->
            val selected = index == preview.selectedDeparture
            DepartureCard(
                countdown = countdownOf(trip.time, now),
                realTime = trip.realTime,
                selected = selected,
                container = when {
                    selected -> accent
                    darkTheme -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> MaterialTheme.colorScheme.surface
                },
                content = if (selected) onAccent else deepAccent,
                glow = accent,
                enabled = enabled,
                onClick = { onDepartureClick(index) },
            )
        }
    }
}

@Composable
private fun DepartureCard(
    countdown: Countdown,
    realTime: Boolean,
    selected: Boolean,
    container: Color,
    content: Color,
    glow: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    var numberSize by remember(countdown.value) {
        mutableStateOf(
            when {
                countdown.value.length <= 2 -> 46.sp
                countdown.value.length <= 3 -> 36.sp
                else -> 26.sp
            },
        )
    }
    Box(modifier = Modifier.width(108.dp).height(CardHeight)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(CardHeight).shadow(
                    elevation = if (selected) 12.dp else 4.dp,
                    shape = shape,
                    ambientColor = glow,
                    spotColor = glow,
                ).clip(shape).background(container)
                .clickable(enabled = enabled, role = Role.Tab, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    this.selected = selected
                    contentDescription = buildString {
                        append(countdown.spoken)
                        if (realTime) append(", real time")
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = countdown.value,
                    color = content,
                    fontSize = numberSize,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    onTextLayout = { layout ->
                        if (layout.hasVisualOverflow && numberSize > 16.sp) {
                            numberSize *= 0.9f
                        }
                    },
                )
                if (countdown.unit != null) {
                    Text(
                        text = countdown.unit,
                        color = content,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (realTime) {
                Icon(
                    imageVector = liveIcon,
                    contentDescription = null,
                    tint = content.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp)
                        .size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun EarlierStopsRow(
    count: Int,
    expanded: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(
            onClickLabel = if (expanded) "Hide earlier stops" else "Show earlier stops",
            role = Role.Button,
            onClick = onClick,
        ),
    ) {
        RailLine(top = Rail.None, bottom = Rail.Thin, accent = accent, dot = Dot.Passed) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (count == 1) "1 earlier stop" else "$count earlier stops",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (expanded) "▴" else "▾",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        RailLine(top = Rail.Thin, bottom = Rail.Thin, accent = accent) {
            StopDivider()
        }
    }
}

@Composable
private fun StopRow(
    stop: LinePreviewStop,
    accent: Color,
    deepAccent: Color,
    above: Rail,
    below: Rail,
    walkMinutes: Int?,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    val textColor = if (stop.upcoming) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        RailLine(top = above, bottom = above, accent = accent) {
            Spacer(modifier = Modifier.height(if (walkMinutes != null) 12.dp else 16.dp))
            if (walkMinutes != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = walkIcon,
                        contentDescription = null,
                        tint = deepAccent,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "$walkMinutes min",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = deepAccent,
                    )
                }
            }
        }
        RailLine(
            top = above,
            bottom = below,
            accent = accent,
            dot = if (stop.upcoming) Dot.Upcoming else Dot.Passed,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stop.name.toDisplayCase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (stop.upcoming) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp, end = 64.dp),
                )
                Text(
                    text = stop.time?.clockLabel().orEmpty(),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    textAlign = TextAlign.End,
                )
            }
        }
        RailLine(top = below, bottom = below, accent = accent) {
            Spacer(modifier = Modifier.height(16.dp))
            if (showDivider) StopDivider()
        }
    }
}

@Composable
private fun StopDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

private enum class Dot { Upcoming, Passed }

/**
 * One horizontal band of a stop row. The rail column draws [top] down to the middle and
 * [bottom] from there, so the bands of consecutive rows join into one continuous rail.
 */
@Composable
private fun RailLine(
    top: Rail,
    bottom: Rail,
    accent: Color,
    dot: Dot? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier.width(48.dp).fillMaxHeight()
                .drawBehind { drawRail(top, bottom, accent, dot) },
            contentAlignment = Alignment.Center,
        ) {
            when (dot) {
                Dot.Upcoming -> Box(
                    modifier = Modifier.size(8.dp).background(Color.White, CircleShape)
                )

                Dot.Passed -> Box(
                    modifier = Modifier.size(8.dp)
                        .background(accent.copy(alpha = 0.55f), CircleShape)
                )

                null -> Unit
            }
        }
        Column(modifier = Modifier.padding(end = SheetInset), content = content)
    }
}

private fun DrawScope.drawRail(top: Rail, bottom: Rail, accent: Color, dot: Dot?) {
    val x = size.width / 2f
    val middle = if (dot != null) size.height / 2f else size.height
    drawSegment(top, accent, x, 0f, middle)
    if (dot == null) return
    drawSegment(bottom, accent, x, middle, size.height)
    if (dot == Dot.Upcoming && (top != Rail.Thick || bottom != Rail.Thick)) {
        drawCircle(color = accent, radius = ThickRailWidth.toPx() / 2f, center = Offset(x, middle))
    }
}

private fun DrawScope.drawSegment(rail: Rail, accent: Color, x: Float, from: Float, to: Float) {
    if (rail == Rail.None || to <= from) return
    val thick = rail == Rail.Thick
    drawLine(
        color = if (thick) accent else accent.copy(alpha = 0.35f),
        start = Offset(x, from),
        end = Offset(x, to),
        strokeWidth = (if (thick) ThickRailWidth else 4.dp).toPx(),
        cap = StrokeCap.Butt,
    )
}

@Preview
@Composable
private fun LineSheetPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        LineSheet(preview = previewLinePreview(), onStopClick = {})
    }
}

@Preview
@Composable
private fun LineSheetDarkPreview() {
    TowardsPreview(themeMode = ThemeMode.Dark) {
        LineSheet(preview = previewLinePreview(), onStopClick = {})
    }
}
