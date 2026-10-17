package com.medtrack.lookup

import org.junit.Assert.assertEquals
import org.junit.Test

class TextMatchTest {
    private fun brand(name: String, composition: String = "x", discontinued: Boolean = false) =
        Candidate(Brand(name, composition, null), discontinued)

    @Test fun picksPossibleNamesAndTheirStrength() {
        val text = """
            Dr. A. Kumar MBBS
            Patient name: Ravi   Age 45
            1) Tab Pantonix 40 - 1-0-0 before breakfast
            2) Cap. Dolo 650mg  x 5 days
        """.trimIndent()
        assertEquals(
            listOf(ReadName("Kumar", null), ReadName("Ravi", null), ReadName("Pantonix", "40"), ReadName("Dolo", "650")),
            possibleNames(text),
        )
    }

    @Test fun skipsWordsMixedWithDigits() {
        assertEquals(emptyList<ReadName>(), possibleNames("BD 1-0-1 10ml x5"))
    }

    @Test fun matchesAMisreadLetter() {
        val candidates = listOf(brand("Pantonix 40 Tablet"), brand("Pantop 40 Tablet"), brand("Pantodac 40 Tablet"))
        assertEquals(listOf("Pantonix 40 Tablet"), closestBrands(ReadName("Pantomix", "40"), candidates).map { it.name })
    }

    @Test fun prefersTheWrittenStrengthThenBrandsOnSale() {
        val candidates = listOf(brand("Dolo 500 Tablet"), brand("Dolo 650 Tablet", discontinued = true), brand("Dolo 650 Tablet DT"))
        assertEquals(listOf("Dolo 650 Tablet DT", "Dolo 650 Tablet", "Dolo 500 Tablet"), closestBrands(ReadName("Dolo", "650"), candidates).map { it.name })
    }

    @Test fun shortWordsMustMatchExactly() {
        assertEquals(emptyList<Brand>(), closestBrands(ReadName("Dulo", null), listOf(brand("Dolo 650 Tablet"))))
    }

    @Test fun editDistanceCountsChanges() {
        assertEquals(0, editDistance("pantonix", "pantonix"))
        assertEquals(1, editDistance("pantomix", "pantonix"))
        assertEquals(1, editDistance("pantonx", "pantonix"))
    }

    @Test fun searchPrefixesTryLookalikeLetters() {
        val prefixes = searchPrefixes("Polo")
        assertEquals("pol", prefixes.first())
        assertEquals(true, "dol" in prefixes)
        assertEquals(true, "mec" in searchPrefixes("Nec"))
    }

    @Test fun savedMedicationsMatchAndComeFirst() {
        val mine = Candidate(Brand("Mecmaxol", "Methylcobalamin", null, saved = true), false)
        assertEquals(listOf("Mecmaxol"), closestBrands(ReadName("Mecmoxol", null), listOf(brand("Mecmo Tablet"), mine)).map { it.name })
    }

    @Test fun lookalikeLettersStillMatch() {
        // As read by the phone from a handwritten prescription: "Qolo 6 So" and "hecmool".
        assertEquals(listOf("Dolo 650 Tablet"), closestBrands(ReadName("Qolo", "6"), listOf(brand("Dolo 650 Tablet"), brand("Solo Gel"))).map { it.name })
        val mine = Candidate(Brand("Mecmaxol", "Methylcobalamin", null, saved = true), false)
        assertEquals(listOf("Mecmaxol"), closestBrands(ReadName("hecmool", null), listOf(mine)).map { it.name })
    }
}
