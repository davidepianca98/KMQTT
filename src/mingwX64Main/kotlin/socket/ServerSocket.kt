package socket

import getErrno
import getEwouldblock
import kotlinx.cinterop.*
import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import mqtt.broker.ClientConnection5
import mqtt.broker.cluster.ClusterConnection
import mqtt.broker.cluster.ClusterDiscoveryConnection
import mqtt.broker.udp.UDPConnectionsMap
import platform.posix.*
import platform.windows.inet_pton
import platform.windows.select
import socket.tcp.IOException
import socket.tcp.Socket
import socket.udp.UDPSocket


actual open class ServerSocket actual constructor(private val broker: Broker) : ServerSocketInterface {

    private var running = true
    private var mqttSocket = INVALID_SOCKET
    private var mqttUdpSocket = INVALID_SOCKET
    private var clusteringSocket = INVALID_SOCKET
    private var discoverySocket = INVALID_SOCKET

    protected val clients = mutableMapOf<SOCKET, Any?>()
    protected val writeRequest = mutableListOf<SOCKET>()

    protected val buffer = ByteArray(broker.maximumPacketSize.toInt())

    private val readfds = nativeHeap.alloc<fd_set>()
    private val writefds = nativeHeap.alloc<fd_set>()
    private val errorfds = nativeHeap.alloc<fd_set>()

    private fun prepareStreamSocket(socket: SOCKET, port: Int) = memScoped {
        val reuseAddr = alloc<uint32_tVar>()
        reuseAddr.value = 1u
        if (setsockopt(socket, SOL_SOCKET, SO_REUSEADDR, reuseAddr.ptr.toString(), 4) == SOCKET_ERROR) {
            WSACleanup()
            throw IOException("Setsockopt")
        }

        val serverAddress = alloc<sockaddr_in>()
        memset(serverAddress.ptr, 0, sockaddr_in.size.convert())
        serverAddress.sin_family = AF_INET.convert()
        serverAddress.sin_addr.S_un.S_addr = posix_htons(0).convert()
        serverAddress.sin_port = posix_htons(port.convert()).convert()

        if (bind(socket, serverAddress.ptr.reinterpret(), sockaddr_in.size.convert()) == SOCKET_ERROR) {
            WSACleanup()
            throw IOException("Failed bind")
        }

        val on = alloc<uint32_tVar>()
        on.value = 1u
        if (ioctlsocket(socket, FIONBIO.toInt(), on.ptr.reinterpret()) == SOCKET_ERROR) {
            WSACleanup()
            throw IOException("Failed ioctlsocket")
        }

        if (listen(socket, broker.backlog) == SOCKET_ERROR) {
            WSACleanup()
            throw IOException("Failed listen")
        }
    }

    private fun prepareDatagramSocket(socket: SOCKET, port: Int) = memScoped {
        val serverAddress = alloc<sockaddr_in>()
        memset(serverAddress.ptr, 0, sockaddr_in.size.convert())
        serverAddress.sin_family = AF_INET.convert()
        serverAddress.sin_addr.S_un.S_addr = posix_htons(0).convert()
        serverAddress.sin_port = posix_htons(port.convert()).convert()

        if (bind(socket, serverAddress.ptr.reinterpret(), sockaddr_in.size.convert()) == -1) {
            throw IOException("Failed bind")
        }

        val on = alloc<uint32_tVar>()
        on.value = 1u
        if (ioctlsocket(socket, FIONBIO.toInt(), on.ptr.reinterpret()) == SOCKET_ERROR) {
            WSACleanup()
            throw IOException("Failed ioctlsocket")
        }

        if (setsockopt(socket, SOL_SOCKET, SO_BROADCAST, on.ptr.toString(), 4) == SOCKET_ERROR) {
            WSACleanup()
            throw IOException("Failed setsockopt")
        }
    }

    init {
        memScoped {
            val wsaData = alloc<WSADATA>()
            if (WSAStartup(0x0202u, wsaData.ptr) != 0)
                throw IOException("Failed WSAStartup")

            mqttSocket = socket(AF_INET, SOCK_STREAM, 0)
            if (mqttSocket == INVALID_SOCKET) {
                WSACleanup()
                throw IOException("Invalid socket")
            }
            prepareStreamSocket(mqttSocket, broker.port)

            if (broker.enableUdp) {
                mqttUdpSocket = socket(AF_INET, SOCK_DGRAM, 0)
                if (mqttUdpSocket == INVALID_SOCKET) {
                    throw IOException("Invalid socket")
                }
                prepareDatagramSocket(mqttUdpSocket, broker.port)
                clients[mqttUdpSocket] = UDPConnectionsMap(UDPSocket(mqttUdpSocket.convert()), broker)
            }

            if (broker.cluster != null) {
                clusteringSocket = socket(AF_INET, SOCK_STREAM, 0)
                if (clusteringSocket == INVALID_SOCKET) {
                    throw IOException("Invalid socket")
                }
                prepareStreamSocket(clusteringSocket, broker.cluster.tcpPort)

                discoverySocket = socket(AF_INET, SOCK_DGRAM, 0)
                if (discoverySocket == INVALID_SOCKET) {
                    throw IOException("Invalid socket")
                }
                prepareDatagramSocket(discoverySocket, broker.cluster.discoveryPort)
                val clusterConnection = ClusterDiscoveryConnection(UDPSocket(discoverySocket.convert()), broker)
                clients[discoverySocket] = clusterConnection
                clusterConnection.sendDiscovery(broker.cluster.discoveryPort)
            }
        }
    }

    actual open fun close() {
        running = false
        nativeHeap.free(readfds)
        nativeHeap.free(writefds)
        nativeHeap.free(errorfds)
        WSACleanup()
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
            if (clusteringSocket != INVALID_SOCKET)
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
                select(0, readfds.ptr, writefds.ptr, errorfds.ptr, timeoutStruct.ptr)
            }

            if (posix_FD_ISSET(mqttSocket.convert(), readfds.ptr) == 1) {
                tcpServerSocketAccept(mqttSocket)
            }
            if (clusteringSocket != INVALID_SOCKET) {
                if (posix_FD_ISSET(clusteringSocket.convert(), readfds.ptr) == 1) {
                    tcpServerSocketAccept(clusteringSocket)
                }
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
                        shutdown(socket.key, SD_SEND)
                        closesocket(socket.key)
                        val socketAttachment = socket.value
                        if (socketAttachment is ClientConnection) {
                            socketAttachment.closedWithException()
                        }
                    }
                }
            }
        }
    }

    private fun tcpServerSocketAccept(serverSocket: SOCKET) {
        val newSocket = accept(serverSocket, null, null)
        if (newSocket == INVALID_SOCKET) {
            val error = getErrno()
            if (error == getEwouldblock())
                return
            throw IOException("Invalid socket, errno: $error")
        }
        memScoped {
            val on = alloc<uint32_tVar>()
            on.value = 1u
            if (ioctlsocket(newSocket, FIONBIO.toInt(), on.ptr.reinterpret()) == SOCKET_ERROR) {
                WSACleanup()
                throw IOException("Failed ioctlsocket")
            }
        }
        accept(newSocket)
    }

    open fun accept(socket: SOCKET) {
        clients[socket] =
            ClientConnection5(Socket(socket.toInt(), writeRequest.map { it.toInt() }.toMutableList(), buffer), broker)
    }

    override fun addClusterConnection(address: String): ClusterConnection? {
        if (broker.cluster != null) {
            memScoped {
                val socket = socket(AF_INET, SOCK_STREAM, 0)
                if (socket == INVALID_SOCKET) {
                    throw IOException("Invalid socket: error $errno")
                }

                val serverAddress = alloc<sockaddr_in>()
                memset(serverAddress.ptr, 0, sockaddr_in.size.convert())
                serverAddress.sin_family = AF_INET.convert()
                inet_pton(AF_INET, address, serverAddress.sin_addr.ptr)
                serverAddress.sin_port = posix_htons(broker.cluster.tcpPort.convert()).convert()

                val on = alloc<uint32_tVar>()
                on.value = 1u
                if (ioctlsocket(socket, FIONBIO.toInt(), on.ptr.reinterpret()) == SOCKET_ERROR) {
                    WSACleanup()
                    throw IOException("Failed ioctlsocket")
                }

                val clusterConnection =
                    ClusterConnection(Socket(socket.toInt(), writeRequest.map { it.toInt() }.toMutableList(), buffer))
                broker.addClusterConnection(address, clusterConnection)
                clients[socket] = clusterConnection

                return clusterConnection
            }
        }
        return null
    }

}
