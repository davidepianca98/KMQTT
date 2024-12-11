package io.github.davidepianca98.socket

import kotlinx.atomicfu.locks.withLock
import io.github.davidepianca98.mqtt.broker.Broker
import io.github.davidepianca98.mqtt.broker.cluster.ClusterConnection
import io.github.davidepianca98.socket.tcp.TCPEventHandler
import io.github.davidepianca98.socket.tls.TLSServerSocket
import io.github.davidepianca98.socket.udp.UDPEventHandler

internal class ServerSocketLoop(private val broker: Broker) {

    private val serverSocket = if (broker.tlsSettings == null)
        ServerSocket(broker, this::selectCallback)
    else
        TLSServerSocket(broker, this::selectCallback)

    /**
     * Blocking run the server socket loop
     */
    fun run() {
        while (serverSocket.isRunning()) {
            serverSocket.select(250)
            broker.cleanUpOperations()
        }
    }

    /**
     * Non blocking run the server socket loop, run a single step and return
     */
    fun step() {
        if (serverSocket.isRunning()) {
            serverSocket.select(1)
            broker.cleanUpOperations()
        }
    }

    fun isRunning(): Boolean {
        return serverSocket.isRunning()
    }

    private fun selectCallback(attachment: Any?, state: SocketState): Boolean {
        return broker.lock.withLock {
            when (attachment) {
                is TCPEventHandler -> handleTcpEvent(attachment, state)
                is UDPEventHandler -> handleUdpEvent(attachment, state)
                else -> true
            }
        }
    }

    private fun handleTcpEvent(tcpEventHandler: TCPEventHandler, state: SocketState): Boolean {
        try {
            when (state) {
                SocketState.READ -> {
                    do {
                        val data = tcpEventHandler.read()
                        data?.let {
                            tcpEventHandler.dataReceived(it)
                        }
                    } while (data != null)
                }
                SocketState.WRITE -> {
                    tcpEventHandler.sendRemaining()
                }
            }
            return true
        } catch (e: SocketClosedException) {
            tcpEventHandler.closedGracefully()
            if (tcpEventHandler is ClusterConnection) {
                broker.removeClusterConnection(tcpEventHandler)
            }
        } catch (e: IOException) {
            tcpEventHandler.closedWithException()
            if (tcpEventHandler is ClusterConnection) {
                broker.removeClusterConnection(tcpEventHandler)
            }
        }
        return false
    }

    private fun handleUdpEvent(udpEventHandler: UDPEventHandler, state: SocketState): Boolean {
        if (state == SocketState.READ) {
            udpEventHandler.dataReceived()
        }
        return true
    }

    fun addClusterConnection(address: String): ClusterConnection? {
        return serverSocket.addClusterConnection(address)
    }

    fun stop() {
        serverSocket.close()
    }
}
