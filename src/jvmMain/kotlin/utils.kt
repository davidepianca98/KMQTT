import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
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

fun main() {
    Broker().listen()
}
