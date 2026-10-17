package com.medtrack.lookup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** A brand as sold in India, with its composition as printed on the pack. [saved] = from the user's own medications. */
data class Brand(val name: String, val composition: String, val manufacturer: String?, val saved: Boolean = false)

/**
 * Searches the offline list of Indian brands in assets/brands.db (built by tools/build_brand_index.py).
 * The asset is copied to app storage once per install or update, because SQLite can't read inside the APK.
 * Without the asset (a fresh clone that hasn't built it), searches return nothing.
 */
class BrandIndex(private val context: Context) {
    private val mutex = Mutex()
    private var db: SQLiteDatabase? = null
    private var unavailable = false

    suspend fun search(term: String, limit: Int = 8): List<Brand> = withContext(Dispatchers.IO) {
        val key = brandKey(term)
        if (key.length < 2) return@withContext emptyList()
        val database = open() ?: return@withContext emptyList()
        val candidates = database.rawQuery(
            "SELECT b.name, c.text, m.name, b.discontinued FROM brands b " +
                "JOIN compositions c ON c.id = b.composition LEFT JOIN makers m ON m.id = b.maker " +
                "WHERE b.key >= ? AND b.key < ? LIMIT 400",
            arrayOf(key, "$key{"), // '{' sorts just after 'z', and keys hold only [a-z0-9]
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(Candidate(Brand(c.getString(0), c.getString(1), c.getString(2)), c.getInt(3) == 1))
            }
        }
        rankBrands(term, candidates).take(limit)
    }

    /**
     * The likely medicines in [text] read from a photo (a pack or strip): for each word that could be a name, the brands it is closest
     * to, allowing a few misread letters. [saved] are the user's own medications, matched too (and first): they
     * cover brands missing from the list. Words that match nothing are left out. The generic name printed on the
     * pack is used too: it rules out brand matches with other ingredients, and adds brands with that ingredient
     * (the pack's form and strength first), which covers brand names in a stylised logo that can't be read.
     */
    suspend fun matchText(text: String, saved: List<Brand> = emptyList()): List<TextMatch> = withContext(Dispatchers.IO) {
        val database = open()
        val own = saved.map { Candidate(it, discontinued = false) }
        val byBrand = possibleNames(text).mapNotNull { read ->
            val listed = database?.let { db ->
                searchPrefixes(read.word).flatMap { prefix ->
                    db.rawQuery(
                        "SELECT b.name, c.text, m.name, b.discontinued FROM brands b " +
                            "JOIN compositions c ON c.id = b.composition LEFT JOIN makers m ON m.id = b.maker " +
                            "WHERE b.key >= ? AND b.key < ? LIMIT 3000",
                        arrayOf(prefix, "$prefix{"),
                    ).use { c ->
                        buildList {
                            while (c.moveToNext()) add(Candidate(Brand(c.getString(0), c.getString(1), c.getString(2)), c.getInt(3) == 1))
                        }
                    }
                }
            }.orEmpty()
            closestBrands(read, own + listed).takeIf { it.isNotEmpty() }?.let { TextMatch(read.word, it) }
        }.distinctBy { match -> match.brands.first().name.lowercase() }
        combineMatches(byBrand, matchGeneric(database, text, own))
    }

    /** Brands containing a generic name printed in [text] (words of 6 letters or more), the printed strength first. */
    private fun matchGeneric(database: SQLiteDatabase?, text: String, own: List<Candidate>): List<TextMatch> {
        val strengths = strengthsIn(text)
        val form = formIn(text)
        return possibleNames(text).filter { it.word.length >= 6 }.mapNotNull { read ->
            val listed = database?.rawQuery(
                "SELECT b.name, c.text, m.name, b.discontinued FROM compositions c " +
                    "JOIN brands b ON b.composition = c.id LEFT JOIN makers m ON m.id = b.maker " +
                    "WHERE c.text LIKE ? LIMIT 3000",
                arrayOf("%${read.word}%"),
            )?.use { c ->
                buildList {
                    while (c.moveToNext()) add(Candidate(Brand(c.getString(0), c.getString(1), c.getString(2)), c.getInt(3) == 1))
                }
            }.orEmpty()
            brandsWithIngredient(read.word, strengths, form, own + listed).takeIf { it.isNotEmpty() }?.let { TextMatch(read.word, it) }
        }.distinctBy { match -> match.brands.first().name.lowercase() }
    }

    private suspend fun open(): SQLiteDatabase? = mutex.withLock {
        db?.let { return it }
        if (unavailable) return null
        try {
            val file = File(context.noBackupFilesDir, "brands.db")
            val installed = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
            if (!file.exists() || file.lastModified() < installed) {
                val tmp = File(file.path + ".tmp")
                context.assets.open("brands.db").use { input -> tmp.outputStream().use { input.copyTo(it) } }
                check(tmp.renameTo(file)) { "Couldn't move the brand list into place" }
            }
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).also { db = it }
        } catch (e: java.io.FileNotFoundException) {
            unavailable = true
            null
        } catch (e: Exception) {
            Log.e("MedTrack", "Couldn't open the brand list", e)
            null
        }
    }
}

internal data class Candidate(val brand: Brand, val discontinued: Boolean)

/** Lower-case letters and digits only, so "PAN-40", "Pan 40" and "pan40" match. */
internal fun brandKey(text: String): String = text.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }

/**
 * Best first: names where the typed text is a whole word ("Dolo" → "Dolo 650 Tablet" before "Dologel"),
 * then brands still on sale, then shorter names.
 */
internal fun rankBrands(term: String, candidates: List<Candidate>): List<Brand> {
    val typed = term.trim().lowercase()
    fun wholeWord(name: String): Boolean {
        val lower = name.lowercase()
        return lower.startsWith(typed) && (lower.length == typed.length || !lower[typed.length].isLetter())
    }
    return candidates
        .sortedWith(compareBy<Candidate>({ !wholeWord(it.brand.name) }, { it.discontinued }, { it.brand.name.length }, { it.brand.name }))
        .map { it.brand }
        .distinctBy { it.name.lowercase() to it.composition.lowercase() }
}
