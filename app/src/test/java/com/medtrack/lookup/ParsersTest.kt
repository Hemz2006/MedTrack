package com.medtrack.lookup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsersTest {
    @Test fun suggestionsPreferRxnormNameAndSkipNameless() = assertEquals(
        listOf(Suggestion("40790", "pantoprazole")),
        parseSuggestions(Fixtures.SUGGEST_PANTAPRAZOLE),
    )

    @Test fun rxcui() {
        assertEquals("40790", parseRxcui(Fixtures.RXCUI_PANTOPRAZOLE))
        assertNull(parseRxcui(Fixtures.RXCUI_NONE))
    }

    @Test fun related() {
        val r = parseRelated(Fixtures.RELATED_APAP_CAFFEINE)
        assertEquals(listOf(Suggestion("161", "acetaminophen"), Suggestion("1886", "caffeine")), r.ingredients)
        assertEquals(2, r.clinicalDrugNames.size)
    }

    @Test fun strengthsSingleIngredient() = assertEquals(
        "20 MG, 40 MG, 0.4 MG/ML",
        parseStrengths("pantoprazole", parseRelated(Fixtures.RELATED_PANTOPRAZOLE).clinicalDrugNames),
    )

    @Test fun strengthsCombination() {
        val names = parseRelated(Fixtures.RELATED_APAP_CAFFEINE).clinicalDrugNames
        assertEquals("450 MG", parseStrengths("acetaminophen", names))
        assertEquals("25 MG, 30 MG", parseStrengths("caffeine", names))
        assertNull(parseStrengths("ibuprofen", names))
    }

    @Test fun formula() {
        assertEquals("C16H15F2N3O4S", parseFormula(Fixtures.PUBCHEM_PANTOPRAZOLE))
        assertNull(parseFormula(Fixtures.PUBCHEM_NOT_FOUND))
    }

    @Test fun medlineSummaryIsCleanAndShort() {
        val s = parseMedlineSummary(Fixtures.MEDLINE_PANTOPRAZOLE)!!
        assertTrue(s.startsWith("Pantoprazole is used to treat gastroesophageal reflux disease"))
        assertTrue(s.length <= 301)
        assertTrue("  " !in s)
        assertEquals(
            "Acetaminophen is used to relieve mild to moderate pain. It is also used to reduce fever.",
            parseMedlineSummary(Fixtures.MEDLINE_ACETAMINOPHEN),
        )
        assertNull(parseMedlineSummary(Fixtures.MEDLINE_EMPTY))
    }
}
