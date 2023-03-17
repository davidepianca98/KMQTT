import kotlinx.cinterop.*
import platform.posix.*
import socket.IOException
import socket.tls.TLSClientEngine
import socket.tls.TLSSocket

actual class TLSClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    readTimeOut: Int
) : TLSSocket(
    socketsInit().run {
        socket(AF_INET, SOCK_STREAM, 0)
    },
    TLSClientEngine(),
    null,
    ByteArray(maximumPacketSize)
) {

    init {
        memScoped {
            val ip = getaddrinfo(address, port.toString()) ?: throw IOException("Failed resolving address")

            if (connect(socket, ip, sizeOf<sockaddr_in>().convert()) == -1) {
                throw IOException("Socket connect failed, error ${getErrno()}")
            }

            if (set_socket_timeout(socket, readTimeOut.toLong()) == -1) {
                socketsCleanup()
                throw IOException("Failed setsockopt for timeout with error ${getErrno()}")
            }
        }
    }
}
