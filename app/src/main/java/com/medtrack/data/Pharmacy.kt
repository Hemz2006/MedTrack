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
