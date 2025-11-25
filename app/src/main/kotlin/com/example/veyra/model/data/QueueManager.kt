package com.example.veyra.model.data

import androidx.compose.runtime.*
import com.example.veyra.model.Music

object QueueManager {

    private val _queue = mutableStateListOf<Music>()
    val queue: List<Music>
        get() = _queue

    private var _currentIndex by mutableStateOf(-1)
    val currentIndex: Int
        get() = _currentIndex

    val hasCurrent: Boolean
        get() = _currentIndex in queue.indices

    var isLaunched by mutableStateOf(false)

    /**
     * Remplace entierement la file de lecture
     *
     * @param musics liste de morceaux a mettre dans la file
     * @param startIndex index du morceau a considerer comme "courant"
     */
    fun setQueue(musics: List<Music>, startIndex: Int = 0) {
        _queue.clear()
        _queue.addAll(musics)

        _currentIndex = if (_queue.isEmpty()) -1
        else startIndex.coerceIn(0, _queue.lastIndex)
    }

    /**
     * Vide completement la file de lecture
     */
    fun clearQueue() {
        _queue.clear()
        _currentIndex = -1
    }

    /**
     * Renvoie le morceau courant
     */
    fun getCurrent(): Music? = _queue.getOrNull(_currentIndex)

    /**
     * Renvoie une copie immuable de la file
     */
    fun getAll(): List<Music> = _queue.toList()

    /**
     * Force la selection d'un index dans la file
     * et renvoie le morceau correspondant
     */
    fun playFromIndex(index: Int): Music? {
        if (index !in _queue.indices) return null
        _currentIndex = index
        return _queue[_currentIndex]
    }

    /**
     * Passe au morceau suivant
     */
    fun getNext(): Music? {
        if (_queue.isEmpty()) return null

        val nextIndex = _currentIndex + 1
        if (nextIndex !in _queue.indices) return null

        _currentIndex = nextIndex
        return _queue[_currentIndex]
    }

    /**
     * Passe au morceau precedent
     */
    fun getPrevious(): Music? {
        if (_queue.isEmpty()) return null

        val previousIndex = _currentIndex - 1
        if (previousIndex !in _queue.indices) return null

        _currentIndex = previousIndex
        return _queue[_currentIndex]
    }

    /**
     * Ajoute le morceau a la fin de la file
     * Si la file etait vide, il devient le morceau courant
     */
    fun addToEnd(music: Music) {
        _queue.add(music)

        if (_currentIndex == -1) {
            _currentIndex = 0
        }
    }

    /**
     * Ajoute le morceau juste apres le courant
     */
    fun insertNext(music: Music) {
        if (_queue.isEmpty() || _currentIndex !in _queue.indices) {
            _queue.add(music)
            _currentIndex = 0
            return
        }

        val insertIndex = (_currentIndex + 1).coerceAtMost(_queue.size)
        _queue.add(insertIndex, music)
    }

    /**
     * Supprime un element a un index donne
     */
    fun removeAt(index: Int) {
        if (index !in _queue.indices) return

        _queue.removeAt(index)

        when {
            _queue.isEmpty() -> {
                _currentIndex = -1
            }
            index < _currentIndex -> {
                _currentIndex -= 1
            }
            index == _currentIndex -> {
                _currentIndex = _currentIndex.coerceAtMost(_queue.lastIndex)
            }
        }
    }

    /**
     * Deplace un element dans la file
     *
     * @param fromIndex index de depart
     * @param toIndex index d'arrivee
     */
    fun move(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in _queue.indices || toIndex !in _queue.indices) return
        if (fromIndex == toIndex) return

        val item = _queue.removeAt(fromIndex)
        _queue.add(toIndex, item)

        _currentIndex = when (_currentIndex) {
            fromIndex -> toIndex
            in (fromIndex + 1)..toIndex -> _currentIndex - 1
            in toIndex..<fromIndex -> _currentIndex + 1
            else -> _currentIndex
        }
    }
}