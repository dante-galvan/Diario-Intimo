package com.rork.diariointimo.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Local, encrypted, single-device storage for diary pages.
 * The architecture keeps a suspending surface so cloud sync or backups can be
 * layered on later without touching the UI.
 */
class DiaryRepository(context: Context) {
    private val file = File(context.filesDir, FILE_NAME)
    private val photoDir = File(context.filesDir, PHOTOS_DIR)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): List<DiaryEntry> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        val decrypted = SecretVault.decrypt(file.readBytes()) ?: return@withContext emptyList()
        runCatching {
            val archive = json.decodeFromString(
                DiaryArchive.serializer(),
                dropPhotoEntries(decrypted.decodeToString())
            )
            // The photo-letter feature is gone: free the private photo copies once.
            photoDir.deleteRecursively()
            archive.entries
        }.getOrElse { error ->
            Log.w(TAG, "Diary archive could not be read")
            emptyList()
        }
    }

    /**
     * One-time migration: pages saved as photo letters are removed entirely,
     * so the feature can be retired without ever decoding its old shape.
     */
    private fun dropPhotoEntries(raw: String): String {
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return raw
        val entries = root["entries"] as? JsonArray ?: return raw
        val kept = entries.filterNot { element ->
            runCatching {
                element.jsonObject["letterType"]?.jsonPrimitive?.content == "PHOTOS"
            }.getOrDefault(false)
        }
        return json.encodeToString(
            JsonObject.serializer(),
            JsonObject(mapOf("entries" to JsonArray(kept)))
        )
    }

    suspend fun save(entries: List<DiaryEntry>) = withContext(Dispatchers.IO) {
        runCatching {
            val payload = json.encodeToString(DiaryArchive.serializer(), DiaryArchive(entries))
            // Atomic swap: the diary is written aside and renamed, so a crash
            // mid-save can never truncate the real archive.
            val temp = File(file.parentFile, FILE_NAME + ".tmp")
            temp.writeBytes(SecretVault.encrypt(payload.encodeToByteArray()))
            if (!temp.renameTo(file)) {
                file.writeBytes(temp.readBytes())
                temp.delete()
            }
        }.onFailure { Log.w(TAG, "Diary archive could not be written") }
        Unit
    }

    private companion object {
        const val TAG = "DiaryRepository"
        const val FILE_NAME = "journal.dat"
        const val PHOTOS_DIR = "photos"
    }
}
