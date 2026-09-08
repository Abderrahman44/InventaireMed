package org.abdat.inventory

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform