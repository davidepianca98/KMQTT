package socket

expect class Socket {

    fun send(data: UByteArray)

    fun sendRemaining()

    fun read(): UByteArray?
}

open class IOException(message: String? = null) : Exception(message)
class SocketClosedException : IOException()
