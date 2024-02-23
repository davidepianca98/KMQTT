import socket.tls.TLSClientSettings
import socket.tls.TLSSocket

public expect class TLSClientSocket(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    readTimeOut: Int,
    tlsSettings: TLSClientSettings,
    checkCallback: () -> Unit
) : TLSSocket {

    public val handshakeComplete: Boolean
}