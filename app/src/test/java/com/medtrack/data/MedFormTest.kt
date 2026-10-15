package com.medtrack.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MedFormTest {
    @Test fun guessesRespiratoryFormsFromPackNames() {
        assertEquals(MedForm.NEBULISER, guessForm("Budecort 0.5mg Respules 2ml"))
        assertEquals(MedForm.NEBULISER, guessForm("Ipravent Respirator Solution"))
        assertEquals(MedForm.ROTACAP, guessForm("Foracort 100 Rotacap"))
        assertEquals(MedForm.INHALER, guessForm("Asthalin 100mcg Inhaler"))
        assertEquals(MedForm.INHALER, guessForm("Seroflo 125 Synchrobreathe"))
    }

    @Test fun guessesEverydayForms() {
        assertEquals(MedForm.TABLET, guessForm("Dolo 650 Tablet"))
        assertEquals(MedForm.CAPSULE, guessForm("Becosules Capsule"))
        assertEquals(MedForm.SYRUP, guessForm("Ascoril LS Syrup"))
        assertEquals(MedForm.DROPS, guessForm("Dolo Drops"))
        assertEquals(MedForm.INJECTION, guessForm("Mecobal Injection"))
        assertNull(guessForm("Mecmaxol"))
    }

    @Test fun doseLineCombinesFormAndDose() {
        val med = newMedication(LocalDate.parse("2026-10-13"), Instant.EPOCH)
        assertNull(med.doseLineOn(LocalDate.parse("2026-10-13")))
        assertEquals("Inhaler · 2 puffs", med.copy(form = MedForm.INHALER, doseText = "2 puffs").doseLineOn(LocalDate.parse("2026-10-13")))
        assertEquals("1 tablet", med.copy(doseText = "1 tablet").doseLineOn(LocalDate.parse("2026-10-13")))
    }
}
