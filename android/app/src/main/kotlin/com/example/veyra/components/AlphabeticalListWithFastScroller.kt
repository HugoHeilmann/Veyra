package com.example.veyra.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veyra.model.Section
import kotlinx.coroutines.launch
import kotlin.collections.forEach
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max



/**
 * Liste + index alphabétique à DROITE (côte à côte, pas d’overlay).
 * Si l’écran est trop petit, on sous-échantillonne l’index (A, C, E, …, Z)
 * et le mapping des clics/drag suit exactement les lettres AFFICHÉES.
 */
@Composable
fun <T> AlphabeticalListWithFastScroller(
    sections: List<Section<T>>,
    itemContent: @Composable (T) -> Unit,
    headerContent: @Composable (String) -> Unit,
    listState: LazyListState
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 1) Indices de début de section (O(n))
    val headerStartIndices = remember(sections) {
        val out = ArrayList<Int>(sections.size)
        var acc = 0
        sections.forEach { s ->
            out += acc
            acc += 1 + s.items.size // 1 header + N items
        }
        out
    }

    // 2) Labels
    val allLabels = remember(sections) { sections.map { it.label } }

    // 3) État scroller
    var scrollerHeightPx by remember { mutableIntStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    var previewLabel by remember { mutableStateOf<String?>(null) }
    var previewCenterY by remember { mutableFloatStateOf(0f) }

    // 4) Affichage compressé des labels
    val (displayLabels, displayToRealIndex) = remember(allLabels, scrollerHeightPx) {
        calculateDisplayLabelsAndMapping(allLabels, scrollerHeightPx, density)
    }

    // 5) Throttle: ne scroller que si la cible change
    var lastTargetSection by remember { mutableIntStateOf(-1) }

    // 6) Un seul job de scroll à la fois
    var scrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    fun launchScroll(toIndex: Int) {
        if (toIndex < 0) return
        scrollJob?.cancel()
        scrollJob = scope.launch {
            listState.scrollToItem(toIndex)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            // --- Liste ---
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                userScrollEnabled = !isDragging // évite les conflits pendant le drag
            ) {
                sections.forEachIndexed { _, section ->
                    item(
                        key = "header_${section.label}",
                        contentType = "header"
                    ) {
                        headerContent(section.label)
                    }
                    items(
                        count = section.items.size,
                        key = { i -> "item_${section.label}_$i" },
                        contentType = { "item" }
                    ) { i ->
                        itemContent(section.items[i])
                    }
                }
            }

            // --- Index alphabétique ---
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight()
                    .padding(end = 6.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.small
                    )
                    .onGloballyPositioned { scrollerHeightPx = it.size.height }
                    // TAP: une seule animation
                    .pointerInput(displayLabels) {
                        detectTapGestures { offset ->
                            if (scrollerHeightPx <= 0 || displayLabels.isEmpty()) return@detectTapGestures
                            val idx = ((offset.y / scrollerHeightPx) * displayLabels.size)
                                .toInt().coerceIn(0, displayLabels.lastIndex)
                            val realIdx = displayToRealIndex[idx]
                            previewLabel = allLabels[realIdx]
                            isDragging = false
                            lastTargetSection = -1
                            val listIndex = headerStartIndices[realIdx]
                            launchScroll(listIndex)
                        }
                    }
                    // DRAG: scrollToItem instantané + throttle
                    .pointerInput(displayLabels) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                lastTargetSection = -1
                            },
                            onDragEnd = {
                                isDragging = false
                                previewLabel = null
                                lastTargetSection = -1
                            },
                            onDragCancel = {
                                isDragging = false
                                previewLabel = null
                                lastTargetSection = -1
                            }
                        ) { change, _ ->
                            change.consume()
                            if (scrollerHeightPx <= 0 || displayLabels.isEmpty()) return@detectDragGestures
                            val y = change.position.y.coerceIn(0f, scrollerHeightPx.toFloat())
                            previewCenterY = y
                            val idx = ((y / scrollerHeightPx) * displayLabels.size)
                                .toInt().coerceIn(0, displayLabels.lastIndex)
                            val realIdx = displayToRealIndex[idx]
                            if (realIdx != lastTargetSection) {
                                lastTargetSection = realIdx
                                previewLabel = allLabels[realIdx]
                                val listIndex = headerStartIndices[realIdx]
                                launchScroll(listIndex)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    displayLabels.forEach { lbl ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Bulle d’aperçu
        if (isDragging && previewLabel != null && scrollerHeightPx > 0) {
            val offsetY = with(density) {
                (previewCenterY - scrollerHeightPx / 2f).toDp()
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-56).dp, y = offsetY)
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        shape = MaterialTheme.shapes.large
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = previewLabel!!,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

private fun calculateDisplayLabelsAndMapping(
    allLabels: List<String>,
    scrollerHeightPx: Int,
    density: Density
): Pair<List<String>, List<Int>> {
    // Cas trivial : pas de taille / pas de labels -> identité
    if (scrollerHeightPx <= 0 || allLabels.isEmpty()) {
        return allLabels to allLabels.indices.toList()
    }

    val minSlotPx = with(density) { 12.dp.toPx() }
    val maxVisible = max(1, floor(scrollerHeightPx / minSlotPx).toInt())

    // Si tout tient, on affiche tout, mapping 1:1
    if (allLabels.size <= maxVisible) {
        return allLabels to allLabels.indices.toList()
    }

    // Sinon, on sous-échantillonne en sautant régulièrement des labels
    val step = ceil(allLabels.size / maxVisible.toFloat()).toInt().coerceAtLeast(1)

    val indices = mutableListOf<Int>()
    var i = 0
    while (i < allLabels.size) {
        indices += i
        i += step
    }

    // On force le dernier point à mapper sur le dernier label réel
    if (indices.last() != allLabels.lastIndex) {
        indices[indices.lastIndex] = allLabels.lastIndex
    }

    val labels = indices.map { allLabels[it] }
    return labels to indices
}
