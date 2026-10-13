package com.medtrack.lookup

import org.junit.Assert.assertEquals
import org.junit.Test

class BrandRankingTest {
    private fun c(name: String, discontinued: Boolean = false) = Candidate(Brand(name, "x", null), discontinued)

    @Test fun keyIgnoresCaseSpacesAndPunctuation() = assertEquals("pan40", brandKey(" PAN-40 "))

    @Test fun wholeWordMatchesComeFirstThenOnSaleThenShorter() {
        val ranked = rankBrands("dolo", listOf(c("Dologel"), c("Dolo 650 Tablet", discontinued = true), c("Dolo 500 Tablet"), c("Dolo Drops")))
        assertEquals(listOf("Dolo Drops", "Dolo 500 Tablet", "Dolo 650 Tablet", "Dologel"), ranked.map { it.name })
    }

    @Test fun duplicatesCollapse() =
        assertEquals(1, rankBrands("pan", listOf(c("PAN 40 Tablet"), c("Pan 40 tablet"))).size)
}
