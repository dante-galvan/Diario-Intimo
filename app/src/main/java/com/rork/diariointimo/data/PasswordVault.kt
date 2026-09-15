package com.rork.diariointimo.data

import android.content.Context
import android.util.Base64
import android.util.Log
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password storage. The password is never persisted: only a salted PBKDF2 digest
 * is kept, and that digest is additionally encrypted with the keystore key.
 */
class PasswordVault(context: Context) {
    private val prefs = context.getSharedPreferences("diary_lock", Context.MODE_PRIVATE)

    fun hasPassword(): Boolean = prefs.contains(KEY_DIGEST) && prefs.contains(KEY_SALT)

    /** Removes every trace of the password when protection is switched off. */
    fun clear() {
        prefs.edit().clear().apply()
    }

    fun setPassword(password: String) {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val digest = derive(password, salt)
        prefs.edit()
            .putString(KEY_SALT, salt.encode())
            .putString(KEY_DIGEST, SecretVault.encrypt(digest).encode())
            .apply()
    }

    fun verify(password: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null)?.decode() ?: return false
        val stored = prefs.getString(KEY_DIGEST, null)?.decode()?.let { SecretVault.decrypt(it) }
            ?: return false
        return stored.contentEquals(derive(password, salt))
    }

    /** Counts failed unlock attempts, shown on the lock screen. */
    fun recordFailure(): Int {
        val failures = prefs.getInt(KEY_FAILURES, 0) + 1
        prefs.edit().putInt(KEY_FAILURES, failures).apply()
        return failures
    }

    fun failures(): Int = prefs.getInt(KEY_FAILURES, 0)

    fun resetFailures() {
        prefs.edit().putInt(KEY_FAILURES, 0).apply()
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val algorithm = runCatching { SecretKeyFactory.getInstance(STRONG_ALGORITHM) }
            .getOrElse {
                Log.i(TAG, "Falling back to legacy key derivation")
                SecretKeyFactory.getInstance(LEGACY_ALGORITHM)
            }
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return algorithm.generateSecret(spec).encoded
    }

    private fun ByteArray.encode(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.decode(): ByteArray? =
        runCatching { Base64.decode(this, Base64.NO_WRAP) }.getOrNull()

    private companion object {
        const val TAG = "PasswordVault"
        const val KEY_SALT = "salt"
        const val KEY_DIGEST = "digest"
        const val KEY_FAILURES = "failures"
        const val SALT_SIZE = 16
        const val ITERATIONS = 60_000
        const val KEY_BITS = 256
        const val STRONG_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val LEGACY_ALGORITHM = "PBKDF2WithHmacSHA1"
    }
}
