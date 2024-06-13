import socket.tcp.Socket

public expect class ClientSocket(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    readTimeOut: Int,
    connectTimeOut: Int,
    checkCallback: () -> Unit
) : Socket
