import socket.tls.TLSSocket

actual class TLSClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    readTimeOut: Int
) : TLSSocket(tls.connect(port, address), { _, _ -> true }) // TODO use timeout