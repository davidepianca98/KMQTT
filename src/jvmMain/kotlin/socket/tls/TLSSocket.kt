package socket.tls

import socket.streams.ByteArrayOutputStream
import socket.tcp.IOException
import socket.tcp.Socket
import socket.tcp.SocketClosedException
import toUByteArray
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult
import javax.net.ssl.SSLException


actual class TLSSocket(
    key: SelectionKey,
    private var sendBuffer: ByteBuffer,
    private var receiveBuffer: ByteBuffer,
    private var sendAppBuffer: ByteBuffer,
    private var receiveAppBuffer: ByteBuffer,
    private val engine: SSLEngine
) : Socket(key, sendBuffer, receiveBuffer) {

    private var cacheReceiveBuffer = ByteBuffer.allocate(1024)
    private var cacheBufferReadMode = false
    private val readClearTextData = mutableListOf<UByteArray>()

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
        do {
            try {
                sendBuffer.clear()
                val result = engine.wrap(sendAppBuffer, sendBuffer)
                @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
                when (result.status) {
                    SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                        throw SSLException("Buffer Underflow in wrap")
                    }
                    SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                        handleSendBufferOverflow()
                        send(data)
                    }
                    SSLEngineResult.Status.OK -> {
                        super.sendFromBuffer()
                    }
                    SSLEngineResult.Status.CLOSED -> {
                        engine.closeOutbound()
                        close()
                        throw SocketClosedException()
                    }
                }
            } catch (e: SSLException) {
                e.printStackTrace()
                engine.closeOutbound()
                close()
                throw IOException(e.message)
            }
        } while (sendAppBuffer.hasRemaining())
    }

    private fun runHandshake(): Boolean {
        while (engine.handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED
            && engine.handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
        ) {
            @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
            when (engine.handshakeStatus) {
                SSLEngineResult.HandshakeStatus.NEED_WRAP -> {
                    send(UByteArray(0))
                    runHandshake()
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
                SSLEngineResult.HandshakeStatus.NEED_UNWRAP -> return false
                else -> throw Exception("Unknown Hanshake Status")
            }
        }
        return true
    }

    private fun putOrEnlarge() {
        try {
            if (cacheBufferReadMode) {
                cacheReceiveBuffer = cacheReceiveBuffer.compact() // Change to write mode preserving unread data
                cacheBufferReadMode = false
            }
            cacheReceiveBuffer.put(receiveBuffer)
        } catch (e: BufferOverflowException) {
            val newBuffer = ByteBuffer.allocate(cacheReceiveBuffer.capacity() + receiveBuffer.remaining())
            cacheReceiveBuffer.flip()
            newBuffer.put(cacheReceiveBuffer)
            cacheReceiveBuffer = newBuffer
            cacheReceiveBuffer.put(receiveBuffer)
        }
    }

    override fun read(): UByteArray? {
        try {
            super.readToBuffer()

            putOrEnlarge()

            cacheReceiveBuffer.flip() // Cache buffer is needed as we are using one receive buffer for the whole server, we need to cache data to avoid overwriting from other sockets when buffer underflow is thrown
            cacheBufferReadMode = true
            while (cacheReceiveBuffer.hasRemaining()) {
                receiveAppBuffer.clear()
                try {
                    val result = engine.unwrap(cacheReceiveBuffer, receiveAppBuffer)
                    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
                    when (result.status) {
                        SSLEngineResult.Status.OK -> {
                            if (runHandshake()) {
                                receiveAppBuffer.flip()
                                readClearTextData.add(receiveAppBuffer.toUByteArray())
                            }
                        }
                        SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                            handleReceiveBufferUnderflow()
                            return null
                        }
                        SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                            handleReceiveBufferOverflow()
                        }
                        SSLEngineResult.Status.CLOSED -> {
                            engine.closeOutbound()
                            close()
                            throw SocketClosedException()
                        }
                    }
                } catch (e: SSLException) {
                    e.printStackTrace()
                    engine.closeOutbound()
                    close()
                    throw IOException(e.message)
                }
            }
            if (runHandshake()) {
                val array = if (readClearTextData.size > 1) {
                    val stream = ByteArrayOutputStream()
                    readClearTextData.forEach {
                        stream.write(it)
                    }
                    stream.toByteArray()
                } else {
                    readClearTextData.getOrNull(0)
                }
                readClearTextData.clear()
                return array
            }
        } catch (e: SocketClosedException) {
            engine.closeOutbound()
            throw e
        }
        return null
    }
}
