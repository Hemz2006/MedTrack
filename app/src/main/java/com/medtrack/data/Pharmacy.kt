package com.medtrack.data

import java.net.URLEncoder

/** Online pharmacies a medication can be ordered from: each opens on its search results for the medication. */
enum class Pharmacy(val label: String, private val searchBase: String) {
    TATA_1MG("Tata 1mg", "https://www.1mg.com/search/all?name="),
    PHARMEASY("PharmEasy", "https://pharmeasy.in/search/all?name="),
    NETMEDS("Netmeds", "https://www.netmeds.com/products?q="),
    ;

    /** The search page for [name], e.g. "Dolo 650" (spaces as %20, which all three sites read correctly). */
    fun searchUrl(name: String): String = searchBase + URLEncoder.encode(name.trim(), "UTF-8").replace("+", "%20")
}

/** A name to search the pharmacies by: [label] says which name it is, [term] is what gets searched. */
data class OrderSearch(val label: String, val term: String)

/**
 * The names a medication can be searched by: the name on the pack, then the generic name (or, without one, the
 * ingredients). A pharmacy may not stock the brand but sell the same composition under another name.
 */
fun MedicationEntity.orderSearches(): List<OrderSearch> {
    val brand = brandName?.trim()?.takeIf { it.isNotEmpty() }
    val generic = genericName.ifBlank { ingredients.joinToString(" + ") { it.name } }
        // "Aprepitant + Aprepitant" searches as "Aprepitant".
        .split("+").map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
        .joinToString(" + ").takeIf { it.isNotEmpty() }
    return buildList {
        brand?.let { add(OrderSearch("Name on the pack", it)) }
        if (generic != null && !generic.equals(brand, ignoreCase = true)) add(OrderSearch("Generic name", generic))
    }
}
