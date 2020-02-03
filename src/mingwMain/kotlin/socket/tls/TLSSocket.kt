package socket.tls

import platform.posix.SOCKET
import socket.Socket

actual class TLSSocket(
    private val socket: SOCKET,
    private val writeRequest: MutableList<SOCKET>,
    private val buffer: ByteArray
) : Socket(socket, writeRequest, buffer)
