package com.rork.diariointimo.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.rork.diariointimo.data.BiometricGate
import com.rork.diariointimo.data.RecoveryCheck
import com.rork.diariointimo.i18n.AppStrings
import com.rork.diariointimo.i18n.LocalStrings
import com.rork.diariointimo.ui.components.DeskBackground
import com.rork.diariointimo.ui.components.Envelope
import com.rork.diariointimo.ui.components.InkRule
import com.rork.diariointimo.ui.components.SealedButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.rork.diariointimo.ui.components.paperGrain
import com.rork.diariointimo.ui.theme.DiaryDim
import com.rork.diariointimo.ui.theme.LocalDiaryColors
import com.rork.diariointimo.ui.theme.SansField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LockMode { UNLOCK, CREATE, CHOOSE }

/** Every state of the letter: secret, its confirmation, recovery and biometrics. */
private enum class Stage {
    CHOOSE, SECRET, CONFIRM, RECOVERY_SETUP, RECOVERY_ANSWER, RECOVERY_NEW, RECOVERY_CONFIRM, BIOMETRY
}

private const val MIN_PASSWORD_LENGTH = 4
private const val PRESET_QUESTION_COUNT = 6

/**
 * The heart of the app: a letter that arrives on a candle-lit desk. On the
 * first visit it creates the secret, then asks for a recovery method before
 * offering biometrics; afterwards it asks for the secret, offers recovery
 * when it is forgotten, or opens with the native biometric gate.
 */
@Composable
fun LockScreen(
    mode: LockMode,
    biometrySupported: Boolean,
    biometryEnabled: Boolean,
    failedAttempts: Int,
    recoveryQuestion: String?,
    onSubmit: suspend (String) -> Boolean,
    onRecoveryAnswer: suspend (String) -> RecoveryCheck,
    onSaveRecovery: (question: String?, answer: String?) -> Unit,
    onResetPassword: suspend (String) -> Unit,
    onBiometryChange: (Boolean) -> Unit,
    onSkipProtection: () -> Unit,
    onOpened: () -> Unit
) {
    val strings = LocalStrings.current
    val colors = LocalDiaryColors.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val activity = LocalContext.current as? FragmentActivity

    val flapAngle = remember { Animatable(0f) }
    val letterReveal = remember { Animatable(0f) }
    val sealScale = remember { Animatable(1f) }
    val stageAlpha = remember { Animatable(0f) }
    val stageLift = remember { Animatable(46f) }
    val shake = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val inkBlot = remember { Animatable(0f) }

    var stage by remember {
        mutableStateOf(if (mode == LockMode.CHOOSE) Stage.CHOOSE else Stage.SECRET)
    }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newConfirm by remember { mutableStateOf("") }
    var recoveryAnswer by remember { mutableStateOf("") }
    var recoveryVerified by remember { mutableStateOf(false) }
    var passwordOnly by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var writtenChars by remember { mutableIntStateOf(0) }
    var showPassword by remember { mutableStateOf(false) }

    // First-run recovery setup fields.
    var useQuestionMethod by remember { mutableStateOf(true) }
    var questionIndex by remember { mutableIntStateOf(0) }
    var customQuestion by remember { mutableStateOf("") }
    var setupAnswer by remember { mutableStateOf("") }

    val presetQuestions = remember {
        listOf(strings.question1, strings.question2, strings.question3, strings.question4, strings.question5, strings.question6)
    }
    // Resolve the chosen question index across all languages to avoid losing the
    // selection when the app language changes after the question was set up.
    val resolvedQuestionIndex by remember {
        mutableIntStateOf(
            if (recoveryQuestion != null) {
                val localIndex = presetQuestions.indexOf(recoveryQuestion)
                if (localIndex >= 0) localIndex else resolveQuestionIndex(recoveryQuestion)
            } else 0
        )
    }
    val chosenQuestion: String? = when {
        questionIndex in 0 until PRESET_QUESTION_COUNT -> presetQuestions[questionIndex]
        questionIndex == PRESET_QUESTION_COUNT -> customQuestion.trim().takeIf { it.isNotEmpty() }
        else -> null
    }
    val recoveryReady = useQuestionMethod && chosenQuestion != null && setupAnswer.trim().length >= 2

    val current = when (stage) {
        Stage.CONFIRM -> confirmation
        Stage.RECOVERY_NEW -> newPassword
        Stage.RECOVERY_CONFIRM -> newConfirm
        else -> password
    }
    val useBiometry = mode == LockMode.UNLOCK &&
        stage == Stage.SECRET && biometrySupported && biometryEnabled && !passwordOnly
    val interactive = letterReveal.value > 0.98f && !closing && !busy

    LaunchedEffect(Unit) {
        stageAlpha.animateTo(1f, tween(1100, easing = LinearEasing))
        stageLift.animateTo(0f, tween(1200, easing = EaseOutCubic))
        delay(180)
        scope.launch { sealScale.animateTo(0f, tween(420, easing = EaseInOutCubic)) }
        flapAngle.animateTo(-172f, tween(1250, easing = EaseInOutCubic))
        letterReveal.animateTo(1f, tween(950, easing = EaseOutCubic))
        delay(120)
        // The keyboard only makes sense when a secret is expected.
        if (mode == LockMode.CREATE || !(biometrySupported && biometryEnabled)) {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }

    // The recovery answer and the new password focus themselves, ready to type.
    LaunchedEffect(stage) {
        showPassword = false
        if (stage == Stage.RECOVERY_ANSWER || stage == Stage.RECOVERY_NEW) {
            delay(260)
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
        // After choosing protection, the create-secret field takes over.
        if (stage == Stage.SECRET && mode != LockMode.UNLOCK) {
            delay(200)
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }

    fun fail(message: String, clearSecret: Boolean = true) {
        errorMessage = message
        scope.launch {
            inkBlot.snapTo(0f)
            launch { inkBlot.animateTo(1f, tween(900, easing = EaseOutCubic)) }
            repeat(3) {
                shake.animateTo(9f, tween(60))
                shake.animateTo(-9f, tween(60))
            }
            shake.animateTo(0f, tween(90))
            delay(260)
            if (clearSecret) {
                when (stage) {
                    Stage.CONFIRM -> confirmation = ""
                    Stage.RECOVERY_NEW -> newPassword = ""
                    Stage.RECOVERY_CONFIRM -> newConfirm = ""
                    else -> password = ""
                }
                writtenChars = 0
            }
        }
    }

    fun sealAndEnter() {
        closing = true
        keyboard?.hide()
        scope.launch {
            letterReveal.animateTo(0f, tween(620, easing = EaseInOutCubic))
            launch { sealScale.animateTo(1f, tween(420, easing = EaseOutCubic)) }
            flapAngle.animateTo(0f, tween(620, easing = EaseInOutCubic))
            delay(120)
            flash.animateTo(1f, tween(520, easing = EaseInOutCubic))
            onOpened()
        }
    }

    fun attemptBiometry(onSuccess: () -> Unit) {
        val gate = activity
        if (gate == null) {
            fail(strings.biometryFallback, clearSecret = false)
            return
        }
        keyboard?.hide()
        BiometricGate.authenticate(
            activity = gate,
            title = strings.biometryDialogTitle,
            subtitle = strings.biometryDialogSubtitle,
            negativeText = strings.usePassword
        ) { ok ->
            if (ok) onSuccess() else fail(strings.biometryFallback, clearSecret = false)
        }
    }

    BackHandler(enabled = interactive && stage != Stage.CHOOSE) {
        when (stage) {
            Stage.SECRET -> if (mode == LockMode.UNLOCK) Unit
            Stage.CONFIRM -> {
                stage = Stage.SECRET
                confirmation = ""
                errorMessage = null
            }
            Stage.RECOVERY_SETUP -> {
                stage = Stage.CONFIRM
                errorMessage = null
            }
            Stage.RECOVERY_ANSWER -> {
                stage = Stage.SECRET
                recoveryAnswer = ""
                recoveryVerified = false
                errorMessage = null
            }
            Stage.RECOVERY_NEW -> {
                stage = Stage.RECOVERY_ANSWER
                errorMessage = null
            }
            Stage.RECOVERY_CONFIRM -> {
                stage = Stage.RECOVERY_NEW
                newConfirm = ""
                errorMessage = null
            }
            Stage.BIOMETRY -> {
                onBiometryChange(false)
                sealAndEnter()
            }
            else -> Unit
        }
    }

    fun submit() {
        if (busy || closing) return
        when (stage) {
            Stage.SECRET -> if (mode != LockMode.UNLOCK) {
                if (password.length < MIN_PASSWORD_LENGTH) fail(strings.passwordTooShort)
                else {
                    stage = Stage.CONFIRM
                    writtenChars = 0
                    errorMessage = null
                }
            } else {
                if (password.isEmpty()) return
                busy = true
                scope.launch {
                    val accepted = onSubmit(password)
                    busy = false
                    if (accepted) sealAndEnter() else fail(strings.wrongPassword)
                }
            }
            Stage.CONFIRM -> if (confirmation != password) {
                fail(strings.passwordsDoNotMatch)
            } else {
                stage = Stage.RECOVERY_SETUP
                writtenChars = 0
                errorMessage = null
            }
            Stage.RECOVERY_SETUP -> {
                val hasQuestion = useQuestionMethod && chosenQuestion != null && setupAnswer.trim().length >= 2
                if (!hasQuestion) {
                    fail(strings.recoveryNeeded, clearSecret = false)
                } else {
                    busy = true
                    scope.launch {
                        onSaveRecovery(chosenQuestion, setupAnswer)
                        onSubmit(password)
                        busy = false
                        keyboard?.hide()
                        if (biometrySupported) {
                            stage = Stage.BIOMETRY
                            errorMessage = null
                        } else {
                            sealAndEnter()
                        }
                    }
                }
            }
            Stage.RECOVERY_ANSWER -> {
                if (recoveryAnswer.isBlank()) return
                busy = true
                scope.launch {
                    when (onRecoveryAnswer(recoveryAnswer)) {
                        is RecoveryCheck.Correct -> {
                            busy = false
                            recoveryVerified = true
                            stage = Stage.RECOVERY_NEW
                            writtenChars = 0
                            errorMessage = null
                        }
                        RecoveryCheck.Wrong -> {
                            busy = false
                            fail(strings.recoveryWrongAnswer, clearSecret = false)
                        }
                    }
                }
            }
            Stage.RECOVERY_NEW -> if (newPassword.length < MIN_PASSWORD_LENGTH) {
                fail(strings.passwordTooShort)
            } else {
                stage = Stage.RECOVERY_CONFIRM
                writtenChars = 0
                errorMessage = null
            }
            Stage.RECOVERY_CONFIRM -> if (newConfirm != newPassword) {
                fail(strings.passwordsDoNotMatch)
            } else {
                busy = true
                scope.launch {
                    onResetPassword(newPassword)
                    busy = false
                    keyboard?.hide()
                    errorMessage = strings.recoverySaved
                    delay(750)
                    sealAndEnter()
                }
            }
            Stage.BIOMETRY -> Unit
            Stage.CHOOSE -> Unit
        }
    }

    val bigTitle = when (stage) {
        Stage.CHOOSE -> strings.protectionQuestionTitle
        Stage.BIOMETRY -> strings.biometryTitle
        Stage.CONFIRM, Stage.RECOVERY_CONFIRM -> strings.confirmPrompt
        Stage.RECOVERY_SETUP -> strings.recoverySection
        Stage.RECOVERY_ANSWER -> strings.recoveryTitle
        Stage.RECOVERY_NEW -> strings.recoveryNewPrompt
        Stage.SECRET -> if (mode == LockMode.CREATE) strings.createPrompt else strings.tagline
    }
    val subtitle = when (stage) {
        Stage.CHOOSE -> strings.protectionQuestionBody
        Stage.BIOMETRY -> strings.biometryBody
        Stage.CONFIRM -> strings.confirmSubtitle
        Stage.RECOVERY_CONFIRM -> strings.recoveryNewSubtitle
        Stage.RECOVERY_SETUP -> strings.recoveryIntro
        Stage.RECOVERY_ANSWER -> strings.recoveryBody
        Stage.RECOVERY_NEW -> strings.recoveryNewSubtitle
        Stage.SECRET -> if (mode == LockMode.CREATE) strings.setupSubtitle else strings.unlockPrompt
    }
    val action = when (stage) {
        Stage.CONFIRM, Stage.RECOVERY_CONFIRM -> strings.createAction
        Stage.RECOVERY_SETUP -> strings.recoverySetupAction
        Stage.RECOVERY_ANSWER -> strings.recoveryVerify
        Stage.RECOVERY_NEW -> strings.continueAction
        Stage.SECRET -> if (mode == LockMode.CREATE) strings.continueAction else strings.unlockAction
        Stage.BIOMETRY -> ""
        Stage.CHOOSE -> ""
    }

    DeskBackground(glow = stageAlpha.value) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = DiaryDim.space5),
            contentAlignment = Alignment.Center
        ) {
            // Responsive envelope: it never exceeds the space the screen offers,
            // so small phones and open keyboards keep every element visible.
            val envWidth = minOf(320.dp, maxWidth)
            val envHeight = minOf(520.dp, maxHeight)
            val letterWidth = envWidth - 38.dp
            val baseLetterHeight = (envHeight - 170.dp).coerceAtLeast(260.dp).coerceAtMost(340.dp)
            val tallLetterHeight = maxOf(baseLetterHeight, (envHeight - 110.dp).coerceAtMost(460.dp))
            val tallStage = stage == Stage.RECOVERY_SETUP || stage == Stage.RECOVERY_ANSWER
            val letterHeight by animateDpAsState(
                targetValue = if (tallStage) tallLetterHeight else baseLetterHeight,
                animationSpec = tween(560),
                label = "letterHeight"
            )

            Box(
                modifier = Modifier
                    .width(envWidth)
                    .height(envHeight)
                    .graphicsLayer {
                        alpha = stageAlpha.value
                        translationY = stageLift.value * density
                        translationX = shake.value * density
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Envelope(
                    flapAngle = flapAngle.value,
                    sealScale = sealScale.value,
                    width = envWidth - 20.dp,
                    height = (envWidth - 20.dp) * 0.593f,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // The letter, pulled up out of the envelope's mouth.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = -(envHeight.value * 0.227f).dp)
                        .width(letterWidth)
                        .height(letterHeight * letterReveal.value)
                        .clipToBounds(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val secretEntry: @Composable () -> Unit = {
                        SecretEntry(
                            action = action,
                            value = current,
                            writtenChars = writtenChars,
                            interactive = interactive,
                            showPassword = showPassword,
                            biometryLink = if (mode == LockMode.UNLOCK && stage == Stage.SECRET &&
                                biometrySupported && biometryEnabled
                            ) strings.biometryUnlock else null,
                            forgotLink = if (mode == LockMode.UNLOCK && stage == Stage.SECRET &&
                                recoveryQuestion != null
                            ) strings.forgotPassword else null,
                            focusRequester = focusRequester,
                            inkBlot = inkBlot.value,
                            onValueChange = { next ->
                                if (next.length <= 40) {
                                    when (stage) {
                                        Stage.CONFIRM -> confirmation = next
                                        Stage.RECOVERY_NEW -> newPassword = next
                                        Stage.RECOVERY_CONFIRM -> newConfirm = next
                                        else -> password = next
                                    }
                                    writtenChars = next.length
                                    errorMessage = null
                                }
                            },
                            onSubmit = ::submit,
                            onToggleVisibility = { showPassword = !showPassword },
                            onLink = {
                                passwordOnly = false
                                errorMessage = null
                            },
                            onForgot = {
                                stage = Stage.RECOVERY_ANSWER
                                writtenChars = 0
                                errorMessage = null
                            }
                        )
                    }
                    LetterSheet(
                        title = bigTitle,
                        subtitle = subtitle,
                        errorMessage = errorMessage,
                        compact = tallStage
                    ) {
                        when (stage) {
                            Stage.CHOOSE -> ProtectionChoice(
                                yesLabel = strings.protectionYes,
                                noLabel = strings.protectionNo,
                                enabled = interactive,
                                onYes = {
                                    stage = Stage.SECRET
                                    writtenChars = 0
                                    errorMessage = null
                                },
                                onNo = onSkipProtection
                            )
                            Stage.BIOMETRY -> BiometryOffer(
                                enableLabel = strings.biometryEnable,
                                notNowLabel = strings.biometryNotNow,
                                enabled = interactive,
                                onEnable = {
                                    attemptBiometry {
                                        onBiometryChange(true)
                                        sealAndEnter()
                                    }
                                },
                                onNotNow = {
                                    onBiometryChange(false)
                                    sealAndEnter()
                                }
                            )
                            Stage.SECRET -> if (useBiometry) {
                                BiometryChoice(
                                    unlockLabel = strings.biometryUnlock,
                                    passwordLabel = strings.usePassword,
                                    enabled = interactive,
                                    onUnlock = { attemptBiometry { sealAndEnter() } },
                                    onPassword = {
                                        passwordOnly = true
                                        errorMessage = null
                                    }
                                )
                            } else {
                                secretEntry()
                            }
                            Stage.RECOVERY_SETUP -> RecoverySetupForm(
                                strings = strings,
                                presetQuestions = presetQuestions,
                                useQuestion = useQuestionMethod,
                                questionIndex = questionIndex,
                                customQuestion = customQuestion,
                                answer = setupAnswer,
                                enabled = interactive,
                                ready = recoveryReady,
                                onUseQuestion = { useQuestionMethod = it },
                                onQuestionIndex = { questionIndex = it },
                                onCustomQuestion = {
                                    customQuestion = it
                                    errorMessage = null
                                },
                                onAnswer = {
                                    setupAnswer = it
                                    errorMessage = null
                                },
                                onSubmit = ::submit,
                                onBack = {
                                    stage = Stage.CONFIRM
                                    errorMessage = null
                                }
                            )
                            Stage.RECOVERY_ANSWER -> RecoveryAnswerForm(
                                question = recoveryQuestion.orEmpty(),
                                answer = recoveryAnswer,
                                enabled = interactive,
                                focusRequester = focusRequester,
                                backLabel = strings.usePassword,
                                onAnswer = {
                                    recoveryAnswer = it
                                    errorMessage = null
                                },
                                onSubmit = ::submit,
                                onBack = {
                                    stage = Stage.SECRET
                                    recoveryAnswer = ""
                                    recoveryVerified = false
                                    errorMessage = null
                                }
                            )
                            else -> {
                                val showBackLink = stage == Stage.RECOVERY_NEW || stage == Stage.RECOVERY_CONFIRM
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    SecretEntry(
                                        action = action,
                                        value = current,
                                        writtenChars = writtenChars,
                                        interactive = interactive,
                                        showPassword = showPassword,
                                        biometryLink = if (mode == LockMode.UNLOCK && stage == Stage.SECRET &&
                                            biometrySupported && biometryEnabled
                                        ) strings.biometryUnlock else null,
                                        forgotLink = if (mode == LockMode.UNLOCK && stage == Stage.SECRET &&
                                            recoveryQuestion != null
                                        ) strings.forgotPassword else null,
                                        focusRequester = focusRequester,
                                        inkBlot = inkBlot.value,
                                        onValueChange = { next ->
                                            if (next.length <= 40) {
                                                when (stage) {
                                                    Stage.CONFIRM -> confirmation = next
                                                    Stage.RECOVERY_NEW -> newPassword = next
                                                    Stage.RECOVERY_CONFIRM -> newConfirm = next
                                                    else -> password = next
                                                }
                                                writtenChars = next.length
                                                errorMessage = null
                                            }
                                        },
                                        onSubmit = ::submit,
                                        onToggleVisibility = { showPassword = !showPassword },
                                        onLink = {
                                            passwordOnly = false
                                            errorMessage = null
                                        },
                                        onForgot = {
                                            stage = Stage.RECOVERY_ANSWER
                                            writtenChars = 0
                                            errorMessage = null
                                        }
                                    )
                                    if (showBackLink) {
                                        Spacer(Modifier.height(DiaryDim.space1))
                                        QuietLink(
                                            label = strings.back,
                                            onClick = {
                                                if (stage == Stage.RECOVERY_NEW) {
                                                    stage = Stage.RECOVERY_ANSWER
                                                } else {
                                                    stage = Stage.RECOVERY_NEW
                                                    newConfirm = ""
                                                }
                                                errorMessage = null
                                            },
                                            enabled = interactive
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (flash.value > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(flash.value)
                        .background(colors.paperLight)
                )
            }
        }
    }
}

/** The paper sheet inside the envelope: headline, subtitle, body and error line. */
@Composable
private fun LetterSheet(
    title: String,
    subtitle: String,
    errorMessage: String?,
    compact: Boolean,
    content: @Composable () -> Unit
) {
    val colors = LocalDiaryColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(DiaryDim.radiusSheet))
            .background(
                Brush.verticalGradient(
                    0f to colors.paperLight,
                    0.75f to colors.paper,
                    1f to colors.paperDeep
                )
            )
            .paperGrain(seed = 11, count = 180)
            .padding(horizontal = DiaryDim.screenPad, vertical = DiaryDim.screenPad),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(DiaryDim.buttonHeight)
                .height(DiaryDim.dividerHeight)
                .background(colors.gold.copy(alpha = 0.6f))
        )
        Spacer(Modifier.height(if (compact) DiaryDim.space3 else DiaryDim.space4))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontStyle = FontStyle.Italic),
            color = colors.ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(DiaryDim.space2))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium.copy(lineHeight = 17.sp),
            color = colors.inkFaded,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(if (compact) DiaryDim.space3 else DiaryDim.space4))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }

        val errorAlpha by animateFloatAsState(
            targetValue = if (errorMessage != null) 1f else 0f,
            animationSpec = tween(320),
            label = "errorAlpha"
        )
        Box(modifier = Modifier.heightIn(min = DiaryDim.space8), contentAlignment = Alignment.Center) {
            Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
                color = colors.wax,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(errorAlpha)
            )
        }
    }
}

/** Hidden secret field: single BasicTextField with password visual transformation. */
@Composable
private fun SecretEntry(
    action: String,
    value: String,
    writtenChars: Int,
    interactive: Boolean,
    showPassword: Boolean,
    biometryLink: String?,
    forgotLink: String?,
    focusRequester: FocusRequester,
    inkBlot: Float,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleVisibility: () -> Unit,
    onLink: () -> Unit,
    onForgot: () -> Unit
) {
    val colors = LocalDiaryColors.current
    val strings = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = interactive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DiaryDim.fieldHeight)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = colors.ink,
                    fontFamily = SansField.fontFamily,
                    fontWeight = SansField.fontWeight,
                    fontSize = SansField.fontSize,
                    lineHeight = SansField.lineHeight
                ),
                cursorBrush = SolidColor(colors.gold),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        innerTextField()
                    }
                }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(DiaryDim.touchTarget)
                    .padding(DiaryDim.space1)
                    .clip(RoundedCornerShape(DiaryDim.radiusPaper))
                    .clickable(
                        enabled = interactive,
                        onClick = onToggleVisibility
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (showPassword) strings.hidePassword else strings.showPassword,
                    tint = colors.inkFaded,
                    modifier = Modifier.size(DiaryDim.iconMedium)
                )
            }
            if (inkBlot > 0f && inkBlot < 1f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(DiaryDim.iconLarge)
                        .drawBehind {
                            drawCircle(
                                color = colors.inkBlue.copy(alpha = 0.30f * (1f - inkBlot)),
                                radius = size.minDimension * 0.18f * (0.4f + inkBlot),
                                center = Offset(size.width * 0.34f, size.height * 0.62f)
                            )
                            drawCircle(
                                color = colors.inkBlue.copy(alpha = 0.18f * (1f - inkBlot)),
                                radius = size.minDimension * 0.09f * (0.6f + inkBlot * 1.4f),
                                center = Offset(size.width * 0.62f, size.height * 0.78f)
                            )
                        }
                )
            }
        }
        InkRule(modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(DiaryDim.space2))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DiaryDim.buttonHeight + DiaryDim.space1),
            contentAlignment = Alignment.Center
        ) {
            val showButton = value.isNotEmpty() && interactive
            val buttonAlpha by animateFloatAsState(
                targetValue = if (showButton) 1f else 0f,
                animationSpec = tween(220),
                label = "buttonAlpha"
            )
            SealedButton(
                label = action,
                onClick = onSubmit,
                modifier = Modifier.graphicsLayer { alpha = buttonAlpha }
            )
        }
        if (biometryLink != null || forgotLink != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = DiaryDim.space8),
                contentAlignment = Alignment.Center
            ) {
                val showLinks = value.isEmpty() && interactive
                val linksAlpha by animateFloatAsState(
                    targetValue = if (showLinks) 1f else 0f,
                    animationSpec = tween(220),
                    label = "linksAlpha"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { alpha = linksAlpha }
                ) {
                    if (biometryLink != null) {
                        QuietLink(label = biometryLink, onClick = onLink)
                    }
                    if (forgotLink != null) {
                        QuietLink(label = forgotLink, onClick = onForgot)
                    }
                }
            }
        }
    }
}

/** First-run recovery setup: the personal question and its answer. */
@Composable
private fun RecoverySetupForm(
    strings: AppStrings,
    presetQuestions: List<String>,
    useQuestion: Boolean,
    questionIndex: Int,
    customQuestion: String,
    answer: String,
    enabled: Boolean,
    ready: Boolean,
    onUseQuestion: (Boolean) -> Unit,
    onQuestionIndex: (Int) -> Unit,
    onCustomQuestion: (String) -> Unit,
    onAnswer: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MethodRow(
            label = strings.recoveryMethodQuestion,
            checked = useQuestion,
            enabled = enabled,
            onChecked = onUseQuestion
        )
        AnimatedVisibility(
            visible = useQuestion,
            enter = fadeIn(tween(240)) + expandVertically(tween(280)),
            exit = fadeOut(tween(180)) + shrinkVertically(tween(220))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(DiaryDim.space2))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(DiaryDim.space2)
                ) {
                    presetQuestions.forEachIndexed { index, question ->
                        QuestionChip(
                            label = question,
                            selected = questionIndex == index,
                            enabled = enabled,
                            onClick = { onQuestionIndex(index) }
                        )
                    }
                    QuestionChip(
                        label = strings.customQuestionLabel,
                        selected = questionIndex == PRESET_QUESTION_COUNT,
                        enabled = enabled,
                        onClick = { onQuestionIndex(PRESET_QUESTION_COUNT) }
                    )
                }
                if (questionIndex == PRESET_QUESTION_COUNT) {
                    Spacer(Modifier.height(DiaryDim.space2))
                    PaperField(
                        value = customQuestion,
                        onValueChange = onCustomQuestion,
                        placeholder = strings.customQuestionLabel,
                        enabled = enabled
                    )
                }
                Spacer(Modifier.height(DiaryDim.space2))
                PaperField(
                    value = answer,
                    onValueChange = onAnswer,
                    placeholder = strings.answerLabel,
                    enabled = enabled
                )
            }
        }

        Spacer(Modifier.height(DiaryDim.space3))
        SealedButton(
            label = strings.recoverySetupAction,
            onClick = onSubmit,
            enabled = enabled && ready
        )
        Spacer(Modifier.height(DiaryDim.space1))
        QuietLink(label = strings.usePassword, onClick = onBack, enabled = enabled)
    }
}

/** The «¿Olvidaste tu contraseña?» letter: the configured question and its answer. */
@Composable
private fun RecoveryAnswerForm(
    question: String,
    answer: String,
    enabled: Boolean,
    focusRequester: FocusRequester,
    backLabel: String,
    onAnswer: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalDiaryColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (question.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(DiaryDim.radiusPaper),
                color = colors.paper.copy(alpha = 0.7f),
                border = androidx.compose.foundation.BorderStroke(DiaryDim.dividerHeight, colors.edge.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = DiaryDim.space4, vertical = DiaryDim.space4)
                )
            }
        }
        Spacer(Modifier.height(DiaryDim.space3))
        PaperField(
            value = answer,
            onValueChange = onAnswer,
            placeholder = LocalStrings.current.answerLabel,
            enabled = enabled,
            focusRequester = focusRequester,
            onImeAction = onSubmit
        )
        Spacer(Modifier.height(DiaryDim.space3))
        SealedButton(
            label = LocalStrings.current.recoveryVerify,
            onClick = onSubmit,
            enabled = enabled && answer.isNotBlank()
        )
        Spacer(Modifier.height(DiaryDim.space1))
        QuietLink(label = backLabel, onClick = onBack, enabled = enabled)
    }
}

/** Paper toggle used to pick the recovery method. */
@Composable
private fun MethodRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onChecked: (Boolean) -> Unit
) {
    val colors = LocalDiaryColors.current
    Surface(
        onClick = { onChecked(!checked) },
        enabled = enabled,
        shape = RoundedCornerShape(DiaryDim.radiusPaper),
        color = if (checked) colors.paperLight else colors.paperLight.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            DiaryDim.dividerHeight,
            if (checked) colors.gold.copy(alpha = 0.6f) else colors.edge
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DiaryDim.space3, vertical = DiaryDim.space3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(DiaryDim.iconMedium)
                    .drawBehind {
                        drawCircle(
                            color = if (checked) colors.gold else colors.edge,
                            radius = size.minDimension / 2f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = DiaryDim.space1.toPx())
                        )
                        if (checked) {
                            drawCircle(
                                color = colors.gold,
                                radius = size.minDimension * 0.22f
                            )
                        }
                    }
            )
            Spacer(Modifier.width(DiaryDim.space3))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** A short paper chip for the preset questions. */
@Composable
private fun QuestionChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalDiaryColors.current
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(DiaryDim.radiusPaper),
        color = if (selected) colors.ink else colors.paperLight.copy(alpha = 0.75f),
        border = androidx.compose.foundation.BorderStroke(
            DiaryDim.dividerHeight,
            if (selected) colors.gold.copy(alpha = 0.6f) else colors.edge
        ),
        modifier = Modifier.semantics {
            stateDescription = if (selected) "selected" else "not selected"
            role = Role.Tab
        }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.paperLight else colors.inkSoft,
            modifier = Modifier
                .width(DiaryDim.questionChipWidth)
                .padding(horizontal = DiaryDim.space2, vertical = DiaryDim.space2)
        )
    }
}

/** Single-line paper field for visible recovery text. */
@Composable
private fun PaperField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    focusRequester: FocusRequester? = null,
    onImeAction: (() -> Unit)? = null
) {
    val colors = LocalDiaryColors.current
    Surface(
        shape = RoundedCornerShape(DiaryDim.radiusInput),
        color = colors.paperLight.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(DiaryDim.dividerHeight, colors.edge),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DiaryDim.fieldHeight)
                .padding(horizontal = DiaryDim.fieldHorizontalPad),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = SansField,
                    color = colors.inkWhisper
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                textStyle = SansField.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.gold),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onImeAction?.invoke() })
            )
        }
    }
}

/** First-run question: enable the native biometric gate, or continue without it. */
@Composable
private fun BiometryOffer(
    enableLabel: String,
    notNowLabel: String,
    enabled: Boolean,
    onEnable: () -> Unit,
    onNotNow: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SealedButton(
            label = enableLabel,
            onClick = onEnable,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(DiaryDim.space1))
        QuietLink(label = notNowLabel, onClick = onNotNow, enabled = enabled)
    }
}

/** Locked diary with biometry on: open natively, or fall back to the password. */
@Composable
private fun BiometryChoice(
    unlockLabel: String,
    passwordLabel: String,
    enabled: Boolean,
    onUnlock: () -> Unit,
    onPassword: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SealedButton(
            label = unlockLabel,
            onClick = onUnlock,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(DiaryDim.space1))
        QuietLink(label = passwordLabel, onClick = onPassword, enabled = enabled)
    }
}

/** First-run question: protect the diary with a password, or keep it open. */
@Composable
private fun ProtectionChoice(
    yesLabel: String,
    noLabel: String,
    enabled: Boolean,
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SealedButton(
            label = yesLabel,
            onClick = onYes,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(DiaryDim.space1))
        QuietLink(label = noLabel, onClick = onNo, enabled = enabled)
    }
}

/** Quiet letterspaced text action, like a pencil note under the wax seal. */
@Composable
private fun QuietLink(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    val colors = LocalDiaryColors.current
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkFaded,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(if (enabled) 1f else 0.4f)
                .padding(horizontal = DiaryDim.space4, vertical = DiaryDim.space3)
        )
    }
}
