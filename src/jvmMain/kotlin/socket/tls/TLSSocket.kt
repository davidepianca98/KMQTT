package socket.tls

import socket.Socket
import socket.SocketClosedException
import toUByteArray
import java.nio.ByteBuffer
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult


actual class TLSSocket(
    private val sendBuffer: ByteBuffer,
    private val receiveBuffer: ByteBuffer,
    private val sendAppBuffer: ByteBuffer,
    private val receiveAppBuffer: ByteBuffer,
    private val engine: SSLEngine
) : Socket(sendBuffer, receiveBuffer) {

    init {
        engine.useClientMode = false
        engine.beginHandshake()
    }

    override fun send(data: UByteArray) {
        sendAppBuffer.clear()
        sendAppBuffer.put(data.toByteArray())
        sendAppBuffer.flip()
        do {
            sendBuffer.clear()
            val result = engine.wrap(sendAppBuffer, sendBuffer) // TODO handle SSLException
            when (result.status) {
                SSLEngineResult.Status.BUFFER_UNDERFLOW -> TODO()
                SSLEngineResult.Status.BUFFER_OVERFLOW -> TODO()
                SSLEngineResult.Status.OK -> {
                    super.sendFromBuffer()
                }
                SSLEngineResult.Status.CLOSED -> {
                    engine.closeOutbound()
                    // TODO do handshake
                    close()
                }
                else -> TODO()
            }
        } while (sendAppBuffer.hasRemaining())
    }

    private fun runDelegatedTasks() {
        if (engine.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            do {
                val task = engine.delegatedTask
                task?.run()
            } while (task != null)
            if (engine.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                throw Exception("handshake shouldn't need additional tasks")
            }
        }
    }

    override fun read(): UByteArray? { // TODO handle handshake
        try {
            super.readToBuffer()

            while (receiveBuffer.hasRemaining()) {
                receiveAppBuffer.clear()
                val result = engine.unwrap(receiveBuffer, receiveAppBuffer) // TODO handle SSLException
                when (result.status) {
                    SSLEngineResult.Status.OK -> {
                        if (engine.handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED && engine.handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                            runDelegatedTasks()
                            while (engine.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                                send(UByteArray(0))
                            }
                        } else {
                            receiveAppBuffer.flip()
                            return receiveAppBuffer.toUByteArray()
                        }
                    }
                    SSLEngineResult.Status.BUFFER_UNDERFLOW -> TODO()
                    SSLEngineResult.Status.BUFFER_OVERFLOW -> TODO()
                    SSLEngineResult.Status.CLOSED -> {
                        engine.closeOutbound()
                        // TODO do handshake
                        close()
                    }
                    else -> TODO()
                }
            }
        } catch (e: SocketClosedException) {
            engine.closeInbound()
            throw e
        }
        return null
    }
}
