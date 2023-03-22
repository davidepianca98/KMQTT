import socket.tcp.Socket

expect class ClientSocket(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    readTimeOut: Int,
    checkCallback: () -> Unit
) : Socket
