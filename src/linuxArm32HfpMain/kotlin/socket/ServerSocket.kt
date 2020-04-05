package socket

import kotlinx.cinterop.*
import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import mqtt.broker.cluster.ClusterConnection
import mqtt.broker.cluster.ClusterDiscoveryConnection
import mqtt.broker.udp.UDPConnectionsMap
import platform.linux.inet_pton
import platform.posix.*
import socket.tcp.IOException
import socket.tcp.Socket
import socket.udp.UDPSocket

actual open class ServerSocket actual constructor(private val broker: Broker) : ServerSocketInterface {

    private var running = true
    private var mqttSocket = -1
    private var mqttUdpSocket = -1
    private var clusteringSocket = -1
    private var discoverySocket = -1
    private var maxFd = 0

    protected val clients = mutableMapOf<Int, Any?>()
    protected val writeRequest = mutableListOf<Int>()

    protected val buffer = ByteArray(broker.maximumPacketSize.toInt())

    private val readfds = nativeHeap.alloc<fd_set>()
    private val writefds = nativeHeap.alloc<fd_set>()
    private val errorfds = nativeHeap.alloc<fd_set>()

    private fun prepareStreamSocket(socket: Int, port: Int) = memScoped {
        val reuseAddr = alloc<uint32_tVar>()
        reuseAddr.value = 1u
        if (setsockopt(socket, SOL_SOCKET, SO_REUSEADDR, reuseAddr.ptr, 4u) == -1) {
            throw IOException("Setsockopt")
        }

        val serverAddress = alloc<sockaddr_in>()
        memset(serverAddress.ptr, 0, sockaddr_in.size.convert())
        serverAddress.sin_family = AF_INET.convert()
        serverAddress.sin_addr.s_addr = posix_htons(0).convert()
        serverAddress.sin_port = posix_htons(port.convert()).convert()

        if (bind(socket, serverAddress.ptr.reinterpret(), sockaddr_in.size.convert()) == -1) {
            throw IOException("Failed bind")
        }

        if (fcntl(socket, F_SETFL, O_NONBLOCK) == -1) {
            throw IOException("Failed ioctlsocket")
        }

        if (listen(socket, broker.backlog) == -1) {
            throw IOException("Failed listen")
        }
    }

    private fun prepareDatagramSocket(socket: Int, port: Int) = memScoped {
        val serverAddress = alloc<sockaddr_in>()
        memset(serverAddress.ptr, 0, sockaddr_in.size.convert())
        serverAddress.sin_family = AF_INET.convert()
        serverAddress.sin_addr.s_addr = INADDR_ANY
        serverAddress.sin_port = posix_htons(port.convert()).convert()

        if (bind(socket, serverAddress.ptr.reinterpret(), sockaddr_in.size.convert()) == -1) {
            throw IOException("Failed bind")
        }

        if (fcntl(socket, F_SETFL, O_NONBLOCK) == -1) {
            throw IOException("Failed ioctlsocket")
        }
    }

    init {
        memScoped {
            mqttSocket = socket(AF_INET, SOCK_STREAM, 0)
            if (mqttSocket == -1) {
                throw IOException("Invalid socket: error $errno")
            }
            prepareStreamSocket(mqttSocket, broker.port)

            if (broker.enableUdp) {
                mqttUdpSocket = socket(AF_INET, SOCK_DGRAM, 0)
                if (mqttUdpSocket == -1) {
                    throw IOException("Invalid socket: error $errno")
                }
                prepareDatagramSocket(mqttUdpSocket, broker.port)
                clients[mqttUdpSocket] = UDPConnectionsMap(UDPSocket(mqttUdpSocket), broker)
            }

            if (broker.cluster != null) {
                clusteringSocket = socket(AF_INET, SOCK_STREAM, 0)
                if (clusteringSocket == -1) {
                    throw IOException("Invalid socket: error $errno")
                }
                prepareStreamSocket(clusteringSocket, broker.cluster.tcpPort)

                discoverySocket = socket(AF_INET, SOCK_DGRAM, 0)
                if (discoverySocket == -1) {
                    throw IOException("Invalid socket: error $errno")
                }
                prepareDatagramSocket(discoverySocket, broker.cluster.discoveryPort)
                val clusterConnection = ClusterDiscoveryConnection(UDPSocket(discoverySocket), broker)
                clients[discoverySocket] = clusterConnection
                clusterConnection.sendDiscovery(broker.cluster.discoveryPort)
            }
            maxFd = listOf(mqttSocket, mqttUdpSocket, clusteringSocket, discoverySocket).max()!!
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
        block: (attachment: Any?, state: ServerSocketLoop.SocketState) -> Boolean
    ) {
        if (isRunning()) {
            posix_FD_ZERO(readfds.ptr)
            posix_FD_ZERO(writefds.ptr)
            posix_FD_ZERO(errorfds.ptr)
            posix_FD_SET(mqttSocket.convert(), readfds.ptr)
            posix_FD_SET(clusteringSocket.convert(), readfds.ptr)
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
                select(maxFd + 1, readfds.ptr, writefds.ptr, errorfds.ptr, timeoutStruct.ptr)
            }

            if (posix_FD_ISSET(mqttSocket.convert(), readfds.ptr) == 1) {
                tcpServerSocketAccept(mqttSocket)
            }
            if (posix_FD_ISSET(clusteringSocket.convert(), readfds.ptr) == 1) {
                tcpServerSocketAccept(clusteringSocket)
            }
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
                        val socketAttachment = socket.value
                        if (socketAttachment is ClientConnection) {
                            socketAttachment.closedWithException()
                        }
                    }
                }
            }
        }
    }

    private fun tcpServerSocketAccept(serverSocket: Int) {
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
    }

    open fun accept(socket: Int) {
        clients[socket] = ClientConnection(Socket(socket, writeRequest, buffer), broker)
    }

    override fun addClusterConnection(address: String): ClusterConnection? {
        if (broker.cluster != null) {
            memScoped {
                val socket = socket(AF_INET, SOCK_STREAM, 0)
                if (socket == -1) {
                    throw IOException("Invalid socket: error $errno")
                }

                val serverAddress = alloc<sockaddr_in>()
                memset(serverAddress.ptr, 0, sockaddr_in.size.convert())
                serverAddress.sin_family = AF_INET.convert()
                inet_pton(AF_INET, address, serverAddress.sin_addr.ptr)
                serverAddress.sin_port = posix_htons(broker.cluster.tcpPort.convert()).convert()

                if (fcntl(socket, F_SETFL, O_NONBLOCK) == -1) {
                    throw IOException("Failed ioctlsocket")
                }

                val clusterConnection = ClusterConnection(Socket(socket, writeRequest, buffer))
                broker.addClusterConnection(address, clusterConnection)
                clients[socket] = clusterConnection

                return clusterConnection
            }
        }
        return null
    }

}
