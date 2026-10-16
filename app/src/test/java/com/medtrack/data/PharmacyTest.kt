package com.medtrack.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PharmacyTest {
    @Test fun searchesByNameWithSpacesEncoded() {
        assertEquals("https://www.1mg.com/search/all?name=Dolo%20650", Pharmacy.TATA_1MG.searchUrl("Dolo 650"))
        assertEquals("https://pharmeasy.in/search/all?name=Dolo%20650", Pharmacy.PHARMEASY.searchUrl(" Dolo 650 "))
        assertEquals("https://www.netmeds.com/products?q=Dolo%20650", Pharmacy.NETMEDS.searchUrl("Dolo 650"))
    }

    @Test fun encodesCharactersThatWouldBreakTheAddress() {
        // A composition name can hold "+", "&" or "/", which must not end or split the search term.
        assertEquals(
            "https://www.1mg.com/search/all?name=Paracetamol%20%2B%20Caffeine%20%26%2010%2F5",
            Pharmacy.TATA_1MG.searchUrl("Paracetamol + Caffeine & 10/5"),
        )
    }

    private fun med(generic: String, brand: String? = null, ingredients: List<Ingredient> = emptyList()) =
        MedicationEntity(genericName = generic, brandName = brand, ingredients = ingredients, steps = emptyList(), createdAt = java.time.Instant.EPOCH)

    @Test fun offersThePackNameThenTheGenericName() {
        assertEquals(
            listOf(OrderSearch("Name on the pack", "Dolo 650"), OrderSearch("Generic name", "Paracetamol 650mg")),
            med("Paracetamol 650mg", brand = "Dolo 650").orderSearches(),
        )
    }

    @Test fun offersOnlyTheGenericNameWithoutABrand() {
        assertEquals(listOf(OrderSearch("Generic name", "Pantoprazole")), med("Pantoprazole", brand = " ").orderSearches())
    }

    @Test fun usesTheIngredientsWhenThereIsNoGenericName() {
        assertEquals(
            listOf(OrderSearch("Name on the pack", "Montek LC"), OrderSearch("Generic name", "Levocetirizine + Montelukast")),
            med("", brand = "Montek LC", ingredients = listOf(Ingredient("Levocetirizine", "5mg"), Ingredient("Montelukast"))).orderSearches(),
        )
    }

    @Test fun doesNotRepeatAGenericNameThatMatchesTheBrand() {
        assertEquals(listOf(OrderSearch("Name on the pack", "Mecmaxol")), med("mecmaxol", brand = "Mecmaxol").orderSearches())
    }

    @Test fun dropsRepeatedPartsOfTheGenericName() {
        assertEquals(OrderSearch("Generic name", "Aprepitant"), med("Aprepitant + Aprepitant", brand = "Aprecap 125/80").orderSearches()[1])
    }
}
