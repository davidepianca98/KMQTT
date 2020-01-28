package socket

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.*

actual class Socket(private val socket: SOCKET, private val writeRequest: MutableList<SOCKET>) {

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

    fun sendRemaining() {
        pendingSendData.forEach {
            send(it)
        }
    }

}
