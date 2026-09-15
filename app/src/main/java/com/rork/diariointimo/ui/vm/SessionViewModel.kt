package com.rork.diariointimo.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.rork.diariointimo.data.AppearanceMode
import com.rork.diariointimo.data.AutoLockDelay
import com.rork.diariointimo.data.BiometricGate
import com.rork.diariointimo.data.PasswordVault
import com.rork.diariointimo.data.RecoveryCheck
import com.rork.diariointimo.data.RecoveryVault
import com.rork.diariointimo.data.SettingsStore
import com.rork.diariointimo.i18n.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class SessionUiState(
    val hasPassword: Boolean = false,
    val isUnlocked: Boolean = false,
    val language: AppLanguage = AppLanguage.EN,
    val appearance: AppearanceMode = AppearanceMode.LIGHT,
    val autoLock: AutoLockDelay = AutoLockDelay.ONE,
    val biometryEnabled: Boolean = false,
    val biometryAvailable: Boolean = false,
    val failedAttempts: Int = 0,
    val recoveryQuestion: String? = null,
    /** null while undecided (first run), "password" or "none" once the user chose. */
    val protectionChoice: String? = null
)

/** Owns the lock state of the diary, the password, recovery and global preferences. */
class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val passwords = PasswordVault(application)
    private val recovery = RecoveryVault(application)
    private val settings = SettingsStore(application)

    private val _uiState = MutableStateFlow(
        SessionUiState(
            hasPassword = passwords.hasPassword(),
            language = settings.language,
            appearance = settings.appearance,
            autoLock = settings.autoLock,
            biometryAvailable = BiometricGate.isSupported(application),
            biometryEnabled = settings.biometryEnabled && BiometricGate.isSupported(application),
            failedAttempts = passwords.failures(),
            recoveryQuestion = recovery.questionText(),
            protectionChoice = settings.protectionChoice
        )
    )
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    /** Wall-clock instant of the last user interaction, used by the auto-lock. */
    var lastInteractionAt: Long = System.currentTimeMillis()
        private set

    fun registerInteraction() {
        lastInteractionAt = System.currentTimeMillis()
    }

    /** Checks the secret and keeps the failed-attempt counter for the lock screen. */
    suspend fun tryUnlock(password: String): Boolean {
        val accepted = withContext(Dispatchers.Default) { passwords.verify(password) }
        if (accepted) {
            passwords.resetFailures()
            _uiState.update { it.copy(failedAttempts = 0) }
        } else {
            val failures = passwords.recordFailure()
            _uiState.update { it.copy(failedAttempts = failures) }
        }
        return accepted
    }

    suspend fun createPassword(password: String) {
        withContext(Dispatchers.Default) {
            passwords.setPassword(password)
            passwords.resetFailures()
        }
        settings.protectionChoice = CHOICE_PASSWORD
        _uiState.update {
            it.copy(hasPassword = true, failedAttempts = 0, protectionChoice = CHOICE_PASSWORD)
        }
    }

    /** The user declined protection on the first run: the diary stays open. */
    fun chooseNoPassword() {
        settings.protectionChoice = CHOICE_NONE
        _uiState.update { it.copy(protectionChoice = CHOICE_NONE) }
        revealDiary()
    }

    /**
     * Switches protection off for good: password, failures, recovery and
     * biometry are wiped; the letters are never touched.
     */
    suspend fun disablePassword() {
        withContext(Dispatchers.Default) {
            passwords.clear()
            recovery.clearQuestion()
        }
        settings.biometryEnabled = false
        settings.protectionChoice = CHOICE_NONE
        _uiState.update {
            it.copy(
                hasPassword = false,
                biometryEnabled = false,
                failedAttempts = 0,
                recoveryQuestion = null,
                protectionChoice = CHOICE_NONE
            )
        }
    }

    /** Called when the opening animation finishes. */
    fun revealDiary() {
        registerInteraction()
        _uiState.update {
            it.copy(
                hasPassword = passwords.hasPassword(),
                protectionChoice = settings.protectionChoice,
                isUnlocked = true
            )
        }
    }

    suspend fun changePassword(current: String, next: String): Boolean {
        val valid = withContext(Dispatchers.Default) { passwords.verify(current) }
        if (valid) withContext(Dispatchers.Default) { passwords.setPassword(next) }
        return valid
    }

    /** Persists the personal question used to recover access. */
    fun saveRecovery(question: String?, answer: String?) {
        if (question.isNullOrBlank() || answer.isNullOrBlank()) {
            recovery.clearQuestion()
        } else {
            recovery.setQuestion(question, answer)
        }
        _uiState.update { it.copy(recoveryQuestion = recovery.questionText()) }
    }

    /** Verifies the recovery answer for real; wrong answers trigger growing waits. */
    suspend fun verifyRecoveryAnswer(answer: String): RecoveryCheck =
        recovery.verifyAnswer(answer)

    /** Only called after the recovery answer was verified in the same session. */
    suspend fun resetPassword(newPassword: String) {
        withContext(Dispatchers.Default) {
            passwords.setPassword(newPassword)
            passwords.resetFailures()
        }
        _uiState.update { it.copy(hasPassword = true, failedAttempts = 0) }
    }

    fun lock() {
        _uiState.update { it.copy(isUnlocked = false) }
    }

    /** Locks the diary when the configured inactivity window has elapsed. */
    fun lockIfExpired(now: Long = System.currentTimeMillis()) {
        // A diary without protection is never locked.
        if (!_uiState.value.hasPassword) return
        val delay = _uiState.value.autoLock
        if (delay == AutoLockDelay.NEVER) return
        if (now - lastInteractionAt >= delay.millis) lock()
    }

    fun setLanguage(language: AppLanguage) {
        settings.language = language
        _uiState.update { it.copy(language = language) }
    }

    fun setAppearance(mode: AppearanceMode) {
        settings.appearance = mode
        _uiState.update { it.copy(appearance = mode) }
    }

    fun setAutoLock(delay: AutoLockDelay) {
        settings.autoLock = delay
        _uiState.update { it.copy(autoLock = delay) }
    }

    fun setBiometryEnabled(enabled: Boolean) {
        settings.biometryEnabled = enabled
        _uiState.update { it.copy(biometryEnabled = enabled) }
    }

    private companion object {
        const val CHOICE_PASSWORD = "password"
        const val CHOICE_NONE = "none"
    }
}
