package edu.kiet.innogeeks.model

data class UserData(
    val uid: String,
    val email: String,
    val name: String,
    val role: String, // "admin", "user", "coordinator", "student"
    val domain: String,  // Will be "none" for admin/user
    val libraryId: String
)

