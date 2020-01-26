package socket

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual class ServerSocket actual constructor(host: String, port: Int, backlog: Int) {

    private val socket = AsynchronousServerSocketChannel.open()

    init {
        socket.bind(InetSocketAddress(host, port), backlog)
    }

    actual suspend fun accept(): Socket {
        val socket = socket.saccept()
        return Socket(socket)
    }

    private suspend fun AsynchronousServerSocketChannel.saccept() = suspendCoroutine<AsynchronousSocketChannel> { c ->
        this.accept(Unit, object : CompletionHandler<AsynchronousSocketChannel, Unit> {
            override fun completed(result: AsynchronousSocketChannel, attachment: Unit) =
                Unit.apply { c.resume(result) }

            override fun failed(exc: Throwable, attachment: Unit) = Unit.apply { c.resumeWithException(exc) }
        })
    }

    actual fun close() {
        socket.close()
    }

}
