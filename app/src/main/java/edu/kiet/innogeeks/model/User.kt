package edu.kiet.innogeeks.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    var isSelected: Boolean = false
)
