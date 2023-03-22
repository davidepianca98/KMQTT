import kotlinx.cinterop.*
import platform.posix.*
import socket.IOException
import socket.tcp.Socket

actual class ClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    private val readTimeOut: Int,
    checkCallback: () -> Unit
) : Socket(
    socketsInit().run {
        socket(AF_INET, SOCK_STREAM, 0)
    },
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
            fdZero(readfds.ptr)
            fdSet(socket.convert(), readfds.ptr)

            select(
                socket + 1,
                readfds.ptr,
                null,
                null,
                readTimeOut.toLong()
            )

            if (fdIsSet(socket.convert(), readfds.ptr) == 1) {
                return super.read()
            } else {
                return null
            }
        }
    }
}