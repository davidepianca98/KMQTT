import kotlinx.cinterop.*
import platform.posix.*
import socket.IOException
import socket.tcp.Socket

actual class ClientSocket actual constructor(address: String, port: Int, maximumPacketSize: Int, readTimeOut: Int)
    : Socket(socket(AF_INET, SOCK_STREAM, 0), null, ByteArray(maximumPacketSize)) {

    init {
        memScoped {
            socketsInit()
            val serverAddress = sockaddrIn(AF_INET.convert(), port.convert())
            inet_pton(AF_INET, address, serverAddress.sin_addr.ptr)

            if (connect(socket, serverAddress.reinterpret<sockaddr>().ptr, sizeOf<sockaddr_in>().convert()) != 0) {
                throw IOException()
            }

            val on = alloc<timeval>()
            on.tv_sec = 0
            on.tv_usec = readTimeOut * 1000
            if (setsockopt(socket, SOL_SOCKET, SO_RCVTIMEO, on.ptr, sizeOf<timeval>().toUInt()) == -1) {
                socketsCleanup()
                throw IOException("Failed ioctlsocket")
            }
        }
    }
}