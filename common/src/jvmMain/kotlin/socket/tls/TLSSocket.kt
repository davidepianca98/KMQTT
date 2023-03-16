package socket.tls

import socket.IOException
import socket.SocketClosedException
import socket.streams.ByteArrayOutputStream
import socket.tcp.Socket
import toUByteArray
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult
import javax.net.ssl.SSLException


actual open class TLSSocket(
    channel: SocketChannel,
    key: SelectionKey?,
    private var sendBuffer: ByteBuffer,
    private var receiveBuffer: ByteBuffer,
    private var sendAppBuffer: ByteBuffer,
    private var receiveAppBuffer: ByteBuffer,
    private val engine: SSLEngine
) : Socket(channel, key, sendBuffer, receiveBuffer) {

    init {
        engine.beginHandshake()
        runHandshake(engine.useClientMode)
    }

    private fun handleReceiveBufferUnderflow() {
        if (engine.session.packetBufferSize > receiveBuffer.capacity()) {
            val newBuffer = ByteBuffer.allocate(engine.session.packetBufferSize)
            receiveBuffer.flip()
            newBuffer.put(receiveBuffer)
            receiveBuffer = newBuffer
        }
    }

    private fun handleReceiveBufferOverflow() {
        val size = engine.session.applicationBufferSize + receiveAppBuffer.position()
        val newBuffer = ByteBuffer.allocate(size)
        receiveAppBuffer.flip()
        newBuffer.put(receiveAppBuffer)
        receiveAppBuffer = newBuffer
    }

    private fun handleSendBufferOverflow() {
        sendBuffer = if (engine.session.packetBufferSize > sendBuffer.capacity()) {
            ByteBuffer.allocate(engine.session.applicationBufferSize)
        } else {
            ByteBuffer.allocate(sendBuffer.capacity() * 2)
        }
    }

    override fun send(data: UByteArray) {
        sendAppBuffer.clear()
        val dataByteArray = data.toByteArray()
        try {
            sendAppBuffer.put(dataByteArray)
        } catch (e: BufferOverflowException) {
            sendAppBuffer = ByteBuffer.allocate(sendAppBuffer.capacity() + dataByteArray.size)
            sendAppBuffer.put(dataByteArray)
        }
        sendAppBuffer.flip()
        try {
            do {
                val result = engine.wrap(sendAppBuffer, sendBuffer)
                @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
                when (result.status) {
                    SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                        throw SSLException("Buffer Underflow in wrap")
                    }
                    SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                        handleSendBufferOverflow()
                    }
                    SSLEngineResult.Status.OK -> {
                        super.sendFromBuffer()
                    }
                    SSLEngineResult.Status.CLOSED -> {
                        throw SocketClosedException()
                    }
                }
            } while (result.status != SSLEngineResult.Status.OK)
        } catch (e: SocketClosedException) {
            engine.closeOutbound()
            close()
            throw e
        } catch (e: SSLException) {
            e.printStackTrace()
            engine.closeOutbound()
            close()
            throw IOException(e.message)
        }
    }

    private fun runHandshake(full: Boolean): Boolean {
        while (engine.handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED
            && engine.handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
        ) {
            @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
            when (engine.handshakeStatus) {
                SSLEngineResult.HandshakeStatus.NEED_WRAP -> {
                    send(UByteArray(0))
                }
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING -> return true
                SSLEngineResult.HandshakeStatus.FINISHED -> return true
                SSLEngineResult.HandshakeStatus.NEED_TASK -> {
                    do {
                        val task = engine.delegatedTask
                        task?.run()
                    } while (task != null)
                    if (engine.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                        throw Exception("Handshake shouldn't need additional tasks")
                    }
                }
                SSLEngineResult.HandshakeStatus.NEED_UNWRAP -> {
                    if (!read0() && !full) {
                        return false
                    }
                }
                else -> throw Exception("Unknown Handshake Status")
            }
        }
        return true
    }

    private fun read0(): Boolean {
        try {
            do {
                receiveBuffer.flip()
                val result = engine.unwrap(receiveBuffer, receiveAppBuffer)
                receiveBuffer.compact()
                @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
                when (result.status) {
                    SSLEngineResult.Status.OK -> {}
                    SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                        handleReceiveBufferUnderflow()
                        if (super.readToBuffer() == 0) {
                            return false
                        }
                    }
                    SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                        handleReceiveBufferOverflow()
                    }
                    SSLEngineResult.Status.CLOSED -> {
                        throw SocketClosedException()
                    }
                }
            } while (result.status != SSLEngineResult.Status.OK || (engine.handshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED || engine.handshakeStatus == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING))
        } catch (e: SocketClosedException) {
            engine.closeOutbound()
            close()
            throw e
        } catch (e: SSLException) {
            e.printStackTrace()
            engine.closeOutbound()
            send(UByteArray(0))
            close()
            throw IOException(e.message)
        }
        return true
    }

    override fun read(): UByteArray? {
        runHandshake(engine.useClientMode)
        read0()
        receiveAppBuffer.flip()
        val result = receiveAppBuffer.toUByteArray()
        return if (result.isEmpty()) {
            null
        } else {
            result
        }
    }
}