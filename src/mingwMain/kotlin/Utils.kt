import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.windows.FILETIME
import platform.windows.GetSystemTimeAsFileTime

actual fun currentTimeMillis(): Long {
    memScoped {
        val systemTime = alloc<FILETIME>()
        GetSystemTimeAsFileTime(systemTime.ptr)
        val millisFrom1601 = (systemTime.dwHighDateTime.toULong() shl 32) + systemTime.dwLowDateTime.toULong()
        return ((millisFrom1601 - 116444736000000000u) / 10000u).toLong()
    }
}
