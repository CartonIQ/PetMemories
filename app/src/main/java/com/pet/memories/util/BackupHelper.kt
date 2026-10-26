package com.pet.memories.util

import com.pet.memories.data.*
import org.json.JSONArray
import org.json.JSONObject

object BackupHelper {

    fun exportToJson(
        pets: List<PetEntity>,
        memories: List<MemoryEntity>,
        weights: List<WeightEntity>,
        vaccines: List<VaccineEntity>,
        expenses: List<ExpenseEntity>
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val petsArray = JSONArray()
        for (p in pets) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("breed", p.breed)
                put("birthday", p.birthday)
                put("petType", p.petType)
                put("microchip", p.microchip ?: "")
                put("weight", p.weight ?: 0.0)
            }
            petsArray.put(obj)
        }
        root.put("pets", petsArray)

        val memoriesArray = JSONArray()
        for (m in memories) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("petId", m.petId)
                put("caption", m.caption)
                put("date", m.date)
                put("createdAt", m.createdAt)
            }
            memoriesArray.put(obj)
        }
        root.put("memories", memoriesArray)

        val weightsArray = JSONArray()
        for (w in weights) {
            val obj = JSONObject().apply {
                put("id", w.id)
                put("petId", w.petId)
                put("weight", w.weight)
                put("date", w.date)
            }
            weightsArray.put(obj)
        }
        root.put("weights", weightsArray)

        val vaccinesArray = JSONArray()
        for (v in vaccines) {
            val obj = JSONObject().apply {
                put("id", v.id)
                put("petId", v.petId)
                put("name", v.name)
                put("dueDate", v.dueDate)
                put("completed", v.completed)
            }
            vaccinesArray.put(obj)
        }
        root.put("vaccines", vaccinesArray)

        val expensesArray = JSONArray()
        for (e in expenses) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("petId", e.petId ?: 0L)
                put("title", e.title)
                put("amount", e.amount)
                put("category", e.category)
                put("date", e.date)
            }
            expensesArray.put(obj)
        }
        root.put("expenses", expensesArray)

        return root.toString(2)
    }

    data class ParsedBackup(
        val pets: List<PetEntity>,
        val memories: List<MemoryEntity>,
        val weights: List<WeightEntity>,
        val vaccines: List<VaccineEntity>,
        val expenses: List<ExpenseEntity>
    )

    fun parseJson(jsonString: String): ParsedBackup {
        val root = JSONObject(jsonString)

        val pets = mutableListOf<PetEntity>()
        val petsArray = root.optJSONArray("pets") ?: JSONArray()
        for (i in 0 until petsArray.length()) {
            val o = petsArray.getJSONObject(i)
            pets.add(
                PetEntity(
                    id = o.optLong("id", 0L),
                    name = o.getString("name"),
                    breed = o.optString("breed", ""),
                    birthday = o.getString("birthday"),
                    petType = o.optString("petType", "Dog"),
                    microchip = o.optString("microchip").ifEmpty { null },
                    weight = if (o.has("weight")) o.optDouble("weight") else null
                )
            )
        }

        val memories = mutableListOf<MemoryEntity>()
        val memArray = root.optJSONArray("memories") ?: JSONArray()
        for (i in 0 until memArray.length()) {
            val o = memArray.getJSONObject(i)
            memories.add(
                MemoryEntity(
                    id = o.optLong("id", 0L),
                    petId = o.getLong("petId"),
                    caption = o.getString("caption"),
                    date = o.getString("date"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val weights = mutableListOf<WeightEntity>()
        val wArray = root.optJSONArray("weights") ?: JSONArray()
        for (i in 0 until wArray.length()) {
            val o = wArray.getJSONObject(i)
            weights.add(
                WeightEntity(
                    id = o.optLong("id", 0L),
                    petId = o.getLong("petId"),
                    weight = o.getDouble("weight"),
                    date = o.getString("date")
                )
            )
        }

        val vaccines = mutableListOf<VaccineEntity>()
        val vArray = root.optJSONArray("vaccines") ?: JSONArray()
        for (i in 0 until vArray.length()) {
            val o = vArray.getJSONObject(i)
            vaccines.add(
                VaccineEntity(
                    id = o.optLong("id", 0L),
                    petId = o.getLong("petId"),
                    name = o.getString("name"),
                    dueDate = o.getString("dueDate"),
                    completed = o.optBoolean("completed", false)
                )
            )
        }

        val expenses = mutableListOf<ExpenseEntity>()
        val eArray = root.optJSONArray("expenses") ?: JSONArray()
        for (i in 0 until eArray.length()) {
            val o = eArray.getJSONObject(i)
            expenses.add(
                ExpenseEntity(
                    id = o.optLong("id", 0L),
                    petId = if (o.has("petId") && o.getLong("petId") > 0) o.getLong("petId") else null,
                    title = o.getString("title"),
                    amount = o.getDouble("amount"),
                    category = o.optString("category", "Food"),
                    date = o.getString("date")
                )
            )
        }

        return ParsedBackup(pets, memories, weights, vaccines, expenses)
    }

    fun getBackupSummary(parsed: ParsedBackup): String {
        return "${parsed.pets.size} Pets, ${parsed.memories.size} Memories, ${parsed.weights.size} Weight logs, ${parsed.vaccines.size} Vaccines, ${parsed.expenses.size} Expenses"
    }
}
