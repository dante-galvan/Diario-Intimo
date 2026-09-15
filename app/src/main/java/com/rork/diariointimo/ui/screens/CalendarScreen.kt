package com.rork.diariointimo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rork.diariointimo.i18n.DateFormats
import com.rork.diariointimo.i18n.LocalAppLocale
import com.rork.diariointimo.i18n.LocalStrings
import com.rork.diariointimo.ui.components.AppearInk
import com.rork.diariointimo.ui.components.InkIconButton
import com.rork.diariointimo.ui.components.PaperBackground
import com.rork.diariointimo.ui.theme.DiaryDim
import com.rork.diariointimo.ui.theme.HandCardExcerpt
import com.rork.diariointimo.ui.theme.LocalDiaryColors
import com.rork.diariointimo.ui.vm.DiaryUiState
import com.rork.diariointimo.ui.vm.daysWithEntries
import com.rork.diariointimo.ui.vm.entriesOn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields

/** Month by month navigation through the diary. */
@Composable
fun CalendarScreen(
    state: DiaryUiState,
    onOpenEntry: (String) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val colors = LocalDiaryColors.current
    val locale = LocalAppLocale.current
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(today.withDayOfMonth(1)) }
    var selected by remember { mutableStateOf(today) }
    var forward by remember { mutableStateOf(true) }
    val marked = state.daysWithEntries()
    val scroll = rememberScrollState()

    PaperBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scroll)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DiaryDim.headerPad, vertical = DiaryDim.space3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InkIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = strings.back,
                    onClick = onBack
                )
                Spacer(Modifier.width(DiaryDim.space3))
                Text(
                    text = strings.calendar,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.ink
                )
            }

            Spacer(Modifier.height(DiaryDim.space3))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DiaryDim.screenPad),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InkIconButton(
                    icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = strings.previousMonth,
                    onClick = {
                        forward = false
                        month = month.minusMonths(1)
                    }
                )
                Text(
                    text = DateFormats.monthYear(month, locale),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = DiaryDim.space2)
                )
                InkIconButton(
                    icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = strings.nextMonth,
                    onClick = {
                        forward = true
                        month = month.plusMonths(1)
                    }
                )
            }

            Spacer(Modifier.height(DiaryDim.space4))

            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    val offset = if (forward) 1 else -1
                    (slideInHorizontally(tween(340)) { full -> offset * full / 3 } + fadeIn(tween(280)))
                        .togetherWith(
                            slideOutHorizontally(tween(340)) { full -> -offset * full / 3 } +
                                fadeOut(tween(220))
                        )
                },
                label = "month"
            ) { visibleMonth ->
                MonthGrid(
                    month = visibleMonth,
                    selected = selected,
                    today = today,
                    marked = marked,
                    onSelect = { selected = it }
                )
            }

            Spacer(Modifier.height(DiaryDim.space6))

            Column(modifier = Modifier.padding(horizontal = DiaryDim.screenPad)) {
                Text(
                    text = DateFormats.longDate(selected, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.inkFaded
                )
                Spacer(Modifier.height(DiaryDim.space3))
                val dayEntries = state.entriesOn(selected)
                if (dayEntries.isEmpty()) {
                    AppearInk {
                        Text(
                            text = strings.noEntriesThisDay,
                            style = MaterialTheme.typography.titleLarge.copy(fontStyle = FontStyle.Italic),
                            color = colors.inkWhisper,
                            modifier = Modifier.padding(vertical = DiaryDim.space4)
                        )
                    }
                } else {
                    dayEntries.forEachIndexed { index, entry ->
                        AppearInk(delayMillis = index * 60) {
                            Surface(
                                onClick = { onOpenEntry(entry.id) },
                                shape = RoundedCornerShape(2.dp),
                                color = colors.paperLight.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, colors.edge),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = DiaryDim.space3)
                            ) {
                                Column(modifier = Modifier.padding(DiaryDim.space4)) {
                                    Text(
                                        text = entry.title.ifBlank { strings.untitledEntry },
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colors.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(DiaryDim.space1))
                                    Text(
                                        text = if (entry.isPrivate) {
                                            strings.privateBadge
                                        } else {
                                            entry.excerpt(90)
                                        },
                                        style = HandCardExcerpt,
                                        color = colors.inkSoft,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(DiaryDim.space10))
        }
    }
}

@Composable
private fun MonthGrid(
    month: LocalDate,
    selected: LocalDate,
    today: LocalDate,
    marked: Set<Long>,
    onSelect: (LocalDate) -> Unit
) {
    val locale = LocalAppLocale.current
    val colors = LocalDiaryColors.current
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val weekDays = remember(firstDayOfWeek) {
        List(7) { index -> firstDayOfWeek.plus(index.toLong()) }
    }
    val daysInMonth = month.lengthOfMonth()
    val leadingBlanks = ((month.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
    val cells = leadingBlanks + daysInMonth
    val rows = (cells + 6) / 7

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = DiaryDim.space4)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day: DayOfWeek ->
                Text(
                    text = DateFormats.weekdayInitial(day, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkWhisper,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(DiaryDim.space3))
        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(7) { column ->
                    val index = row * 7 + column
                    val dayNumber = index - leadingBlanks + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNumber in 1..daysInMonth) {
                            val date = month.withDayOfMonth(dayNumber)
                            DayCell(
                                date = date,
                                isSelected = date == selected,
                                isToday = date == today,
                                hasEntry = marked.contains(date.toEpochDay()),
                                onClick = { onSelect(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasEntry: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalDiaryColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(DiaryDim.space1)
            .background(
                color = if (isSelected) colors.ink else androidx.compose.ui.graphics.Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = when {
                isSelected -> colors.paperLight
                isToday -> colors.gold
                else -> colors.inkSoft
            }
        )
        Spacer(Modifier.height(DiaryDim.space1))
        Box(
            modifier = Modifier
                .size(if (hasEntry) 5.dp else 0.dp)
                .background(
                    color = if (isSelected) colors.goldLight else colors.gold.copy(alpha = 0.8f),
                    shape = CircleShape
                )
        )
    }
}
