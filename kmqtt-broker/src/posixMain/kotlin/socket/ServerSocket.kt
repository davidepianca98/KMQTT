package socket

import bind
import close
import getEagain
import getErrno
import getEwouldblock
import inet_pton
import kotlinx.cinterop.*
import listen
import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import mqtt.broker.cluster.ClusterConnection
import mqtt.broker.cluster.ClusterDiscoveryConnection
import mqtt.broker.udp.UDPConnectionsMap
import platform.posix.*
import select
import set_non_blocking
import setsockopt
import shutdown
import sockaddrIn
import socket
import socket.tcp.Socket
import socket.tcp.WebSocket
import socket.udp.UDPSocket
import socketsCleanup
import socketsInit
import accept as posixAccept

internal actual open class ServerSocket actual constructor(
    private val broker: Broker,
    private val selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : ServerSocketInterface {

    private var running = true
    private var mqttSocket = -1
    private var mqttUdpSocket = -1
    private var clusteringSocket = -1
    private var discoverySocket = -1
    private var mqttWebSocket = -1
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
            socketsCleanup()
            throw IOException("Setsockopt")
        }

        val serverAddress = sockaddrIn(AF_INET.convert(), port.convert())

        if (bind(socket, serverAddress.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()) == -1) {
            socketsCleanup()
            throw IOException("Failed bind")
        }

        if (set_non_blocking(socket) == -1) {
            socketsCleanup()
            throw IOException("Failed ioctlsocket")
        }

        if (listen(socket, broker.backlog) == -1) {
            socketsCleanup()
            throw IOException("Failed listen")
        }
    }

    private fun prepareDatagramSocket(socket: Int, port: Int) = memScoped {
        val serverAddress = sockaddrIn(AF_INET.convert(), port.convert())

        if (bind(socket, serverAddress.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()) == -1) {
            socketsCleanup()
            throw IOException("Failed bind")
        }

        if (set_non_blocking(socket) == -1) {
            socketsCleanup()
            throw IOException("Failed ioctlsocket")
        }

        val on = alloc<uint32_tVar>()
        on.value = 1u
        if (setsockopt(socket, SOL_SOCKET, SO_BROADCAST, on.ptr, 4u) == -1) {
            socketsCleanup()
            throw IOException("Failed setsockopt")
        }
    }

    init {
        memScoped {
            socketsInit()
            mqttSocket = socket(AF_INET, SOCK_STREAM, 0)
            if (mqttSocket == -1) {
                socketsCleanup()
                throw IOException("Invalid socket: error $errno")
            }
            prepareStreamSocket(mqttSocket, broker.port)

            if (broker.enableUdp) {
                mqttUdpSocket = socket(AF_INET, SOCK_DGRAM, 0)
                if (mqttUdpSocket == -1) {
                    socketsCleanup()
                    throw IOException("Invalid socket: error $errno")
                }
                prepareDatagramSocket(mqttUdpSocket, broker.port)
                clients[mqttUdpSocket] = UDPConnectionsMap(UDPSocket(mqttUdpSocket), broker)
            }

            if (broker.webSocketPort != null) {
                mqttWebSocket = socket(AF_INET, SOCK_STREAM, 0)
                if (mqttWebSocket == -1) {
                    socketsCleanup()
                    throw IOException("Invalid socket: error $errno")
                }
                prepareStreamSocket(mqttWebSocket, broker.webSocketPort)
            }

            if (broker.cluster != null) {
                val cluster = broker.cluster
                clusteringSocket = socket(AF_INET, SOCK_STREAM, 0)
                if (clusteringSocket == -1) {
                    socketsCleanup()
                    throw IOException("Invalid socket: error $errno")
                }
                prepareStreamSocket(clusteringSocket, cluster.tcpPort)

                if (!cluster.dnsDiscovery) {
                    discoverySocket = socket(AF_INET, SOCK_DGRAM, 0)
                    if (discoverySocket == -1) {
                        socketsCleanup()
                        throw IOException("Invalid socket: error $errno")
                    }
                    prepareDatagramSocket(discoverySocket, cluster.discoveryPort)
                    val clusterConnection = ClusterDiscoveryConnection(UDPSocket(discoverySocket), broker)
                    clients[discoverySocket] = clusterConnection
                    clusterConnection.sendDiscovery(cluster.discoveryPort)
                } else {
                    // TODO dns lookup
                    // broker.addClusterConnection(address)
                }
            }
            maxFd = listOf(mqttSocket, mqttUdpSocket, mqttWebSocket, clusteringSocket, discoverySocket).maxOrNull()!!
        }
    }

    actual open fun close() {
        running = false
        nativeHeap.free(readfds)
        nativeHeap.free(writefds)
        nativeHeap.free(errorfds)
        if (mqttSocket != -1) {
            shutdown(mqttSocket)
            close(mqttSocket)
        }
        if (mqttUdpSocket != -1) {
            shutdown(mqttSocket)
            close(mqttSocket)
        }
        if (mqttWebSocket != -1) {
            shutdown(mqttSocket)
            close(mqttSocket)
        }
        if (clusteringSocket != -1) {
            shutdown(mqttSocket)
            close(mqttSocket)
        }
        if (discoverySocket != -1) {
            shutdown(mqttSocket)
            close(mqttSocket)
        }
        socketsCleanup()
    }

    actual fun isRunning(): Boolean = running

    actual fun select(timeout: Long) {
        if (isRunning()) {
            posix_FD_ZERO(readfds.ptr)
            posix_FD_ZERO(writefds.ptr)
            posix_FD_ZERO(errorfds.ptr)
            posix_FD_SET(mqttSocket.convert(), readfds.ptr)
            if (mqttWebSocket != -1)
                posix_FD_SET(mqttWebSocket.convert(), readfds.ptr)
            if (clusteringSocket != -1)
                posix_FD_SET(clusteringSocket.convert(), readfds.ptr)
            clients.forEach {
                posix_FD_SET(it.key.convert(), readfds.ptr)
                posix_FD_SET(it.key.convert(), errorfds.ptr)
            }
            writeRequest.forEach {
                posix_FD_SET(it.convert(), writefds.ptr)
            }
            memScoped {
                select(
                    maxFd + 1,
                    readfds.ptr,
                    writefds.ptr,
                    errorfds.ptr,
                    timeout
                )
            }

            if (posix_FD_ISSET(mqttSocket.convert(), readfds.ptr) != 0) {
                tcpServerSocketAccept(mqttSocket, TCPSocketType.MQTT)
            }
            if (mqttWebSocket != -1) {
                if (posix_FD_ISSET(mqttWebSocket.convert(), readfds.ptr) != 0) {
                    tcpServerSocketAccept(mqttWebSocket, TCPSocketType.MQTTWS)
                }
            }
            if (clusteringSocket != -1) {
                if (posix_FD_ISSET(clusteringSocket.convert(), readfds.ptr) != 0) {
                    tcpServerSocketAccept(clusteringSocket, TCPSocketType.CLUSTER)
                }
            }
            clients.forEach { socket ->
                when {
                    posix_FD_ISSET(socket.key.convert(), readfds.ptr) != 0 -> {
                        if (!selectCallback(socket.value, SocketState.READ))
                            clients.remove(socket.key)
                    }
                    posix_FD_ISSET(socket.key.convert(), writefds.ptr) != 0 -> {
                        writeRequest.remove(socket.key)
                        if (!selectCallback(socket.value, SocketState.WRITE))
                            clients.remove(socket.key)
                    }
                    posix_FD_ISSET(socket.key.convert(), errorfds.ptr) != 0 -> {
                        clients.remove(socket.key)
                        shutdown(socket.key)
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

    enum class TCPSocketType {
        MQTT,
        MQTTWS,
        CLUSTER
    }

    private fun tcpServerSocketAccept(serverSocket: Int, type: TCPSocketType) {
        val newSocket = posixAccept(serverSocket, null, null)
        if (newSocket == -1) {
            val error = getErrno()
            if (error != getEwouldblock() && error != getEagain())
                throw IOException("Invalid socket, errno: $error")
        } else {
            if (set_non_blocking(newSocket) == -1) {
                throw IOException("Failure setting client socket non blocking")
            }
            if (maxFd < newSocket)
                maxFd = newSocket
            accept(newSocket, type)
        }
    }

    open fun accept(socket: Int, type: TCPSocketType) {
        val sock = Socket(socket, writeRequest, buffer)
        clients[socket] = when (type) {
            TCPSocketType.MQTT -> ClientConnection(sock, broker)
            TCPSocketType.MQTTWS -> ClientConnection(WebSocket(sock), broker)
            TCPSocketType.CLUSTER -> {
                TODO("Cluster not yet complete in Native")
                //val clusterConnection = ClusterConnection(sock, broker)
                //val remoteAddress = (channel.socket().remoteSocketAddress as InetSocketAddress).address.hostAddress
                //broker.addClusterConnection(remoteAddress, clusterConnection)
                //clusterConnection
            }
        }

    }

    actual override fun addClusterConnection(address: String): ClusterConnection? {
        if (broker.cluster != null) {
            val cluster = broker.cluster
            memScoped {
                val socket = socket(AF_INET, SOCK_STREAM, 0)
                if (socket == -1) {
                    throw IOException("Invalid socket: error $errno")
                }

                val serverAddress = sockaddrIn(AF_INET.convert(), cluster.tcpPort.convert())
                inet_pton(AF_INET, address, serverAddress.sin_addr.ptr)

                if (set_non_blocking(socket) == -1) {
                    throw IOException("Failed ioctlsocket")
                }

                val clusterConnection = ClusterConnection(Socket(socket, writeRequest, buffer), broker)
                broker.addClusterConnection(address, clusterConnection)
                clients[socket] = clusterConnection

                return clusterConnection
            }
        }
        return null
    }

}
