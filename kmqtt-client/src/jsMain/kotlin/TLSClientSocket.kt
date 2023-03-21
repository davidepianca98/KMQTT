import socket.tls.TLSSocket

actual class TLSClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    readTimeOut: Int,
    tlsSettings: TLSClientSettings
) : TLSSocket(tls.connect(port, address), { _, _ -> true }) // TODO use timeout
{
    actual val handshakeComplete: Boolean
        get() = TODO("Not yet implemented")
}