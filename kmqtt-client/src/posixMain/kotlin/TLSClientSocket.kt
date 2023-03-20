import kotlinx.cinterop.*
import platform.posix.*
import socket.IOException
import socket.tls.TLSClientEngine
import socket.tls.TLSSocket

actual class TLSClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    private val readTimeOut: Int
) : TLSSocket(
    socketsInit().run {
        socket(AF_INET, SOCK_STREAM, 0)
    },
    TLSClientEngine(),
    null,
    ByteArray(maximumPacketSize)
) {

    private val readfds = nativeHeap.alloc<fd_set>()

    init {
        memScoped {
            val ip = getaddrinfo(address, port.toString()) ?: throw IOException("Failed resolving address")

            if (connect(socket, ip, sizeOf<sockaddr_in>().convert()) == -1) {
                throw IOException("Socket connect failed, error ${getErrno()}")
            }

            if (set_non_blocking(socket) == -1) {
                socketsCleanup()
                throw IOException("Failed ioctlsocket for non blocking with error ${getErrno()}")
            }
        }
    }

    override fun read(): UByteArray? {
        memScoped {
            posix_FD_ZERO(readfds.ptr)
            posix_FD_SET(socket.convert(), readfds.ptr)

            select(
                socket + 1,
                readfds.ptr,
                null,
                null,
                readTimeOut.toLong()
            )

            if (posix_FD_ISSET(socket.convert(), readfds.ptr) == 1) {
                return super.read()
            } else {
                return null
            }
        }
    }
}
