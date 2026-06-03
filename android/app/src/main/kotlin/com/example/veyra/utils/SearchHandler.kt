package com.example.veyra.utils

import com.example.veyra.model.Music
import java.text.Normalizer

fun filterResults(list: List<Music>, searchText: String): List<Music> {
    val query = normalizeForSearch(searchText)

    if (query.isBlank()) return list

    return list.filter { music ->
        fuzzyContains(music.name, query) ||
        fuzzyContains(music.artist, query) ||
        fuzzyContains(music.album, query)
    }
}

private fun normalizeForSearch(value: String): String {
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "") // Remove accents
        .lowercase()
        .replace("\\s+".toRegex(), "") // Remove spaces
        .trim()
}

private fun fuzzyContains(value: String?, query: String): Boolean {
    if (value.isNullOrBlank()) return false

    val target = normalizeForSearch(value)

    // Cas simples : pas d'accents, pas d'espaces
    if (target.contains(query)) return true

    // Évite les faux positifs sur les recherches trop courtes
    if (query.length < 3) return false

    // Tolérance : oubli, ajout ou remplacement d'une lettre
    return containsWithOneError(target, query)
}

private fun containsWithOneError(target: String, query: String): Boolean {
    val lengthsToTest = listOf(
        query.length - 1,
        query.length,
        query.length + 1
    ).filter { it > 0 }

    for (length in lengthsToTest) {
        if (length > target.length) continue

        for (i in 0..target.length - length) {
            val part = target.substring(i, i + length)

            if (levenshteinDistance(part, query) <= 1) {
                return true
            }
        }
    }

    return false
}

private fun levenshteinDistance(a: String, b: String): Int {
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }

    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j

    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1

            dp[i][j] = minOf(
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1,
                dp[i - 1][j - 1] + cost
            )
        }
    }

    return dp[a.length][b.length]
}

fun updateArtistMap(list: List<Music>): Map<String, List<Music>> {
    return list
        .filter { !it.artist.isNullOrBlank() }
        .groupBy {
            it.artist!!
                .replace(Regex("\\s+(ft\\.?|feat\\.?|featuring)\\s+.*", RegexOption.IGNORE_CASE), "")
                .trim()
        }
}

fun updateAlbumMap(list: List<Music>): Map<String, List<Music>> {
    return list
        .filter { !it.album.isNullOrBlank() }
        .groupBy { it.album!! }
}