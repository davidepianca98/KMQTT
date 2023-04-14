import kotlinx.cinterop.staticCFunction
import platform.windows.*

internal actual fun setShutdownHook(hook: () -> Unit) {

    val pointer = staticCFunction<DWORD, WINBOOL> {
        try {
            // Not yet possible to call hook in native
        } catch (_: Throwable) {

        }
        FALSE
    }

    if (SetConsoleCtrlHandler(pointer, TRUE) == 0)
        throw Exception("Failed setting CTRL C handler")
}
