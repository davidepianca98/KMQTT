package socket

expect class UDPSocket {

    fun send(data: UByteArray, address: String, port: Int)

    fun read(): UDPReadData?
}
