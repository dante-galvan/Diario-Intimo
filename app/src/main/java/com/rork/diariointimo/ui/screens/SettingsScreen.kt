package com.rork.diariointimo.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.rork.diariointimo.data.AppearanceMode
import com.rork.diariointimo.data.AutoLockDelay
import com.rork.diariointimo.i18n.AppLanguage
import com.rork.diariointimo.i18n.AppStrings
import com.rork.diariointimo.i18n.LocalStrings
import com.rork.diariointimo.ui.components.AppearInk
import com.rork.diariointimo.ui.components.InkIconButton
import com.rork.diariointimo.ui.components.PaperBackground
import com.rork.diariointimo.ui.components.SealedButton
import com.rork.diariointimo.ui.theme.DiaryDim
import com.rork.diariointimo.ui.theme.LocalDiaryColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    language: AppLanguage,
    appearance: AppearanceMode,
    autoLock: AutoLockDelay,
    hasPassword: Boolean,
    biometryEnabled: Boolean,
    biometryAvailable: Boolean,
    recoveryQuestion: String?,
    onOpenLanguage: () -> Unit,
    onOpenPassword: () -> Unit,
    onOpenRecovery: () -> Unit,
    onOpenActivate: () -> Unit,
    onAppearanceChange: (AppearanceMode) -> Unit,
    onAutoLockChange: (AutoLockDelay) -> Unit,
    onBiometryChange: (Boolean) -> Unit,
    onDisablePassword: () -> Unit,
    onLock: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val colors = LocalDiaryColors.current
    val scroll = rememberScrollState()
    var askDeactivate by remember { mutableStateOf(false) }

    PaperBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scroll)
                .navigationBarsPadding()
        ) {
            ScreenHeader(title = strings.settings, onBack = onBack)

            Section(title = strings.appearance) {
                Column(verticalArrangement = Arrangement.spacedBy(DiaryDim.space2)) {
                    AppearanceMode.entries.forEach { mode ->
                        ChoiceRow(
                            label = appearanceLabel(mode, strings),
                            selected = mode == appearance,
                            onClick = { onAppearanceChange(mode) }
                        )
                    }
                }
            }

            Section(title = strings.sectionLanguage) {
                SettingRow(
                    label = strings.language,
                    value = language.nativeName ?: strings.languageAuto,
                    onClick = onOpenLanguage
                )
                Hint(strings.languageHint)
            }

            Section(title = strings.sectionSecurity) {
                if (!hasPassword) {
                    SettingRow(
                        label = strings.passwordProtection,
                        value = strings.passwordProtectionOff,
                        onClick = {}
                    )
                    Spacer(Modifier.height(DiaryDim.space3))
                    SealedButton(
                        label = strings.activatePassword,
                        onClick = onOpenActivate,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Hint(strings.activatePasswordHint)
                } else {
                    SettingRow(
                        label = strings.passwordProtection,
                        value = strings.passwordProtectionOn,
                        onClick = {}
                    )
                    Spacer(Modifier.height(DiaryDim.space3))
                    SettingRow(
                        label = strings.changePassword,
                        value = "· · · · ·",
                        onClick = onOpenPassword
                    )
                    Hint(strings.noRecoveryWarning, italic = true)
                    Spacer(Modifier.height(DiaryDim.space3))
                    SettingRow(
                        label = strings.recoverySection,
                        value = if (recoveryQuestion != null) {
                            strings.recoveryMethodQuestion
                        } else {
                            strings.recoveryMethodNone
                        },
                        onClick = onOpenRecovery
                    )
                    if (biometryAvailable) {
                        Spacer(Modifier.height(DiaryDim.space4))
                        ToggleRow(
                            label = strings.biometrySetting,
                            checked = biometryEnabled,
                            onChecked = onBiometryChange
                        )
                        Hint(strings.biometryBody)
                    }
                    Spacer(Modifier.height(DiaryDim.space4))
                    Text(
                        text = strings.autoLock,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink
                    )
                    Spacer(Modifier.height(DiaryDim.space3))
                    Column(verticalArrangement = Arrangement.spacedBy(DiaryDim.space2)) {
                        AutoLockDelay.entries.forEach { option ->
                            ChoiceRow(
                                label = autoLockLabel(option, strings),
                                selected = option == autoLock,
                                onClick = { onAutoLockChange(option) }
                            )
                        }
                    }
                    Hint(strings.autoLockHint)
                    Spacer(Modifier.height(DiaryDim.space3))
                    SettingRow(
                        label = strings.deactivatePassword,
                        value = "",
                        onClick = { askDeactivate = true }
                    )
                }
            }

            Section(title = strings.sectionPrivacy) {
                Text(
                    text = strings.privacyTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontStyle = FontStyle.Italic),
                    color = colors.ink
                )
                Spacer(Modifier.height(DiaryDim.space2))
                Text(
                    text = strings.privacyBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkFaded
                )
            }

            if (hasPassword) {
                Spacer(Modifier.height(DiaryDim.space2))
                SealedButton(
                    label = strings.lockDiary,
                    onClick = onLock,
                    modifier = Modifier
                        .padding(horizontal = DiaryDim.screenPad)
                        .fillMaxWidth()
                )
            }

            Spacer(Modifier.height(DiaryDim.space8))
            Text(
                text = strings.aboutTitle,
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkWhisper,
                modifier = Modifier.padding(horizontal = DiaryDim.screenPad)
            )
            Spacer(Modifier.height(DiaryDim.space2))
            Text(
                text = strings.aboutBody,
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkWhisper,
                modifier = Modifier.padding(horizontal = DiaryDim.screenPad)
            )
            Spacer(Modifier.height(DiaryDim.space12))
        }
    }

    if (askDeactivate) {
        AlertDialog(
            onDismissRequest = { askDeactivate = false },
            containerColor = colors.paperLight,
            titleContentColor = colors.ink,
            textContentColor = colors.inkSoft,
            title = {
                Text(text = strings.deactivateTitle, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(text = strings.deactivateBody, style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                TextButton(onClick = {
                    askDeactivate = false
                    onDisablePassword()
                }) {
                    Text(strings.deactivatePassword, color = colors.wax)
                }
            },
            dismissButton = {
                TextButton(onClick = { askDeactivate = false }) {
                    Text(strings.cancel, color = colors.inkFaded)
                }
            }
        )
    }
}

/** Turning protection on from Settings: create → confirm → recovery + biometry. */
@Composable
fun ActivatePasswordScreen(
    biometryAvailable: Boolean,
    biometryEnabled: Boolean,
    onActivate: suspend (String) -> Unit,
    onBiometryChange: (Boolean) -> Unit,
    onOpenRecovery: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val colors = LocalDiaryColors.current
    val scope = rememberCoroutineScope()
    var next by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var created by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    PaperBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            ScreenHeader(title = strings.activatePassword, onBack = onBack)
            Column(
                modifier = Modifier.padding(horizontal = DiaryDim.screenPad),
                verticalArrangement = Arrangement.spacedBy(DiaryDim.space4)
            ) {
                if (!created) {
                    Hint(strings.activatePasswordHint)
                    SecretField(
                        label = strings.newPassword,
                        value = next,
                        onValueChange = { next = it; error = null },
                        showPassword = showPassword,
                        onToggleVisibility = { showPassword = !showPassword }
                    )
                    SecretField(
                        label = strings.repeatPassword,
                        value = repeat,
                        onValueChange = { repeat = it; error = null },
                        imeAction = ImeAction.Done,
                        showPassword = showPassword,
                        onToggleVisibility = { showPassword = !showPassword }
                    )
                    Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
                        color = colors.wax
                    )
                    SealedButton(
                        label = strings.activatePassword,
                        enabled = next.isNotEmpty() && repeat.isNotEmpty(),
                        onClick = {
                            when {
                                next.length < 4 -> error = strings.passwordTooShort
                                next != repeat -> error = strings.passwordsDoNotMatch
                                else -> scope.launch {
                                    onActivate(next)
                                    created = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = strings.passwordProtectionOn,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontStyle = FontStyle.Italic
                        ),
                        color = colors.gold
                    )
                    SettingRow(
                        label = strings.recoverySection,
                        value = strings.recoveryMethodNone,
                        onClick = onOpenRecovery
                    )
                    Hint(strings.recoveryIntro)
                    if (biometryAvailable) {
                        ToggleRow(
                            label = strings.biometrySetting,
                            checked = biometryEnabled,
                            onChecked = onBiometryChange
                        )
                        Hint(strings.biometryBody)
                    }
                    SealedButton(
                        label = strings.continueAction,
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageScreen(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val colors = LocalDiaryColors.current
    PaperBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            ScreenHeader(title = strings.language, onBack = onBack)
            Column(
                modifier = Modifier.padding(horizontal = DiaryDim.screenPad),
                verticalArrangement = Arrangement.spacedBy(DiaryDim.space3)
            ) {
                AppLanguage.entries.forEachIndexed { index, option ->
                    AppearInk(delayMillis = index * 45) {
                        ChoiceRow(
                            label = option.nativeName ?: strings.languageAuto,
                            selected = option == selected,
                            onClick = { onSelect(option) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(DiaryDim.space4))
            Text(
                text = strings.languageHint,
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkWhisper,
                modifier = Modifier.padding(horizontal = DiaryDim.screenPad)
            )
        }
    }
}

@Composable
fun ChangePasswordScreen(
    onChange: suspend (current: String, next: String) -> Boolean,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val colors = LocalDiaryColors.current
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    PaperBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            ScreenHeader(title = strings.changePassword, onBack = onBack)
            Column(
                modifier = Modifier.padding(horizontal = DiaryDim.screenPad),
                verticalArrangement = Arrangement.spacedBy(DiaryDim.space4)
            ) {
                SecretField(
                    label = strings.currentPassword,
                    value = current,
                    onValueChange = { current = it; error = null },
                    showPassword = showPassword,
                    onToggleVisibility = { showPassword = !showPassword }
                )
                SecretField(
                    label = strings.newPassword,
                    value = next,
                    onValueChange = { next = it; error = null },
                    showPassword = showPassword,
                    onToggleVisibility = { showPassword = !showPassword }
                )
                SecretField(
                    label = strings.repeatPassword,
                    value = repeat,
                    onValueChange = { repeat = it; error = null },
                    imeAction = ImeAction.Done,
                    showPassword = showPassword,
                    onToggleVisibility = { showPassword = !showPassword }
                )

                val errorAlpha by animateFloatAsState(
                    targetValue = if (error != null) 1f else 0f,
                    animationSpec = tween(240),
                    label = "passwordError"
                )
                val successAlpha by animateFloatAsState(
                    targetValue = if (success) 1f else 0f,
                    animationSpec = tween(240),
                    label = "passwordSuccess"
                )
                Box(modifier = Modifier.heightIn(min = DiaryDim.space6)) {
                    Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontStyle = FontStyle.Italic
                        ),
                        color = colors.wax,
                        modifier = Modifier.alpha(errorAlpha)
                    )
                    Text(
                        text = strings.passwordChanged,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontStyle = FontStyle.Italic
                        ),
                        color = colors.gold,
                        modifier = Modifier.alpha(successAlpha)
                    )
                }

                SealedButton(
                    label = strings.save,
                    enabled = current.isNotEmpty() && next.isNotEmpty() && repeat.isNotEmpty(),
                    onClick = {
                        when {
                            next.length < 4 -> error = strings.passwordTooShort
                            next != repeat -> error = strings.passwordsDoNotMatch
                            else -> scope.launch {
                                val ok = onChange(current, next)
                                if (!ok) {
                                    error = strings.currentPasswordWrong
                                } else {
                                    success = true
                                    current = ""
                                    next = ""
                                    repeat = ""
                                    delay(1200)
                                    onBack()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    val colors = LocalDiaryColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DiaryDim.headerPad, vertical = DiaryDim.space3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InkIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = LocalStrings.current.back,
            onClick = onBack
        )
        Spacer(Modifier.width(DiaryDim.space3))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.ink
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val colors = LocalDiaryColors.current
    Column(modifier = Modifier.padding(horizontal = DiaryDim.screenPad, vertical = DiaryDim.space4)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.gold
            )
            Spacer(Modifier.width(DiaryDim.space3))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(colors.gold.copy(alpha = 0.28f))
            )
        }
        Spacer(Modifier.height(DiaryDim.space3))
        content()
    }
}

@Composable
private fun Hint(text: String, italic: Boolean = false) {
    val colors = LocalDiaryColors.current
    Spacer(Modifier.height(DiaryDim.space3))
    if (text.isNotEmpty()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal
            ),
            color = if (italic) colors.inkFaded else colors.inkWhisper
        )
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    val colors = LocalDiaryColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DiaryDim.radiusPaper),
        color = colors.paperLight.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, colors.edge),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DiaryDim.space4, vertical = DiaryDim.space4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                modifier = Modifier.weight(1f)
            )
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkFaded
                )
            }
        }
    }
}

/** Paper row with a switch, used for the biometric gate. */
@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val colors = LocalDiaryColors.current
    Surface(
        shape = RoundedCornerShape(DiaryDim.radiusPaper),
        color = colors.paperLight.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, colors.edge),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = DiaryDim.space4, end = DiaryDim.space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = DiaryDim.space4)
            )
            Switch(
                checked = checked,
                onCheckedChange = onChecked,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = colors.gold,
                    checkedThumbColor = colors.paperLight
                )
            )
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalDiaryColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DiaryDim.radiusPaper),
        color = if (selected) colors.ink else colors.paperLight.copy(alpha = 0.7f),
        border = BorderStroke(
            1.dp,
            if (selected) colors.gold.copy(alpha = 0.6f) else colors.edge
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DiaryDim.space4, vertical = DiaryDim.space3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) colors.paperLight else colors.inkSoft,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colors.goldLight,
                    modifier = Modifier.size(DiaryDim.iconMedium)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(DiaryDim.iconSmall)
                        .background(Color.Transparent, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun SecretField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Next,
    showPassword: Boolean = false,
    onToggleVisibility: (() -> Unit)? = null
) {
    val colors = LocalDiaryColors.current
    val strings = LocalStrings.current
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkFaded
        )
        Spacer(Modifier.height(DiaryDim.space2))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DiaryDim.fieldHeight)
                .background(colors.paperLight.copy(alpha = 0.8f), RoundedCornerShape(DiaryDim.radiusPaper))
                .padding(horizontal = DiaryDim.space3),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = "· · · ·",
                    style = com.rork.diariointimo.ui.theme.SansField.copy(color = colors.inkWhisper),
                    modifier = if (onToggleVisibility != null) Modifier.padding(end = DiaryDim.touchTarget) else Modifier
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onToggleVisibility != null) Modifier.padding(end = DiaryDim.touchTarget) else Modifier),
                textStyle = com.rork.diariointimo.ui.theme.SansField.copy(color = if (showPassword) colors.ink else Color.Transparent),
                cursorBrush = SolidColor(colors.gold),
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = imeAction
                )
            )
            if (onToggleVisibility != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(DiaryDim.touchTarget)
                        .padding(DiaryDim.space1)
                        .clip(RoundedCornerShape(DiaryDim.radiusPaper))
                        .clickable(onClick = onToggleVisibility),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showPassword) strings.hidePassword else strings.showPassword,
                        tint = colors.inkFaded,
                        modifier = Modifier.size(DiaryDim.iconMedium)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.edge)
        )
    }
}

private fun appearanceLabel(mode: AppearanceMode, strings: AppStrings): String = when (mode) {
    AppearanceMode.LIGHT -> strings.appearanceLight
    AppearanceMode.DARK -> strings.appearanceDark
    AppearanceMode.SYSTEM -> strings.appearanceAuto
}

private fun autoLockLabel(delay: AutoLockDelay, strings: AppStrings): String = when (delay) {
    AutoLockDelay.IMMEDIATE -> strings.autoLockImmediate
    AutoLockDelay.NEVER -> strings.autoLockNever
    else -> strings.minutesLabel(delay.minutes)
}
