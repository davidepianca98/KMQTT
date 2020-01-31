package socket

import kotlinx.cinterop.*
import mqtt.Broker
import mqtt.ClientConnection
import platform.posix.*
import platform.windows.select


actual open class ServerSocket actual constructor(
    host: String,
    private val port: Int,
    private val backlog: Int,
    private val broker: Broker
) {

    private var running = true
    private var serverSocket = INVALID_SOCKET

    private val clients = mutableMapOf<SOCKET, ClientConnection>()
    private val writeRequest = mutableListOf<SOCKET>()

    private val buffer = ByteArray(broker.maximumPacketSize.toInt())

    private val readfds = nativeHeap.alloc<fd_set>()
    private val writefds = nativeHeap.alloc<fd_set>()
    private val errorfds = nativeHeap.alloc<fd_set>()

    init {
        memScoped {
            val wsaData = alloc<WSADATA>()
            if (WSAStartup(0x0202u, wsaData.ptr) != 0)
                throw IOException("Failed WSAStartup")

            serverSocket = socket(AF_INET, SOCK_STREAM, 0)
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
            if (ioctlsocket(serverSocket, FIONBIO.toInt(), on.ptr.reinterpret()) == SOCKET_ERROR) {
                WSACleanup()
                throw IOException("Failed ioctlsocket")
            }

            if (listen(serverSocket, backlog) == SOCKET_ERROR) {
                WSACleanup()
                throw IOException("Failed listen")
            }
        }
    }

    actual fun close() {
        running = false
        nativeHeap.free(readfds)
        nativeHeap.free(writefds)
        nativeHeap.free(errorfds)
        WSACleanup()
    }

    actual fun isRunning(): Boolean = running

    actual fun select(
        timeout: Long,
        block: (socket: ClientConnection, state: ServerSocketLoop.SocketState) -> Boolean
    ) {
        if (isRunning()) {
            posix_FD_ZERO(readfds.ptr)
            posix_FD_ZERO(writefds.ptr)
            posix_FD_ZERO(errorfds.ptr)
            posix_FD_SET(serverSocket.convert(), readfds.ptr)
            clients.forEach {
                posix_FD_SET(it.key.convert(), readfds.ptr)
                posix_FD_SET(it.key.convert(), errorfds.ptr)
            }
            writeRequest.forEach {
                posix_FD_SET(it.convert(), writefds.ptr)
            }
            memScoped {
                val timeoutStruct = alloc<timeval>()
                timeoutStruct.tv_sec = 0
                timeoutStruct.tv_usec = (timeout * 1000).toInt()
                select(0, readfds.ptr, writefds.ptr, errorfds.ptr, timeoutStruct.ptr)
            }

            if (posix_FD_ISSET(serverSocket.convert(), readfds.ptr) == 1) {
                val newSocket = accept(serverSocket, null, null)
                if (newSocket == INVALID_SOCKET) {
                    throw IOException("Invalid socket")
                }
                clients[newSocket] = ClientConnection(Socket(newSocket, writeRequest, buffer), broker)
            } else {
                clients.forEach { socket ->
                    when {
                        posix_FD_ISSET(socket.key.convert(), readfds.ptr) == 1 -> {
                            if (!block(socket.value, ServerSocketLoop.SocketState.READ))
                                clients.remove(socket.key)
                        }
                        posix_FD_ISSET(socket.key.convert(), writefds.ptr) == 1 -> {
                            writeRequest.remove(socket.key)
                            if (!block(socket.value, ServerSocketLoop.SocketState.WRITE))
                                clients.remove(socket.key)
                        }
                        posix_FD_ISSET(socket.key.convert(), errorfds.ptr) == 1 -> {
                            clients.remove(socket.key)
                            shutdown(socket.key, SD_SEND)
                            closesocket(socket.key)
                            socket.value.closedWithException()
                        }
                    }
                }
            }
        }
    }

}
