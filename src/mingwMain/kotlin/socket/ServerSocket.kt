package socket

import kotlinx.cinterop.*
import platform.posix.INVALID_SOCKET
import platform.posix.SOCKET_ERROR
import platform.posix.WSADATA
import platform.posix.memset
import platform.windows.*

actual class ServerSocket actual constructor(host: String, private val port: Int, private val backlog: Int) {

    private var serverSocket: ULong = INVALID_SOCKET

    init {
        memScoped {
            val wsaData = alloc<WSADATA>()
            if (WSAStartup(0x0202u, wsaData.ptr) != 0)
                throw IOException("Failed WSAStartup")

            val hints = alloc<addrinfo>()
            memset(hints.ptr, 0, addrinfo.size.toULong())
            hints.ai_family = AF_INET
            hints.ai_socktype = SOCK_STREAM
            hints.ai_protocol = IPPROTO_TCP
            hints.ai_flags = AI_PASSIVE

            val result = allocPointerTo<addrinfo>()
            if (getaddrinfo(null, port.toString(), hints.ptr, result.ptr) != 0)
                throw IOException("Failed getaddrinfo")

            val addrinfoResult = result.pointed!!
            serverSocket = socket(addrinfoResult.ai_family, addrinfoResult.ai_socktype, addrinfoResult.ai_protocol)
            if (serverSocket == INVALID_SOCKET) {
                val error = WSAGetLastError()
                freeaddrinfo(result.value)
                WSACleanup()
                throw IOException("socket failed with error $error")
            }

            if (bind(serverSocket, addrinfoResult.ai_addr, addrinfoResult.ai_addrlen.toInt()) == SOCKET_ERROR) {
                val error = WSAGetLastError()
                freeaddrinfo(result.value)
                closesocket(serverSocket)
                WSACleanup()
                throw IOException("bind failed with error $error")
            }

            freeaddrinfo(result.value)

            if (listen(serverSocket, backlog) == SOCKET_ERROR) {
                val error = WSAGetLastError()
                closesocket(serverSocket)
                WSACleanup()
                throw IOException("listen failed with error $error")
            }
        }
    }

    actual fun accept(): Socket {
        val clientSocket = accept(serverSocket, null, null)
        if (clientSocket == INVALID_SOCKET) {
            val error = WSAGetLastError()
            closesocket(serverSocket)
            WSACleanup()
            throw IOException("accept failed with error $error")
        }
        return Socket(clientSocket)
    }

    actual fun close() {
        closesocket(serverSocket)
        WSACleanup()
    }

}
