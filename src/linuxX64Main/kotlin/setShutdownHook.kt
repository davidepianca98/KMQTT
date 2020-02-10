import kotlinx.cinterop.staticCFunction
import platform.posix.SIGINT
import platform.posix.SIGTERM
import platform.posix.signal

actual fun setShutdownHook(hook: () -> Unit) {
    val pointer = staticCFunction<Int, Unit> {
        try {
            // Not yet possible to call hook in native
        } catch (e: Throwable) {

        }
    }
    signal(SIGINT, pointer)
    signal(SIGTERM, pointer)
}
