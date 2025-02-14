package edu.kiet.innogeeks.model

import com.google.common.collect.DiscreteDomain

data class person(
    val name: String,
    val email: String,
    val branch: String?=null,
    val domain: String?=null,
    val libraryId: String?=null,
)

