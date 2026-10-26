package com.pet.memories.util

import java.time.LocalDate

object PetFacts {
    val facts = listOf(
        "A dog's sense of smell is roughly 10,000 to 100,000 times more sensitive than a human's!",
        "Cats spend approximately 30% to 50% of their awake hours grooming themselves.",
        "Dogs dream just like humans do! Small twitches and soft whimpers mean they are in REM sleep.",
        "A group of kittens is called a 'kindle', while a group of adult cats is called a 'clowder'.",
        "Dogs' nose prints are unique, just like human fingerprints, and can uniquely identify them.",
        "Purring doesn't just indicate happiness in cats—it also promotes bone healing and self-soothing.",
        "Dogs can recognize and understand up to 150 to 250 words and hand gestures.",
        "Cat whiskers are deeply rooted in nerve endings and help them measure spaces in the dark.",
        "Petting a dog or cat has been clinically proven to lower blood pressure and boost oxytocin.",
        "A dog's hearing is so sharp they can hear sounds four times farther away than humans can.",
        "Cats have 32 muscles in each outer ear, allowing them to rotate their ears 180 degrees.",
        "Dalmatians are born completely pure white—their iconic dark spots develop as they grow!"
    )

    fun getDailyFact(date: LocalDate = LocalDate.now()): String {
        val index = (date.dayOfYear + date.year) % facts.size
        return facts[index]
    }

    fun getRandomFact(): String {
        return facts.random()
    }
}
