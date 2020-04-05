package socket.udp

import kotlinx.cinterop.*
import platform.linux.inet_ntop
import platform.posix.*
import socket.tcp.IOException
import socket.tcp.SocketClosedException

actual class UDPSocket(private val socket: Int) {

    private val buffer = ByteArray(2048)

    actual fun send(data: UByteArray, address: String, port: Int) {
        memScoped {
            val serverAddress = alloc<sockaddr_in>()
            memset(serverAddress.ptr, 0, sockaddr_in.size.convert())
            serverAddress.sin_family = AF_INET.convert()
            serverAddress.sin_addr.s_addr = INADDR_ANY
            serverAddress.sin_port = posix_htons(port.convert()).convert()

            data.toByteArray().usePinned { pinned ->
                if (sendto(
                        socket,
                        pinned.addressOf(0),
                        data.size.toULong(),
                        0,
                        serverAddress.reinterpret<sockaddr>().ptr,
                        sockaddr_in.size.convert()
                    ) == -1L
                ) {
                    throw IOException("Failed sendto error: $errno")
                }
            }
        }
    }

    actual fun read(): UDPReadData? {
        memScoped {
            buffer.usePinned { pinned ->
                val peerAddress = alloc<sockaddr_in>()
                memset(peerAddress.ptr, 0, sockaddr_in.size.convert())
                val addressLen = alloc<socklen_tVar>()
                val length = recvfrom(
                    socket,
                    pinned.addressOf(0),
                    buffer.size.convert(),
                    0,
                    peerAddress.reinterpret<sockaddr>().ptr,
                    addressLen.ptr
                )
                when {
                    length == 0L -> {
                        close(socket)
                        throw SocketClosedException()
                    }
                    length > 0 -> {
                        val address = ByteArray(30)
                        address.usePinned { addressBuf ->
                            inet_ntop(AF_INET, peerAddress.sin_addr.ptr, addressBuf.addressOf(0), addressLen.value)
                        }
                        return UDPReadData(
                            pinned.get().toUByteArray().copyOfRange(0, length.toInt()),
                            address.toKString(),
                            peerAddress.sin_port.toInt()
                        )
                    }
                    else -> {
                        if (errno != EAGAIN && errno != EWOULDBLOCK) {
                            close(socket)
                            throw IOException("Recv error: $errno")
                        } else {
                            return null
                        }
                    }
                }
            }
        }
    }

}
