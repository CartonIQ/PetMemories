package com.pet.memories.util

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object AgeCalculator {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun calculateAge(birthdayIso: String, targetDate: LocalDate = LocalDate.now()): String {
        return try {
            val birthDate = LocalDate.parse(birthdayIso, formatter)
            if (birthDate.isAfter(targetDate)) {
                return "Just born"
            }
            val period = Period.between(birthDate, targetDate)
            when {
                period.years > 0 -> {
                    if (period.months > 0) "${period.years}y ${period.months}m" else "${period.years} years"
                }
                period.months > 0 -> {
                    if (period.days > 0) "${period.months}m ${period.days}d" else "${period.months} months"
                }
                else -> {
                    val days = ChronoUnit.DAYS.between(birthDate, targetDate)
                    if (days <= 0) "Today" else "\$days days"
                }
            }
        } catch (e: Exception) {
            "Unknown age"
        }
    }

    fun daysUntilNextBirthday(birthdayIso: String, today: LocalDate = LocalDate.now()): String {
        return try {
            val birthDate = LocalDate.parse(birthdayIso, formatter)
            var nextBirthday = birthDate.withYear(today.year)
            if (nextBirthday.isBefore(today)) {
                nextBirthday = nextBirthday.plusYears(1)
            }
            val days = ChronoUnit.DAYS.between(today, nextBirthday)
            when (days) {
                0L -> "Today! 🎂🎉"
                1L -> "Tomorrow! 🎈"
                else -> "in $days days"
            }
        } catch (e: Exception) {
            "N/A"
        }
    }
}
