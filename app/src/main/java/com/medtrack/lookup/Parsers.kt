package com.medtrack.lookup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class Suggestion(val rxcui: String, val name: String)

data class RelatedConcepts(val ingredients: List<Suggestion>, val clinicalDrugNames: List<String>)

private val json = Json { ignoreUnknownKeys = true }

private fun parse(body: String): JsonObject? = json.parseToJsonElement(body) as? JsonObject
private fun JsonElement?.obj(key: String): JsonElement? = (this as? JsonObject)?.get(key)
private fun JsonElement?.array(key: String): List<JsonElement> = (obj(key) as? JsonArray).orEmpty()
private fun JsonElement?.str(key: String): String? = (obj(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

/** RxNorm approximateTerm: one suggestion per rxcui, preferring the RxNorm-sourced name. */
internal fun parseSuggestions(body: String): List<Suggestion> {
    val byRxcui = LinkedHashMap<String, MutableList<JsonElement>>()
    parse(body).obj("approximateGroup").array("candidate").forEach { candidate ->
        val rxcui = candidate.str("rxcui") ?: return@forEach
        byRxcui.getOrPut(rxcui) { mutableListOf() } += candidate
    }
    return byRxcui.mapNotNull { (rxcui, candidates) ->
        val name = candidates.firstOrNull { it.str("source") == "RXNORM" }?.str("name")
            ?: candidates.firstNotNullOfOrNull { it.str("name") }
            ?: return@mapNotNull null
        Suggestion(rxcui, name.lowercase())
    }
}

internal fun parseRxcui(body: String): String? =
    (parse(body).obj("idGroup").array("rxnormId").firstOrNull() as? JsonPrimitive)?.content

internal fun parseRelated(body: String): RelatedConcepts {
    val groups = parse(body).obj("relatedGroup").array("conceptGroup")
    fun concepts(tty: String) = groups.filter { it.str("tty") == tty }.flatMap { it.array("conceptProperties") }
    return RelatedConcepts(
        ingredients = concepts("IN").mapNotNull { c ->
            val rxcui = c.str("rxcui")
            val name = c.str("name")
            if (rxcui != null && name != null) Suggestion(rxcui, name) else null
        },
        clinicalDrugNames = concepts("SCD").mapNotNull { it.str("name") },
    )
}

private const val UNIT = """(?:MG|MCG|G|ML|UNT|MEQ|%)(?:/(?:ML|MG|ACTUAT|HR|G))?"""

/** Strengths of [ingredient] found in clinical drug names such as "acetaminophen 450 MG / caffeine 30 MG Oral Tablet". */
internal fun parseStrengths(ingredient: String, clinicalDrugNames: List<String>): String? {
    val pattern = Regex("""(?:^|\s)${Regex.escape(ingredient)} ([\d.]+ $UNIT)""", RegexOption.IGNORE_CASE)
    val found = clinicalDrugNames
        .flatMap { name -> pattern.findAll(name).map { it.groupValues[1].uppercase() }.toList() }
        .distinct()
    if (found.isEmpty()) return null
    return found
        .sortedWith(compareBy<String>({ it.substringAfter(' ') }, { it.substringBefore(' ').toDoubleOrNull() ?: 0.0 }))
        .joinToString(", ")
}

internal fun parseFormula(body: String): String? =
    parse(body).obj("PropertyTable").array("Properties").firstOrNull().str("MolecularFormula")

internal fun parseMedlineSummary(body: String): String? {
    val raw = parse(body).obj("feed").array("entry")
        .firstNotNullOfOrNull { it.obj("summary").str("_value")?.takeIf(String::isNotBlank) }
        ?: return null
    return shortSummary(raw)
}

/** Strips HTML and keeps the first two sentences, capped at [maxChars]. */
internal fun shortSummary(html: String, maxChars: Int = 300): String {
    val text = html
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace(Regex("\\s+"), " ")
        .trim()
    val twoSentences = text.split(Regex("(?<=[.!?])\\s+")).take(2).joinToString(" ")
    return if (twoSentences.length <= maxChars) twoSentences
    else twoSentences.take(maxChars).substringBeforeLast(' ') + "…"
}
