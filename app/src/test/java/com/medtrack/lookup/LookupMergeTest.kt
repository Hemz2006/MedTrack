package com.medtrack.lookup

import com.medtrack.data.Ingredient
import com.medtrack.data.SourceStatus.FAILED
import com.medtrack.data.SourceStatus.NOT_RUN
import com.medtrack.data.SourceStatus.OK
import com.medtrack.data.newMedication
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class LookupMergeTest {
    private val blank = newMedication(LocalDate.parse("2026-10-13"), Instant.EPOCH).copy(genericName = "pantoprazole")
    private val full = LookupResult(
        rxcui = "40790",
        ingredients = listOf(Ingredient("pantoprazole", "40 MG", "C16H15F2N3O4S")),
        usedFor = "Treats reflux.",
        rxnorm = OK, pubchem = OK, medline = OK,
    )

    @Test fun appliesEverySuccessfulSource() {
        val m = blank.applyLookup(full, onlyMissing = false)
        assertEquals("40790", m.rxcui)
        assertEquals(full.ingredients, m.ingredients)
        assertEquals("Treats reflux.", m.usedFor)
        assertEquals(listOf(OK, OK, OK), listOf(m.rxnormStatus, m.pubchemStatus, m.medlineStatus))
    }

    @Test fun notRunSourcesLeaveFieldsAlone() {
        val partial = full.copy(pubchem = NOT_RUN, medline = NOT_RUN, usedFor = null)
        val m = blank.copy(usedFor = "typed by me").applyLookup(partial, onlyMissing = false)
        assertEquals("typed by me", m.usedFor)
        assertEquals(NOT_RUN, m.medlineStatus)
    }

    @Test fun onlyMissingKeepsUserEditsOfSuccessfulSources() {
        val edited = blank.applyLookup(full.copy(pubchem = FAILED), onlyMissing = false)
            .let { it.copy(ingredients = listOf(Ingredient("pantoprazole", "40 MG (my note)", null)), usedFor = "my words") }
        val retried = edited.applyLookup(full, onlyMissing = true)
        assertEquals(listOf(Ingredient("pantoprazole", "40 MG (my note)", "C16H15F2N3O4S")), retried.ingredients)
        assertEquals("my words", retried.usedFor)
        assertEquals(OK, retried.pubchemStatus)
    }

    @Test fun failureKeepsOldData() {
        val m = blank.applyLookup(full, onlyMissing = false).applyLookup(LookupResult(rxnorm = FAILED), onlyMissing = false)
        assertEquals(FAILED, m.rxnormStatus)
        assertEquals(full.ingredients, m.ingredients)
    }
}
