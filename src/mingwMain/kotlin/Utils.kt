import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import platform.windows.FILETIME
import platform.windows.GetSystemTimeAsFileTime

actual fun currentTimeMillis(): Long {
    memScoped {
        val systemTime = alloc<FILETIME>()
        GetSystemTimeAsFileTime(systemTime.ptr)
        val millisFrom1601 = (((systemTime.dwHighDateTime shl 32) + systemTime.dwLowDateTime)) / 10000u
        return (millisFrom1601 - 116444736000000000u).toLong()
    }
}

actual fun runCoroutine(block: suspend () -> Unit) {
    runBlocking {
        block()
    }
}

actual suspend fun launchCoroutine(block: suspend () -> Unit) {
    coroutineScope {
        launch {
            block()
        }
    }
}
