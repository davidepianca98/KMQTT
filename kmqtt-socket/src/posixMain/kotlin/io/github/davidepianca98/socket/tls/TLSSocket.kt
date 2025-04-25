package io.github.davidepianca98.socket.tls

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import io.github.davidepianca98.socket.IOException
import io.github.davidepianca98.socket.SocketClosedException
import io.github.davidepianca98.socket.streams.ByteArrayOutputStream
import io.github.davidepianca98.socket.tcp.Socket

public actual open class TLSSocket(
    socket: Int,
    protected val engine: TLSEngine,
    writeRequest: MutableList<Int>?,
    buffer: ByteArray
) : Socket(socket, writeRequest, buffer) {

    private val buf = ByteArray(16 * 1024)
    private val sendBuf = ByteArray(16 * 1024)

    private val pendingSend = mutableListOf<UByteArray>()

    private fun socketSend() {
        sendBuf.usePinned { pinnedSendBuf ->
            do {
                val number = engine.bioRead(pinnedSendBuf.addressOf(0), sendBuf.size)
                if (number > 0) {
                    super.send(pinnedSendBuf.get().toUByteArray().copyOfRange(0, number))
                } else if (engine.bioShouldRetry) {
                    close()
                    throw IOException("OpenSSL shouldn't retry to read BIO")
                }
            } while (number > 0)
        }
    }

    override fun send(data: UByteArray) {
        if (!send0(data)) {
            pendingSend.add(data.copyOf())
        }
    }

    private fun send0(data: UByteArray): Boolean {
        var length = data.size
        var index = 0
        data.toByteArray().usePinned { pinnedBuf ->
            while (length > 0) {
                val result = engine.write(pinnedBuf.addressOf(index), length)
                socketSend()
                if (result > 0) {
                    length -= result
                    index += result
                } else {
                    when (val status = engine.getError(result)) {
                        0 -> { // OK
                            break
                        }
                        2 -> { // WANT_READ
                            if (read() == null) {
                                break
                            }
                        }
                        3 -> { // WANT_WRITE
                            throw IOException("OpenSSL want write in write")
                        }
                        6 -> { // ZERO_RETURN
                            close()
                            throw SocketClosedException()
                        }
                        else -> {
                            close()
                            throw IOException("OpenSSL error $status")
                        }
                    }
                }
            }
        }
        return length <= 0
    }

    override fun read(): UByteArray? {
        if (engine.isInitFinished) {
            val iterator = pendingSend.iterator()
            while (iterator.hasNext()) {
                val value = iterator.next()
                if (send0(value)) {
                    iterator.remove()
                }
            }
        }

        val data = super.read() ?: return null

        val returnData = ByteArrayOutputStream()

        var length = data.size
        var index = 0
        data.toByteArray().usePinned { pinned ->
            while (length > 0) {
                val n = engine.bioWrite(pinned.addressOf(index), length)
                if (n <= 0) {
                    close()
                    throw IOException("Failed in BIO_write")
                }

                index += n
                length -= n

                buf.usePinned { pinnedBuf ->
                    var number: Int
                    do {
                        number = engine.read(pinnedBuf.addressOf(0), buf.size)
                        socketSend()
                        if (number > 0) {
                            returnData.write(pinnedBuf.get().toUByteArray(), 0, number)
                        } else {
                            when (val status = engine.getError(number)) {
                                0 -> {} // OK
                                2 -> {} // WANT_READ
                                3 -> { // WANT_WRITE
                                    throw IOException("OpenSSL want write in read")
                                }
                                6 -> { // ZERO_RETURN
                                    close()
                                    throw SocketClosedException()
                                }
                                else -> {
                                    close()
                                    throw IOException("OpenSSL error $status")
                                }
                            }
                        }
                    } while (number > 0)
                }
            }
        }
        return returnData.toByteArray()
    }

    override fun close() {
        engine.close()
        super.close()
    }
}