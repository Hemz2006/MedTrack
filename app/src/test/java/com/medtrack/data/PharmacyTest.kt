package com.medtrack.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PharmacyTest {
    @Test fun searchesByNameWithSpacesEncoded() {
        assertEquals("https://www.1mg.com/search/all?name=Dolo%20650", Pharmacy.TATA_1MG.searchUrl("Dolo 650"))
        assertEquals("https://pharmeasy.in/search/all?name=Dolo%20650", Pharmacy.PHARMEASY.searchUrl(" Dolo 650 "))
        assertEquals("https://www.netmeds.com/products?q=Dolo%20650", Pharmacy.NETMEDS.searchUrl("Dolo 650"))
    }

    @Test fun encodesCharactersThatWouldBreakTheAddress() {
        // A composition name can hold "+", "&" or "/", which must not end or split the search term.
        assertEquals(
            "https://www.1mg.com/search/all?name=Paracetamol%20%2B%20Caffeine%20%26%2010%2F5",
            Pharmacy.TATA_1MG.searchUrl("Paracetamol + Caffeine & 10/5"),
        )
    }
}
