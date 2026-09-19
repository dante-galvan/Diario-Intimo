package com.rork.diariointimo.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rork.diariointimo.ui.screens.ActivatePasswordScreen
import com.rork.diariointimo.ui.screens.CalendarScreen
import com.rork.diariointimo.ui.screens.ChangePasswordScreen
import com.rork.diariointimo.ui.screens.EditorScreen
import com.rork.diariointimo.ui.screens.HomeScreen
import com.rork.diariointimo.ui.screens.LanguageScreen
import com.rork.diariointimo.ui.screens.RecoveryScreen
import com.rork.diariointimo.ui.screens.SettingsScreen
import com.rork.diariointimo.ui.ads.InterstitialAdManager
import com.rork.diariointimo.ui.vm.DiaryViewModel
import com.rork.diariointimo.ui.vm.SessionViewModel
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor/{entryId}"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val LANGUAGE = "language"
    const val PASSWORD = "password"
    const val ACTIVATE = "activate"
    const val RECOVERY = "recovery"

    fun editor(entryId: String): String = "editor/$entryId"
}

/** The unlocked diary. The lock screen lives above this graph, in the root layout. */
@Composable
fun AppNavigation(
    session: SessionViewModel,
    diary: DiaryViewModel,
    navController: NavHostController = rememberNavController()
) {
    val diaryState by diary.uiState.collectAsStateWithLifecycle()
    val sessionState by session.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) { diary.loadIfNeeded() }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            fadeIn(tween(340)) + slideInHorizontally(tween(380)) { it / 8 }
        },
        exitTransition = {
            fadeOut(tween(220)) + slideOutHorizontally(tween(380)) { -it / 12 }
        },
        popEnterTransition = {
            fadeIn(tween(320)) + slideInHorizontally(tween(360)) { -it / 12 }
        },
        popExitTransition = {
            fadeOut(tween(240)) + slideOutHorizontally(tween(360)) { it / 8 }
        }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                state = diaryState,
                hasPassword = sessionState.hasPassword,
                onQueryChange = diary::setQuery,
                onFilterChange = diary::setFilter,
                onOpenEntry = { id -> navController.navigate(Routes.editor(id)) },
                onNewEntry = {
                    val entry = diary.createEntry()
                    navController.navigate(Routes.editor(entry.id))
                },
                onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onLock = {
                    session.lock()
                    diary.forget()
                }
            )
        }

        composable(
            route = Routes.EDITOR,
            enterTransition = { fadeIn(tween(360)) + scaleIn(tween(420), initialScale = 0.96f) },
            popExitTransition = { fadeOut(tween(260)) + scaleOut(tween(320), targetScale = 0.97f) }
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId")
            val entry = diaryState.entries.firstOrNull { it.id == entryId }
            val activity = context as? android.app.Activity
            var leaving by rememberSaveable { mutableStateOf(false) }
            if (entry == null) {
                LaunchedEffect(Unit) {
                    if (!leaving) navController.popBackStack()
                }
            } else {
                fun leave() {
                    if (leaving) return
                    leaving = true
                    navController.popBackStack()
                }
                EditorScreen(
                    entry = entry,
                    onChange = { title, body ->
                        diary.updateEntry(entry.id) { it.copy(title = title, body = body) }
                    },
                    onSignatureChange = { strokes -> diary.setSignature(entry.id, strokes) },
                    onToggleFavorite = { diary.toggleFavorite(entry.id) },
                    onTogglePrivate = { diary.togglePrivate(entry.id) },
                    onDelete = {
                        diary.deleteEntry(entry.id)
                        leave()
                    },
                    onBack = {
                        diary.discardIfBlank(entry.id)
                        InterstitialAdManager.incrementAction()
                        if (activity != null) {
                            InterstitialAdManager.maybeShow(activity) { leave() }
                        } else {
                            leave()
                        }
                    }
                )
            }
        }

        composable(Routes.CALENDAR) {
            CalendarScreen(
                state = diaryState,
                onOpenEntry = { id -> navController.navigate(Routes.editor(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                language = sessionState.language,
                appearance = sessionState.appearance,
                autoLock = sessionState.autoLock,
                hasPassword = sessionState.hasPassword,
                biometryEnabled = sessionState.biometryEnabled,
                biometryAvailable = sessionState.biometryAvailable,
                recoveryQuestion = sessionState.recoveryQuestion,
                onOpenLanguage = { navController.navigate(Routes.LANGUAGE) },
                onOpenPassword = { navController.navigate(Routes.PASSWORD) },
                onOpenRecovery = { navController.navigate(Routes.RECOVERY) },
                onOpenActivate = { navController.navigate(Routes.ACTIVATE) },
                onAppearanceChange = session::setAppearance,
                onAutoLockChange = session::setAutoLock,
                onBiometryChange = session::setBiometryEnabled,
                onDisablePassword = { scope.launch { session.disablePassword() } },
                onLock = {
                    session.lock()
                    diary.forget()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.LANGUAGE) {
            LanguageScreen(
                selected = sessionState.language,
                onSelect = session::setLanguage,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PASSWORD) {
            ChangePasswordScreen(
                onChange = { current, next -> session.changePassword(current, next) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ACTIVATE) {
            ActivatePasswordScreen(
                biometryAvailable = sessionState.biometryAvailable,
                biometryEnabled = sessionState.biometryEnabled,
                onActivate = { password -> session.createPassword(password) },
                onBiometryChange = session::setBiometryEnabled,
                onOpenRecovery = { navController.navigate(Routes.RECOVERY) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.RECOVERY) {
            RecoveryScreen(
                question = sessionState.recoveryQuestion,
                onSave = session::saveRecovery,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
