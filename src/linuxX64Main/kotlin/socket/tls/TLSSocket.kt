package socket.tls

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import openssl.*
import socket.IOException
import socket.Socket
import socket.SocketClosedException
import socket.streams.ByteArrayOutputStream

actual class TLSSocket(
    socket: Int,
    private val engine: OpenSSLEngine,
    writeRequest: MutableList<Int>,
    buffer: ByteArray
) : Socket(socket, writeRequest, buffer) {

    private val buf = ByteArray(4096)
    private val encryptedBuf = ByteArray(4096)

    data class OpenSSLEngine(val context: CPointer<SSL>, val readBio: CPointer<BIO>, val writeBio: CPointer<BIO>)

    private fun socketSend(data: UByteArray) {
        super.send(data)
    }

    override fun send(data: UByteArray) {
        if (SSL_is_init_finished(engine.context) == 0)
            return

        var length = data.size
        var index = 0
        data.toByteArray().usePinned { pinnedBuf ->
            while (length > 0) {
                val result = SSL_write(engine.context, pinnedBuf.addressOf(index), length)
                val status = SSL_get_error(engine.context, result)
                if (result > 0) {
                    length -= result
                    index += result

                    encryptedBuf.usePinned { pinnedEncryptedBuf ->
                        do {
                            val number = BIO_read(engine.writeBio, pinnedEncryptedBuf.addressOf(0), buf.size)
                            if (number > 0) {
                                socketSend(pinnedEncryptedBuf.get().toUByteArray().copyOfRange(0, number))
                            } else if (BIO_test_flags(engine.writeBio, BIO_FLAGS_SHOULD_RETRY) == 0) {
                                close()
                                throw IOException()
                            }
                        } while (number > 0)
                    }
                } else if (status == SSL_ERROR_ZERO_RETURN || status == SSL_ERROR_SYSCALL) {
                    close()
                    throw IOException()
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
                val n = BIO_write(engine.readBio, pinned.addressOf(index), length)
                if (n <= 0) {
                    close()
                    throw IOException("Failed in BIO_write")
                }

                index += n
                length -= n
                if (SSL_is_init_finished(engine.context) == 0) {
                    val acceptResult = SSL_accept(engine.context)
                    when (SSL_get_error(engine.context, acceptResult)) {
                        SSL_ERROR_WANT_READ -> {
                            buf.usePinned { pinnedBuf ->
                                do {
                                    val number = BIO_read(engine.writeBio, pinnedBuf.addressOf(0), buf.size)
                                    if (number > 0) {
                                        socketSend(pinnedBuf.get().toUByteArray().copyOfRange(0, number))
                                    } else if (BIO_test_flags(engine.writeBio, BIO_FLAGS_SHOULD_RETRY) == 0) {
                                        close()
                                        throw IOException()
                                    }
                                } while (number > 0)
                            }
                        }
                        SSL_ERROR_ZERO_RETURN, SSL_ERROR_SYSCALL -> {
                            close()
                            throw IOException()
                        }
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
                        number = SSL_read(engine.context, pinnedBuf.addressOf(0), buf.size)
                        if (number > 0) {
                            returnData.write(pinnedBuf.get().toUByteArray(), 0, number)
                        }
                    } while (number > 0)

                    when (SSL_get_error(engine.context, number)) {
                        SSL_ERROR_WANT_READ -> {
                            do {
                                val result = BIO_read(engine.writeBio, pinnedBuf.addressOf(0), buf.size)
                                if (result > 0) {
                                    socketSend(pinnedBuf.get().toUByteArray().copyOfRange(0, result))
                                } else if (BIO_test_flags(engine.writeBio, BIO_FLAGS_SHOULD_RETRY) == 0) {
                                    close()
                                    throw IOException()
                                }
                            } while (result > 0)
                        }
                        SSL_ERROR_ZERO_RETURN, SSL_ERROR_SYSCALL -> {
                            close()
                            throw IOException()
                        }
                    }
                }
            }
        }
        return returnData.toByteArray()
    }

    override fun close() {
        super.close()
        SSL_free(engine.context)
    }
}
