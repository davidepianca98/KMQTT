package io.github.davidepianca98.socket.udp

import io.github.davidepianca98.close
import io.github.davidepianca98.getEagain
import io.github.davidepianca98.getErrno
import io.github.davidepianca98.getEwouldblock
import io.github.davidepianca98.inet_ntop
import io.github.davidepianca98.inet_pton
import io.github.davidepianca98.getPortFromSockaddrIn
import kotlinx.cinterop.*
import io.github.davidepianca98.memset
import platform.posix.AF_INET
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import io.github.davidepianca98.recvfrom
import io.github.davidepianca98.sendto
import io.github.davidepianca98.sockaddrIn
import io.github.davidepianca98.socket.IOException
import io.github.davidepianca98.socket.SocketClosedException
import io.github.davidepianca98.socklen_tVar

internal actual class UDPSocket(private val socket: Int) {

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
                        sizeOf<sockaddr_in>().convert()
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
                memset(peerAddress.ptr, 0, sizeOf<sockaddr_in>().convert())
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
