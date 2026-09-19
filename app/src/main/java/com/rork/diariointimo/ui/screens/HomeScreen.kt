package com.rork.diariointimo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rork.diariointimo.data.DiaryEntry
import com.rork.diariointimo.i18n.AppStrings
import com.rork.diariointimo.i18n.DateFormats
import com.rork.diariointimo.i18n.LocalAppLocale
import com.rork.diariointimo.i18n.LocalStrings
import com.rork.diariointimo.ui.components.AppearInk
import com.rork.diariointimo.ui.components.FountainPen
import com.rork.diariointimo.ui.components.InkIconButton
import com.rork.diariointimo.ui.components.PaperBackground
import com.rork.diariointimo.ui.components.PaperChip
import com.rork.diariointimo.ui.components.SealedButton
import com.rork.diariointimo.ui.components.paperGrain
import com.rork.diariointimo.ui.ads.BannerAdView
import com.rork.diariointimo.ui.theme.DiaryDim
import com.rork.diariointimo.ui.theme.HandCardExcerpt
import com.rork.diariointimo.ui.theme.LocalDiaryColors
import com.rork.diariointimo.ui.vm.DiaryUiState
import com.rork.diariointimo.ui.vm.EntryFilter
import com.rork.diariointimo.ui.vm.visibleEntries
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

@Composable
fun HomeScreen(
    state: DiaryUiState,
    hasPassword: Boolean,
    onQueryChange: (String) -> Unit,
    onFilterChange: (EntryFilter) -> Unit,
    onOpenEntry: (String) -> Unit,
    onNewEntry: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onLock: () -> Unit
) {
    val strings = LocalStrings.current
    val colors = LocalDiaryColors.current
    val locale = LocalAppLocale.current
    var searching by remember { mutableStateOf(false) }
    val entries = state.visibleEntries()
    val today = remember { LocalDate.now() }

    PaperBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = DiaryDim.screenPad, end = DiaryDim.headerPad, top = DiaryDim.space4),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AppearInk {
                        Text(
                            text = greeting(strings),
                            style = MaterialTheme.typography.displayMedium,
                            color = colors.ink
                        )
                    }
                    Spacer(Modifier.height(DiaryDim.space2))
                    AppearInk(delayMillis = 90) {
                        Text(
                            text = DateFormats.longDate(today, locale),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.inkFaded
                        )
                    }
                }
                Spacer(Modifier.width(DiaryDim.space2))
                AppearInk(delayMillis = 140) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(horizontalArrangement = Arrangement.spacedBy(DiaryDim.space2)) {
                            InkIconButton(
                                icon = Icons.Outlined.Search,
                                contentDescription = strings.search,
                                active = searching,
                                onClick = {
                                    searching = !searching
                                    if (!searching) onQueryChange("")
                                }
                            )
                            InkIconButton(
                                icon = Icons.Outlined.CalendarToday,
                                contentDescription = strings.calendar,
                                onClick = onOpenCalendar
                            )
                        }
                        Spacer(Modifier.height(DiaryDim.space2))
                        Row(horizontalArrangement = Arrangement.spacedBy(DiaryDim.space2)) {
                            if (hasPassword) {
                                InkIconButton(
                                    icon = Icons.Outlined.Lock,
                                    contentDescription = strings.lockDiary,
                                    onClick = onLock
                                )
                            }
                            InkIconButton(
                                icon = Icons.Outlined.Settings,
                                contentDescription = strings.settings,
                                onClick = onOpenSettings
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = searching,
                enter = fadeIn(tween(220)) + expandVertically(tween(260)),
                exit = fadeOut(tween(160)) + shrinkVertically(tween(220))
            ) {
                SearchField(
                    value = state.query,
                    placeholder = strings.searchPlaceholder,
                    onValueChange = onQueryChange,
                    modifier = Modifier.padding(horizontal = DiaryDim.screenPad, vertical = DiaryDim.space3)
                )
            }

            Spacer(Modifier.height(DiaryDim.space4))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DiaryDim.screenPad),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaperChip(
                    label = strings.filterAll,
                    selected = state.filter == EntryFilter.ALL,
                    onClick = { onFilterChange(EntryFilter.ALL) }
                )
                Spacer(Modifier.width(DiaryDim.space2))
                PaperChip(
                    label = strings.filterFavorites,
                    selected = state.filter == EntryFilter.FAVORITES,
                    onClick = { onFilterChange(EntryFilter.FAVORITES) }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = strings.entriesCount(entries.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkWhisper
                )
            }

            Spacer(Modifier.height(DiaryDim.space4))

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (entries.isEmpty()) {
                    EmptyPages(
                        title = when {
                            state.query.isNotBlank() -> strings.searchEmptyTitle
                            state.filter == EntryFilter.FAVORITES -> strings.favoritesEmptyTitle
                            else -> strings.emptyDiary
                        },
                        body = when {
                            state.query.isNotBlank() -> strings.searchEmptyBody
                            state.filter == EntryFilter.FAVORITES -> strings.favoritesEmptyBody
                            else -> strings.emptyBody
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = DiaryDim.screenPad,
                            end = DiaryDim.screenPad,
                            // Clearance for the floating action button, its fade, and the banner.
                            bottom = DiaryDim.fabClearance + DiaryDim.space8 + DiaryDim.space10
                        ),
                        verticalArrangement = Arrangement.spacedBy(DiaryDim.space3)
                    ) {
                        items(entries, key = { it.id }) { entry ->
                            EntryCard(
                                entry = entry,
                                strings = strings,
                                locale = locale,
                                onClick = { onOpenEntry(entry.id) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, colors.paper, colors.paperDeep)
                            )
                        )
                )
                SealedButton(
                    label = "+  " + strings.newEntry,
                    onClick = onNewEntry,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = DiaryDim.space6)
                        .fillMaxWidth(0.82f)
                )
            }

            BannerAdView(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = DiaryDim.space2)
            )
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDiaryColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.paperLight.copy(alpha = 0.75f), RoundedCornerShape(DiaryDim.radiusInput))
            .border(BorderStroke(DiaryDim.dividerHeight, colors.edge), RoundedCornerShape(DiaryDim.radiusInput))
            .padding(horizontal = DiaryDim.fieldHorizontalPad, vertical = DiaryDim.space3)
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkWhisper
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.gold),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default
        )
    }
}

@Composable
private fun EntryCard(
    entry: DiaryEntry,
    strings: AppStrings,
    locale: Locale,
    onClick: () -> Unit
) {
    val colors = LocalDiaryColors.current
    val date = LocalDate.ofEpochDay(entry.epochDay)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DiaryDim.radiusPaper),
        color = colors.paperLight.copy(alpha = 0.88f),
        border = BorderStroke(DiaryDim.dividerHeight, colors.edge.copy(alpha = 0.8f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .paperGrain(seed = entry.id.hashCode(), count = 60)
                .padding(vertical = DiaryDim.space4, horizontal = DiaryDim.space4)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(DiaryDim.touchTarget)
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.ink
                )
                Text(
                    text = DateFormats.shortMonth(date, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkWhisper
                )
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = DiaryDim.space3)
                    .width(DiaryDim.dividerHeight)
                    .fillMaxHeight()
                    .background(colors.gold.copy(alpha = 0.35f))
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.title.ifBlank { strings.untitledEntry },
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (entry.isFavorite) {
                        Spacer(Modifier.width(DiaryDim.space2))
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = strings.filterFavorites,
                            tint = colors.gold,
                            modifier = Modifier.size(DiaryDim.iconSmall)
                        )
                    }
                    if (entry.isPrivate) {
                        Spacer(Modifier.width(DiaryDim.space2))
                        Box(
                            modifier = Modifier
                                .size(DiaryDim.space2)
                                .background(colors.wax, RoundedCornerShape(50))
                        )
                    }
                }
                Spacer(Modifier.height(DiaryDim.space1))
                Text(
                    text = when {
                        entry.isPrivate -> "· · · · · · · · · ·"
                        entry.body.isBlank() -> strings.noText
                        else -> entry.excerpt()
                    },
                    style = HandCardExcerpt,
                    color = if (entry.isPrivate) colors.inkWhisper else colors.inkSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyPages(title: String, body: String) {
    val colors = LocalDiaryColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = DiaryDim.space10),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppearInk {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FountainPen(height = 108.dp, tilt = -28f)
                Spacer(Modifier.height(DiaryDim.space6))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontStyle = FontStyle.Italic),
                    color = colors.inkSoft,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(DiaryDim.space3))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkWhisper,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(DiaryDim.fabClearance))
            }
        }
    }
}

private fun greeting(strings: AppStrings): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 12 -> strings.goodMorning
        hour < 20 -> strings.goodAfternoon
        else -> strings.goodEvening
    }
}
