import socket.tcp.Socket

actual class ClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    readTimeOut: Int
) : Socket(net.Socket(), { _, _ -> true }) {

    init {
        socket.connect(port, address)
    }
}
