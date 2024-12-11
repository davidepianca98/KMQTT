package io.github.davidepianca98

import kotlinx.cinterop.*
import platform.posix.*
import io.github.davidepianca98.socket.IOException
import io.github.davidepianca98.socket.tcp.Socket

public actual class ClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    private val readTimeOut: Int,
    private val connectTimeOut: Int,
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

            if (set_send_socket_timeout(socket, connectTimeOut.convert()) == -1) {
                socketsCleanup()
                throw IOException("Socket connect timeout set failed, error ${getErrno()}")
            }

            if (connect(socket, ip, sizeOf<sockaddr_in>().convert()) == -1) {
                socketsCleanup()
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

            if (fdIsSet(socket.convert(), readfds.ptr) != 0) {
                return super.read()
            } else {
                return null
            }
        }
    }
}