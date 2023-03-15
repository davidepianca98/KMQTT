import platform.posix.AF_INET
import platform.posix.SOCK_STREAM
import socket.tls.TLSClientEngine
import socket.tls.TLSSocket

actual class TLSClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    readTimeOut: Int
) : TLSSocket(socket(AF_INET, SOCK_STREAM, 0), TLSClientEngine(), null, ByteArray(maximumPacketSize))
