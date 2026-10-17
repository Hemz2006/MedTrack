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

    @Test fun hyphenatedBrandNamesAreRead() {
        // The Posoxil-GR pack: brand with a hyphen, strength after the "GR".
        val names = possibleNames("Posaconazole Gastro-\nResistant Tablets 300 mg\nPosoxil-GR 300")
        assertEquals(true, ReadName("Posoxil", "300") in names)
        assertEquals(true, names.any { it.word == "Posaconazole" })
    }

    @Test fun strengthsAreReadAsCompositionsWriteThem() {
        assertEquals(setOf("300mg", "5ml"), strengthsIn("Tablets 300 mg ... each 5 ML"))
    }

    @Test fun genericNameFindsBrandsWithThatIngredientAndStrength() {
        val mine = Candidate(Brand("Posoxil GR 300mg Tablet", "Posaconazole (300mg)", null, saved = true), false)
        val candidates = listOf(
            brand("Posoxil GR 100mg Tablet", "Posaconazole (100mg)"),
            brand("Noxafil 300 Tablet", "Posaconazole (300mg)"),
            brand("Posahit 300mg Injection", "Posaconazole (300mg)"),
            brand("Paracip 500 Tablet", "Paracetamol (500mg)"),
            mine,
        )
        // Own medication first; then tablets (the printed form) before injections, the printed strength first.
        assertEquals(
            listOf("Posoxil GR 300mg Tablet", "Noxafil 300 Tablet", "Posoxil GR 100mg Tablet"),
            brandsWithIngredient("Posaconazole", setOf("300mg"), formIn("Gastro-Resistant Tablets"), candidates).map { it.name },
        )
    }
}
