package com.pet.memories.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.pet.memories.data.PetEntity
import java.io.File
import java.io.FileOutputStream

object PetPassportGenerator {

    fun generateAndSharePassport(context: Context, pet: PetEntity, ownerPhone: String = "Not specified"): Bitmap {
        val width = 1080
        val height = 1520
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply { color = Color.parseColor("#FFF9F5") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Header Banner (Navy Passport Blue #1A365D)
        val headerPaint = Paint().apply { color = Color.parseColor("#1A365D") }
        canvas.drawRoundRect(40f, 40f, (width - 40).toFloat(), 240f, 32f, 32f, headerPaint)

        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 52f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PET PASSPORT & ID", (width / 2).toFloat(), 130f, headerTextPaint)

        val subHeaderTextPaint = Paint().apply {
            color = Color.parseColor("#FFECCF")
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("OFFICIAL COMPANION IDENTIFICATION", (width / 2).toFloat(), 185f, subHeaderTextPaint)

        // Photo Frame Card
        val photoCardPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(60f, 280f, (width - 60).toFloat(), 720f, 36f, 36f, photoCardPaint)

        // Border around photo card
        val strokePaint = Paint().apply {
            color = Color.parseColor("#FFE3DA")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(60f, 280f, (width - 60).toFloat(), 720f, 36f, 36f, strokePaint)

        // Photo Placeholder / Circle
        val circlePaint = Paint().apply { color = Color.parseColor("#FF7A59") }
        var photoDrawn = false
        if (pet.photoUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(android.net.Uri.parse(pet.photoUri))
                val petBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (petBitmap != null) {
                    val size = Math.min(petBitmap.width, petBitmap.height)
                    val xOffset = (petBitmap.width - size) / 2
                    val yOffset = (petBitmap.height - size) / 2
                    val squareBitmap = android.graphics.Bitmap.createBitmap(petBitmap, xOffset, yOffset, size, size)
                    val scaled = android.graphics.Bitmap.createScaledBitmap(squareBitmap, 300, 300, true)
                    canvas.drawCircle(240f, 500f, 150f, circlePaint)
                    canvas.save()
                    val path = android.graphics.Path()
                    path.addCircle(240f, 500f, 150f, android.graphics.Path.Direction.CW)
                    canvas.clipPath(path)
                    canvas.drawBitmap(scaled, 90f, 350f, null)
                    canvas.restore()
                    photoDrawn = true
                }
            } catch (e: Exception) {
            }
        }
        if (!photoDrawn) {
            canvas.drawCircle(240f, 500f, 150f, circlePaint)
            val initialPaint = Paint().apply {
                color = Color.WHITE
                textSize = 100f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(pet.name.take(1).uppercase(), 240f, 535f, initialPaint)
        }

        // Name & Tag in Photo Card
        val namePaint = Paint().apply {
            color = Color.parseColor("#1A365D")
            textSize = 64f
            isFakeBoldText = true
        }
        canvas.drawText(pet.name, 440f, 460f, namePaint)

        val breedPaint = Paint().apply {
            color = Color.parseColor("#685E5A")
            textSize = 36f
        }
        canvas.drawText("${pet.breed.ifEmpty { "Mixed Breed" }} • ${pet.petType}", 440f, 520f, breedPaint)

        val agePaint = Paint().apply {
            color = Color.parseColor("#FF7A59")
            textSize = 34f
            isFakeBoldText = true
        }
        val ageStr = AgeCalculator.calculateAge(pet.birthday)
        canvas.drawText("Age: $ageStr", 440f, 575f, agePaint)

        // ID Details Table Card
        val infoCardPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRoundRect(60f, 760f, (width - 60).toFloat(), 1380f, 36f, 36f, infoCardPaint)
        canvas.drawRoundRect(60f, 760f, (width - 60).toFloat(), 1380f, 36f, 36f, strokePaint)

        val labelPaint = Paint().apply {
            color = Color.parseColor("#8C7E78")
            textSize = 30f
        }
        val valuePaint = Paint().apply {
            color = Color.parseColor("#2B2320")
            textSize = 36f
            isFakeBoldText = true
        }

        var yPos = 840f
        val lineSpacing = 95f

        val fields = listOf(
            "Microchip Number" to (pet.microchip ?: "Not Microchipped"),
            "Date of Birth" to pet.birthday,
            "Recorded Weight" to if (pet.weight != null) "${pet.weight} kg" else "Not Recorded",
            "Emergency Owner Contact" to ownerPhone,
            "Verification Status" to "Offline Registry (PetMemories)"
        )

        for ((label, value) in fields) {
            canvas.drawText(label, 100f, yPos, labelPaint)
            canvas.drawText(value, 100f, yPos + 40f, valuePaint)
            yPos += lineSpacing
        }

        // Footer Barcode Simulation
        val barcodePaint = Paint().apply {
            color = Color.parseColor("#1A365D")
            strokeWidth = 6f
        }
        var barX = 100f
        while (barX < width - 100) {
            val step = if ((barX.toInt() % 7) == 0) 14f else 8f
            canvas.drawLine(barX, 1310f, barX, 1350f, barcodePaint)
            barX += step
        }

        // Save & Share
        return bitmap
    }

    private fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String, subject: String) {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, fileName)
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        fos.flush()
        fos.close()

        val contentUri: Uri = FileProvider.getUriForFile(context, "com.pet.memories.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Pet Passport"))
    }
}
