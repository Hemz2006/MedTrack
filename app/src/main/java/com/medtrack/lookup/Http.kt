package com.medtrack.lookup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.Reader
import javax.net.ssl.HttpsURLConnection
import java.net.URL

data class HttpResponse(val code: Int, val body: String)

/** Performs a GET. Throws IOException when the network is unavailable. */
typealias Fetch = suspend (url: String) -> HttpResponse

/** The lookups' replies are a few kilobytes; anything far larger is refused rather than read into memory. */
internal const val MAX_RESPONSE_CHARS = 2_000_000

/** Reads all of [reader], or throws [IOException] once it passes [max] characters. */
internal fun readCapped(reader: Reader, max: Int = MAX_RESPONSE_CHARS): String {
    val out = StringBuilder()
    val buffer = CharArray(8 * 1024)
    while (true) {
        val n = reader.read(buffer)
        if (n < 0) return out.toString()
        if (out.length + n > max) throw IOException("Response larger than $max characters")
        out.appendRange(buffer, 0, n)
    }
}

val urlConnectionFetch: Fetch = { url ->
    withContext(Dispatchers.IO) {
        // Encrypted connections only: the lookups go to fixed https addresses, and a plain-http one is a mistake.
        val connection = URL(url).openConnection() as? HttpsURLConnection ?: throw IOException("Not an https address")
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            HttpResponse(code, stream?.bufferedReader()?.use { readCapped(it) } ?: "")
        } finally {
            connection.disconnect()
        }
    }
}
