import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun currentTimeMillis(): Long {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

actual fun runCoroutine(block: suspend CoroutineScope.() -> Unit) {
    runBlocking {
        block()
    }
}
