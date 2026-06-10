package com.jacksonfdam.beam

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform