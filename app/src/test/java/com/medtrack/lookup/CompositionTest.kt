package com.medtrack.lookup

import com.medtrack.data.Ingredient
import org.junit.Assert.assertEquals
import org.junit.Test

class CompositionTest {
    @Test fun packLabelFormat() = assertEquals(
        listOf(Ingredient("Amoxycillin", "500mg"), Ingredient("Clavulanic Acid", "125mg")),
        parseComposition("Amoxycillin  (500mg)  + Clavulanic Acid (125mg) "),
    )

    @Test fun aliasInBracketsStaysInTheName() = assertEquals(
        listOf(Ingredient("Vitamin B6 (Pyridoxine)", "10mg"), Ingredient("Thiamine(Vitamin B1)")),
        parseComposition("Vitamin B6 (Pyridoxine) (10mg) + Thiamine(Vitamin B1)"),
    )

    @Test fun naStrengthIsDropped() = assertEquals(listOf(Ingredient("Aceclofenac")), parseComposition("Aceclofenac (NA)"))

    @Test fun typedListsWithCommasAndBareStrengths() = assertEquals(
        listOf(Ingredient("calcium carbonate", "500mg"), Ingredient("calcitriol", "0.25mcg"), Ingredient("folic acid")),
        parseComposition("calcium carbonate 500 mg, calcitriol 0.25mcg, folic acid"),
    )

    @Test fun strengthsPerVolumeAndCommasInsideBracketsSurvive() = assertEquals(
        listOf(Ingredient("Ambroxol", "30mg/5ml")),
        parseComposition("Ambroxol (30mg/5ml)"),
    )

    @Test fun blankGivesNothing() = assertEquals(emptyList<Ingredient>(), parseComposition("  + , "))

    @Test fun formatsBackToPackStyle() = assertEquals(
        "Paracetamol (650mg) + Caffeine",
        listOf(Ingredient("Paracetamol", "650mg"), Ingredient("Caffeine")).formatComposition(),
    )

    @Test fun searchNameDropsAliases() = assertEquals("Vitamin B6", Ingredient("Vitamin B6 (Pyridoxine)").searchName())
}
