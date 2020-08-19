package socket.udp

import close
import getEagain
import getErrno
import getEwouldblock
import getPortFromSockaddrIn
import inet_ntop
import inet_pton
import kotlinx.cinterop.*
import memset
import platform.posix.AF_INET
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import recvfrom
import sendto
import sockaddrIn
import socket.tcp.IOException
import socket.tcp.SocketClosedException
import socklen_tVar

actual class UDPSocket(private val socket: Int) {

    private val buffer = ByteArray(2048)

    actual fun send(data: UByteArray, address: String, port: Int) {
        memScoped {
            val serverAddress = sockaddrIn(AF_INET.convert(), port.convert())
            inet_pton(AF_INET, address, serverAddress.sin_addr.ptr)

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
                    throw IOException("Failed sendto error: ${getErrno()}")
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
                            getPortFromSockaddrIn(peerAddress).toInt()
                        )
                    }
                    else -> {
                        val error = getErrno()
                        if (error != getEagain() && error != getEwouldblock()) {
                            close(socket)
                            throw IOException("Recvfrom error: $error")
                        } else {
                            return null
                        }
                    }
                }
            }
        }
    }

}
