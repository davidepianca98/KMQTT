package socket.tls

import socket.IOException
import socket.Socket
import socket.SocketClosedException
import toUByteArray
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

    init {
        engine.useClientMode = false
        engine.beginHandshake()
    }

    private fun handleReceiveBufferUnderflow() {
        if (engine.session.packetBufferSize > receiveBuffer.capacity()) {
            receiveBuffer = ByteBuffer.allocate(engine.session.packetBufferSize)
        } else {
            receiveBuffer.compact()
        }
    }

    private fun handleReceiveBufferOverflow() {
        if (engine.session.applicationBufferSize > receiveAppBuffer.capacity()) {
            receiveAppBuffer = ByteBuffer.allocate(engine.session.applicationBufferSize)
        } else {
            receiveAppBuffer.compact()
        }
    }

    private fun handleSendBufferUnderflow() {
        if (engine.session.packetBufferSize > sendBuffer.capacity()) {
            sendBuffer = ByteBuffer.allocate(engine.session.packetBufferSize)
        } else {
            sendBuffer.compact()
        }
    }

    private fun handleSendBufferOverflow() {
        if (engine.session.applicationBufferSize > sendAppBuffer.capacity()) {
            sendAppBuffer = ByteBuffer.allocate(engine.session.applicationBufferSize)
        } else {
            sendAppBuffer.compact()
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
                        handleSendBufferUnderflow()
                        send(data)
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
                throw IOException()
            }
        } while (sendAppBuffer.hasRemaining())
    }

    private fun runHandshake(): Boolean {
        while (engine.handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED
            && engine.handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
        ) {
            @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
            when (engine.handshakeStatus) {
                SSLEngineResult.HandshakeStatus.NEED_WRAP -> send(UByteArray(0))
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING -> return true
                SSLEngineResult.HandshakeStatus.FINISHED -> return true
                SSLEngineResult.HandshakeStatus.NEED_TASK -> {
                    do {
                        val task = engine.delegatedTask
                        task?.run()
                    } while (task != null)
                    if (engine.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                        throw Exception("handshake shouldn't need additional tasks")
                    }
                }
                SSLEngineResult.HandshakeStatus.NEED_UNWRAP, SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN -> return false
            }
        }
        return true
    }

    override fun read(): UByteArray? {
        try {
            super.readToBuffer()

            while (receiveBuffer.hasRemaining()) {
                receiveAppBuffer.clear()
                try {
                    val result = engine.unwrap(receiveBuffer, receiveAppBuffer)
                    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
                    when (result.status) {
                        SSLEngineResult.Status.OK -> {
                            if (runHandshake()) {
                                receiveAppBuffer.flip()
                                return receiveAppBuffer.toUByteArray()
                            }
                        }
                        SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                            handleReceiveBufferUnderflow()
                            return read()
                        }
                        SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                            handleReceiveBufferOverflow()
                            return read()
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
                    throw IOException()
                }
            }
        } catch (e: SocketClosedException) {
            engine.closeOutbound()
            throw e
        }
        return null
    }
}
