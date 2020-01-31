package socket

interface SocketImpl {

    fun send(data: UByteArray)

    fun sendRemaining()

    fun read(): UByteArray?
}
