package socket

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.SOCKET
import platform.posix.SOCKET_ERROR
import platform.windows.WSAGetLastError

actual class Socket(private val socket: SOCKET) {

    actual fun send(data: UByteArray) { // TODO maybe if not sent the whole length we must request write and then retry
        data.toByteArray().usePinned { pinned ->
            if (platform.posix.send(socket, pinned.addressOf(0), data.size, 0) == SOCKET_ERROR) {
                throw IOException("Error in send ${WSAGetLastError()}")
            }
        }
    }

}
