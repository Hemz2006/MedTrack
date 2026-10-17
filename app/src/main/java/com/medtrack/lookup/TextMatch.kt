package com.medtrack.lookup

/** A word on a medicine pack (or other photo) that might be a medicine name, with the number just after it (often the strength). */
data class ReadName(val word: String, val strength: String?)

/** What one word in the photo most likely is: the closest brands, best first. */
data class TextMatch(val read: String, val brands: List<Brand>)

/** Words that are never medicine names: dosage forms, timing, and the headings printed on packs and forms. */
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
    // Printed on packs: release types, makers' words and storage notes.
    "gastro", "resistant", "release", "prolonged", "extended", "sustained", "modified", "controlled", "film", "coated",
    "chewable", "dispersible", "oral", "healthcare", "pharma", "pharmaceuticals", "laboratories", "labs", "limited",
    "ltd", "store", "below", "keep", "reach", "children", "protect", "light", "moisture", "manufactured", "marketed",
    "only", "schedule", "warning", "contains", "each", "strip", "batch", "expiry", "price", "inclusive", "taxes",
)

/**
 * The words in a photo's text worth looking up: three letters or more, not a dosage form or timing word, with the
 * number after each kept as a strength hint ("Pan 40" → "Pan", "40").
 */
fun possibleNames(text: String): List<ReadName> {
    val found = mutableListOf<ReadName>()
    for (line in text.lines()) {
        // Hyphens and slashes split words too: "Posoxil-GR 300" is "Posoxil", "GR", "300".
        val tokens = line.split(Regex("[\\s,;:()\\[\\]\\-/®™]+")).map { it.trim('.', '*', '\'', '"') }.filter { it.isNotEmpty() }
        tokens.forEachIndexed { i, token ->
            val letters = token.filter { it.isLetter() }
            if (letters.length < 3 || letters.length != token.length) return@forEachIndexed
            if (letters.lowercase() in NOT_MEDICINES) return@forEachIndexed
            // The strength is the number just after, or after a short code such as "GR" or "SR" ("Posoxil-GR 300").
            fun number(t: String?) = t?.takeWhile { it.isDigit() || it == '.' }?.takeIf { it.any(Char::isDigit) }
            val next = tokens.getOrNull(i + 1)
            val isCode = next != null && next.length <= 3 && next.all { it.isLetter() } && next.lowercase() !in NOT_MEDICINES
            val strength = number(next) ?: if (isCode) number(tokens.getOrNull(i + 2)) else null
            found += ReadName(letters, strength)
        }
    }
    return found.distinctBy { it.word.lowercase() to it.strength }
}

/** The strengths printed in [text], as written in compositions: "300 mg" and "300mg" both give "300mg". */
fun strengthsIn(text: String): Set<String> =
    Regex("(\\d+(?:\\.\\d+)?)\\s*(mg|mcg|g|ml|iu)\\b", RegexOption.IGNORE_CASE).findAll(text)
        .map { it.groupValues[1] + it.groupValues[2].lowercase() }.toSet()

/** Dosage forms as brand names spell them, in the order to check. */
private val FORMS = listOf("tablet", "capsule", "syrup", "suspension", "injection", "drops", "cream", "gel", "ointment", "inhaler", "rotacap")

/** The dosage form printed in [text] ("Tablets" → "tablet"), or null when none is. */
fun formIn(text: String): String? = FORMS.firstOrNull { text.contains(it, ignoreCase = true) }

/**
 * Brands whose composition names [generic] (a generic name printed on the pack, e.g. "Posaconazole"), best first:
 * the user's own medications, then the pack's [form] (tablets, not injections), then a strength printed on the pack
 * ([strengths]), then single-ingredient medicines, brands still on sale, and shorter names.
 */
internal fun brandsWithIngredient(generic: String, strengths: Set<String>, form: String?, candidates: List<Candidate>, limit: Int = 3): List<Brand> =
    candidates
        .filter { it.brand.composition.contains(generic, ignoreCase = true) }
        .sortedWith(
            compareBy<Candidate>(
                { !it.brand.saved },
                { form != null && !it.brand.name.contains(form, ignoreCase = true) },
                { c -> strengths.none { c.brand.composition.replace(" ", "").contains(it, ignoreCase = true) } },
                { it.brand.composition.count { ch -> ch == '+' } },
                { it.discontinued },
                { it.brand.name.length },
                { it.brand.name },
            ),
        )
        .map { it.brand }
        .distinctBy { it.name.lowercase() to it.composition.lowercase() }
        .take(limit)

/**
 * Joins brand-name matches with matches from generic names printed on the pack. Packs nearly always print the
 * generic name, so when one was found ([generic] isn't empty), brand matches that don't contain it are dropped: a
 * word like "Gastro" matching an unrelated brand is a false lead.
 */
internal fun combineMatches(byBrand: List<TextMatch>, generic: List<TextMatch>): List<TextMatch> {
    if (generic.isEmpty()) return byBrand
    val printed = generic.map { it.read }
    val agreeing = byBrand.mapNotNull { match ->
        val kept = match.brands.filter { brand -> printed.any { brand.composition.contains(it, ignoreCase = true) } }
        kept.takeIf { it.isNotEmpty() }?.let { match.copy(brands = it) }
    }
    return (agreeing + generic).distinctBy { it.brands.first().name.lowercase() }
}

/** Letters that handwriting recognition often mistakes for one another. */
private val LOOKALIKES = listOf("adopq", "hmn", "ijl", "ce", "uv")

/** [key] with each lookalike letter replaced by its group's first, so "qolo" and "dolo" compare equal. */
internal fun lookalikeKey(key: String): String =
    key.map { ch -> LOOKALIKES.firstOrNull { ch in it }?.first() ?: ch }.joinToString("")

/**
 * The first three letters of [word]'s key, plus versions with one letter swapped for a lookalike (for "Polo",
 * also "dol" and "ool"), since the brand search starts from those letters and handwriting often blurs them.
 */
internal fun searchPrefixes(word: String): List<String> {
    val prefix = brandKey(word).take(3)
    if (prefix.length < 3) return listOf(prefix)
    return buildList {
        add(prefix)
        prefix.forEachIndexed { i, ch ->
            LOOKALIKES.filter { ch in it }.flatMap { it.toList() }.distinct().filter { it != ch }.forEach { other ->
                add(prefix.substring(0, i) + other + prefix.substring(i + 1))
            }
        }
    }.distinct()
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
 * The brands whose first word is closest to [read], allowing a few misread letters (letters that look alike in
 * handwriting, such as "q" for "d" or "h" for "m", don't count), best first: the fewest misreads, then the closest
 * spelling, then the user's own medications, a matching strength, brands still on sale, and shorter names.
 */
internal fun closestBrands(read: ReadName, candidates: List<Candidate>, limit: Int = 3): List<Brand> {
    val key = brandKey(read.word)
    val loose = lookalikeKey(key)
    val allowed = allowedMisreads(key.length)
    return candidates
        .map { candidate ->
            val head = headKey(candidate.brand.name)
            Triple(candidate, editDistance(loose, lookalikeKey(head)), editDistance(key, head))
        }
        .filter { it.second <= allowed }
        .sortedWith(
            compareBy<Triple<Candidate, Int, Int>>(
                { it.second },
                { it.third },
                { !it.first.brand.saved },
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
