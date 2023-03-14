package socket.tls

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import socket.IOException
import socket.SocketClosedException
import socket.streams.ByteArrayOutputStream
import socket.tcp.Socket

actual class TLSSocket(
    socket: Int,
    private val engine: TLSEngine,
    writeRequest: MutableList<Int>,
    buffer: ByteArray
) : Socket(socket, writeRequest, buffer) {

    private val buf = ByteArray(4096)
    private val encryptedBuf = ByteArray(4096)

    private fun socketSend(data: UByteArray) {
        super.send(data)
    }

    override fun send(data: UByteArray) {
        if (!engine.isInitFinished)
            return

        var length = data.size
        var index = 0
        data.toByteArray().usePinned { pinnedBuf ->
            while (length > 0) {
                val result = engine.write(pinnedBuf.addressOf(index), length)
                val status = engine.getError(result)
                if (result > 0) {
                    length -= result
                    index += result

                    encryptedBuf.usePinned { pinnedEncryptedBuf ->
                        do {
                            val number = engine.bioRead(pinnedEncryptedBuf.addressOf(0), buf.size)
                            if (number > 0) {
                                socketSend(pinnedEncryptedBuf.get().toUByteArray().copyOfRange(0, number))
                            } else if (engine.bioShouldRetry) {
                                close()
                                throw IOException("OpenSSL shouldn't retry to read BIO")
                            }
                        } while (number > 0)
                    }
                } else if (status == TLSError.ERROR) {
                    close()
                    throw IOException("OpenSSL error $status")
                } else if (result == 0) {
                    break
                }
            }
        }
    }

    override fun read(): UByteArray? {
        val data = super.read() ?: return null

        val returnData = ByteArrayOutputStream()

        var length = data.size
        var index = 0
        data.toByteArray().usePinned { pinned ->
            while (length > 0) {
                val n = engine.write(pinned.addressOf(index), length)
                if (n <= 0) {
                    close()
                    throw IOException("Failed in BIO_write")
                }

                index += n
                length -= n
                if (!engine.isInitFinished) {
                    val acceptResult = engine.accept()
                    when (val status = engine.getError(acceptResult)) {
                        TLSError.WANT_READ -> {
                            buf.usePinned { pinnedBuf ->
                                do {
                                    val number = engine.bioRead(pinnedBuf.addressOf(0), buf.size)
                                    if (number > 0) {
                                        socketSend(pinnedBuf.get().toUByteArray().copyOfRange(0, number))
                                    } else if (engine.bioShouldRetry) {
                                        close()
                                        throw IOException("OpenSSL shouldn't retry to read BIO")
                                    }
                                } while (number > 0)
                            }
                        }
                        TLSError.ERROR -> {
                            close()
                            throw IOException("OpenSSL error $status")
                        }
                        TLSError.OK -> {}
                    }

                    if (acceptResult < 0) {
                        return null
                    } else if (acceptResult == 0) {
                        close()
                        throw SocketClosedException()
                    }
                }

                buf.usePinned { pinnedBuf ->
                    var number: Int
                    do {
                        number = engine.read(pinnedBuf.addressOf(0), buf.size)
                        if (number > 0) {
                            returnData.write(pinnedBuf.get().toUByteArray(), 0, number)
                        }
                    } while (number > 0)

                    when (val status = engine.getError(number)) {
                        TLSError.WANT_READ -> {
                            do {
                                val result = engine.bioRead(pinnedBuf.addressOf(0), buf.size)
                                if (result > 0) {
                                    socketSend(pinnedBuf.get().toUByteArray().copyOfRange(0, result))
                                } else if (engine.bioShouldRetry) {
                                    close()
                                    throw IOException("OpenSSL shouldn't retry to read BIO")
                                }
                            } while (result > 0)
                        }
                        TLSError.ERROR -> {
                            close()
                            throw IOException("OpenSSL error $status")
                        }
                        TLSError.OK -> {}
                    }
                }
            }
        }
        return returnData.toByteArray()
    }

    override fun close() {
        super.close()
        engine.close()
    }
}
