package com.rork.diariointimo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.rork.diariointimo.data.DiaryEntry
import com.rork.diariointimo.data.SignatureStroke
import com.rork.diariointimo.export.LetterSheetExporter
import com.rork.diariointimo.export.SheetFormat
import com.rork.diariointimo.export.SheetSnapshot
import com.rork.diariointimo.i18n.DateFormats
import com.rork.diariointimo.i18n.LocalAppLocale
import com.rork.diariointimo.i18n.LocalStrings
import com.rork.diariointimo.ui.components.FountainPen
import com.rork.diariointimo.ui.components.InkIconButton
import com.rork.diariointimo.ui.components.PaperBackground
import com.rork.diariointimo.ui.components.PaperChip
import com.rork.diariointimo.ui.components.SealedButton
import com.rork.diariointimo.ui.components.drawRuledLines
import com.rork.diariointimo.ui.theme.DiaryColors
import com.rork.diariointimo.ui.theme.DiaryDim
import com.rork.diariointimo.ui.theme.HandSheetBody
import com.rork.diariointimo.ui.theme.LocalDiaryColors
import com.rork.diariointimo.ui.theme.SerifFamily
import com.rork.diariointimo.ui.theme.SerifSheetTitle
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class InkDrop(val id: Long, val x: Float, val y: Float)

private const val BODY_LINE_HEIGHT_SP = 34
private const val AUTOSAVE_DELAY_MS = 650L

/**
 * A single diary sheet: ruled paper, a pen that rides in its own reserved
 * gutter (never over the words), a finger signature and the share/save actions.
 */
@Composable
fun EditorScreen(
    entry: DiaryEntry,
    onChange: (title: String, body: String) -> Unit,
    onSignatureChange: (List<SignatureStroke>) -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePrivate: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val locale = LocalAppLocale.current
    val density = LocalDensity.current
    val colors = LocalDiaryColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val bodyFocus = remember { FocusRequester() }

    // Saveable across configuration changes so no written word is ever lost.
    var title by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(entry.title))
    }
    var body by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(entry.body, TextRange(entry.body.length)))
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var writing by remember { mutableStateOf(false) }
    var bodyFocused by remember { mutableStateOf(false) }
    var savedVisible by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var pendingExportShare by remember { mutableStateOf<Boolean?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var exportSuccess by remember { mutableStateOf(false) }
    val drops = remember { mutableStateListOf<InkDrop>() }

    val caret = layout?.let { result ->
        val index = body.selection.start.coerceIn(0, body.text.length)
        runCatching { result.getCursorRect(index) }.getOrNull()
    }
    // The pen tip rides at the caret's line height, inside its own gutter.
    val penY by animateDpAsState(
        targetValue = with(density) { (caret?.bottom ?: 0f).toDp() },
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 520f),
        label = "penY"
    )
    val penAlpha by animateFloatAsState(
        targetValue = if (bodyFocused) 1f else 0f,
        animationSpec = tween(420),
        label = "penAlpha"
    )

    // A brand-new sheet gets a first flourish drawn by the pen.
    val flourish = remember { Animatable(if (entry.isBlank) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (entry.isBlank) {
            delay(430)
            flourish.animateTo(1f, tween(760, easing = EaseOutCubic))
        }
    }

    LaunchedEffect(title.text, body.text) {
        if (title.text == entry.title && body.text == entry.body) return@LaunchedEffect
        delay(AUTOSAVE_DELAY_MS)
        onChange(title.text, body.text)
        savedVisible = true
        delay(1400)
        savedVisible = false
    }

    LaunchedEffect(writing) {
        if (writing) {
            delay(900)
            writing = false
        }
    }

    LaunchedEffect(Unit) {
        if (entry.isBlank) {
            delay(420)
            runCatching { bodyFocus.requestFocus() }
        }
    }

    LaunchedEffect(notice) {
        if (notice != null) {
            delay(2400)
            notice = null
        }
    }

    LaunchedEffect(exportSuccess) {
        if (exportSuccess) {
            delay(3200)
            exportSuccess = false
        }
    }

    // On Android 9 or older, saving the sheet outside the app first needs the
    // system storage permission; the chosen export resumes after the answer.
    var pendingSaveFormat by remember { mutableStateOf<SheetFormat?>(null) }
    var permissionOutcome by remember { mutableStateOf<Boolean?>(null) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionOutcome = granted }

    fun runExport(format: SheetFormat, share: Boolean) {
        if (exporting) return
        if (!share && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingSaveFormat = format
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        exporting = true
        scope.launch {
            val result = runCatching {
                val snapshot = SheetSnapshot(
                    title = title.text.ifBlank { strings.untitledEntry },
                    body = body.text,
                    dateText = DateFormats.longDate(LocalDate.ofEpochDay(entry.epochDay), locale),
                    signature = entry.signature,
                    colors = colors,
                    footer = strings.appName
                )
                val exporter = LetterSheetExporter(context)
                val file = if (format == SheetFormat.PNG) {
                    exporter.exportPng(snapshot)
                } else {
                    exporter.exportPdf(snapshot)
                }
                if (share) {
                    exporter.shareFile(file, format)
                } else {
                    check(exporter.saveToGallery(file, format)) { "the sheet was not saved" }
                }
            }
            exporting = false
            result.fold(
                onSuccess = { if (share) notice = strings.exportSaved else exportSuccess = true },
                onFailure = { notice = strings.exportError }
            )
        }
    }

    LaunchedEffect(permissionOutcome) {
        val granted = permissionOutcome ?: return@LaunchedEffect
        val format = pendingSaveFormat
        permissionOutcome = null
        pendingSaveFormat = null
        if (format == null) return@LaunchedEffect
        if (granted) {
            runExport(format, share = false)
        } else {
            notice = strings.exportError
        }
    }

    PaperBackground {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
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
                    onClick = {
                        onChange(title.text, body.text)
                        onBack()
                    }
                )
                Spacer(Modifier.weight(1f))
                AnimatedVisibility(
                    visible = savedVisible || notice != null,
                    enter = fadeIn(tween(260)),
                    exit = fadeOut(tween(400))
                ) {
                    WrittenLabel(text = notice ?: strings.saved, color = colors.gold)
                }
                Spacer(Modifier.width(DiaryDim.space3))
                InkIconButton(
                    icon = if (entry.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (entry.isFavorite) {
                        strings.removeFromFavorites
                    } else {
                        strings.addToFavorites
                    },
                    active = entry.isFavorite,
                    onClick = onToggleFavorite
                )
                Spacer(Modifier.width(DiaryDim.space2))
                WaxToggle(
                    active = entry.isPrivate,
                    description = strings.markPrivate,
                    onClick = onTogglePrivate
                )
                Spacer(Modifier.width(DiaryDim.space2))
                InkIconButton(
                    icon = Icons.Outlined.DeleteOutline,
                    contentDescription = strings.deleteEntry,
                    onClick = { askDelete = true }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = DiaryDim.screenPad)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = DateFormats.longDate(LocalDate.ofEpochDay(entry.epochDay), locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.inkWhisper
                )
                Spacer(Modifier.height(DiaryDim.space3))

                Box {
                    if (title.text.isEmpty()) {
                        Text(
                            text = strings.titlePlaceholder,
                            style = SerifSheetTitle,
                            color = colors.inkWhisper.copy(alpha = 0.6f)
                        )
                    }
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = SerifSheetTitle.copy(color = colors.ink),
                        cursorBrush = SolidColor(colors.gold),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        )
                    )
                }

                Spacer(Modifier.height(DiaryDim.space3))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DiaryDim.dividerHeight)
                        .drawBehind {
                            drawRect(
                                color = colors.gold.copy(alpha = 0.4f),
                                size = androidx.compose.ui.geometry
                                    .Size(size.width * flourish.value, size.height)
                            )
                        }
                )
                Spacer(Modifier.height(DiaryDim.space5))

                val lineHeightPx = with(density) { BODY_LINE_HEIGHT_SP.sp.toPx() }
                val ruleStart = with(density) { 4.dp.toPx() }
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = DiaryDim.sheetMinHeight)
                        .drawBehind {
                            drawRuledLines(
                                lineHeightPx = lineHeightPx,
                                color = colors.edge.copy(alpha = 0.55f),
                                startY = ruleStart
                            )
                        }
                ) {
                    // A reserved margin keeps the pen off the words:
                    // TEXTO ← espacio → LAPICERA, always.
                    val penGutter = when {
                        maxWidth < 320.dp -> 44.dp
                        maxWidth < 400.dp -> 68.dp
                        else -> 96.dp
                    }
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (body.text.isEmpty()) {
                                Text(
                                    text = strings.bodyPlaceholder,
                                    style = HandSheetBody.copy(fontStyle = FontStyle.Italic),
                                    color = colors.inkWhisper.copy(alpha = 0.7f)
                                )
                            }
                            BasicTextField(
                                value = body,
                                onValueChange = { next ->
                                    if (next.text.length > body.text.length) {
                                        writing = true
                                        caret?.let { rect ->
                                            drops.add(InkDrop(System.nanoTime(), rect.left, rect.bottom))
                                            if (drops.size > 6) drops.removeAt(0)
                                        }
                                    }
                                    body = next
                                },
                                onTextLayout = { layout = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = DiaryDim.sheetMinHeight)
                                    .focusRequester(bodyFocus)
                                    .onFocusChanged {
                                        bodyFocused = it.isFocused
                                        if (!it.isFocused) writing = false
                                    },
                                textStyle = HandSheetBody.copy(color = colors.inkBlue),
                                cursorBrush = SolidColor(colors.gold.copy(alpha = 0.8f)),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Default
                                )
                            )

                            drops.forEach { drop ->
                                key(drop.id) {
                                    InkBloom(
                                        x = with(density) { drop.x.toDp() },
                                        y = with(density) { drop.y.toDp() },
                                        ink = colors.inkBlue,
                                        onFinished = { drops.remove(drop) }
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier.width(penGutter),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (caret != null && bodyFocused) {
                                val penHeight = 84.dp
                                Box(
                                    modifier = Modifier
                                        .offset(y = penY - penHeight + 6.dp)
                                        .graphicsLayer { alpha = penAlpha }
                                ) {
                                    FountainPen(height = penHeight, tilt = 18f, writing = writing)
                                }
                            }
                        }
                    }

                }

                Spacer(Modifier.height(DiaryDim.space5))

                // The finger signature, when the sheet carries one.
                if (entry.signature.isNotEmpty()) {
                    SignatureView(
                        strokes = entry.signature,
                        ink = colors.inkBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DiaryDim.signatureHeight)
                    )
                    Spacer(Modifier.height(DiaryDim.space2))
                    Row(horizontalArrangement = Arrangement.spacedBy(DiaryDim.space2)) {
                        PaperChip(
                            label = strings.editSignature,
                            selected = false,
                            onClick = { showSignaturePad = true }
                        )
                        PaperChip(
                            label = strings.removeSignature,
                            selected = false,
                            onClick = { onSignatureChange(emptyList()) }
                        )
                    }
                } else {
                    PaperChip(
                        label = strings.addSignature,
                        selected = false,
                        onClick = { showSignaturePad = true }
                    )
                }

                Spacer(Modifier.height(DiaryDim.space6))
                Text(
                    text = strings.wordsCount(
                        body.text.split(Regex("\\s+")).count { it.isNotBlank() }
                    ) + "  ·  " + strings.savedAutomatically,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkWhisper
                )
                Spacer(Modifier.height(DiaryDim.space5))
                Row(horizontalArrangement = Arrangement.spacedBy(DiaryDim.space3)) {
                    SealedButton(
                        label = strings.share,
                        icon = Icons.Outlined.Share,
                        enabled = !exporting,
                        modifier = Modifier.weight(1f),
                        onClick = { pendingExportShare = true }
                    )
                    SealedButton(
                        label = strings.saveSheet,
                        icon = Icons.Outlined.Download,
                        enabled = !exporting,
                        modifier = Modifier.weight(1f),
                        onClick = { pendingExportShare = false }
                    )
                }
                Spacer(Modifier.height(DiaryDim.space12))
            }
        }
        AnimatedVisibility(
            visible = exportSuccess,
            enter = fadeIn(tween(220)) + scaleIn(
                initialScale = 0.72f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 340f)
            ),
            exit = fadeOut(tween(340)) + scaleOut(targetScale = 0.9f, animationSpec = tween(340)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            SuccessSeal(
                title = strings.exportSuccessTitle,
                body = strings.exportSuccessBody,
                onClose = { exportSuccess = false }
            )
        }
        }
    }

    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            containerColor = colors.paperLight,
            titleContentColor = colors.ink,
            textContentColor = colors.inkSoft,
            title = {
                Text(text = strings.deleteConfirmTitle, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(text = strings.deleteConfirmBody, style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                TextButton(onClick = {
                    askDelete = false
                    onDelete()
                }) {
                    Text(strings.delete, color = colors.wax)
                }
            },
            dismissButton = {
                TextButton(onClick = { askDelete = false }) {
                    Text(strings.cancel, color = colors.inkFaded)
                }
            }
        )
    }

    if (showSignaturePad) {
        SignaturePad(
            initial = entry.signature,
            onSave = { strokes ->
                onSignatureChange(strokes)
                showSignaturePad = false
            },
            onDismiss = { showSignaturePad = false }
        )
    }

    pendingExportShare?.let { share ->
        AlertDialog(
            onDismissRequest = { pendingExportShare = null },
            containerColor = colors.paperLight,
            titleContentColor = colors.ink,
            textContentColor = colors.inkSoft,
            title = {
                Text(text = strings.exportFormatTitle, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(DiaryDim.space3)) {
                    ExportOption(
                        icon = Icons.Outlined.Image,
                        label = strings.exportImage,
                        onClick = {
                            pendingExportShare = null
                            runExport(SheetFormat.PNG, share)
                        }
                    )
                    ExportOption(
                        icon = Icons.Outlined.PictureAsPdf,
                        label = strings.exportPdf,
                        onClick = {
                            pendingExportShare = null
                            runExport(SheetFormat.PDF, share)
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingExportShare = null }) {
                    Text(strings.cancel, color = colors.inkFaded)
                }
            }
        )
    }
}

/** Floating wax seal that confirms the sheet truly landed on the device. */
@Composable
private fun SuccessSeal(title: String, body: String, onClose: () -> Unit) {
    val colors = LocalDiaryColors.current
    Surface(
        shape = RoundedCornerShape(DiaryDim.radiusPaper),
        color = colors.paperLight,
        border = BorderStroke(DiaryDim.dividerHeight, colors.gold.copy(alpha = 0.55f)),
        shadowElevation = 8.dp,
        modifier = Modifier
            .padding(horizontal = DiaryDim.screenPad)
            .clickable(onClick = onClose)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DiaryDim.space4, vertical = DiaryDim.space3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(DiaryDim.touchTarget)
                    .background(colors.gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.paperLight,
                    modifier = Modifier.size(DiaryDim.iconMedium)
                )
            }
            Spacer(Modifier.width(DiaryDim.space3))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFamily),
                    color = colors.ink
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkSoft
                )
            }
        }
    }
}

/** The «Guardado» label appears as if the pen had just written it. */
@Composable
private fun WrittenLabel(text: String, color: Color) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(text) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(620, easing = EaseOutCubic))
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier.drawWithContent {
            clipRect(right = size.width * reveal.value) { this@drawWithContent.drawContent() }
        }
    )
}

/** A drop of ink that blooms on the paper and dries out. */
@Composable
private fun InkBloom(x: Dp, y: Dp, ink: Color, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(900, easing = EaseOutCubic))
        onFinished()
    }
    Box(
        modifier = Modifier
            .offset(x = x - 6.dp, y = y - 12.dp)
            .size(12.dp)
            .drawBehind {
                drawCircle(
                    color = ink.copy(alpha = 0.22f * (1f - progress.value)),
                    radius = size.minDimension * (0.18f + progress.value * 0.42f),
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
    )
}

/** A signature shown inside the sheet, in ink. */
@Composable
private fun SignatureView(
    strokes: List<SignatureStroke>,
    ink: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawSignatureStrokes(strokes.map { it.toOffsets() }, ink)
    }
}

/** Full-screen paper canvas where the user signs with a finger. */
@Composable
private fun SignaturePad(
    initial: List<SignatureStroke>,
    onSave: (List<SignatureStroke>) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val colors = LocalDiaryColors.current
    var strokes by remember { mutableStateOf(initial.map { it.toOffsets() }) }
    var redoStack by remember { mutableStateOf(listOf<List<Offset>>()) }
    var active by remember { mutableStateOf(listOf<Offset>()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = colors.paper) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(DiaryDim.space5)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strings.signatureTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = SerifFamily),
                        color = colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    InkIconButton(
                        icon = Icons.Outlined.Close,
                        contentDescription = strings.cancel,
                        onClick = onDismiss
                    )
                }
                Spacer(Modifier.height(DiaryDim.space2))
                Text(
                    text = strings.signatureHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkWhisper
                )
                Spacer(Modifier.height(DiaryDim.space3))

                Surface(
                    color = colors.paperLight,
                    border = BorderStroke(DiaryDim.dividerHeight, colors.edge),
                    shape = RoundedCornerShape(DiaryDim.radiusPaper)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { position ->
                                        active = listOf(
                                            Offset(
                                                position.x / size.width,
                                                position.y / size.height
                                            )
                                        )
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        active = active + Offset(
                                            change.position.x / size.width,
                                            change.position.y / size.height
                                        )
                                    },
                                    onDragEnd = {
                                        if (active.size > 1) {
                                            strokes = strokes + listOf(active)
                                            redoStack = emptyList()
                                        }
                                        active = emptyList()
                                    },
                                    onDragCancel = { active = emptyList() }
                                )
                            }
                    ) {
                        drawSignatureStrokes(strokes + listOf(active), colors.inkBlue)
                    }
                }

                Spacer(Modifier.height(DiaryDim.space3))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DiaryDim.space2),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InkIconButton(
                        icon = Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = strings.undo,
                        enabled = strokes.isNotEmpty(),
                        onClick = {
                            redoStack = redoStack + listOf(strokes.last())
                            strokes = strokes.dropLast(1)
                        }
                    )
                    InkIconButton(
                        icon = Icons.AutoMirrored.Outlined.Redo,
                        contentDescription = strings.redo,
                        enabled = redoStack.isNotEmpty(),
                        onClick = {
                            strokes = strokes + listOf(redoStack.last())
                            redoStack = redoStack.dropLast(1)
                        }
                    )
                    InkIconButton(
                        icon = Icons.Outlined.CleaningServices,
                        contentDescription = strings.clearSignature,
                        enabled = strokes.isNotEmpty(),
                        onClick = {
                            redoStack = redoStack + listOf(strokes.last())
                            strokes = emptyList()
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    SealedButton(
                        label = strings.save,
                        enabled = strokes.isNotEmpty(),
                        onClick = { onSave(strokes.map { it.toStroke() }) }
                    )
                }
            }
        }
    }
}

/** One format row inside the export dialog. */
@Composable
private fun ExportOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colors = LocalDiaryColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DiaryDim.radiusPaper),
        color = colors.paper,
        border = BorderStroke(DiaryDim.dividerHeight, colors.edge.copy(alpha = 0.8f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(DiaryDim.space3)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.gold,
                modifier = Modifier.size(DiaryDim.iconMedium)
            )
            Spacer(Modifier.width(DiaryDim.space3))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink
            )
        }
    }
}

/** Draws normalized strokes, scaled to the canvas with a soft ink bleed. */
private fun DrawScope.drawSignatureStrokes(strokes: List<List<Offset>>, ink: Color) {
    strokes.forEach { stroke ->
        if (stroke.size < 2) return@forEach
        val path = Path()
        stroke.forEachIndexed { index, point ->
            val scaled = Offset(point.x * size.width, point.y * size.height)
            if (index == 0) path.moveTo(scaled.x, scaled.y) else path.lineTo(scaled.x, scaled.y)
        }
        drawPath(
            path,
            ink.copy(alpha = 0.16f),
            style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path,
            ink.copy(alpha = 0.92f),
            style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun SignatureStroke.toOffsets(): List<Offset> =
    points.chunked(2).filter { it.size == 2 }.map { Offset(it[0], it[1]) }

private fun List<Offset>.toStroke(): SignatureStroke =
    SignatureStroke(flatMap { listOf(it.x, it.y) })

/** Wax seal toggle marking a page as intimate. */
@Composable
private fun WaxToggle(active: Boolean, description: String, onClick: () -> Unit) {
    val colors = LocalDiaryColors.current
    val scale = remember { Animatable(1f) }
    LaunchedEffect(active) {
        scale.animateTo(if (active) 1.16f else 0.94f, tween(170))
        scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
    }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) colors.wax else colors.paperLight.copy(alpha = 0.7f),
        border = BorderStroke(
            DiaryDim.dividerHeight,
            if (active) colors.waxDeep else colors.edge.copy(alpha = 0.55f)
        ),
        modifier = Modifier
            .size(DiaryDim.touchTarget)
            .semantics { contentDescription = description }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .drawBehind {
                        val stroke = Stroke(width = size.minDimension * 0.14f)
                        drawCircle(
                            color = if (active) colors.paperLight.copy(alpha = 0.9f) else colors.inkFaded,
                            radius = size.minDimension * 0.42f,
                            style = stroke
                        )
                        drawCircle(
                            color = if (active) colors.paperLight.copy(alpha = 0.9f) else colors.inkFaded,
                            radius = size.minDimension * 0.12f
                        )
                    }
            )
        }
    }
}
