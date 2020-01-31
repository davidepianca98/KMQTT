package socket

expect open class Socket : SocketImpl {

    override fun send(data: UByteArray)

    override fun sendRemaining()

    override fun read(): UByteArray?
}

open class IOException(message: String? = null) : Exception(message)
class SocketClosedException : IOException()
