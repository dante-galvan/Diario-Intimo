package com.rork.diariointimo.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.rork.diariointimo.R
import com.rork.diariointimo.ui.theme.DiaryColors
import com.rork.diariointimo.data.SignatureStroke
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The two shapes a sheet can be exported in. */
enum class SheetFormat { PNG, PDF }

/**
 * Everything a sheet needs to be redrawn outside of the screen: the words,
 * the date, the signature and the palette in use.
 */
data class SheetSnapshot(
    val title: String,
    val body: String,
    val dateText: String,
    val signature: List<SignatureStroke>,
    val colors: DiaryColors,
    val footer: String
)

/**
 * Turns a diary sheet into a shareable file. The rendering mirrors the app's
 * own sheet — paper grain, ruled lines, serif title, handwriting, signature
 * and the paper's footer.
 */
class LetterSheetExporter(private val context: Context) {

    /** Renders the sheet and writes a PNG into the export cache. */
    suspend fun exportPng(snapshot: SheetSnapshot): File = withContext(Dispatchers.IO) {
        val bitmap = render(snapshot)
        val file = newFile("png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        file
    }

    /** Renders the sheet and writes it as a paginated A4 PDF. */
    suspend fun exportPdf(snapshot: SheetSnapshot): File = withContext(Dispatchers.IO) {
        val bitmap = render(snapshot)
        val document = PdfDocument()
        val margin = 40
        val contentWidth = PAGE_WIDTH - margin * 2
        val scale = contentWidth.toFloat() / bitmap.width
        val contentHeight = PAGE_HEIGHT - margin * 2
        val sliceSource = (contentHeight / scale).toInt().coerceAtLeast(1)

        var top = 0
        var pageNumber = 1
        while (top < bitmap.height) {
            val bottom = min(top + sliceSource, bitmap.height)
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            val canvas = page.canvas
            canvas.drawColor(bitmap.getPixel(4, 4))
            val destination = Rect(
                margin,
                margin,
                margin + contentWidth,
                margin + ((bottom - top) * scale).toInt()
            )
            canvas.drawBitmap(bitmap, Rect(0, top, bitmap.width, bottom), destination, null)
            document.finishPage(page)
            top = bottom
            pageNumber++
        }

        val file = newFile("pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        bitmap.recycle()
        file
    }

    /** Opens the native Android share sheet for the exported file. */
    fun shareFile(file: File, format: SheetFormat) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.export", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (format == SheetFormat.PNG) "image/png" else "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Saves the sheet where the user can actually find it: the system gallery
     * for images (Pictures/"Diario Intimo") and Downloads for PDFs. The save
     * counts as done only when the bytes really reached the system store.
     */
    suspend fun saveToGallery(file: File, format: SheetFormat): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val mime = if (format == SheetFormat.PNG) "image/png" else "application/pdf"
                    val collection = if (format == SheetFormat.PNG) {
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    }
                    val folder = if (format == SheetFormat.PNG) {
                        Environment.DIRECTORY_PICTURES
                    } else {
                        Environment.DIRECTORY_DOWNLOADS
                    }
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                        put(MediaStore.MediaColumns.MIME_TYPE, mime)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/Diario Intimo")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(collection, values) ?: return@runCatching false
                    val copied = resolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { it.copyTo(out) }
                    } ?: return@runCatching false
                    if (copied <= 0L) return@runCatching false
                    resolver.update(
                        uri,
                        ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                        null,
                        null
                    )
                    // The sheet is saved only when the system copy is complete.
                    val registeredSize = resolver.query(
                        uri,
                        arrayOf(MediaStore.MediaColumns.SIZE),
                        null,
                        null,
                        null
                    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else -1L } ?: -1L
                    registeredSize > 0L
                } else {
                    // Pre-Android-10: write straight into the public folder the
                    // storage permission grants access to.
                    val dir = File(
                        Environment.getExternalStoragePublicDirectory(
                            if (format == SheetFormat.PNG) {
                                Environment.DIRECTORY_PICTURES
                            } else {
                                Environment.DIRECTORY_DOCUMENTS
                            }
                        ),
                        "Diario Intimo"
                    )
                    dir.mkdirs()
                    val target = File(dir, file.name)
                    file.copyTo(target, overwrite = true)
                    target.exists() && target.length() == file.length() && target.length() > 0
                }
            }.getOrDefault(false)
        }

    // ---------------------------------------------------------------- render

    private fun render(snapshot: SheetSnapshot): Bitmap {
        val colors = snapshot.colors
        val width = SHEET_WIDTH
        val margin = 84f
        val contentWidth = (width - margin * 2).toInt()

        val serif = ResourcesCompat.getFont(context, R.font.cormorant_bold) ?: Typeface.SERIF
        val hand = ResourcesCompat.getFont(context, R.font.caveat_regular) ?: Typeface.SANS_SERIF
        val sans = ResourcesCompat.getFont(context, R.font.inter_regular) ?: Typeface.SANS_SERIF

        val datePaint = TextPaint().apply {
            typeface = sans
            textSize = 30f
            letterSpacing = 0.14f
            color = colors.inkFaded.toArgb()
            isAntiAlias = true
        }
        val titlePaint = TextPaint().apply {
            typeface = serif
            textSize = 78f
            color = colors.ink.toArgb()
            isAntiAlias = true
        }
        val bodyPaint = TextPaint().apply {
            typeface = hand
            textSize = 46f
            color = colors.inkBlue.toArgb()
            isAntiAlias = true
        }
        val footerPaint = Paint().apply {
            typeface = sans
            textSize = 26f
            letterSpacing = 0.2f
            color = colors.inkWhisper.toArgb()
            isAntiAlias = true
        }

        val dateLayout = staticLayout(snapshot.dateText.uppercase(Locale.getDefault()), datePaint, contentWidth)
        val titleLayout = staticLayout(snapshot.title, titlePaint, contentWidth)
        val bodyLayout = staticLayout(snapshot.body.ifBlank { " " }, bodyPaint, contentWidth)

        val signatureHeight = if (snapshot.signature.isNotEmpty()) SIGNATURE_HEIGHT + 40f else 0f

        var height = (margin * 2 + dateLayout.height + 66f + titleLayout.height +
            BODY_SPACING + bodyLayout.height + 70f + signatureHeight + 130f)
        height = height.coerceAtLeast(1500f)

        val bitmap = Bitmap.createBitmap(width, height.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(colors.paper.toArgb())

        // Paper grain, seeded so every export feels like the same paper.
        val grain = Random(21)
        val grainPaint = Paint().apply { color = colors.inkFaded.toArgb(); isAntiAlias = true }
        repeat(340) {
            grainPaint.alpha = ((0.02f + grain.nextFloat() * 0.07f) * 255).toInt()
            canvas.drawCircle(
                grain.nextFloat() * width,
                grain.nextFloat() * height,
                0.4f + grain.nextFloat() * 1.6f,
                grainPaint
            )
        }

        var y = margin
        canvas.save()
        canvas.translate(margin, y)
        dateLayout.draw(canvas)
        canvas.restore()
        y += dateLayout.height + 26f

        // The thin gold rule under the date.
        canvas.drawRect(
            margin,
            y,
            margin + contentWidth * 0.42f,
            y + 3f,
            Paint().apply { color = colors.gold.copy(alpha = 0.6f).toArgb() }
        )
        y += 44f

        canvas.save()
        canvas.translate(margin, y)
        titleLayout.draw(canvas)
        canvas.restore()
        y += titleLayout.height + BODY_SPACING

        // Ruled lines behind the handwriting.
        val linePaint = Paint().apply {
            color = colors.edge.copy(alpha = 0.5f).toArgb()
            strokeWidth = 2f
        }
        var lineY = y + 66f
        val bodyBottom = y + bodyLayout.height + 22f
        while (lineY < bodyBottom) {
            canvas.drawLine(margin, lineY, width - margin, lineY, linePaint)
            lineY += 78f
        }

        canvas.save()
        canvas.translate(margin, y)
        bodyLayout.draw(canvas)
        canvas.restore()
        y += bodyLayout.height + 70f

        // The finger-drawn signature, in ink.
        if (snapshot.signature.isNotEmpty()) {
            val boxHeight = SIGNATURE_HEIGHT
            val boxWidth = contentWidth * 0.72f
            val strokePaint = Paint().apply {
                color = colors.inkBlue.toArgb()
                style = Paint.Style.STROKE
                strokeWidth = 5f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }
            snapshot.signature.forEach { stroke ->
                val path = android.graphics.Path()
                var started = false
                stroke.points.chunked(2).filter { it.size == 2 }.forEach { pair ->
                    val px = margin + pair[0] * boxWidth
                    val py = y + pair[1] * boxHeight
                    if (!started) {
                        path.moveTo(px, py)
                        started = true
                    } else {
                        path.lineTo(px, py)
                    }
                }
                canvas.drawPath(path, strokePaint)
            }
            y += signatureHeight
        }

        canvas.drawText(snapshot.footer.uppercase(Locale.getDefault()), margin, height - margin * 0.5f, footerPaint)
        return bitmap
    }

    private fun staticLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.32f)
            .setIncludePad(false)
            .build()

    private fun newFile(extension: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        // Millisecond precision keeps every export in its own file.
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
        return File(dir, "carta-$stamp.$extension")
    }

    private companion object {
        const val SHEET_WIDTH = 1080
        const val PAGE_WIDTH = 1240
        const val PAGE_HEIGHT = 1754
        const val SIGNATURE_HEIGHT = 200f
        const val BODY_SPACING = 40f
    }
}
