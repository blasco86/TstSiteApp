package org.tstsite.tstsiteapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform