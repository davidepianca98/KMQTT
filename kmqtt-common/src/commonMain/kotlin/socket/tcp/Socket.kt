package socket.tcp

import socket.SocketInterface

public expect open class Socket : SocketInterface {

    override fun send(data: UByteArray)

    override fun sendRemaining()

    override fun read(): UByteArray?

    override fun close()
}