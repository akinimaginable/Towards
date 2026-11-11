package org.etrange.towards.ui.trip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview
import kotlin.time.Instant

private enum class TimeField {
    None,
    Date,
    Time,
}

/**
 * Leave now, depart at, or arrive by. A narrow window uses a bottom sheet. A wide window uses a
 * dialog so the picker stays with the side panel. Date and time open their own dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripTimeSheet(
    time: TripTime,
    wide: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (TripTime) -> Unit,
) {
    val zone = remember { TimeZone.currentSystemDefault() }
    var draft by remember { mutableStateOf(time) }
    var scheduled by remember { mutableStateOf(time.scheduledInstant(timeZone = zone)) }
    var editing by remember { mutableStateOf(TimeField.None) }

    fun applyInstant(instant: Instant) {
        scheduled = instant
        draft = draft.at(instant)
    }

    val picker = @Composable { modifier: Modifier ->
        TripTimePickerContent(
            choice = draft,
            scheduled = scheduled,
            zone = zone,
            onChoiceChange = { next ->
                draft = next.at(scheduled)
            },
            onDateClick = { editing = TimeField.Date },
            onTimeClick = { editing = TimeField.Time },
            onDismiss = onDismiss,
            onConfirm = { onConfirm(draft) },
            modifier = modifier,
        )
    }

    if (wide) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier.width(420.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.background,
            ) {
                picker(Modifier)
            }
        }
    } else {
        val sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Expanded,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            picker(Modifier.windowInsetsPadding(WindowInsets.safeDrawing))
        }
    }

    when (editing) {
        TimeField.None -> Unit
        TimeField.Date -> DateDialog(
            scheduled = scheduled,
            zone = zone,
            onDismiss = { editing = TimeField.None },
            onConfirm = { millis ->
                val local = scheduled.toLocalDateTime(zone)
                applyInstant(
                    instantFromCalendarDayAndLocalTime(
                        utcDayMillis = millis,
                        hour = local.hour,
                        minute = local.minute,
                        timeZone = zone,
                    ),
                )
                editing = TimeField.None
            },
        )
        TimeField.Time -> TimeDialog(
            scheduled = scheduled,
            zone = zone,
            onDismiss = { editing = TimeField.None },
            onConfirm = { hour, minute ->
                applyInstant(
                    instantFromCalendarDayAndLocalTime(
                        utcDayMillis = utcDayMillisOf(scheduled, zone),
                        hour = hour,
                        minute = minute,
                        timeZone = zone,
                    ),
                )
                editing = TimeField.None
            },
        )
    }
}

@Composable
private fun TripTimePickerContent(
    choice: TripTime,
    scheduled: Instant,
    zone: TimeZone,
    onChoiceChange: (TripTime) -> Unit,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "When",
            style = MaterialTheme.typography.titleMedium,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val options = listOf(
                TripTime.LeaveNow,
                TripTime.DepartAt(scheduled),
                TripTime.ArriveBy(scheduled),
            )
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = choice::class == option::class,
                    onClick = { onChoiceChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                    label = {
                        Text(
                            text = option.shortLabel(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    },
                )
            }
        }
        if (choice != TripTime.LeaveNow) {
            ScheduleRow(
                label = "Date",
                value = formatTripDate(scheduled, zone),
                actionLabel = "Change date",
                onClick = onDateClick,
            )
            ScheduleRow(
                label = "Time",
                value = formatTripClock(scheduled, zone),
                actionLabel = "Change time",
                onClick = onTimeClick,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Button(onClick = onConfirm) {
                Text("Search")
            }
        }
    }
}

@Composable
private fun ScheduleRow(
    label: String,
    value: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = actionLabel, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(
    scheduled: Instant,
    zone: TimeZone,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = utcDayMillisOf(scheduled, zone),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let(onConfirm) ?: onDismiss()
                },
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    scheduled: Instant,
    zone: TimeZone,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val local = scheduled.toLocalDateTime(zone)
    val state = rememberTimePickerState(
        initialHour = local.hour,
        initialMinute = local.minute,
        is24Hour = true,
    )
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Time",
                modifier = Modifier.padding(start = 24.dp, top = 16.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        TimePicker(state = state)
    }
}

private fun TripTime.at(instant: Instant): TripTime = when (this) {
    TripTime.LeaveNow -> TripTime.LeaveNow
    is TripTime.DepartAt -> TripTime.DepartAt(instant)
    is TripTime.ArriveBy -> TripTime.ArriveBy(instant)
}

private fun TripTime.shortLabel(): String = when (this) {
    TripTime.LeaveNow -> "Leave now"
    is TripTime.DepartAt -> "Depart at"
    is TripTime.ArriveBy -> "Arrive by"
}

@Preview
@Composable
private fun TripTimeSheetLeaveNowPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        TripTimePickerContent(
            choice = TripTime.LeaveNow,
            scheduled = Instant.parse("2026-11-10T17:30:00Z"),
            zone = TimeZone.UTC,
            onChoiceChange = {},
            onDateClick = {},
            onTimeClick = {},
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview
@Composable
private fun TripTimeSheetArriveByPreview() {
    TowardsPreview(themeMode = ThemeMode.Dark) {
        TripTimePickerContent(
            choice = TripTime.ArriveBy(Instant.parse("2026-11-10T17:30:00Z")),
            scheduled = Instant.parse("2026-11-10T17:30:00Z"),
            zone = TimeZone.UTC,
            onChoiceChange = {},
            onDateClick = {},
            onTimeClick = {},
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview(name = "Time · Wide", widthDp = 800, heightDp = 600)
@Composable
private fun TripTimeSheetWidePreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        TripTimeSheet(
            time = TripTime.ArriveBy(Instant.parse("2026-11-10T17:30:00Z")),
            wide = true,
            onDismiss = {},
            onConfirm = {},
        )
    }
}
