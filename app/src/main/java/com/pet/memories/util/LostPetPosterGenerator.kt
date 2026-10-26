package com.pet.memories.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.pet.memories.data.PetEntity
import java.io.File
import java.io.FileOutputStream

object LostPetPosterGenerator {

    fun generateAndSharePoster(context: Context, pet: PetEntity, contactPhone: String = "PLEASE CALL US"): Bitmap {
        val width = 1080
        val height = 1520
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // High-contrast Urgent Poster Background
        val bgPaint = Paint().apply { color = Color.parseColor("#FFFDF5") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Vibrant Red Urgent Banner (#D32F2F)
        val bannerPaint = Paint().apply { color = Color.parseColor("#D32F2F") }
        canvas.drawRect(0f, 0f, width.toFloat(), 260f, bannerPaint)

        val rewardTextPaint = Paint().apply {
            color = Color.parseColor("#FFEB3B")
            textSize = 50f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("★ ★ REWARD OFFERED ★ ★", (width / 2).toFloat(), 90f, rewardTextPaint)

        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 92f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("LOST PET!", (width / 2).toFloat(), 200f, headerTextPaint)

        // Photo Box
        val photoCardPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRoundRect(60f, 300f, (width - 60).toFloat(), 760f, 32f, 32f, photoCardPaint)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#D32F2F")
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawRoundRect(60f, 300f, (width - 60).toFloat(), 760f, 32f, 32f, borderPaint)

        // Silhouette/Avatar circle
        val circlePaint = Paint().apply { color = Color.parseColor("#FFCDD2") }
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
                    val scaled = android.graphics.Bitmap.createScaledBitmap(squareBitmap, 320, 320, true)
                    canvas.drawCircle((width / 2).toFloat(), 530f, 160f, circlePaint)
                    canvas.save()
                    val path = android.graphics.Path()
                    path.addCircle((width / 2).toFloat(), 530f, 160f, android.graphics.Path.Direction.CW)
                    canvas.clipPath(path)
                    canvas.drawBitmap(scaled, (width / 2 - 160).toFloat(), 370f, null)
                    canvas.restore()
                    photoDrawn = true
                }
            } catch (e: Exception) {
            }
        }
        if (!photoDrawn) {
            canvas.drawCircle((width / 2).toFloat(), 530f, 160f, circlePaint)
            val initialPaint = Paint().apply {
                color = Color.parseColor("#D32F2F")
                textSize = 140f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(pet.name.take(1).uppercase(), (width / 2).toFloat(), 580f, initialPaint)
        }
        // Name & Characteristics
        val namePaint = Paint().apply {
            color = Color.parseColor("#1E1E1E")
            textSize = 76f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(pet.name.uppercase(), (width / 2).toFloat(), 860f, namePaint)

        val descPaint = Paint().apply {
            color = Color.parseColor("#424242")
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        val details = "${pet.breed.ifEmpty { "Mixed" }} • ${pet.petType} • Age: ${AgeCalculator.calculateAge(pet.birthday)}"
        canvas.drawText(details, (width / 2).toFloat(), 920f, descPaint)

        if (!pet.microchip.isNullOrBlank()) {
            val chipPaint = Paint().apply {
                color = Color.parseColor("#757575")
                textSize = 34f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Microchip ID: ${pet.microchip}", (width / 2).toFloat(), 970f, chipPaint)
        }

        // Big Contact Callout Card
        val calloutPaint = Paint().apply { color = Color.parseColor("#D32F2F") }
        canvas.drawRoundRect(60f, 1040f, (width - 60).toFloat(), 1440f, 32f, 32f, calloutPaint)

        val callPromptPaint = Paint().apply {
            color = Color.WHITE
            textSize = 44f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("IF SEEN, PLEASE CALL IMMEDIATELY:", (width / 2).toFloat(), 1130f, callPromptPaint)

        val phonePaint = Paint().apply {
            color = Color.parseColor("#FFEB3B")
            textSize = 72f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(contactPhone, (width / 2).toFloat(), 1250f, phonePaint)

        val footerPaint = Paint().apply {
            color = Color.WHITE
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Much loved family member. No questions asked.", (width / 2).toFloat(), 1360f, footerPaint)

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
        context.startActivity(Intent.createChooser(intent, "Share Lost Pet Poster"))
    }
}
