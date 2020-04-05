package socket.tcp

import socket.SocketInterface

expect open class Socket : SocketInterface {

    override fun send(data: UByteArray)

    override fun sendRemaining()

    override fun read(): UByteArray?

    override fun close()
}

open class IOException(message: String? = null) : Exception(message)
class SocketClosedException : IOException()
