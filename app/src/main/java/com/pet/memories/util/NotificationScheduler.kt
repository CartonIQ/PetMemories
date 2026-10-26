package com.pet.memories.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class VaccineReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val petName = inputData.getString("pet_name") ?: "Your pet"
        val vaccineName = inputData.getString("vaccine_name") ?: "Vaccine"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "pet_health_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pet Health & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for pet vaccines and medication"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🐾 Vaccine Due Today!")
            .setContentText("\$petName is due for \$vaccineName. Keep them healthy and protected!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        return Result.success()
    }
}

object NotificationScheduler {
    fun scheduleVaccineReminder(context: Context, petName: String, vaccineName: String, dueDateIso: String): String {
        return try {
            val dueDate = LocalDate.parse(dueDateIso, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val today = LocalDate.now()
            val delayDays = ChronoUnit.DAYS.between(today, dueDate).coerceAtLeast(0)

            val inputData = Data.Builder()
                .putString("pet_name", petName)
                .putString("vaccine_name", vaccineName)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<VaccineReminderWorker>()
                .setInitialDelay(delayDays, TimeUnit.DAYS)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            workRequest.id.toString()
        } catch (e: Exception) {
            ""
        }
    }
}
