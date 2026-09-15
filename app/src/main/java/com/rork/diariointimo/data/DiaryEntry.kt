package com.rork.diariointimo.data

import kotlinx.serialization.Serializable

/**
 * One finger-drawn signature stroke. Points are flat `x, y` pairs normalized
 * to the 0..1 range of the signature canvas, so the signature scales with the
 * sheet and survives orientation changes.
 */
@Serializable
data class SignatureStroke(val points: List<Float> = emptyList())

/** A single page of the diary. Everything the user writes is kept exactly as typed. */
@Serializable
data class DiaryEntry(
    val id: String,
    val epochDay: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val title: String = "",
    val body: String = "",
    val isFavorite: Boolean = false,
    val isPrivate: Boolean = false,
    val signature: List<SignatureStroke> = emptyList()
) {
    val isBlank: Boolean get() = title.isBlank() && body.isBlank()

    fun excerpt(maxChars: Int = 120): String {
        val flat = body.replace('\n', ' ').trim()
        return if (flat.length <= maxChars) flat else flat.take(maxChars).trimEnd() + "…"
    }

    val wordCount: Int
        get() = body.split(Regex("\\s+")).count { it.isNotBlank() }
}

@Serializable
data class DiaryArchive(val entries: List<DiaryEntry> = emptyList())
