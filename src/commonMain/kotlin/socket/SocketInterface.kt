package socket

interface SocketInterface {

    fun send(data: UByteArray)

    fun sendRemaining()

    fun read(): UByteArray?
}
