package socket.tls

import socket.IOException
import socket.Socket
import socket.SocketClosedException
import toUByteArray
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult
import javax.net.ssl.SSLException


actual class TLSSocket(
    private var sendBuffer: ByteBuffer,
    private var receiveBuffer: ByteBuffer,
    private var sendAppBuffer: ByteBuffer,
    private var receiveAppBuffer: ByteBuffer,
    private val engine: SSLEngine
) : Socket(sendBuffer, receiveBuffer) {

    private var cacheReceiveBuffer = ByteBuffer.allocate(engine.session.packetBufferSize)
    private var cacheBufferReadMode = false

    private fun handleReceiveBufferUnderflow() {
        if (engine.session.packetBufferSize > receiveAppBuffer.capacity()) {
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
        sendAppBuffer.put(data.toByteArray())
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
        var exception: Boolean
        var array = ByteArray(0)
        var putArrayDone = false
        do {
            exception = try {
                if (cacheBufferReadMode) {
                    array = ByteArray(cacheReceiveBuffer.remaining())
                    cacheReceiveBuffer.get(array, 0, cacheReceiveBuffer.remaining())
                    cacheReceiveBuffer.clear()
                    cacheBufferReadMode = false
                }
                if (!putArrayDone) {
                    cacheReceiveBuffer.put(array)
                    putArrayDone = true
                }
                cacheReceiveBuffer.put(receiveBuffer)
                false
            } catch (e: BufferOverflowException) {
                val newBuffer = ByteBuffer.allocate(cacheReceiveBuffer.capacity() + receiveBuffer.remaining())
                cacheReceiveBuffer.flip()
                newBuffer.put(cacheReceiveBuffer)
                cacheReceiveBuffer = newBuffer
                true
            }
        } while (exception)
    }

    override fun read(): UByteArray? {
        try {
            if (super.readToBuffer() == 0)
                return null

            receiveAppBuffer.clear()
            putOrEnlarge()

            cacheReceiveBuffer.flip()
            cacheBufferReadMode = true
            while (cacheReceiveBuffer.hasRemaining()) {
                try {
                    val result = engine.unwrap(cacheReceiveBuffer, receiveAppBuffer)
                    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
                    when (result.status) {
                        SSLEngineResult.Status.OK -> {
                            runHandshake()
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
                            return null
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
                receiveAppBuffer.flip()
                return receiveAppBuffer.toUByteArray()
            }
        } catch (e: SocketClosedException) {
            engine.closeOutbound()
            throw e
        }
        return null
    }
}
