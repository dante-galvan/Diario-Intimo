package com.rork.diariointimo.data

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Outcome of a recovery-answer attempt. */
sealed interface RecoveryCheck {
    data object Correct : RecoveryCheck
    data object Wrong : RecoveryCheck
}

/**
 * Password recovery methods, prepared from the very first run. The security
 * answer is stored exactly like the password itself: a salted PBKDF2 digest
 * encrypted with the keystore key — never as plain text.
 * New methods (email, account, authorized device) can be added to this vault
 * later without touching the password mechanism.
 */
class RecoveryVault(context: Context) {
    private val prefs = context.getSharedPreferences("diary_recovery", Context.MODE_PRIVATE)

    fun hasQuestion(): Boolean = prefs.contains(KEY_QUESTION) && prefs.contains(KEY_ANSWER_DIGEST)

    /** The configured question text, or null. It is stored encrypted for privacy. */
    fun questionText(): String? =
        prefs.getString(KEY_QUESTION, null)?.decode()?.decodeToString()?.takeIf { it.isNotBlank() }

    fun setQuestion(question: String, answer: String) {
        val normalized = answer.normalized()
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_QUESTION, SecretVault.encrypt(question.trim().encodeToByteArray()).encode())
            .putString(KEY_ANSWER_SALT, salt.encode())
            .putString(KEY_ANSWER_DIGEST, SecretVault.encrypt(derive(normalized, salt)).encode())
            .putInt(KEY_ANSWER_LENGTH, normalized.length)
            .apply()
    }

    /** Removes the question when the user turns that method off. */
    fun clearQuestion() {
        prefs.edit()
            .remove(KEY_QUESTION)
            .remove(KEY_ANSWER_SALT)
            .remove(KEY_ANSWER_DIGEST)
            .remove(KEY_ANSWER_LENGTH)
            .apply()
    }

    /**
     * Verifies the answer for real against the stored digest. A wrong answer
     * simply fails, so the rightful owner can retry immediately.
     */
    suspend fun verifyAnswer(answer: String): RecoveryCheck = withContext(Dispatchers.Default) {
        if (!hasQuestion()) return@withContext RecoveryCheck.Wrong

        val salt = prefs.getString(KEY_ANSWER_SALT, null)?.decode() ?: return@withContext RecoveryCheck.Wrong
        val stored = prefs.getString(KEY_ANSWER_DIGEST, null)?.decode()?.let { SecretVault.decrypt(it) }
            ?: return@withContext RecoveryCheck.Wrong

        if (stored.contentEquals(derive(answer.normalized(), salt))) {
            RecoveryCheck.Correct
        } else {
            RecoveryCheck.Wrong
        }
    }

    private fun derive(secret: String, salt: ByteArray): ByteArray {
        val algorithm = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return algorithm.generateSecret(spec).encoded
    }

    /** Loose matching: case and extra spaces never block the rightful owner. */
    private fun String.normalized(): String = trim().replace(WHITESPACE, " ").lowercase()

    private fun ByteArray.encode(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.decode(): ByteArray? = runCatching { Base64.decode(this, Base64.NO_WRAP) }.getOrNull()

    private companion object {
        const val KEY_QUESTION = "question"
        const val KEY_ANSWER_SALT = "answer_salt"
        const val KEY_ANSWER_DIGEST = "answer_digest"
        const val KEY_ANSWER_LENGTH = "answer_length"
        const val SALT_SIZE = 16
        const val ITERATIONS = 60_000
        const val KEY_BITS = 256
        val WHITESPACE = Regex("\\s+")
    }
}
