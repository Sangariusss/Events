package com.komiker.events.data.models

sealed class TagItem {

    data class Header(val name: String) : TagItem()
    data class SubTag(val name: String, val category: String) : TagItem()
}