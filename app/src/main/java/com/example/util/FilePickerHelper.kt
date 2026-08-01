package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.AttachedFile
import java.io.InputStream
import java.util.zip.ZipInputStream

object FilePickerHelper {

    private val TEXT_EXTENSIONS = setOf(
        "js", "jsx", "ts", "tsx", "py", "java", "kt", "kts", "c", "cpp", "h", "hpp", "cs",
        "html", "css", "json", "xml", "sh", "bash", "zsh", "sql", "md", "txt", "log",
        "env", "yml", "yaml", "properties", "gradle", "go", "rs", "php", "rb", "swift", "asm"
    )

    fun processUri(context: Context, uri: Uri): AttachedFile? {
        return try {
            val contentResolver = context.contentResolver
            var fileName = "unknown_file"
            var fileSize: Long = 0

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val ext = fileName.substringAfterLast('.', "").lowercase()
            val formattedSize = formatFileSize(fileSize)
            val isTextFile = TEXT_EXTENSIONS.contains(ext) || mimeType.startsWith("text/") || mimeType.contains("json") || mimeType.contains("javascript") || mimeType.contains("xml")

            val payload: String = if (isTextFile && fileSize < 5 * 1024 * 1024) { // Up to 5MB direct text read
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                } ?: "[Empty or unreadable text file]"
            } else if (ext == "apk" || ext == "zip" || ext == "jar") {
                // Archive file structure inspection
                val entries = mutableListOf<String>()
                try {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        ZipInputStream(inputStream).use { zipStream ->
                            var entry = zipStream.nextEntry
                            var count = 0
                            while (entry != null && count < 50) {
                                entries.add(" - ${entry.name} (${formatFileSize(entry.size)})")
                                count++
                                entry = zipStream.nextEntry
                            }
                            if (entry != null) entries.add(" - ... [Additional archive entries omitted]")
                        }
                    }
                } catch (e: Exception) {
                    entries.add(" [Structure parsing note: Encrypted or specialized archive compression]")
                }

                buildString {
                    appendLine("ARCHIVE METADATA INSPECTION")
                    appendLine("File Name: $fileName")
                    appendLine("File Size: $formattedSize ($fileSize bytes)")
                    appendLine("MIME Type: $mimeType")
                    appendLine("Package Format: ${ext.uppercase()}")
                    if (entries.isNotEmpty()) {
                        appendLine("\nARCHIVE CONTENT LISTING (Top 50 entries):")
                        entries.forEach { appendLine(it) }
                    }
                }
            } else {
                // Binary or large file metadata summary
                buildString {
                    appendLine("BINARY FILE ATTACHMENT DETAILS")
                    appendLine("File Name: $fileName")
                    appendLine("File Size: $formattedSize ($fileSize bytes)")
                    appendLine("MIME Type: $mimeType")
                    appendLine("Extension: ${if (ext.isNotBlank()) ext else "none"}")
                    appendLine("Status: Binary stream attached for code audit and vulnerability context.")
                }
            }

            AttachedFile(
                name = fileName,
                sizeBytes = fileSize,
                formattedSize = formattedSize,
                mimeType = mimeType,
                extension = ext,
                isText = isTextFile,
                contentPayload = payload
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun processBitmap(bitmap: android.graphics.Bitmap, fileName: String = "camera_capture.jpg"): AttachedFile {
        val bytes = bitmap.byteCount.toLong()
        val formattedSize = formatFileSize(bytes)
        
        val payload = buildString {
            appendLine("CAMERA CAPTURE ATTACHMENT")
            appendLine("Image Dimension: ${bitmap.width}x${bitmap.height} px")
            appendLine("Format: JPEG Image Stream")
            appendLine("Status: Photo captured via device camera for visual security inspection.")
        }

        return AttachedFile(
            name = fileName,
            sizeBytes = bytes,
            formattedSize = formattedSize,
            mimeType = "image/jpeg",
            extension = "jpg",
            isText = false,
            contentPayload = payload
        )
    }

    fun createFallbackCameraBitmap(): android.graphics.Bitmap {
        val width = 640
        val height = 480
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#18181B")
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#DC2626")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawRect(10f, 10f, (width - 10).toFloat(), (height - 10).toFloat(), borderPaint)

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("CAMERA SNAPSHOT CAPTURE", (width / 2).toFloat(), (height / 2 - 20).toFloat(), textPaint)

        val subTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#A1A1AA")
            textSize = 20f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        canvas.drawText("Timestamp: $time", (width / 2).toFloat(), (height / 2 + 30).toFloat(), subTextPaint)

        return bitmap
    }

    fun processLocationScan(latitude: Double, longitude: Double, accuracy: Float, altitude: Double = 0.0, provider: String = "GPS/Network", address: String = ""): AttachedFile {
        val payload = buildString {
            appendLine("GPS & INTERNET LOCATION TELEMETRY SCAN")
            appendLine("----------------------------------------")
            appendLine("Latitude: $latitude")
            appendLine("Longitude: $longitude")
            if (altitude != 0.0) {
                appendLine("Altitude: ${String.format(java.util.Locale.getDefault(), "%.1f", altitude)} meters")
            }
            appendLine("Accuracy: ±$accuracy meters")
            appendLine("Provider Source: $provider")
            if (address.isNotBlank()) {
                appendLine("Resolved Address: $address")
            }
            appendLine("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", java.util.Locale.getDefault()).format(java.util.Date())}")
            appendLine("Geofence Status: [ACQUIRED VIA DEVICE GPS & INTERNET NETWORK]")
            appendLine("Context: Detailed geospatial reconnaissance payload generated successfully.")
        }

        return AttachedFile(
            name = "location_telemetry.gps",
            sizeBytes = payload.toByteArray().size.toLong(),
            formattedSize = formatFileSize(payload.toByteArray().size.toLong()),
            mimeType = "application/gps-telemetry",
            extension = "gps",
            isText = true,
            contentPayload = payload
        )
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
