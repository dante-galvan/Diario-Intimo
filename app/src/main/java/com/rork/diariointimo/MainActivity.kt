package com.rork.diariointimo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rork.diariointimo.data.AppearanceMode
import com.rork.diariointimo.data.BiometricGate
import com.rork.diariointimo.i18n.LocalAppLocale
import com.rork.diariointimo.i18n.LocalStrings
import com.rork.diariointimo.i18n.localeFor
import com.rork.diariointimo.i18n.resolveLanguage
import com.rork.diariointimo.i18n.stringsFor
import com.rork.diariointimo.ui.components.PenIntro
import com.rork.diariointimo.ui.navigation.AppNavigation
import com.rork.diariointimo.ui.screens.LockMode
import com.rork.diariointimo.ui.screens.LockScreen
import com.rork.diariointimo.ui.theme.AppTheme
import com.rork.diariointimo.ui.vm.DiaryViewModel
import com.rork.diariointimo.ui.vm.SessionViewModel
import kotlinx.coroutines.delay
import java.util.Locale

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DiaryApp() }
    }
}

@Composable
private fun DiaryApp() {
    val session: SessionViewModel = viewModel()
    val diary: DiaryViewModel = viewModel()
    val sessionState by session.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val deviceLocale: Locale = remember(configuration) {
        val locales = configuration.locales
        if (locales.isEmpty) Locale.getDefault() else locales.get(0)
    }
    val language = resolveLanguage(sessionState.language, deviceLocale.toLanguageTag())

    // The diary's palette: warm daylight paper, «Noche de tinta», or the system.
    val dark = when (sessionState.appearance) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Auto-lock when the diary returns from the background after the configured delay.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, sessionState.autoLock) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> session.registerInteraction()
                Lifecycle.Event.ON_START -> session.lockIfExpired()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(sessionState.isUnlocked) {
        if (!sessionState.isUnlocked) diary.forget()
    }

    // A diary without protection never asks for a secret: it opens by itself.
    LaunchedEffect(sessionState.hasPassword, sessionState.protectionChoice, sessionState.isUnlocked) {
        if (!sessionState.hasPassword && sessionState.protectionChoice == PROTECTION_NONE && !sessionState.isUnlocked) {
            session.revealDiary()
        }
    }

    // Inactivity lock while the diary stays open.
    LaunchedEffect(sessionState.isUnlocked, sessionState.autoLock) {
        val window = sessionState.autoLock.millis
        if (!sessionState.isUnlocked || window <= 0L) return@LaunchedEffect
        while (true) {
            delay(15_000)
            session.lockIfExpired()
        }
    }

    AppTheme(dark = dark) {
        CompositionLocalProvider(
            LocalStrings provides stringsFor(language),
            LocalAppLocale provides localeFor(language, deviceLocale)
        ) {
            val strings = LocalStrings.current

            // Offer the native biometric prompt once per lock session; failures
            // and cancellations never make it reappear until the diary relocks.
            var biometryPrompted by remember { mutableStateOf(false) }
            LaunchedEffect(sessionState.isUnlocked, sessionState.biometryEnabled) {
                if (sessionState.isUnlocked) {
                    biometryPrompted = false
                    return@LaunchedEffect
                }
                if (biometryPrompted || !sessionState.hasPassword) return@LaunchedEffect
                if (!sessionState.biometryEnabled) return@LaunchedEffect
                biometryPrompted = true
                val activity = context as? FragmentActivity ?: return@LaunchedEffect
                delay(420)
                BiometricGate.authenticate(
                    activity = activity,
                    title = strings.biometryDialogTitle,
                    subtitle = strings.biometryDialogSubtitle,
                    negativeText = strings.usePassword
                ) { ok ->
                    if (ok) session.revealDiary()
                }
            }

            // The pen signs the diary open, once per session. Tap to skip.
            var showIntro by remember { mutableStateOf(true) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                session.registerInteraction()
                            }
                        }
                    }
            ) {
                if (showIntro) {
                    PenIntro(
                        title = strings.appName,
                        tagline = strings.tagline,
                        onFinished = { showIntro = false }
                    )
                } else {
                    // Without a password and once the user chose «no protection»,
                    // the diary counts as open without ever showing a lock.
                    val effectivelyUnlocked = sessionState.isUnlocked ||
                        (!sessionState.hasPassword && sessionState.protectionChoice == PROTECTION_NONE)
                    Crossfade(
                        targetState = effectivelyUnlocked,
                        animationSpec = tween(520),
                        label = "lock"
                    ) { unlocked ->
                        if (unlocked) {
                            AppNavigation(session = session, diary = diary)
                        } else {
                            LockScreen(
                                mode = if (sessionState.hasPassword) LockMode.UNLOCK else LockMode.CHOOSE,
                                biometrySupported = sessionState.biometryAvailable,
                                biometryEnabled = sessionState.biometryEnabled,
                                failedAttempts = sessionState.failedAttempts,
                                recoveryQuestion = sessionState.recoveryQuestion,
                                onSubmit = { secret ->
                                    if (sessionState.hasPassword) {
                                        session.tryUnlock(secret)
                                    } else {
                                        session.createPassword(secret)
                                        true
                                    }
                                },
                                onRecoveryAnswer = session::verifyRecoveryAnswer,
                                onSaveRecovery = session::saveRecovery,
                                onResetPassword = session::resetPassword,
                                onBiometryChange = session::setBiometryEnabled,
                                onSkipProtection = session::chooseNoPassword,
                                onOpened = { session.revealDiary() }
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val PROTECTION_NONE = "none"
