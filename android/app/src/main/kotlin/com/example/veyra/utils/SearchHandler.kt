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
        .flatMap { music ->
            extractArtists(music.artist!!).map { artist ->
                artist to music
            }
        }
        .groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )
}

private fun extractArtists(artist: String): List<String> {
    val parts = artist.split(
        Regex("(?i)\\s+(ft\\.?|feat\\.?|featuring)\\s+")
    )

    val mainArtist = parts.first().trim()

    val feats = parts
        .drop(1)
        .flatMap { it.split("&") }
        .map { it.trim() }
        .filter { it.isNotBlank() }

    return listOf(mainArtist) + feats
}

fun updateAlbumMap(list: List<Music>): Map<String, List<Music>> {
    return list
        .filter { !it.album.isNullOrBlank() }
        .groupBy { it.album!! }
}