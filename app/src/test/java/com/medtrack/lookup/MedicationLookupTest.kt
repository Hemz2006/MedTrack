package com.medtrack.lookup

import com.medtrack.data.Ingredient
import com.medtrack.data.SourceStatus.FAILED
import com.medtrack.data.SourceStatus.NOT_FOUND
import com.medtrack.data.SourceStatus.NOT_RUN
import com.medtrack.data.SourceStatus.OK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MedicationLookupTest {
    private val rxnav = "https://rxnav.nlm.nih.gov/REST"
    private val pubchem = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name"
    private fun medline(rxcui: String) =
        "https://connect.medlineplus.gov/service?mainSearchCriteria.v.cs=2.16.840.1.113883.6.88&mainSearchCriteria.v.c=$rxcui&knowledgeResponseType=application/json"

    /** Serves canned responses; any URL not listed behaves like a network failure. */
    private fun fake(vararg routes: Pair<String, HttpResponse>): Pair<Fetch, MutableList<String>> {
        val calls = mutableListOf<String>()
        val map = routes.toMap()
        val fetch: Fetch = { url: String -> calls += url; map[url] ?: throw IOException("offline: $url") }
        return fetch to calls
    }
    private fun ok(body: String) = HttpResponse(200, body)

    private val pantoprazoleRoutes = arrayOf(
        "$rxnav/rxcui.json?name=pantoprazole&search=2" to ok(Fixtures.RXCUI_PANTOPRAZOLE),
        "$rxnav/rxcui/40790/related.json?tty=IN+SCD" to ok(Fixtures.RELATED_PANTOPRAZOLE),
        "$pubchem/pantoprazole/property/MolecularFormula/JSON" to ok(Fixtures.PUBCHEM_PANTOPRAZOLE),
        medline("40790") to ok(Fixtures.MEDLINE_PANTOPRAZOLE),
    )

    @Test fun fullLookupReportsProgress() = runBlocking {
        val (fetch, _) = fake(*pantoprazoleRoutes)
        val progress = mutableListOf<LookupResult>()
        val r = MedicationLookup(fetch).lookup(" pantoprazole ", onProgress = { progress += it })
        assertEquals("40790", r.rxcui)
        assertEquals(listOf(Ingredient("pantoprazole", "20 MG, 40 MG, 0.4 MG/ML", "C16H15F2N3O4S")), r.ingredients)
        assertTrue(r.usedFor!!.startsWith("Pantoprazole is used to treat"))
        assertEquals(listOf(OK, OK, OK), listOf(r.rxnorm, r.pubchem, r.medline))
        assertEquals(3, progress.size)
        assertEquals(NOT_RUN, progress[0].pubchem)
        assertEquals(r, progress.last())
    }

    @Test fun knownRxcuiSkipsNameResolution() = runBlocking {
        val (fetch, calls) = fake(*pantoprazoleRoutes)
        MedicationLookup(fetch).lookup("pantoprazole", knownRxcui = "40790")
        assertTrue(calls.none { "rxcui.json" in it })
    }

    @Test fun unknownNameIsNotFound() = runBlocking {
        val (fetch, _) = fake("$rxnav/rxcui.json?name=xyzqq&search=2" to ok(Fixtures.RXCUI_NONE))
        val r = MedicationLookup(fetch).lookup("xyzqq")
        assertEquals(listOf(NOT_FOUND, NOT_RUN, NOT_RUN), listOf(r.rxnorm, r.pubchem, r.medline))
    }

    @Test fun offlineMarksRxnormFailed() = runBlocking {
        val (fetch, _) = fake()
        val r = MedicationLookup(fetch).lookup("pantoprazole")
        assertEquals(listOf(FAILED, NOT_RUN, NOT_RUN), listOf(r.rxnorm, r.pubchem, r.medline))
    }

    @Test fun pubchemDownKeepsOtherSources() = runBlocking {
        val (fetch, _) = fake(
            "$rxnav/rxcui.json?name=pantoprazole&search=2" to ok(Fixtures.RXCUI_PANTOPRAZOLE),
            "$rxnav/rxcui/40790/related.json?tty=IN+SCD" to ok(Fixtures.RELATED_PANTOPRAZOLE),
            "$pubchem/pantoprazole/property/MolecularFormula/JSON" to HttpResponse(503, "busy"),
            medline("40790") to ok(Fixtures.MEDLINE_PANTOPRAZOLE),
        )
        val r = MedicationLookup(fetch).lookup("pantoprazole")
        assertEquals(listOf(OK, FAILED, OK), listOf(r.rxnorm, r.pubchem, r.medline))
        assertEquals("pantoprazole", r.ingredients.single().name)
    }

    @Test fun combinationDrugFallsBackToIngredientForUsedFor() = runBlocking {
        val (fetch, _) = fake(
            "$rxnav/rxcui.json?name=acetaminophen+%2F+caffeine&search=2" to ok("""{"idGroup":{"rxnormId":["108038"]}}"""),
            "$rxnav/rxcui/108038/related.json?tty=IN+SCD" to ok(Fixtures.RELATED_APAP_CAFFEINE),
            "$pubchem/acetaminophen/property/MolecularFormula/JSON" to ok(Fixtures.PUBCHEM_ACETAMINOPHEN),
            "$pubchem/caffeine/property/MolecularFormula/JSON" to HttpResponse(404, Fixtures.PUBCHEM_NOT_FOUND),
            medline("108038") to ok(Fixtures.MEDLINE_EMPTY),
            medline("161") to ok(Fixtures.MEDLINE_ACETAMINOPHEN),
        )
        val r = MedicationLookup(fetch).lookup("acetaminophen / caffeine")
        assertEquals(
            listOf(Ingredient("acetaminophen", "450 MG", "C8H9NO2"), Ingredient("caffeine", "25 MG, 30 MG", null)),
            r.ingredients,
        )
        assertEquals(OK, r.pubchem)
        assertTrue(r.usedFor!!.startsWith("Acetaminophen is used"))
    }

    @Test fun suggestNeverThrows() = runBlocking {
        val (fetch, _) = fake()
        assertEquals(emptyList<Suggestion>(), MedicationLookup(fetch).suggest("panto"))
        val (fetch2, _) = fake("$rxnav/approximateTerm.json?term=pantaprazole&maxEntries=8" to ok(Fixtures.SUGGEST_PANTAPRAZOLE))
        assertEquals(listOf(Suggestion("40790", "pantoprazole")), MedicationLookup(fetch2).suggest("pantaprazole"))
    }

    private val labelRoutes = arrayOf(
        "$rxnav/rxcui.json?name=Paracetamol&search=2" to ok("""{"idGroup":{"rxnormId":["161"]}}"""),
        "$rxnav/rxcui.json?name=Caffeine&search=2" to ok(Fixtures.RXCUI_NONE),
        "$pubchem/Paracetamol/property/MolecularFormula/JSON" to ok(Fixtures.PUBCHEM_ACETAMINOPHEN),
        "$pubchem/Caffeine/property/MolecularFormula/JSON" to HttpResponse(404, Fixtures.PUBCHEM_NOT_FOUND),
        medline("161") to ok(Fixtures.MEDLINE_ACETAMINOPHEN),
    )

    @Test fun packIngredientsAreLookedUpOneByOne() = runBlocking {
        val (fetch, _) = fake(*labelRoutes)
        val label = listOf(Ingredient("Paracetamol", "650mg"), Ingredient("Caffeine", "50mg"))
        val r = MedicationLookup(fetch).lookupIngredients(label)
        assertEquals(listOf(Ingredient("Paracetamol", "650mg", "C8H9NO2"), Ingredient("Caffeine", "50mg")), r.ingredients)
        assertEquals("161", r.rxcui)
        assertEquals(listOf(OK, OK, OK), listOf(r.rxnorm, r.pubchem, r.medline))
        assertTrue(r.fromLabel)
    }

    @Test fun nothingFoundStillKeepsThePackIngredients() = runBlocking {
        val (fetch, _) = fake(
            "$rxnav/rxcui.json?name=Mecobalamin&search=2" to ok(Fixtures.RXCUI_NONE),
            "$pubchem/Mecobalamin/property/MolecularFormula/JSON" to HttpResponse(404, Fixtures.PUBCHEM_NOT_FOUND),
        )
        val r = MedicationLookup(fetch).lookupIngredients(listOf(Ingredient("Mecobalamin", "500mcg")))
        assertEquals(listOf(Ingredient("Mecobalamin", "500mcg")), r.ingredients)
        assertEquals(listOf(NOT_FOUND, NOT_FOUND, NOT_FOUND), listOf(r.rxnorm, r.pubchem, r.medline))
    }

    @Test fun bareGenericNameGoesThroughRxNorm() = runBlocking {
        val (fetch, calls) = fake(*pantoprazoleRoutes)
        MedicationLookup(fetch).lookupFor("pantoprazole", emptyList(), null)
        assertTrue(calls.any { "related.json" in it })
    }

    @Test fun brandIngredientKeepsItsPackStrength() = runBlocking {
        val (fetch, calls) = fake(*labelRoutes)
        val r = MedicationLookup(fetch).lookupFor("Paracetamol", listOf(Ingredient("Paracetamol", "650mg")), null)
        assertEquals("650mg", r.ingredients.single().strength)
        assertTrue(calls.none { "related.json" in it })
    }

    @Test fun typedCompositionIsSplit() = runBlocking {
        val (fetch, _) = fake(*labelRoutes)
        val r = MedicationLookup(fetch).lookupFor("Paracetamol 650mg + Caffeine", emptyList(), null)
        assertEquals(listOf("Paracetamol", "Caffeine"), r.ingredients.map { it.name })
        assertEquals("650mg", r.ingredients.first().strength)
    }

    @Test fun brandOnlyWithNothingKnownDoesNoLookup() = runBlocking {
        val (fetch, calls) = fake()
        assertEquals(LookupResult(), MedicationLookup(fetch).lookupFor("", emptyList(), null))
        assertTrue(calls.isEmpty())
    }
}
