package io.github.davidepianca98

import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import platform.posix.timeval

@OptIn(UnsafeNumber::class)
public actual fun currentTimeMillis(): Long {
    memScoped {
        val timeStruct = alloc<timeval>()
        gettimeofday(timeStruct)
        return timeStruct.tv_sec.toLong() * 1000 + timeStruct.tv_usec.toLong() / 1000
    }
}
