package com.example.veyra.model

data class Section<T>(
    val label: String,
    val items: List<T>
)