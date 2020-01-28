import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import platform.posix.gettimeofday
import platform.posix.timeval

actual fun currentTimeMillis(): Long {
    memScoped {
        val tv = alloc<timeval>()
        gettimeofday(tv.ptr, null)
        return (tv.tv_sec * 1000) + (tv.tv_usec / 1000)
    }
}

actual fun runCoroutine(block: suspend CoroutineScope.() -> Unit) {
    runBlocking {
        block()
    }
}
