package com.medtrack.lookup

import com.medtrack.data.Ingredient

private val TRAILING_STRENGTH = Regex("""^(.*\S)\s*\(([^()]*)\)\s*$""")
private val BARE_STRENGTH = Regex("""^(.*?\D)\s+(\d[\d.]*\s*(?:mg|mcg|µg|g|ml|iu|%)(?:\s*/\s*\d*\s*\w+)?)$""", RegexOption.IGNORE_CASE)
private val PARENTHESES = Regex("""\s*\([^()]*\)""")

/**
 * "Amoxycillin (500mg)" or "Amoxycillin 500 mg" → Ingredient("Amoxycillin", "500mg"). The last bracket is the strength
 * when it starts with a number, so aliases such as "Vitamin B6 (Pyridoxine)" or "Thiamine(Vitamin B1)" stay part of the name.
 */
fun parseIngredient(text: String): Ingredient? {
    val clean = text.trim().replace(Regex("""\s+"""), " ")
    if (clean.isEmpty()) return null
    val match = TRAILING_STRENGTH.matchEntire(clean)
        ?: return BARE_STRENGTH.matchEntire(clean)?.let { Ingredient(it.groupValues[1].trim(), it.groupValues[2].replace(" ", "")) }
        ?: Ingredient(clean)
    val (name, inside) = match.destructured
    return when {
        inside.equals("NA", ignoreCase = true) -> Ingredient(name)
        inside.firstOrNull()?.isDigit() == true -> Ingredient(name, inside)
        else -> Ingredient(clean)
    }
}

/** Splits "Calcium Carbonate (500mg) + Calcitriol (0.25mcg)" (or a comma-separated list) into ingredients. */
fun parseComposition(text: String): List<Ingredient> {
    val parts = mutableListOf<String>()
    var depth = 0
    val current = StringBuilder()
    for (ch in text) {
        when {
            ch == '(' -> depth++
            ch == ')' -> depth = maxOf(0, depth - 1)
        }
        if ((ch == '+' || ch == ',') && depth == 0) {
            parts += current.toString()
            current.clear()
        } else {
            current.append(ch)
        }
    }
    parts += current.toString()
    return parts.mapNotNull(::parseIngredient)
}

fun List<Ingredient>.formatComposition(): String =
    joinToString(" + ") { i -> i.strength?.let { "${i.name} ($it)" } ?: i.name }

/** The name to search online: "Vitamin B6 (Pyridoxine)" → "Vitamin B6". */
internal fun Ingredient.searchName(): String = name.replace(PARENTHESES, "").trim().ifEmpty { name }
