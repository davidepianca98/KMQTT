import kotlinx.cinterop.*
import platform.posix.*
import socket.IOException
import socket.tls.TLSSocket

actual class TLSClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    private val readTimeOut: Int,
    tlsSettings: TLSClientSettings
) : TLSSocket(
    socketsInit().run {
        socket(AF_INET, SOCK_STREAM, 0)
    },
    TLSClientEngine(tlsSettings),
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

    actual val handshakeComplete: Boolean
        get() = engine.isInitFinished
}
