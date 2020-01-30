package socket

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix.*

actual class Socket(
    private val socket: SOCKET,
    private val writeRequest: MutableList<SOCKET>,
    private val buffer: ByteArray
) {

    private val pendingSendData = mutableListOf<UByteArray>()

    actual fun send(data: UByteArray) {
        data.toByteArray().usePinned { pinned ->
            val length = send(socket, pinned.addressOf(0), data.size, 0)
            if (length == SOCKET_ERROR) {
                val error = WSAGetLastError()
                if (error == WSAEWOULDBLOCK) {
                    pendingSendData.add(data)
                    writeRequest.add(socket)
                } else {
                    closesocket(socket)
                    throw IOException("Error in send $error")
                }
            } else if (length < data.size) {
                pendingSendData.add(data.copyOfRange(length, data.size))
                writeRequest.add(socket)
            } else {

            }
        }
    }

    actual fun sendRemaining() {
        pendingSendData.forEach {
            send(it)
        }
    }

    actual fun read(): UByteArray? {
        buffer.usePinned { pinned ->
            val length = recv(socket.convert(), pinned.addressOf(0), buffer.size, 0)
            when {
                length == 0 -> {
                    shutdown(socket, SD_SEND)
                    closesocket(socket)
                    throw SocketClosedException()
                }
                length > 0 -> {
                    return pinned.get().toUByteArray().copyOfRange(0, length)
                }
                else -> {
                    if (WSAGetLastError() != WSAEWOULDBLOCK) {
                        shutdown(socket, SD_SEND)
                        closesocket(socket)
                        throw IOException()
                    } else {
                        return null
                    }
                }
            }
        }
    }

}
