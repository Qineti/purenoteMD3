package com.example.data.model

enum class SortField {
    CREATED_AT,
    UPDATED_AT,
    TITLE
}

enum class SortDirection {
    ASC,
    DESC
}

data class SortOption(
    val field: SortField = SortField.UPDATED_AT,
    val direction: SortDirection = SortDirection.DESC
)
