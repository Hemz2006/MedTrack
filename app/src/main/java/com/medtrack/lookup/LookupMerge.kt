package com.medtrack.lookup

import com.medtrack.data.MedicationEntity
import com.medtrack.data.SourceStatus

/**
 * Copies looked-up fields onto the medication. Sources that haven't run are ignored.
 * With [onlyMissing], sources already OK on the medication are left alone (a retry keeps the user's edits).
 * A failed or not-found source only updates its status; existing field values are kept.
 */
fun MedicationEntity.applyLookup(result: LookupResult, onlyMissing: Boolean): MedicationEntity {
    fun take(current: SourceStatus, incoming: SourceStatus) =
        incoming != SourceStatus.NOT_RUN && !(onlyMissing && current == SourceStatus.OK)

    var m = this
    if (take(m.rxnormStatus, result.rxnorm)) {
        m = if (result.rxnorm == SourceStatus.OK) {
            m.copy(rxcui = result.rxcui, ingredients = result.ingredients, rxnormStatus = SourceStatus.OK)
        } else {
            m.copy(rxnormStatus = result.rxnorm)
        }
    }
    if (take(m.pubchemStatus, result.pubchem)) {
        m = if (result.pubchem == SourceStatus.OK) {
            val formulas = result.ingredients.associate { it.name.lowercase() to it.formula }
            m.copy(
                ingredients = m.ingredients.map { i -> formulas[i.name.lowercase()]?.let { i.copy(formula = it) } ?: i },
                pubchemStatus = SourceStatus.OK,
            )
        } else {
            m.copy(pubchemStatus = result.pubchem)
        }
    }
    if (take(m.medlineStatus, result.medline)) {
        m = if (result.medline == SourceStatus.OK) {
            m.copy(usedFor = result.usedFor, medlineStatus = SourceStatus.OK)
        } else {
            m.copy(medlineStatus = result.medline)
        }
    }
    return m
}
