package com.medtrack.lookup

import com.medtrack.data.Ingredient
import com.medtrack.data.SourceStatus
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.URLEncoder

data class LookupResult(
    val rxcui: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val usedFor: String? = null,
    val rxnorm: SourceStatus = SourceStatus.NOT_RUN,
    val pubchem: SourceStatus = SourceStatus.NOT_RUN,
    val medline: SourceStatus = SourceStatus.NOT_RUN,
)

/** Looks a generic name up in RxNorm, then PubChem (formula per ingredient), then MedlinePlus (usage). */
class MedicationLookup(private val fetch: Fetch) {

    suspend fun suggest(term: String): List<Suggestion> = try {
        getOrNull("$RXNAV/approximateTerm.json?term=${enc(term)}&maxEntries=8")?.let(::parseSuggestions).orEmpty()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    }

    /** Runs the sources in order. [onProgress] receives the cumulative result after each one. */
    suspend fun lookup(
        name: String,
        knownRxcui: String? = null,
        onProgress: (LookupResult) -> Unit = {},
    ): LookupResult {
        var result = LookupResult()
        var ingredientRxcuis: List<String> = emptyList()
        try {
            val rxcui = knownRxcui ?: getOrNull("$RXNAV/rxcui.json?name=${enc(name)}&search=2")?.let(::parseRxcui)
            if (rxcui == null) return result.copy(rxnorm = SourceStatus.NOT_FOUND).also(onProgress)
            val related = getOrNull("$RXNAV/rxcui/$rxcui/related.json?tty=IN+SCD")?.let(::parseRelated)
                ?: RelatedConcepts(emptyList(), emptyList())
            val ingredients = related.ingredients.ifEmpty { listOf(Suggestion(rxcui, name.trim().lowercase())) }
            ingredientRxcuis = ingredients.map { it.rxcui }
            result = result.copy(
                rxcui = rxcui,
                rxnorm = SourceStatus.OK,
                ingredients = ingredients.map { Ingredient(it.name, parseStrengths(it.name, related.clinicalDrugNames)) },
            )
            onProgress(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return result.copy(rxnorm = SourceStatus.FAILED).also(onProgress)
        }

        result = withFormulas(result)
        onProgress(result)
        result = withUsedFor(result, listOf(result.rxcui!!) + ingredientRxcuis)
        onProgress(result)
        return result
    }

    private suspend fun withFormulas(result: LookupResult): LookupResult {
        var found = false
        var failed = false
        val ingredients = result.ingredients.map { ingredient ->
            try {
                val formula = getOrNull("$PUBCHEM/compound/name/${encPath(ingredient.name)}/property/MolecularFormula/JSON")
                    ?.let(::parseFormula)
                if (formula != null) found = true
                ingredient.copy(formula = formula)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed = true
                ingredient
            }
        }
        val status = when {
            found -> SourceStatus.OK
            failed -> SourceStatus.FAILED
            else -> SourceStatus.NOT_FOUND
        }
        return result.copy(ingredients = ingredients, pubchem = status)
    }

    private suspend fun withUsedFor(result: LookupResult, rxcuis: List<String>): LookupResult = try {
        val summary = rxcuis.distinct().firstNotNullOfOrNull { id ->
            getOrNull("$MEDLINE?mainSearchCriteria.v.cs=2.16.840.1.113883.6.88&mainSearchCriteria.v.c=$id&knowledgeResponseType=application/json")
                ?.let(::parseMedlineSummary)
        }
        if (summary != null) result.copy(usedFor = summary, medline = SourceStatus.OK)
        else result.copy(medline = SourceStatus.NOT_FOUND)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        result.copy(medline = SourceStatus.FAILED)
    }

    /** Body for 2xx, null for 404, IOException for anything else. */
    private suspend fun getOrNull(url: String): String? {
        val response = fetch(url)
        return when {
            response.code in 200..299 -> response.body
            response.code == 404 -> null
            else -> throw IOException("HTTP ${response.code} for $url")
        }
    }

    private fun enc(text: String) = URLEncoder.encode(text.trim(), "UTF-8")
    private fun encPath(text: String) = enc(text).replace("+", "%20")

    private companion object {
        const val RXNAV = "https://rxnav.nlm.nih.gov/REST"
        const val PUBCHEM = "https://pubchem.ncbi.nlm.nih.gov/rest/pug"
        const val MEDLINE = "https://connect.medlineplus.gov/service"
    }
}
