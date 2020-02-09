package socket

import kotlinx.cinterop.*
import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import platform.posix.*

actual open class ServerSocket actual constructor(private val broker: Broker) : ServerSocketInterface {

    private var running = true
    private var serverSocket = -1
    private var maxFd = 0

    protected val clients = mutableMapOf<Int, ClientConnection>()
    protected val writeRequest = mutableListOf<Int>()

    protected val buffer = ByteArray(broker.maximumPacketSize.toInt())

    private val readfds = nativeHeap.alloc<fd_set>()
    private val writefds = nativeHeap.alloc<fd_set>()
    private val errorfds = nativeHeap.alloc<fd_set>()

    init {
        memScoped {
            serverSocket = socket(AF_INET, SOCK_STREAM, 0)
            if (serverSocket == -1) {
                throw IOException("Invalid socket: error $errno")
            }
            maxFd = serverSocket

            val reuseAddr = alloc<uint32_tVar>()
            reuseAddr.value = 1u
            if (setsockopt(serverSocket, SOL_SOCKET, SO_REUSEADDR, reuseAddr.ptr, 4) == -1) {
                throw IOException("Setsockopt")
            }

            val serverAddress = alloc<sockaddr_in>()
            memset(serverAddress.ptr, 0, sockaddr_in.size.convert())
            serverAddress.sin_family = AF_INET.convert()
            serverAddress.sin_addr.s_addr = posix_htons(0).convert()
            serverAddress.sin_port = posix_htons(broker.port.convert()).convert()

            if (bind(serverSocket, serverAddress.ptr.reinterpret(), sockaddr_in.size.convert()) == -1) {
                throw IOException("Failed bind")
            }

            if (fcntl(serverSocket, F_SETFL, O_NONBLOCK) == -1) {
                throw IOException("Failed ioctlsocket")
            }

            if (listen(serverSocket, broker.backlog) == -1) {
                throw IOException("Failed listen")
            }
        }
    }

    actual open fun close() {
        running = false
        nativeHeap.free(readfds)
        nativeHeap.free(writefds)
        nativeHeap.free(errorfds)
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
                timeoutStruct.tv_usec = timeout * 1000
                select(maxFd + 1, readfds.ptr, writefds.ptr, errorfds.ptr, timeoutStruct.ptr)
            }

            if (posix_FD_ISSET(serverSocket.convert(), readfds.ptr) == 1) {
                val newSocket = accept(serverSocket, null, null)
                if (newSocket == -1) {
                    if (errno != EAGAIN && errno != EWOULDBLOCK)
                        throw IOException("Invalid socket: error $errno")
                } else {
                    if (fcntl(newSocket, F_SETFL, O_NONBLOCK) == -1) {
                        throw IOException("Failure setting client socket non blocking")
                    }
                    if (maxFd < newSocket)
                        maxFd = newSocket
                    accept(newSocket)
                }
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
                            shutdown(socket.key, SHUT_WR)
                            close(socket.key)
                            socket.value.closedWithException()
                        }
                    }
                }
            }
        }
    }

    override fun accept(socket: Any) {
        val newSocket = socket as Int
        clients[newSocket] = ClientConnection(Socket(newSocket, writeRequest, buffer), broker)
    }

}
