package com.medtrack.lookup

import org.junit.Assert.assertEquals
import org.junit.Test

class PrescriptionMatchTest {
    private fun brand(name: String, composition: String = "x", discontinued: Boolean = false) =
        Candidate(Brand(name, composition, null), discontinued)

    @Test fun picksMedicineWordsAndTheirStrength() {
        val text = """
            Dr. A. Kumar MBBS
            Patient name: Ravi   Age 45
            1) Tab Pantonix 40 - 1-0-0 before breakfast
            2) Cap. Dolo 650mg  x 5 days
        """.trimIndent()
        assertEquals(
            listOf(ReadName("Kumar", null), ReadName("Ravi", null), ReadName("Pantonix", "40"), ReadName("Dolo", "650")),
            namesInPrescription(text),
        )
    }

    @Test fun skipsWordsMixedWithDigits() {
        assertEquals(emptyList<ReadName>(), namesInPrescription("BD 1-0-1 10ml x5"))
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
        assertEquals(emptyList<Brand>(), closestBrands(ReadName("Dalo", null), listOf(brand("Dolo 650 Tablet"))))
    }

    @Test fun editDistanceCountsChanges() {
        assertEquals(0, editDistance("pantonix", "pantonix"))
        assertEquals(1, editDistance("pantomix", "pantonix"))
        assertEquals(1, editDistance("pantonx", "pantonix"))
    }
}
