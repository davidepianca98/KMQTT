import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

actual fun currentTimeMillis(): Long {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
