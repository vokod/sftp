package com.awolity.secftp.view.knownhosts

data class KnownHost(
    val address: String,
    val type: String,
    val key: String
)