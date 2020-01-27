package socket

import Broker
import ClientConnection
import kotlinx.cinterop.*
import platform.posix.*
import platform.posix.WSACleanup
import platform.posix.WSAStartup
import platform.windows.*
import platform.windows.AF_INET
import platform.windows.SOCK_STREAM
import platform.windows.accept
import platform.windows.bind
import platform.windows.listen
import platform.windows.socket

actual class ServerSocket actual constructor(
    host: String,
    private val port: Int,
    private val backlog: Int,
    private val broker: Broker
) {

    actual fun run() {
        memScoped {
            val wsaData = alloc<WSADATA>()
            if (WSAStartup(0x0202u, wsaData.ptr) != 0)
                throw IOException("Failed WSAStartup")

            val serverSocket = socket(AF_INET, SOCK_STREAM, 0)
            if (serverSocket == INVALID_SOCKET) {
                WSACleanup()
                throw IOException("Invalid socket")
            }

            val serverAddress = alloc<sockaddr_in>()
            memset(serverAddress.ptr, 0, sockaddr_in.size.convert())
            serverAddress.sin_family = AF_INET.convert()
            serverAddress.sin_addr.S_un.S_addr = posix_htons(0).convert()
            serverAddress.sin_port = posix_htons(port.convert()).convert()

            if (bind(serverSocket, serverAddress.ptr.reinterpret(), sockaddr_in.size.convert()) == SOCKET_ERROR) {
                WSACleanup()
                throw IOException("Failed bind")
            }

            val on = alloc<uint32_tVar>()
            on.value = 1u
            if (platform.posix.ioctlsocket(
                    serverSocket,
                    platform.posix.FIONBIO.toInt(),
                    on.ptr.reinterpret()
                ) == SOCKET_ERROR
            ) {
                WSACleanup()
                throw IOException("Failed ioctlsocket")
            }

            if (listen(serverSocket, backlog) == SOCKET_ERROR) {
                WSACleanup()
                throw IOException("Failed listen")
            }

            val clients = mutableMapOf<SOCKET, ClientConnection>()
            val buffer = ByteArray(broker.maximumPacketSize.toInt())

            val readfds = alloc<fd_set>()
            while (true) {
                posix_FD_ZERO(readfds.ptr)
                posix_FD_SET(serverSocket.convert(), readfds.ptr)
                clients.forEach {
                    posix_FD_SET(it.key.convert(), readfds.ptr)
                }
                select(0, readfds.ptr, null, null, null)
                if (posix_FD_ISSET(serverSocket.convert(), readfds.ptr) == 1) {
                    val newSocket = accept(serverSocket, null, null)
                    if (newSocket == INVALID_SOCKET) {
                        throw IOException("Invalid socket")
                    }
                    clients[newSocket] = ClientConnection(Socket(newSocket), broker)
                } else {
                    clients.forEach { socket ->
                        if (posix_FD_ISSET(socket.key.convert(), readfds.ptr) == 1) {
                            posix_FD_SET(socket.key.convert(), readfds.ptr)
                            buffer.usePinned { pinned ->
                                val length =
                                    platform.posix.recv(socket.key.convert(), pinned.addressOf(0), buffer.size, 0)
                                when {
                                    length == 0 -> {
                                        // TODO disconnection
                                        clients.remove(socket.key)
                                        platform.posix.shutdown(socket.key, SD_SEND)
                                        platform.posix.closesocket(socket.key)
                                    }
                                    length > 0 -> {
                                        socket.value.dataReceived(pinned.get().toUByteArray().copyOfRange(0, length))
                                    }
                                    else -> {
                                        // TODO error
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    actual fun close() {
        WSACleanup()
    }

}
