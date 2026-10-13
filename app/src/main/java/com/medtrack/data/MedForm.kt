package com.medtrack.data

/** What kind of medicine it is, so the app can say "2 puffs" rather than "1 tablet". */
enum class MedForm(val label: String, val dosePresets: List<String>) {
    TABLET("Tablet", listOf("1 tablet", "½ tablet", "2 tablets")),
    CAPSULE("Capsule", listOf("1 capsule", "2 capsules")),
    SYRUP("Syrup", listOf("5 ml", "10 ml")),
    INHALER("Inhaler", listOf("1 puff", "2 puffs")),
    ROTACAP("Rotacap", listOf("1 capsule inhaled", "2 capsules inhaled")),
    NEBULISER("Nebuliser", listOf("1 respule in nebuliser", "½ respule in nebuliser")),
    DROPS("Drops", listOf("1 drop", "2 drops")),
    INJECTION("Injection", listOf("1 injection")),
    CREAM("Cream or gel", listOf("Apply thinly")),
    OTHER("Other", emptyList()),
}

/**
 * The form named in an Indian pack name ("Budecort 0.5mg Respules 2ml" → nebuliser), or null when it doesn't say.
 * Inhaled forms are checked first, so "Rotacap" isn't mistaken for a capsule and "Respirator Solution" for a syrup.
 */
fun guessForm(packName: String): MedForm? {
    val name = packName.lowercase()
    fun has(vararg words: String) = words.any { it in name }
    return when {
        has("respule", "nebul", "respirator solution", "respiratory solution") -> MedForm.NEBULISER
        has("rotacap", "transcap", "respicap", "redicap", "octacap", "dry powder") -> MedForm.ROTACAP
        has("inhaler", "transhaler", "autohaler", "synchrobreathe", "evohaler", "accuhaler", "metered", "aerocap") -> MedForm.INHALER
        has("injection", "vial", "prefilled syringe", "infusion") -> MedForm.INJECTION
        has("drop") -> MedForm.DROPS
        has("syrup", "suspension", "oral solution", "expectorant", "elixir", "linctus") -> MedForm.SYRUP
        has("cream", "gel", "ointment", "lotion") -> MedForm.CREAM
        has("capsule", "softgel") -> MedForm.CAPSULE
        has("tablet") -> MedForm.TABLET
        else -> null
    }
}
