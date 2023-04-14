package socket.udp

internal expect class UDPSocket {

    fun send(data: UByteArray, address: String, port: Int)

    fun read(): UDPReadData?
}
