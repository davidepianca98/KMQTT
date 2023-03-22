import socket.tls.TLSSocket

expect class TLSClientSocket(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    readTimeOut: Int,
    tlsSettings: TLSClientSettings,
    checkCallback: () -> Unit
) : TLSSocket {

    val handshakeComplete: Boolean
}