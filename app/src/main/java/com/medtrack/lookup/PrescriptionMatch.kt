package com.medtrack.lookup

/** A word on a prescription that might be a medicine, with the number written just after it (often the strength). */
data class ReadName(val word: String, val strength: String?)

/** What one word on the prescription most likely is: the closest brands, best first. */
data class PrescriptionMatch(val read: String, val brands: List<Brand>)

/** Words on prescriptions that are never medicine names: dosage forms, timing, and the form's own headings. */
private val NOT_MEDICINES = setOf(
    "tab", "tabs", "tablet", "tablets", "cap", "caps", "capsule", "capsules", "syp", "syrup", "susp", "suspension",
    "inj", "injection", "oint", "ointment", "drop", "drops", "cream", "gel", "lotion", "powder", "sachet", "inhaler",
    "after", "before", "food", "meal", "meals", "breakfast", "lunch", "dinner", "daily", "morning", "night", "evening",
    "noon", "afternoon", "bedtime", "day", "days", "week", "weeks", "month", "months", "once", "twice", "thrice", "times",
    "with", "and", "the", "for", "take", "dose", "doses", "half", "empty", "stomach", "water", "hours", "hrs",
    "patient", "name", "age", "sex", "male", "female", "date", "doctor", "hospital", "clinic", "address", "phone",
    "mobile", "sign", "signature", "advice", "advised", "review", "follow", "diagnosis", "weight", "blood", "pressure",
    "sugar", "test", "tests", "report", "regd", "mbbs", "consultant", "physician", "continue", "stop", "apply",
    "times", "each", "every", "when", "needed", "required", "pain", "fever", "cough", "cold",
)

/**
 * The words on a prescription worth looking up: three letters or more, not a dosage form or timing word, with the
 * number after each kept as a strength hint ("Pan 40" → "Pan", "40").
 */
fun namesInPrescription(text: String): List<ReadName> {
    val found = mutableListOf<ReadName>()
    for (line in text.lines()) {
        val tokens = line.split(Regex("[\\s,;:()\\[\\]]+")).map { it.trim('.', '-', '/', '*', '\'', '"') }.filter { it.isNotEmpty() }
        tokens.forEachIndexed { i, token ->
            val letters = token.filter { it.isLetter() }
            if (letters.length < 3 || letters.length != token.length) return@forEachIndexed
            if (letters.lowercase() in NOT_MEDICINES) return@forEachIndexed
            val strength = tokens.getOrNull(i + 1)?.takeWhile { it.isDigit() || it == '.' }?.takeIf { it.any(Char::isDigit) }
            found += ReadName(letters, strength)
        }
    }
    return found.distinctBy { it.word.lowercase() to it.strength }
}

/** The search key of a brand's first word: "PAN 40 Tablet" → "pan". */
internal fun headKey(brandName: String): String = brandKey(brandName.trim().substringBefore(' '))

/** How many letters may be misread for a word of [length] letters and still count as a match. */
internal fun allowedMisreads(length: Int): Int = when {
    length <= 4 -> 0
    length <= 6 -> 1
    else -> 2
}

/**
 * The brands whose first word is closest to [read], allowing a few misread letters, best first: the fewest
 * misreads, then a matching strength, then brands still on sale, then shorter names.
 */
internal fun closestBrands(read: ReadName, candidates: List<Candidate>, limit: Int = 3): List<Brand> {
    val key = brandKey(read.word)
    val allowed = allowedMisreads(key.length)
    return candidates
        .map { it to editDistance(key, headKey(it.brand.name)) }
        .filter { (_, misreads) -> misreads <= allowed }
        .sortedWith(
            compareBy<Pair<Candidate, Int>>(
                { it.second },
                { read.strength == null || !it.first.brand.name.contains(read.strength) },
                { it.first.discontinued },
                { it.first.brand.name.length },
                { it.first.brand.name },
            ),
        )
        .map { it.first.brand }
        .distinctBy { it.name.lowercase() to it.composition.lowercase() }
        .take(limit)
}

/** Levenshtein distance: the fewest single-letter changes that turn [a] into [b]. */
internal fun editDistance(a: String, b: String): Int {
    var previous = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        val current = IntArray(b.length + 1)
        current[0] = i
        for (j in 1..b.length) {
            current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
        }
        previous = current
    }
    return previous[b.length]
}
